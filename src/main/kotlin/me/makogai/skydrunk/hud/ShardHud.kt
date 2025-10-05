package me.makogai.skydrunk.hud

import me.makogai.skydrunk.bazaar.BazaarPriceFetcher
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.config.PriceSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.text.Text
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.max

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
            "glacite" in n      -> ItemStack(Items.PRISMARINE_SHARD)
            "strider" in n      -> ItemStack(Items.NAUTILUS_SHELL)
            "blaze" in n        -> ItemStack(Items.BLAZE_POWDER)
            "enderman" in n     -> ItemStack(Items.ENDER_PEARL)
            else -> null
        }
    }

    fun render(context: DrawContext, tickCounter: RenderTickCounter) {
        val cfg = McManaged.data()
        val active = me.makogai.skydrunk.hud.ShardSession.currentNameOrNull() != null
        if (!cfg.overlay.showOverlay || !cfg.general.uiEnabled || !active) return

        val shardName = ShardSession.currentNameOrNull() ?: return // show ONLY while tracking
        val mc = MinecraftClient.getInstance() ?: return
        if (mc.options.hudHidden) return

        val scale = cfg.overlay.scale.coerceIn(0.5f, 2.0f)
        val x = cfg.overlay.posX.roundToInt()
        val y = cfg.overlay.posY.roundToInt()

        // Prices
        val id = BazaarPriceFetcher.resolveIdFor(shardName)
        val snap = BazaarPriceFetcher.peek(id)
        val instaSell = snap?.instaSell ?: McManaged.data().hunting.coinsPerShard.roundToLong()
        val sellOrder = snap?.sellOrder ?: instaSell
        val useSrc = McManaged.data().hunting.priceSource

        // Trends
        val trend = BazaarPriceFetcher.trendFor(id)
        val trendTxt = when (trend) { 1 -> "§a▲"; -1 -> "§c▼"; else -> "§7■" }

        // Session metrics (event-driven cached)
        val shards = ShardSession.shardCount()
        val shp = ShardSession.shardsPerHour()
        val cph = ShardSession.coinsPerHour()
        val elapsedMs = ShardSession.elapsedMillis()
        val hours = (elapsedMs / 3_600_000).toInt()
        val minutes = ((elapsedMs % 3_600_000) / 60_000).toInt()
        val elapsedTxt = String.format("%02d:%02d", hours, minutes)

        // Session earnings for each source
        val earnInsta = (shards * instaSell).toLong()
        val earnOrder = (shards * sellOrder).toLong()

        // Card layout
        val font = mc.textRenderer
        val pad = 6
        val lineH = 12
        val headerH = 16
        val contentLines = listOf(
            "Price (insta / order)" to "§a${fmt(instaSell)} §7/ §e${fmt(sellOrder)}",
            "Session shards"        to "%,d".format(shards),
            "Elapsed"               to elapsedTxt,
            "Shards/hr"             to String.format("%.1f", shp),
            "Coins/hr"              to fmt(cph),
            "Earnings"              to "§a${fmt(earnInsta)} §7/ §e${fmt(earnOrder)}"
        )

        // Compute width
        val headerLabel = "§f$shardName §8• §7${if (useSrc == PriceSource.BAZAAR_INSTA_SELL) "Insta-Sell" else "Sell-Order"} $trendTxt"
        val headerW = font.getWidth(Text.of(headerLabel))
        val maxLine = contentLines.maxOf {
            val label = "§7${it.first}: §f${it.second}"
            font.getWidth(Text.of(label))
        }
        val width = max(headerW, maxLine) + pad*2 + 18 // space for icon

        val height = headerH + pad + 1 + contentLines.size * lineH + pad

        context.matrices.push()
        context.matrices.scale(scale, scale, 1f)

        // Backgrounds
        val alpha = (cfg.overlay.opacity * 255f).roundToInt().coerceIn(40, 255)
        val bg = (alpha shl 24) or 0x101010
        val headerBg = (alpha shl 24) or 0x202A38
        val sepColor = (alpha shl 24) or 0x303030

        // Card bg
        context.fill(x, y, x + width, y + height, bg)
        // Header bg
        context.fill(x, y, x + width, y + headerH, headerBg)
        // Separator
        context.fill(x + pad, y + headerH, x + width - pad, y + headerH + 1, sepColor)

        // Icon
        chooseIconFor(shardName)?.let { stack ->
            context.drawItem(stack, x + pad, y + (headerH - 16)/2)
        }

        // Header text
        context.drawText(font, Text.of(headerLabel), x + pad + 18, y + (headerH - 9)/2, 0xFFFFFF, false)

        // Body
        var ty = y + headerH + pad
        for ((label, value) in contentLines) {
            val line = "§7$label: §f$value"
            context.drawText(font, Text.of(line), x + pad, ty, 0xFFFFFF, false)
            ty += lineH
        }

        context.matrices.pop()
    }
}
