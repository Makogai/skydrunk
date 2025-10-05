package me.makogai.skydrunk.ui

import me.makogai.skydrunk.hud.ShardSession
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

object InventoryResetButton {
    fun init() {
        ScreenEvents.AFTER_INIT.register(ScreenEvents.AfterInit { client: MinecraftClient, screen: Screen, w: Int, h: Int ->
            if (screen is InventoryScreen) {
                // Place the button in the top-right of the WINDOW (not the inventory texture),
                // so we don't rely on protected fields of HandledScreen.
                val btnWidth = 70
                val btnHeight = 14
                val margin = 10
                val x = w - btnWidth - margin
                val y = margin

                val btn = ButtonWidget.builder(Text.of("§cReset")) {
                    ShardSession.reset()
                    client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Session reset."))
                }.size(btnWidth, btnHeight).position(x, y).build()

                // Add via Fabric helper, avoids protected addDrawableChild()
                Screens.getButtons(screen).add(btn)
            }
        })
    }
}
