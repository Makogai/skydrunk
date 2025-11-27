package me.makogai.skydrunk.config

import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import me.makogai.skydrunk.config.PriceSource

/* ---------- GENERAL ---------- */

class GeneralCat : Config() {
    @ConfigOption(name = "Enable Skydrunk", desc = "Master toggle for all Skydrunk features.")
    @ConfigEditorBoolean var uiEnabled: Boolean = true

    @ConfigOption(name = "Check for updates on startup", desc = "Looks up latest GitHub release on game start.")
    @ConfigEditorBoolean var autoCheckUpdates: Boolean = true
}

/* ---------- HUNTING / OVERLAY ---------- */

enum class ElapsedFormat { HH_MM, MIN_SEC }

class OverlaySubCat : Config() {
    @ConfigOption(name = "Show Tracker", desc = "Toggle hunting tracker visibility.")
    @ConfigEditorBoolean var showOverlay: Boolean = true

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

    @ConfigOption(
        name = "Elapsed format",
        desc = "How to show elapsed session time."
    )
    @ConfigEditorDropdown(values = ["HH_MM", "MIN_SEC"])
    var elapsedFormat: ElapsedFormat = ElapsedFormat.MIN_SEC

    @ConfigOption(
        name = "Hide after inactivity (minutes)",
        desc = "How long with no new shards before action triggers. 0 = never."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    var idleHideMinutes: Float = 5f

    @ConfigOption(
        name = "Pause instead of hide",
        desc = "If enabled, session pauses after inactivity instead of hiding/clearing."
    )
    @ConfigEditorBoolean
    var pauseOnInactive: Boolean = false
}


class HuntingCat : Config() {
    @ConfigOption(name = "Enable Shard Tracking", desc = "Track shard drops and rates.")
    @ConfigEditorBoolean var enabled: Boolean = true

    @ConfigOption(name = "Auto-update price from Bazaar", desc = "Refresh price snapshot every minute while a shard is active.")
    @ConfigEditorBoolean var autoUpdatePrice: Boolean = true

    @Category(name = "Overlay", desc = "HUD position, scale, opacity, idle behavior.")
    val overlay = OverlaySubCat()

    // Fallback price if Bazaar fetch fails (still shown alongside other value)
    @ConfigOption(name = "Fallback price per shard", desc = "Used when Bazaar snapshot isn't available.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 5_000_000f, minStep = 1000f)
    var coinsPerShardFallback: Double = 150_000.0

    /* ---------- COMPAT FOR OLD CODE VERSION - TODO: FIX THIS ---------- */
    // Old code reads hunting.coinsPerShard – delegate to the fallback.
    var coinsPerShard: Double
        get() = coinsPerShardFallback
        set(value) { coinsPerShardFallback = value }

    // Old code reads hunting.priceSource in a `when` – keep it around.
    var priceSource: PriceSource = PriceSource.BAZAAR_INSTA_SELL

    /* ---------- MILESTONE ---------- */
    class MilestoneCat : Config() {
        @ConfigOption(name = "Enable milestone", desc = "Enable money milestone overlay and tracking.")
        @ConfigEditorBoolean var enabled: Boolean = false

        @ConfigOption(name = "Target amount (coins)", desc = "How many coins until milestone is reached (e.g. 60000000 = 60,000,000)")
        var targetAmount: Double = 0.0

        @ConfigOption(name = "Accumulated amount", desc = "Accumulated coins toward milestone (persists across sessions).")
        var accumulated: Double = 0.0

        @ConfigOption(name = "Accumulated shards", desc = "Accumulated shards towards milestone (persists across sessions).")
        var shardsAccumulated: Long = 0L

        @ConfigOption(name = "Notified (sell order)", desc = "Internal: whether user was notified for sell-order milestone (auto-cleared on reset).")
        var notifiedSellOrder: Boolean = false

        @ConfigOption(name = "Notified (insta-sell)", desc = "Internal: whether user was notified for insta-sell milestone (auto-cleared on reset).")
        var notifiedInstaSell: Boolean = false

        @ConfigOption(name = "Show milestone overlay", desc = "Display the milestone HUD on screen.")
        @ConfigEditorBoolean var showOverlay: Boolean = true

        @ConfigOption(name = "Milestone opacity", desc = "0.10 - 1.00")
        @ConfigEditorSlider(minValue = 0.10f, maxValue = 1.00f, minStep = 0.05f)
        var opacity: Float = 0.95f

        @ConfigOption(name = "Milestone scale", desc = "0.5 - 2.0")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.0f, minStep = 0.05f)
        var scale: Float = 1.0f

        @ConfigOption(name = "Position X", desc = "HUD X for milestone")
        @ConfigEditorSlider(minValue = 0f, maxValue = 10000f, minStep = 1f)
        var posX: Float = 200f

        @ConfigOption(name = "Position Y", desc = "HUD Y for milestone")
        @ConfigEditorSlider(minValue = 0f, maxValue = 10000f, minStep = 1f)
        var posY: Float = 16f

        @ConfigOption(name = "Open Milestone Popup", desc = "Toggle to open the Milestone popup. Will reset automatically.")
        @ConfigEditorBoolean
        var openPopup: Boolean = false

        @ConfigOption(name = "Show Insta-Sell Progress", desc = "If enabled the milestone HUD shows a second bar for insta-sell; otherwise only sell-order is shown.")
        @ConfigEditorBoolean
        var showInstaSell: Boolean = false
    }

