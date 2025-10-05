package me.makogai.skydrunk.bazaar

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.config.PriceSource
import me.makogai.skydrunk.hud.ShardSession
import me.makogai.skydrunk.util.Debug
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

object BazaarPriceFetcher {

    data class Snapshot(val instaSell: Long, val sellOrder: Long, val atMs: Long)

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    /** Built-in name → Bazaar-ID overrides (users can still override with /sd price set). */
    private val DEFAULT_OVERRIDES = mapOf(
        "Glacite Walker" to "SHARD_GLACITE_WALKER",
        "Stridersurfer"  to "SHARD_STRIDER_SURFER",
        // add more here as needed
    )

    private val cache = ConcurrentHashMap<String, Snapshot>()
    private val trend = ConcurrentHashMap<String, Int>() // -1 down, 0 flat, 1 up (for chosen source)
    @Volatile var lastError: String? = null; private set

    private var tick = 0

    fun start() {
        // Refresh the currently tracked shard every ~minute (and compute trend)
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            if (++tick % (20 * 60) != 0) return@EndTick
            ShardSession.currentNameOrNull()?.let { shard ->
                refreshNowFor(shard, announceInChat = false)
            }
        })
    }

    /** “Glacite Walker” -> “SHARD_GLACITE_WALKER” */
    fun guessIdFromShardName(name: String): String =
        "SHARD_" + name.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')

    /** Resolve with user overrides → defaults → guessed id. */
    fun resolveIdFor(name: String): String {
        val user = McManaged.data().priceOverrides[name]
        return user ?: DEFAULT_OVERRIDES[name] ?: guessIdFromShardName(name)
    }

    /** Peek cached snapshot without network. */
    fun peek(id: String): Snapshot? = cache[id]

    /** For HUD arrows. */
    fun trendFor(id: String): Int = trend[id] ?: 0

    /**
     * Re-fetch price for this shard, update coinsPerShard based on user’s priceSource,
     * compute trend, and (optionally) announce result in chat.
     */
    fun refreshNowFor(shardName: String, announceInChat: Boolean = true): Boolean {
        val cfg = McManaged.data()
        val id = resolveIdFor(shardName)

        Debug.info("BZ: checking §f$shardName§7 (id §8$id§7)…")
        val before = cache[id]
        val snap = get(id)
        if (snap == null) {
            lastError?.let { Debug.warn("BZ: $it") }
            Debug.warn("BZ: no price for §f$shardName§7 (id §8$id§7). Try §b/sd price id $id§7 or set an override.")
            return false
        }

        // Decide which price the user is using right now
        val chosenNow = when (cfg.hunting.priceSource) {
            PriceSource.BAZAAR_INSTA_SELL -> snap.instaSell   // what you get instantly
            PriceSource.BAZAAR_SELL_ORDER -> snap.sellOrder   // what buyers pay (≈ your sell order)
        }
        val oldChosen = before?.let {
            when (cfg.hunting.priceSource) {
                PriceSource.BAZAAR_INSTA_SELL -> it.instaSell
                PriceSource.BAZAAR_SELL_ORDER -> it.sellOrder
            }
        } ?: chosenNow

        // Trend
        trend[id] = when {
            chosenNow > oldChosen -> 1
            chosenNow < oldChosen -> -1
            else -> 0
        }

        // Update per-shard price used by session (only when it truly changed)
        val newPrice = chosenNow.toDouble()
        val old = cfg.hunting.coinsPerShard
        if (old != newPrice) {
            cfg.hunting.coinsPerShard = newPrice
            ShardSession.onPriceRefresh()
        }

        if (announceInChat) {
            val arrow = when (trend[id]) { 1 -> "§a▲"; -1 -> "§c▼"; else -> "§7■" }
            Debug.info(
                "BZ: §f$shardName§7 → §aInsta-Sell §f%,d§7 | §eSell-Order §f%,d §8(using: %,d) %s"
                    .format(snap.instaSell, snap.sellOrder, newPrice.toLong(), arrow)
            )
        }
        return true
    }

    /** Get snapshot (≤60s cache) using public v1 quick_status. */
    fun get(id: String): Snapshot? {
        val now = System.currentTimeMillis()
        cache[id]?.let { if (now - it.atMs <= 60_000) return it }

        return getV1QuickStatus(id)?.also {
            cache[id] = it
            lastError = null
        }
    }

    /** v1 (public): quick_status approximation. No API key needed. */
    private fun getV1QuickStatus(id: String): Snapshot? {
        return try {
            val req = HttpRequest.newBuilder(URI.create("https://api.hypixel.net/skyblock/bazaar"))
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build()

            val body = http.send(req, HttpResponse.BodyHandlers.ofString()).body()
            val root = JsonParser.parseString(body).asJsonObject
            val products = root.getAsJsonObject("products") ?: return null
            val product: JsonObject = products.getAsJsonObject(id) ?: return null
            val quick = product.getAsJsonObject("quick_status") ?: return null

            val instaSell = quick.get("sellPrice").asDouble.roundToLong() // you get if insta-sell
            val instaBuy  = quick.get("buyPrice").asDouble.roundToLong()  // buyer insta-buy (≈ your sell order)

            Snapshot(
                instaSell = instaSell,
                sellOrder = instaBuy,
                atMs = System.currentTimeMillis()
            )
        } catch (t: Throwable) {
            lastError = "v1: ${t.javaClass.simpleName}: ${t.message}"
            null
        }
    }
}
