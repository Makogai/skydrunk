package me.makogai.skydrunk.hud

import me.makogai.skydrunk.bazaar.BazaarPriceFetcher
import me.makogai.skydrunk.config.ElapsedFormat
import me.makogai.skydrunk.config.McManaged
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object ShardHud {

    private fun fmt(n: Long): String {
        val abs = kotlin.math.abs(n.toDouble())
        return when {
            abs >= 1_000_000_000 -> String.format("%.1fb", n / 1_000_000_000.0)
            abs >= 1_000_000     -> String.format("%.1fm", n / 1_000_000.0)
            abs >= 1_000         -> String.format("%.1fk", n / 1_000.0)
            else -> "%,d".format(n)
        }
    }

    private fun chooseIconFor(name: String): ItemStack? {
        val n = name.lowercase()
        return when {
            "glacite" in n  -> ItemStack(Items.PRISMARINE_SHARD)
            "strider" in n  -> ItemStack(Items.NAUTILUS_SHELL)
            "blaze" in n    -> ItemStack(Items.BLAZE_POWDER)
            "enderman" in n -> ItemStack(Items.ENDER_PEARL)
            else -> null
        }
    }

    private fun formatElapsed(ms: Long, mode: ElapsedFormat): String {
        val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when (mode) {
            ElapsedFormat.HH_MM -> "%02d:%02d".format(h, m)
            ElapsedFormat.MIN_SEC -> when {
                h > 0 -> "%dh %dm".format(h, m)
                m > 0 -> "%dm %ds".format(m, s)
                else  -> "%ds".format(s)
            }
        }
    }

    fun render(context: DrawContext, tickCounter: RenderTickCounter) {
        val cfgRoot = McManaged.data()
        val hudCfg = cfgRoot.hunting.overlay

        val name = ShardSession.currentNameOrNull() ?: return
        if (!hudCfg.showOverlay || !cfgRoot.general.uiEnabled) return

        val mc = MinecraftClient.getInstance() ?: return
        if (mc.options.hudHidden) return

        val paused = ShardSession.isPaused()

        val scale = hudCfg.scale.coerceIn(0.5f, 2.0f)
        val x = hudCfg.posX.roundToInt()
        val y = hudCfg.posY.roundToInt()

        val id = BazaarPriceFetcher.resolveIdFor(name)
        val snap = BazaarPriceFetcher.peek(id)
        val instaSell = snap?.instaSell ?: cfgRoot.hunting.coinsPerShardFallback.roundToLong()
        val sellOrder = snap?.sellOrder ?: instaSell

        val shards = ShardSession.shardCount()
        val shp = ShardSession.shardsPerHour()

        val elapsed = ShardSession.elapsedMillis()
        val elapsedTxt = formatElapsed(elapsed, hudCfg.elapsedFormat)

        val earnInsta = (shards * instaSell).toLong()
        val earnOrder = (shards * sellOrder).toLong()
        val cphInsta = (shp * instaSell).roundToLong()
        val cphOrder = (shp * sellOrder).roundToLong()

        val tSell = BazaarPriceFetcher.trendSell(id)
        val tBuy  = BazaarPriceFetcher.trendBuy(id)
        val arrowSell = when (tSell) { 1 -> "§a▲"; -1 -> "§c▼"; else -> "§7■" }
        val arrowBuy  = when (tBuy)  { 1 -> "§a▲"; -1 -> "§c▼"; else -> "§7■" }

        val font = mc.textRenderer
        val pad = 6
        val lineH = 12
        val headerH = 16

        val headerLabel = buildString {
            append("§f")
            append(name)
            append(" §8• ")
            append(if (paused) "§cPAUSED" else "§7Active")
            append(" §8| ")
            append("§aIS §7")
            append(arrowSell)
            append(" §8/ §eSO §7")
            append(arrowBuy)
        }

        val lines = listOf(
            "Prices"    to "§aIS §f${fmt(instaSell)} §7/ §eSO §f${fmt(sellOrder)}",
            "Shards"    to "%,d".format(shards),
            "Elapsed"   to elapsedTxt + if (paused) " §c(PAUSED)" else "",
            "Shards/hr" to String.format("%.1f", shp),
            "Coins/hr"  to "§a${fmt(cphInsta)} §7/ §e${fmt(cphOrder)}",
            "Earnings"  to "§a${fmt(earnInsta)} §7/ §e${fmt(earnOrder)}"
        )

        val headerW = font.getWidth(Text.of(headerLabel))
        val maxLine = lines.maxOf {
            val s = "§7${it.first}: §f${it.second}"
            font.getWidth(Text.of(s))
        }
        val width = max(headerW, maxLine) + pad*2 + 18
        val height = headerH + pad + 1 + lines.size * lineH + pad

        context.matrices.push()
        context.matrices.scale(scale, scale, 1f)

        val alpha = (hudCfg.opacity * 255f).roundToInt().coerceIn(40, 255)
        val bgBase = if (paused) 0x0E0E0E else 0x101010
        val headerBase = if (paused) 0x2A1A1A else 0x202A38
        val bg = (alpha shl 24) or bgBase
        val headerBg = (alpha shl 24) or headerBase
        val sepColor = (alpha shl 24) or 0x303030

        context.fill(x, y, x + width, y + height, bg)
        context.fill(x, y, x + width, y + headerH, headerBg)
        context.fill(x + pad, y + headerH, x + width - pad, y + headerH + 1, sepColor)

        chooseIconFor(name)?.let { stack ->
            context.drawItem(stack, x + pad, y + (headerH - 16)/2)
        }

        context.drawText(font, Text.of(headerLabel), x + pad + 18, y + (headerH - 9)/2, 0xFFFFFF, false)

        var ty = y + headerH + pad
        for ((label, value) in lines) {
            val line = "§7$label: §f$value"
            context.drawText(font, Text.of(line), x + pad, ty, 0xFFFFFF, false)
            ty += lineH
        }

        context.matrices.pop()
    }

    /** For DragScreen: returns [x, y, width, height] of the on-screen rect (scaled). */
    fun currentBounds(): IntArray {
        val cfgRoot = McManaged.data()
        val hudCfg = cfgRoot.hunting.overlay
        val name = ShardSession.currentNameOrNull() ?: return intArrayOf(0, 0, 0, 0)

        val mc = MinecraftClient.getInstance() ?: return intArrayOf(0, 0, 0, 0)
        val font = mc.textRenderer

        val pad = 6
        val lineH = 12
        val headerH = 16

        val paused = ShardSession.isPaused()
        val id = BazaarPriceFetcher.resolveIdFor(name)
        val snap = BazaarPriceFetcher.peek(id)
        val instaSell = snap?.instaSell ?: cfgRoot.hunting.coinsPerShardFallback.roundToLong()
        val sellOrder = snap?.sellOrder ?: instaSell

        val headerLabel = buildString {
            append(name)
            append(" • ")
            append(if (paused) "PAUSED" else "Active")
            append(" | IS / SO")
        }
        val lines = listOf(
            "Prices"    to "IS ${fmt(instaSell)} / SO ${fmt(sellOrder)}",
            "Shards"    to "%,d".format(ShardSession.shardCount()),
            "Elapsed"   to "…",
            "Shards/hr" to "…",
            "Coins/hr"  to "…",
            "Earnings"  to "…"
        )
        val headerW = font.getWidth(Text.of(headerLabel))
        val maxLine = lines.maxOf {
            val s = "${it.first}: ${it.second}"
            font.getWidth(Text.of(s))
        }
        val width = max(headerW, maxLine) + pad*2 + 18
        val height = headerH + pad + 1 + lines.size * lineH + pad

        val scale = hudCfg.scale.coerceIn(0.5f, 2.0f)
        val x = hudCfg.posX.roundToInt()
        val y = hudCfg.posY.roundToInt()

        val wScaled = (width * scale).roundToInt()
        val hScaled = (height * scale).roundToInt()
        return intArrayOf(x, y, wScaled, hScaled)
    }
}
