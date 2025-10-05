package me.makogai.skydrunk.detect

import me.makogai.skydrunk.detect.AreaState.Source
import net.minecraft.text.Text

object LocationTrackerTab {
    @Volatile private var lastHeader: String = ""
    @Volatile private var lastFooter: String = ""

    /** Called from the mixin whenever tab header/footer changes. */
    fun onHeaderFooter(header: Text?, footer: Text?) {
        lastHeader = header?.string ?: ""
        lastFooter = footer?.string ?: ""

        // Combine both; Hypixel sometimes rotates info between header/footer
        val combined = (lastHeader + "\n" + lastFooter).lowercase()

        when {
            "dwarven mines" in combined ->
                AreaState.markInMines(Source.TAB, "tab header/footer contains 'Dwarven Mines'")

            // If clearly another SkyBlock area, flip off
            listOf(
                "crystal hollows", "crimson isles", "hub island",
                "foraging island", "dungeon hub", "the end", "garden"
            ).any { it in combined } ->
                AreaState.markNotInMines(Source.TAB, "tab moved to another area")

            else -> {
                // Unknown/neutral change → do nothing (sticky state remains)
            }
        }
    }

    fun isInDwarvenMines(): Boolean = AreaState.isInDwarvenMines()
}
