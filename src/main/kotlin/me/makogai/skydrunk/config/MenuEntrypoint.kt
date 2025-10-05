package me.makogai.skydrunk.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class MenuEntrypoint : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory { parent: Screen? ->
            // A tiny trampoline screen that:
            // 1) Opens the MoulConfig GUI on first tick
            // 2) When you close Moul, it brings you back to Mod Menu (parent)
            object : Screen(Text.of("Skydrunk Settings")) {
                private var opened = false
                override fun tick() {
                    if (!opened) {
                        opened = true
                        McManaged.open()           // shows the MoulConfig GUI
                    } else {
                        // We got focus back after Moul closed — return to Mod Menu
                        client?.setScreen(parent)
                    }
                }
            }
        }
}
