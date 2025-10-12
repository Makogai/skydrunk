package me.makogai.skydrunk.hud

import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.util.Debug
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import kotlin.math.max

object ShardSession {
    @Volatile private var currentName: String? = null

    @Volatile private var startMs: Long = System.currentTimeMillis()
    @Volatile private var totalShards: Int = 0
    @Volatile private var lastActivityMs: Long = 0L

    @Volatile private var paused: Boolean = false
    @Volatile private var pausedAtMs: Long = 0L

    @Volatile private var cachedShardsPerHour: Double = 0.0

    private var tick = 0
    @Volatile private var announcedIdleAction = false

    /** Call once from Client init. */
    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            if (++tick % 20 != 0) return@EndTick // ~1s
            maybeAutoIdleAction()
        })
    }

    /** Start/reset tracking for a given shard type. */
    fun touch(name: String) {
        val now = System.currentTimeMillis()
        if (currentName != name) {
            currentName = name
            startMs = now
            totalShards = 0
            lastActivityMs = 0L
            paused = false
            pausedAtMs = 0L
            announcedIdleAction = false
            recalc(now)
            Debug.info("§7Session started for §f$name§7.")
        }
    }

    /** Manual full reset (keeps current shard active if any). */
    fun reset() {
        val now = System.currentTimeMillis()
        startMs = now
        totalShards = 0
        lastActivityMs = 0L
        paused = false
        pausedAtMs = 0L
        announcedIdleAction = false
        recalc(now)
    }

    /** Fully clear session (no active shard → HUD hides). */
    private fun clear() {
        currentName = null
        reset()
    }

    /** Pause/resume controls (also used by auto-idle). */
    fun pause() {
        if (!paused && currentName != null) {
            paused = true
            pausedAtMs = System.currentTimeMillis()
            announcedIdleAction = true
        }
    }
    fun resume() {
        if (paused) {
            paused = false
            // elapsedMillis() already freezes during pause; nothing else needed
        }
    }

    /** Count new shards. Unpauses if needed. */
    fun addShards(count: Int) {
        if (count <= 0) return
        if (paused) paused = false
        totalShards += count
        lastActivityMs = System.currentTimeMillis()
        announcedIdleAction = false
        recalc(lastActivityMs)
    }

    /** Recalc cached S/H (coins/hr is derived in HUD from live prices). */
    private fun recalc(now: Long) {
        val elapsedMs = if (paused && lastActivityMs != 0L) {
            lastActivityMs - startMs
        } else {
            now - startMs
        }
        val elapsedH = max(elapsedMs / 3_600_000.0, 1e-6)
        cachedShardsPerHour = if (totalShards <= 0) 0.0 else totalShards / elapsedH
    }

    /** Auto pause or hide after inactivity based on config. */
    private fun maybeAutoIdleAction() {
        val name = currentName ?: return
        val cfg = McManaged.data().hunting.overlay

        val mins = cfg.idleHideMinutes.coerceAtLeast(0f)
        if (mins <= 0f) return // disabled
        val timeoutMs = (mins * 60_000f).toLong()

        val now = System.currentTimeMillis()
        val sinceActivity = if (lastActivityMs == 0L) (now - startMs) else (now - lastActivityMs)

        if (sinceActivity >= timeoutMs) {
            if (cfg.pauseOnInactive) {
                if (!paused) pause()
            } else {
                clear()
            }
            if (!announcedIdleAction) {
                Debug.info(
                    "§7Session ${if (cfg.pauseOnInactive) "paused" else "ended"} after §f${mins.toInt()}§7 min of inactivity."
                )
                announcedIdleAction = true
            }
        }
    }

    /** HUD can call this if price changed; nothing to do (we derive coins/hr live). */
    fun onPriceRefresh() { /* no-op */ }

    // ---------- getters used by HUD ----------
    fun shardCount(): Int = totalShards
    fun shardsPerHour(): Double = cachedShardsPerHour
    fun isPaused(): Boolean = paused
    fun elapsedMillis(): Long {
        val now = System.currentTimeMillis()
        return if (paused && lastActivityMs != 0L) lastActivityMs - startMs else now - startMs
    }
    fun currentNameOrNull(): String? = currentName
    fun currentName(): String = currentName ?: "—"
}
