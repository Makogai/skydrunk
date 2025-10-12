package me.makogai.skydrunk.bazaar

import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
import me.makogai.skydrunk.config.McManaged

object BazaarPriceFetcher {

    data class Snapshot(val instaSell: Long, val sellOrder: Long, val atMs: Long)

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    /** Built-in name → Bazaar-ID overrides (users can still override in config map). */
    private val DEFAULT_OVERRIDES = mapOf(
        "Glacite Walker" to "SHARD_GLACITE_WALKER",
        "Stridersurfer"  to "SHARD_STRIDER_SURFER",
    )

    private val cache = ConcurrentHashMap<String, Snapshot>()
    private val trendSellMap = ConcurrentHashMap<String, Int>() // -1 down, 0 flat, 1 up (instaSell)
    private val trendBuyMap  = ConcurrentHashMap<String, Int>() // ... for sellOrder
    @Volatile var lastError: String? = null; private set

    private var tick = 0

    fun start() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            if (++tick % (20 * 60) != 0) return@EndTick // ~1 minute
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

    /** Trends for arrows in HUD. */
    fun trendSell(id: String): Int = trendSellMap[id] ?: 0
    fun trendBuy(id: String): Int = trendBuyMap[id] ?: 0

    /**
     * Re-fetch price for this shard, compute trends for both prices, and optionally announce.
     */
    fun refreshNowFor(shardName: String, announceInChat: Boolean = true): Boolean {
        val id = resolveIdFor(shardName)
        Debug.info("BZ: checking §f$shardName§7 (id §8$id§7)…")

        val before = cache[id]
        val snap = get(id)
        if (snap == null) {
            lastError?.let { Debug.warn("BZ: $it") }
            Debug.warn("BZ: no price for §f$shardName§7 (id §8$id§7).")
            return false
        }

        // Per-price trends
        val prevSell = before?.instaSell ?: snap.instaSell
        val prevBuy  = before?.sellOrder ?: snap.sellOrder
        trendSellMap[id] = when {
            snap.instaSell > prevSell -> 1
            snap.instaSell < prevSell -> -1
            else -> 0
        }
        trendBuyMap[id] = when {
            snap.sellOrder > prevBuy -> 1
            snap.sellOrder < prevBuy -> -1
            else -> 0
        }

        if (announceInChat) {
            val aSell = when (trendSellMap[id]) { 1 -> "§a▲"; -1 -> "§c▼"; else -> "§7■" }
            val aBuy  = when (trendBuyMap[id])  { 1 -> "§a▲"; -1 -> "§c▼"; else -> "§7■" }
            Debug.info(
                "BZ: §f$shardName§7 → §aIS §f%,d %s §7| §eSO §f%,d %s"
                    .format(snap.instaSell, aSell, snap.sellOrder, aBuy)
            )
        }
        return true
    }

    /** Get snapshot (≤60s cache) using public v1 endpoint. */
    fun get(id: String): Snapshot? {
        val now = System.currentTimeMillis()
        cache[id]?.let { if (now - it.atMs <= 60_000) return it }

        return getV1QuickStatus(id)?.also {
            cache[id] = it
            lastError = null
        }
    }

    /** v1 public /skyblock/bazaar (no API key). */
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