    @Category(name = "Milestone", desc = "Money milestone overlay and settings.")
    val milestone = MilestoneCat()
}

/* ---------- DUNGEONS (Tripwire) ---------- */

class DungeonsCat : Config() {
    @ConfigOption(
        name = "Highlight Tripwire",
        desc = "Render a neon box around tripwire strings."
    )
    @ConfigEditorBoolean var highlightTripwire: Boolean = true

    @ConfigOption(
        name = "Tripwire ESP (IN DEVELOPMENT)",
        desc = "Draw tripwire outlines through walls (may cost a little FPS)."
    )
    @ConfigEditorBoolean var tripwireEsp: Boolean = false

    @ConfigOption(
        name = "Tripwire Range",
        desc = "Search radius for tripwire strings (blocks)."
    )
    @ConfigEditorSlider(minValue = 4f, maxValue = 64f, minStep = 1f)
    var tripwireRange: Float = 20f
}

/* ---------- HOME ---------- */

class MainCat : Config() {
    @ConfigOption(name = "Skydrunk — Status", desc = "Read-only information panel")
    @ConfigEditorInfoText(infoTitle = "Welcome to Skydrunk")
    var infoTop: String =
        """
        ✅ Shard Tracking live (Hunting)
        • Auto price updates (if enabled), overlay, per-session stats
        • Use /sd drag to move the tracker
        """.trimIndent()

    @ConfigOption(name = "About", desc = "Support & quick tips")
    @ConfigEditorInfoText
    var infoBottom: String =
        "For mod support contact §b@makogai§r on Discord. Tip: move/scale the tracker via Hunting → Overlay."
}

/* ---------- VIEWMODEL ---------- */

class ViewmodelCat : Config() {
    @ConfigOption(name = "Enable", desc = "Enable custom viewmodel transforms")
    @ConfigEditorBoolean var enabled = true

    @ConfigOption(name = "Open Big Editor", desc = "Run /sd vmedit for a full-screen editor.")
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

/* ---------- ROOT ---------- */

class McRoot : Config() {
    @ConfigOption(name = "Bazaar ID Overrides", desc = "Map shard name to custom bazaar id")
    val priceOverrides: MutableMap<String, String> = linkedMapOf()

    @Category(name = "Home", desc = "Welcome & project status.")
    val home = MainCat()

    @Category(name = "General", desc = "Global settings.")
    val general = GeneralCat()

    @Category(name = "Hunting", desc = "Shard tracking, prices, and overlay.")
    val hunting = HuntingCat()

    // Expose the same milestone as a top-level category so moulconfig displays a Milestone tab
    @Category(name = "Milestone", desc = "Money milestone overlay and settings.")
    val milestone = hunting.milestone

    @Category(name = "Dungeons", desc = "Dungeon QoL features")
    val dungeons = DungeonsCat()

    @Category(name = "Viewmodel", desc = "Main-hand position, scale, animation speed.")
    val viewmodel = ViewmodelCat()
}
