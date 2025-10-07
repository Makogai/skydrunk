package me.makogai.skydrunk.config

import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GeneralCat : Config() {
    @ConfigOption(name = "Enable Skydrunk", desc = "Master toggle for all Skydrunk features.")
    @ConfigEditorBoolean
    var uiEnabled: Boolean = true

    @ConfigOption(name = "Check for updates on startup", desc = "Looks up latest GitHub release on game start.")
    @ConfigEditorBoolean
    var autoCheckUpdates: Boolean = true
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
        desc = "Render a neon box around tripwire strings."
    )
    @ConfigEditorBoolean
    var highlightTripwire: Boolean = true

    @ConfigOption(
        name = "Tripwire ESP (IN DEVELOPMENT)",
        desc = "Draw tripwire outlines through walls (may cost a little FPS)."
    )
    @ConfigEditorBoolean
    var tripwireEsp: Boolean = false


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
    var infoTop: String =
        """
        ✅ Shard Tracking live (Hunting > Shard Tracking)
        • Auto price updates, overlay, per-session stats
        • Use /sd update check to check for new versions
        """.trimIndent()

    @ConfigOption(
        name = "About",
        desc = "Support & quick tips"
    )
    @ConfigEditorInfoText
    var infoBottom: String =
        "For mod support contact §b@makogai§r on Discord. Tip: move/scale the tracker via Overlay."
}

class ViewmodelCat : Config() {
    @ConfigOption(name = "Enable", desc = "Enable custom viewmodel transforms")
    @ConfigEditorBoolean var enabled = true

    @ConfigOption(
        name = "Open Big Editor",
        desc = "Full-screen editor with wide numeric fields. Run /sd vmedit."
    )
    @ConfigEditorInfoText
    var openEditorHint: String = "Run §b/sd vmedit§r to open the big Viewmodel editor."

    @ConfigOption(name = "Affect Offhand", desc = "Apply to offhand too")
    @ConfigEditorBoolean var affectOffHand = false

    @ConfigOption(name = "Offset X", desc = "-1.0 .. 1.0")
    @ConfigEditorSlider(minValue = -1f, maxValue = 1f, minStep = 0.01f) var offX = 0f
    @ConfigOption(name = "Offset Y", desc = "-1.0 .. 1.0")
    @ConfigEditorSlider(minValue = -1f, maxValue = 1f, minStep = 0.01f) var offY = 0f
    @ConfigOption(name = "Offset Z", desc = "-1.0 .. 1.0")
    @ConfigEditorSlider(minValue = -1f, maxValue = 1f, minStep = 0.01f) var offZ = 0f

    @ConfigOption(name = "Scale", desc = "0.1 .. 3.0")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3.0f, minStep = 0.01f) var scale = 1.0f

    @ConfigOption(name = "Equip Speed", desc = "0.25x .. 3.0x (higher = faster)")
    @ConfigEditorSlider(minValue = 0.25f, maxValue = 3.0f, minStep = 0.05f)
    var equipSpeed: Float = 1.0f

    @ConfigOption(name = "Swing Speed (visual)", desc = "0.25x .. 3.0x (visual only)")
    @ConfigEditorSlider(minValue = 0.25f, maxValue = 3.0f, minStep = 0.05f)
    var swingSpeed: Float = 1.0f

    @ConfigOption(name = "Swing Amplitude", desc = "0.0 .. 1.5 (visual swing distance)")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1.5f, minStep = 0.01f)
    var swingAmplitude = 1.0f

    @ConfigOption(name = "Equip Amplitude", desc = "0.0 .. 1.5 (how far it sits during equip)")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1.5f, minStep = 0.01f)
    var equipAmplitude = 1.0f

    @ConfigOption(name = "Rot X (deg)", desc = "-180 .. 180")
    @ConfigEditorSlider(minValue = -180f, maxValue = 180f, minStep = 1f) var rotX = 0f
    @ConfigOption(name = "Rot Y (deg)", desc = "-180 .. 180")
    @ConfigEditorSlider(minValue = -180f, maxValue = 180f, minStep = 1f) var rotY = 0f
    @ConfigOption(name = "Rot Z (deg)", desc = "-180 .. 180")
    @ConfigEditorSlider(minValue = -180f, maxValue = 180f, minStep = 1f) var rotZ = 0f
}

/* --------- ROOT --------- */

class McRoot : Config() {
    @ConfigOption(name = "Bazaar ID Overrides", desc = "Map shard name to custom bazaar id")
    val priceOverrides: MutableMap<String, String> = linkedMapOf()

    @Category(name = "Home", desc = "Welcome & project status.")
    val home = MainCat()

    @Category(name = "General", desc = "Global settings.")
    val general = GeneralCat()

    @Category(name = "Viewmodel", desc = "Main-hand position, scale, animation speed.")
    val viewmodel = ViewmodelCat()

    @Category(name = "Hunting Shards", desc = "Shard tracking and pricing.")
    val hunting = HuntingCat()

    @Category(name = "Overlay", desc = "HUD position, scale, opacity.")
    val overlay = OverlayCat()

    @Category(name = "Crystal Hollows", desc = "QoL for hollows.")
    val hollows = HollowsCat()

    @Category(name = "Dungeons", desc = "Dungeon QoL features")
    val dungeons = DungeonsCat()
}
