package me.makogai.skydrunk.hud

import me.makogai.skydrunk.config.SkydrunkConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import kotlin.math.roundToInt

class DragScreen : Screen(Text.of("Skydrunk – Drag Overlay")) {
    private var dragging = false
    private var dragDX = 0
    private var dragDY = 0

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 1.21.5 signature requires mouseX/mouseY/delta
        renderBackground(context, mouseX, mouseY, delta)

        // render HUD preview while dragging
        // We don’t need tickCounter here; just pass a stub via the HUD if you refactor, but our HUD doesn’t require it.
        ShardHud.render(context, /* tickCounter not used */ net.minecraft.client.MinecraftClient.getInstance().renderTickCounter)

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF)
        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button)
        val cfg = SkydrunkConfig.data
        val scale = cfg.overlay.scale.toFloat().coerceIn(0.5f, 2f)

        val font = textRenderer
        val pad = 4
        val lineH = 10
        val labels = listOf("Glacite Shards", "Session", "Elapsed", "Shards/hr", "Coins/hr")
        val contentWidth = labels.maxOf { font.getWidth(it) } + pad * 2 + 60
        val contentHeight = lineH * labels.size + pad * 2

        val x = (cfg.overlay.posX * scale).roundToInt()
        val y = (cfg.overlay.posY * scale).roundToInt()
        val w = (contentWidth * scale).roundToInt()
        val h = (contentHeight * scale).roundToInt()

        if (mouseX.toInt() in x..(x + w) && mouseY.toInt() in y..(y + h)) {
            dragging = true
            dragDX = mouseX.toInt() - x
            dragDY = mouseY.toInt() - y
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!dragging || button != 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        val cfg = SkydrunkConfig.data
        val scale = cfg.overlay.scale.toFloat().coerceIn(0.5f, 2f)
        val inv = 1f / scale

        val nx = ((mouseX.toInt() - dragDX).toFloat() * inv).roundToInt().coerceAtLeast(0)
        val ny = ((mouseY.toInt() - dragDY).toFloat() * inv).roundToInt().coerceAtLeast(0)
        cfg.overlay.posX = nx
        cfg.overlay.posY = ny
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (dragging && button == 0) {
            dragging = false
            SkydrunkConfig.save()
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun shouldCloseOnEsc() = true

    companion object {
        fun open() {
            MinecraftClient.getInstance().setScreen(DragScreen())
        }
    }
}
