package me.makogai.skydrunk.client

import me.makogai.skydrunk.config.McManaged
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object ConfigOpener {
    @Volatile private var countdown = -1

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (countdown >= 0) {
                countdown--
                if (countdown == 0) {
                    try {
                        McManaged.open()
                    } catch (t: Throwable) {
                        client.inGameHud.chatHud.addMessage(
                            net.minecraft.text.Text.of("§c[Skydrunk] Failed to open: ${t.message}")
                        )
                    } finally {
                        countdown = -1
                    }
                }
            }
        })
    }

    /** Schedule opening after N ticks (2 is safe). */
    fun requestOpen(ticks: Int = 2) { countdown = ticks.coerceAtLeast(1) }
}
