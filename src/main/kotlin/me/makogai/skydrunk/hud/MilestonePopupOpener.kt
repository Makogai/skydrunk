package me.makogai.skydrunk.hud

import net.minecraft.client.MinecraftClient

object MilestonePopupOpener {
    @Volatile private var attemptsLeft = 0

    /** Request opening the popup; will attempt to set the screen for a few client ticks. */
    fun request() {
        attemptsLeft = 5
    }

    /** Called each client tick on the client thread; attempts to open the popup while attemptsLeft > 0. */
    fun tick(client: MinecraftClient) {
        if (attemptsLeft <= 0) return
        attemptsLeft--
        try {
            println("[Skydrunk] MilestonePopupOpener.tick(): trying to setScreen (attemptsLeft=${attemptsLeft})")
            client.setScreen(MilestonePopup())
        } catch (t: Throwable) {
            println("[Skydrunk] MilestonePopupOpener.tick() failed: ${t.message}")
        }
    }
}
