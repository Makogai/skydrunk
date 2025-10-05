package me.makogai.skydrunk.hud

import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.util.Debug
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import kotlin.math.max
import kotlin.math.roundToLong

object ShardSession {
    // active shard being tracked; null = no active session (HUD should hide)
    @Volatile private var currentName: String? = null

    @Volatile private var startMs: Long = System.currentTimeMillis()
    @Volatile private var totalShards: Int = 0

    // last time we saw a NEW shard drop (drives the idle timeout)
    @Volatile private var lastActivityMs: Long = 0L

    // Cached “event-driven” rates (only updated on addShards() or onPriceRefresh())
    @Volatile private var cachedShardsPerHour: Double = 0.0
    @Volatile private var cachedCoinsPerHour: Long = 0L

    // idle timeout (ms)
    private const val IDLE_TIMEOUT_MS = 60_000
    private var tick = 0
    private var announcedIdleEnd = false

    /** Call once from Client init. Sets up a 1s idle check. */
    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            if (++tick % 20 != 0) return@EndTick   // ~1 second
            maybeAutoResetIdle()
        })
    }

    /** Start or continue tracking for the given shard type without wiping counters. */
    fun touch(name: String) {
        val now = System.currentTimeMillis()
        if (currentName != name) {
            // switching shard: fully reset session (you asked for this behavior)
            currentName = name
            startMs = now
            totalShards = 0
            recalc()
            Debug.info("§7Session started for §f$name§7.")
        }
        // don't count as activity unless shards actually dropped
        // (we want HUD to hide if I stop getting shards)
    }

    /** Manually reset the session counts/time (keeps current shard active). */
    fun reset() {
        startMs = System.currentTimeMillis()
        totalShards = 0
        lastActivityMs = 0L
        announcedIdleEnd = false
        recalc()
    }

    /** Fully clear (no active shard => HUD should hide). */
    private fun clear() {
        currentName = null
        reset()
    }

    /** Add shard drops and recompute rates. */
    fun addShards(count: Int) {
        if (count <= 0) return
        totalShards += count
        lastActivityMs = System.currentTimeMillis()
        announcedIdleEnd = false
        recalc()
    }

    /** Recalculate cached rates using current config price. */
    private fun recalc() {
        val elapsedH = max(elapsedMillis() / 3_600_000.0, 1e-6)
        cachedShardsPerHour = if (totalShards <= 0) 0.0 else totalShards / elapsedH
        val price = McManaged.data().hunting.coinsPerShard
        cachedCoinsPerHour = (cachedShardsPerHour * price).roundToLong()
    }

    /** Called when price was refreshed (minute poll or /sd price use-*) */
    fun onPriceRefresh() {
        // Price updates should NOT keep the session alive.
        recalc()
    }

    /** Auto-hide/reset if no shards for IDLE_TIMEOUT_MS. */
    private fun maybeAutoResetIdle() {
        val name = currentName ?: return
        val now = System.currentTimeMillis()

        // If we never got a shard yet, lastActivityMs == 0; treat as active for first minute from start.
        val sinceActivity = if (lastActivityMs == 0L) (now - startMs) else (now - lastActivityMs)

        if (sinceActivity >= IDLE_TIMEOUT_MS) {
            clear()
            if (!announcedIdleEnd) {
                Debug.info("§7Session ended due to inactivity (no new shards for ${IDLE_TIMEOUT_MS / 1000}s).")
                announcedIdleEnd = true
            }
        }
    }

    // ---------- getters used by HUD ----------
    fun shardCount(): Int = totalShards
    fun shardsPerHour(): Double = cachedShardsPerHour
    fun coinsPerHour(): Long = cachedCoinsPerHour
    fun elapsedMillis(): Long = System.currentTimeMillis() - startMs

    /** Returns the active shard name or null if none. */
    fun currentNameOrNull(): String? = currentName

    /** “Pretty” current name for UI (returns "—" if none). */
    fun currentName(): String = currentName ?: "—"
}
