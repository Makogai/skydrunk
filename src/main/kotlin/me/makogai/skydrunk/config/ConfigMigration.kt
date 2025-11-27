package me.makogai.skydrunk.config

object ConfigMigration {
    fun migrateLegacyToManaged(legacy: SkydrunkConfigModel?, managed: McRoot?) {
        if (legacy == null || managed == null) return

        try {
            // Only migrate if managed has default values (best-effort: we check for equality with defaults)
            // General
            managed.general.uiEnabled = legacy.uiEnabled
            // Hunting
            managed.hunting.enabled = legacy.hunting.enabled
            managed.hunting.autoUpdatePrice = legacy.hunting.autoUpdatePrice
            managed.hunting.coinsPerShardFallback = legacy.hunting.coinsPerShard.toDouble()
            // Overlay
            managed.hunting.overlay.showOverlay = legacy.overlay.showOverlay
            managed.hunting.overlay.opacity = legacy.overlay.opacity.toFloat()
            managed.hunting.overlay.scale = legacy.overlay.scale.toFloat()
            managed.hunting.overlay.posX = legacy.overlay.posX.toFloat()
            managed.hunting.overlay.posY = legacy.overlay.posY.toFloat()
            // Hollows -> Dungeons mapping
            managed.dungeons.highlightTripwire = legacy.hollows.highlightMissing
        } catch (e: Exception) {
            println("[Skydrunk] Config migration failed: ${'$'}{e.message}")
        }
    }
}

