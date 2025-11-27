package me.makogai.skydrunk.detect

import me.makogai.skydrunk.bazaar.BazaarPriceFetcher
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.config.PriceSource
import me.makogai.skydrunk.hud.ShardSession
import me.makogai.skydrunk.util.Debug
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.text.Text
import kotlin.math.roundToLong

object ChatDropListener {
    private fun clean(raw: String): String =
        raw.replace(Regex("§."), "").replace(Regex("\\s+"), " ").trim()

    // You caught x3 <Name> Shards!
    private val ownDrop = Regex("""(?i)\bYou caught (?:x)?(\d{1,3}(?:,\d{3})*) ([A-Za-z0-9 '()-]+?) Shards!?""")
    // LOOT SHARE You received 2 <Name> Shards ...
    private val lootShare = Regex("""(?i)\bLOOT SHARE\b.*?\bYou received (\d{1,3}(?:,\d{3})*) ([A-Za-z0-9 '()-]+?) Shards\b""")

    fun init() {
        ClientReceiveMessageEvents.GAME.register(ClientReceiveMessageEvents.Game { message: Text, _ ->
            val cfg = McManaged.data()
            if (!cfg.hunting.enabled) return@Game

            val s = clean(message.string)
            var qty: Int? = null
            var name: String? = null

            lootShare.find(s)?.let {
                qty = it.groupValues[1].replace(",", "").toIntOrNull()
                name = it.groupValues[2].trim()
            }
            if (qty == null || name == null) {
                ownDrop.find(s)?.let {
                    qty = it.groupValues[1].replace(",", "").toIntOrNull()
                    name = it.groupValues[2].trim()
                }
            }
            val count = qty ?: return@Game
            val shardName = name ?: return@Game
            if (count <= 0) return@Game

            // Start/continue session and add count
            ShardSession.touch(shardName)
            ShardSession.addShards(count)

            // Immediately refresh price for THIS shard
            val updated = BazaarPriceFetcher.refreshNowFor(shardName, announceInChat = true)
            if (!updated) {
                val guess = BazaarPriceFetcher.guessIdFromShardName(shardName)
                Debug.warn("Price not found for §f$shardName§7 (try §b/sd price id $guess§7 or set override).")
            } else {
                // ensure coinsPerShard uses the chosen source right now
                val id = McManaged.data().priceOverrides[shardName] ?: BazaarPriceFetcher.guessIdFromShardName(shardName)
                val snap = BazaarPriceFetcher.get(id)
                if (snap != null) {
                    val cfgNow = McManaged.data()
                    val chosenPrice = when (cfgNow.hunting.priceSource) {
                        PriceSource.BAZAAR_INSTA_SELL -> snap.instaSell.toDouble()
                        PriceSource.BAZAAR_SELL_ORDER -> snap.sellOrder.toDouble()
                    }
                    cfgNow.hunting.coinsPerShard = chosenPrice
                    // MILESTONE: accumulate coins (persist across sessions)
                    try {
                        val ms = cfgNow.hunting.milestone
                        if (ms.enabled && ms.targetAmount > 0.0) {
                            // track both coins and shards
                            ms.accumulated = (ms.accumulated + chosenPrice * count).coerceAtLeast(0.0)
                            ms.shardsAccumulated = (ms.shardsAccumulated + count).coerceAtLeast(0L)
                            // persist immediately
                            try { me.makogai.skydrunk.config.McManaged.save() } catch (_: Throwable) {}

                            // compute progress for sell order and insta-sell using shardsAccumulated
                            val insta = snap.instaSell.toLong()
                            val order = snap.sellOrder.toLong()
                            val targetCoins = ms.targetAmount.roundToLong().coerceAtLeast(1L)
                            val progOrder = (ms.shardsAccumulated * order).toDouble() / targetCoins.toDouble()
                            val progInsta = (ms.shardsAccumulated * insta).toDouble() / targetCoins.toDouble()

                            val mc = net.minecraft.client.MinecraftClient.getInstance()
                            // Trigger notification screens once per milestone type
                            if (progOrder >= 1.0 && !ms.notifiedSellOrder) {
                                ms.notifiedSellOrder = true
                                try { me.makogai.skydrunk.config.McManaged.save() } catch (_: Throwable) {}
                                try {
                                    mc.execute { mc.setScreen(me.makogai.skydrunk.hud.MilestoneReachedScreen("Sell-Order reached", "Sell-order milestone reached!", true)) }
                                } catch (_: Throwable) {}
                            }
                            if (progInsta >= 1.0 && !ms.notifiedInstaSell) {
                                ms.notifiedInstaSell = true
                                try { me.makogai.skydrunk.config.McManaged.save() } catch (_: Throwable) {}
                                try {
                                    mc.execute { mc.setScreen(me.makogai.skydrunk.hud.MilestoneReachedScreen("Insta-Sell reached", "Insta-sell milestone reached!", false)) }
                                } catch (_: Throwable) {}
                            }
                        }
                    } catch (e: Exception) {
                        Debug.warn("Milestone accumulation failed: ${'$'}{e.message}")
                    }

                    ShardSession.onPriceRefresh()
                }
            }
        })
    }
}
