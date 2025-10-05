package me.makogai.skydrunk.config

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object McManaged {
    lateinit var managed: ManagedConfig<McRoot>
        private set

    fun init() {
        val cfgDir = FabricLoader.getInstance().configDir.toFile()
        val file = File(cfgDir, "skydrunk-moul.json")
        managed = ManagedConfig.create(file, McRoot::class.java) { /* config builder tweaks if needed */ }
        managed.injectIntoInstance()
    }

    fun open() {
        managed.openConfigGui()
    }

    fun data(): McRoot = managed.instance
}
