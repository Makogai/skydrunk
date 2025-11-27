package me.makogai.skydrunk.viewmodel

import net.minecraft.client.MinecraftClient

object ViewmodelEditorOpener {
    @Volatile private var attemptsLeft = 0

    fun request() {
        attemptsLeft = 5
    }

    fun tick(client: MinecraftClient) {
        if (attemptsLeft <= 0) return
        attemptsLeft--
        try {
            println("[Skydrunk] ViewmodelEditorOpener.tick(): trying to setScreen (attemptsLeft=${attemptsLeft})")
            // Build the screen then set it on the client thread
            val screen = me.makogai.skydrunk.viewmodel.ViewmodelEditor.createScreen()
            client.setScreen(screen)
        } catch (t: Throwable) {
            println("[Skydrunk] ViewmodelEditorOpener.tick() failed: ${t.message}")
        }
    }
}
