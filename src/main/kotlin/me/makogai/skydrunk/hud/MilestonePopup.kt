package me.makogai.skydrunk.hud

import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.util.Debug
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import kotlin.math.roundToLong

class MilestonePopup : Screen(Text.of("Skydrunk — Milestone")) {
    private var textField: TextFieldWidget? = null
    private var renderedOnce = false

    override fun init() {
        super.init()
        val mc = MinecraftClient.getInstance()
        val w = this.width
        val h = this.height

        // debug: announce initialization
        try {
            mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] MilestonePopup.init() called"))
        } catch (_: Exception) {}
        Debug.info("MilestonePopup.init() called")
        println("[Skydrunk] MilestonePopup.init() called")

        // additional diagnostics
        try {
            val cur = mc.currentScreen
            try { mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] currentScreen: ${cur?.javaClass?.name ?: "<null>"}")) } catch (_: Exception) {}
            println("[Skydrunk] currentScreen: ${cur?.javaClass?.name ?: "<null>"}")
            try { mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] screen equals this? ${cur === this}")) } catch (_: Exception) {}
            println("[Skydrunk] screen equals this? ${cur === this}")
            try { mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] width=${w}, height=${h}")) } catch (_: Exception) {}
            println("[Skydrunk] width=${w}, height=${h}")
        } catch (_: Exception) {}

        val fieldX = w / 2 - 100
        val fieldY = h / 2 - 10
        val fieldW = 200
        val fieldH = 20

        textField = TextFieldWidget(mc.textRenderer, fieldX, fieldY, fieldW, fieldH, Text.of("Target"))
        val cfg = McManaged.data()
        // maintain an input value (in coins) and keep textField in sync
        var inputValue = if (cfg.hunting.milestone.targetAmount > 0.0) cfg.hunting.milestone.targetAmount.roundToLong() else 0L
        textField?.text = if (inputValue > 0L) inputValue.toString() else ""
        // ensure the widget is both selectable (focusable) and drawable (visible)
        this.addSelectableChild(textField)
        this.addDrawableChild(textField)
        // set initial focus so the caret is visible
        this.setInitialFocus(textField)

        // quick preset buttons for adjusting the numeric value
        val presets = listOf(-1_000_000L, -100_000L, -10_000L, 10_000L, 100_000L, 1_000_000L)
        val btnW = 40
        val startX = w / 2 - (presets.size * (btnW + 4)) / 2
        var bx = startX
        val by = h / 2 + 44
        for (delta in presets) {
            val label = if (delta < 0) "${delta/1000}k" else "+${delta/1000}k"
            this.addDrawableChild(ButtonWidget.builder(Text.of(label)) {
                // update inputValue and textfield
                inputValue = (inputValue + delta).coerceAtLeast(0L)
                textField?.text = inputValue.toString()
            }.dimensions(bx, by, btnW, 20).build())
            bx += btnW + 4
        }

        // a clear/backspace row
        val bx2 = w / 2 - 100
        this.addDrawableChild(ButtonWidget.builder(Text.of("Clear")) {
            inputValue = 0L; textField?.text = ""
        }.dimensions(bx2, h / 2 + 68, 60, 20).build())
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back")) {
            val s = textField?.text ?: ""
            if (s.isNotEmpty()) {
                val ns = s.dropLast(1)
                textField?.text = ns
                inputValue = ns.replace("[, ]".toRegex(), "").toLongOrNull() ?: 0L
            }
        }.dimensions(bx2 + 64, h / 2 + 68, 60, 20).build())

        this.addDrawableChild(ButtonWidget.builder(Text.of("Save")) { btn ->
            val t = textField?.text ?: ""
            val parsed = t.replace("[, ]".toRegex(), "").toLongOrNull()
            if (parsed != null && parsed > 0L) {
                val cfgNow = McManaged.data()
                // starting a new milestone: reset accumulators and notified flags
                cfgNow.hunting.milestone.accumulated = 0.0
                cfgNow.hunting.milestone.shardsAccumulated = 0L
                cfgNow.hunting.milestone.notifiedSellOrder = false
                cfgNow.hunting.milestone.notifiedInstaSell = false
                cfgNow.hunting.milestone.targetAmount = parsed.toDouble()
                cfgNow.hunting.milestone.enabled = true
                // persist
                try { McManaged.save() } catch (_: Throwable) {}
                MinecraftClient.getInstance().setScreen(null)
            } else {
                // invalid - just close
                MinecraftClient.getInstance().setScreen(null)
            }
        }.dimensions(w/2 - 100, h/2 + 16, 60, 20).build())

        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset")) { _ ->
            val cfgNow = McManaged.data()
            cfgNow.hunting.milestone.accumulated = 0.0
            cfgNow.hunting.milestone.shardsAccumulated = 0L
            cfgNow.hunting.milestone.targetAmount = 0.0
            cfgNow.hunting.milestone.enabled = false
            cfgNow.hunting.milestone.notifiedSellOrder = false
            cfgNow.hunting.milestone.notifiedInstaSell = false
            try { McManaged.save() } catch (_: Throwable) {}
            MinecraftClient.getInstance().setScreen(null)
        }.dimensions(w/2 - 30, h/2 + 16, 60, 20).build())

        this.addDrawableChild(ButtonWidget.builder(Text.of("Close")) { _ ->
            MinecraftClient.getInstance().setScreen(null)
        }.dimensions(w/2 + 40, h/2 + 16, 60, 20).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // draw background
        this.renderBackground(context, mouseX, mouseY, delta)

        // draw title and label so users can see the purpose of this popup even if the text field looks subtle
        val mc = MinecraftClient.getInstance()
        val w = this.width
        val h = this.height
        val title = "§bSkydrunk — Milestone"
        val label = "Target amount (coins)"

        // Draw a box behind the text field to make it stand out
        val boxX = w / 2 - 104
        val boxY = h / 2 - 14
        val boxW = 208
        val boxH = 28
        val bgColor = (180 shl 24) or 0x202020
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, bgColor)

        context.drawCenteredTextWithShadow(mc.textRenderer, Text.of(title), w / 2, h / 2 - 30, 0xFFFFFF)
        context.drawText(mc.textRenderer, Text.of(label), w / 2 - 100, h / 2 - 28, 0xAAAAAA, false)

        if (!renderedOnce) {
            println("[Skydrunk] MilestonePopup.render() called - first frame")
            try {
                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] MilestonePopup.render() called - first frame"))
            } catch (_: Exception) {}
            renderedOnce = true
        }

        super.render(context, mouseX, mouseY, delta)
        // textField is a selectable and drawable child and will be rendered by super
    }

    companion object {
        fun open() {
            val mc = MinecraftClient.getInstance()
            try {
                mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] MilestonePopup.open() scheduling setScreen"))
            } catch (_: Exception) {}
            Debug.info("MilestonePopup.open() scheduling setScreen")
            println("[Skydrunk] MilestonePopup.open() scheduling setScreen")
            mc.execute { mc.setScreen(MilestonePopup()) }
        }
    }
}
