package me.makogai.skydrunk.detect

import me.makogai.skydrunk.util.Debug
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object AreaState {
    enum class Source { LOCRAW, TAB, JOIN }
    @Volatile private var inMines: Boolean? = null
    @Volatile private var lastSource: Source = Source.JOIN
    @Volatile private var lastUpdated: Long = 0L

    private fun chat(msg: String) {
        MinecraftClient.getInstance()?.inGameHud?.chatHud
            ?.addMessage(Text.of("§b[Skydrunk] §7$msg"))
    }

    fun onJoin() {
        val old = inMines
        inMines = null
        lastSource = Source.JOIN
        lastUpdated = System.currentTimeMillis()
        if (old != null) chat("Loading location… (detecting area)")
        Debug.log("AreaState: reset on join")
    }

    /** Mark known-in-mines; only prints when value changes. */
    fun markInMines(src: Source, reason: String) {
        val changed = inMines != true
        inMines = true
        lastSource = src
        lastUpdated = System.currentTimeMillis()
        if (changed) chat("Detected §aDwarven Mines§7 via §f$src§7 ($reason). Tracker ready.")
        Debug.log("AreaState=true src=$src reason=$reason")
    }

    /** Mark known-not-in-mines; only prints when switching away. */
    fun markNotInMines(src: Source, reason: String) {
        val changed = inMines != false
        inMines = false
        lastSource = src
        lastUpdated = System.currentTimeMillis()
        if (changed) chat("Left §cDwarven Mines§7 (via §f$src§7: $reason). Tracker paused.")
        Debug.log("AreaState=false src=$src reason=$reason")
    }

    /** Sticky: returns true if last known was “in mines”. Never flips to false due to staleness. */
    fun isInDwarvenMines(): Boolean = (inMines == true)
}
