package me.makogai.skydrunk

import me.makogai.skydrunk.config.ConfigMigration
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.config.SkydrunkConfig
import net.fabricmc.api.ModInitializer

class Skydrunk : ModInitializer {            // public class, default public no-arg ctor
    override fun onInitialize() {
        println("[Skydrunk] onInitialize()")

        //  Load legacy JSON config (if present)
        SkydrunkConfig.load()

        //  Init moulconfig managed config
        McManaged.init()

        // Migrate legacy settings into managed config (best-effort)
        try {
            ConfigMigration.migrateLegacyToManaged(SkydrunkConfig.data, McManaged.data())
            // Save managed state after migration
            McManaged.save()
        } catch (e: Exception) {
            println("[Skydrunk] Migration error: ${'$'}{e.message}")
        }

        // Register shutdown hook to persist configs
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                SkydrunkConfig.save()
            } catch (e: Exception) {
                println("[Skydrunk] Failed to save legacy config: ${'$'}{e.message}")
            }

            try {
                McManaged.save()
            } catch (_: Throwable) { /* ignore */ }
        })
    }
}
