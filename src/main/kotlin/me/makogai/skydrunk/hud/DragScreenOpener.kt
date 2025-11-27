package me.makogai.skydrunk.hud

import net.minecraft.client.MinecraftClient

object DragScreenOpener {
    @Volatile private var attemptsLeft = 0

    fun request() {
        attemptsLeft = 5
    }

    fun tick(client: MinecraftClient) {
        if (attemptsLeft <= 0) return
        attemptsLeft--
        try {
            println("[Skydrunk] DragScreenOpener.tick(): trying to setScreen (attemptsLeft=${attemptsLeft})")
            client.setScreen(DragScreen())
        } catch (t: Throwable) {
            println("[Skydrunk] DragScreenOpener.tick() failed: ${t.message}")
        }
    }
}

