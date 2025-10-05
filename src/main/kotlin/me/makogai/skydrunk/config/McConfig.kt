package me.makogai.skydrunk.config

import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText

class GeneralCat : Config() {
    @ConfigOption(name = "Enable Skydrunk", desc = "Master toggle for all Skydrunk features.")
    @ConfigEditorBoolean
    var uiEnabled: Boolean = true


}

class HuntingCat : Config() {
    @ConfigOption(name = "Enable Shard Tracking", desc = "Track shard drops and rates.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @ConfigOption(name = "Track Glacite Walkers", desc = "Count Glacite Walker shard drops.")
    @ConfigEditorBoolean
    var trackGlacite: Boolean = true

    @ConfigOption(name = "Auto-reset on /warp", desc = "Reset session when you warp.")
    @ConfigEditorBoolean
    var autoResetOnWarp: Boolean = false

    @ConfigOption(name = "Price Source", desc = "SELL = what you earn, BUY = what you pay.")
    @ConfigEditorDropdown(values = ["BAZAAR_INSTA_SELL", "BAZAAR_SELL_ORDER"])
    var priceSource: PriceSource = PriceSource.BAZAAR_INSTA_SELL

    @ConfigOption(name = "Coins per shard (fallback)", desc = "Used if Bazaar fetch fails or auto-update is off.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 5_000_000f, minStep = 1000f)
    var coinsPerShard: Double = 150_000.0

    @ConfigOption(name = "Auto-update price from Bazaar", desc = "Refresh price every minute and apply it.")
    @ConfigEditorBoolean
    var autoUpdatePrice: Boolean = true

}

class OverlayCat : Config() {
    @ConfigOption(name = "Show Overlay", desc = "Toggle HUD box visibility.")
    @ConfigEditorBoolean
    var showOverlay: Boolean = true

    @ConfigOption(name = "Opacity", desc = "0.10 - 1.00")
    @ConfigEditorSlider(minValue = 0.10f, maxValue = 1.00f, minStep = 0.05f)
    var opacity: Float = 0.90f

    @ConfigOption(name = "Scale", desc = "0.5 - 2.0")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.0f, minStep = 0.05f)
    var scale: Float = 1.0f

    @ConfigOption(name = "Position X", desc = "HUD X")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10000f, minStep = 1f)
    var posX: Float = 16f

    @ConfigOption(name = "Position Y", desc = "HUD Y")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10000f, minStep = 1f)
    var posY: Float = 16f
}

class HollowsCat : Config() {
    @ConfigOption(name = "Show owned crystals", desc = "Show the crystals you own.")
    @ConfigEditorBoolean
    var showOwned: Boolean = true

    @ConfigOption(name = "Highlight missing", desc = "Highlight ones you still need.")
    @ConfigEditorBoolean
    var highlightMissing: Boolean = true
}

class DungeonsCat : Config() {
    @ConfigOption(
        name = "Highlight Tripwire",
        desc = "Render a neon box around tripwire strings through walls."
    )
    @ConfigEditorBoolean
    var highlightTripwire: Boolean = true

    @ConfigOption(
        name = "Tripwire Range",
        desc = "Search radius for tripwire strings (blocks)."
    )
    @ConfigEditorSlider(minValue = 4f, maxValue = 64f, minStep = 1f)
    var tripwireRange: Float = 20f
}

class MainCat : Config() {

    @ConfigOption(
        name = "Skydrunk — Status",
        desc = "Read-only information panel"
    )
    @ConfigEditorInfoText(infoTitle = "Welcome to Skydrunk")
    var info: String =
        """
        ✅ Currently functional: Shard Tracking (Hunting > Shard Tracking)

        • Auto price updates, per-session stats, overlay & tracker.
        • More features are in development. 
        • If something breaks after updates, try toggling 'Enable Skydrunk' in General.

        Tip: Use Overlay to move/scale the tracker HUD. 
        Tip: Use /sd price use-sell/use-buy to toggle between insta sell and sell order 
        """.trimIndent()
}


/* --------- ROOT: categories live on fields here --------- */

class McRoot : Config() {
    @ConfigOption(name = "Bazaar ID Overrides", desc = "Map shard name to custom bazaar id")
    val priceOverrides: MutableMap<String, String> = linkedMapOf()

    @Category(name = "Home", desc = "Welcome & project status.")
    val home = MainCat() // <-- NEW: first so it appears as the main screen

    @Category(name = "General", desc = "Global settings.")
    val general = GeneralCat()

    @Category(name = "Hunting Shards", desc = "Shard tracking and pricing.")
    val hunting = HuntingCat()

    @Category(name = "Overlay", desc = "HUD position, scale, opacity.")
    val overlay = OverlayCat()

    @Category(name = "Crystal Hollows", desc = "QoL for hollows.")
    val hollows = HollowsCat()

    @Category(name = "Dungeons", desc = "Dungeon QoL features")
    val dungeons = DungeonsCat()
}



