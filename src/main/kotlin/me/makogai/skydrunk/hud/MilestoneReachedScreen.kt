package me.makogai.skydrunk.hud

import me.makogai.skydrunk.config.McManaged
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class MilestoneReachedScreen(private val titleText: String, private val bodyText: String, private val isSellOrder: Boolean) : Screen(Text.of(titleText)) {
    override fun init() {
        super.init()
        val mc = MinecraftClient.getInstance()
        val w = this.width
        val h = this.height

        this.addDrawableChild(ButtonWidget.builder(Text.of("Start new milestone")) {
            // open the popup
            mc.setScreen(MilestonePopup())
        }.dimensions(w/2 - 110, h - 80, 100, 20).build())

        this.addDrawableChild(ButtonWidget.builder(Text.of("Dismiss")) {
            // just close
            mc.setScreen(null)
        }.dimensions(w/2 + 10, h - 80, 80, 20).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // full-screen semi-transparent overlay
        context.fill(0, 0, width, height, 0xB0000000.toInt())
        val mc = MinecraftClient.getInstance()
        context.drawCenteredTextWithShadow(mc.textRenderer, Text.of("§6$titleText"), width/2, height/2 - 20, 0xFFFFFF)
        context.drawCenteredTextWithShadow(mc.textRenderer, Text.of("§f$bodyText"), width/2, height/2 + 0, 0xFFFFFF)
        super.render(context, mouseX, mouseY, delta)
    }
}

