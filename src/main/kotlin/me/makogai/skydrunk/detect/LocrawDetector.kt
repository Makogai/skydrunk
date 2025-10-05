package me.makogai.skydrunk.detect

import me.makogai.skydrunk.util.Debug
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object LocrawDetector {
    @Volatile private var lastSeenMs: Long = 0L
    @Volatile private var lastRequestMs: Long = 0L
    @Volatile private var isSkyBlock: Boolean = false
    @Volatile private var mapName: String = ""

    private const val TTL_MS = 60_000L
    private const val REQUEST_COOLDOWN = 30_000L

    private val reGameType = Regex("""(?i)"gametype"\s*:\s*"([^"]+)"""")
    private val reMap      = Regex("""(?i)"map"\s*:\s*"([^"]+)"""")

    private var pendingDelayTicks = -1

    fun init() {
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, _ ->
            pendingDelayTicks = 40 // ~2s
            Debug.chat("JOIN: scheduling /locraw in ~2s")
            Debug.log("JOIN: delay ticks set to 40")
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            val now = System.currentTimeMillis()

            if (pendingDelayTicks >= 0) {
                if (pendingDelayTicks-- == 0) {
                    Debug.chat("Delayed /locraw request…")
                    requestLocraw(now)
                    pendingDelayTicks = -1
                }
            }

            val stale = now - lastSeenMs > TTL_MS
            val cooled = now - lastRequestMs > REQUEST_COOLDOWN
            if (stale && cooled) {
                Debug.chat("Stale location -> requesting /locraw")
                requestLocraw(now)
            }
        })

        ClientReceiveMessageEvents.GAME.register(ClientReceiveMessageEvents.Game { msg: Text, _ ->
            val s = msg.string
            if (s.startsWith("{") && s.contains("\"gametype\"", true)) {
                Debug.log("locraw raw: $s")
            }
            parseAndStore(s)
        })
    }

    fun debugRequestNow() {
        Debug.chat("Manual /locraw requested")
        requestLocraw(System.currentTimeMillis())
    }

    private fun onHypixel(): Boolean {
        val addr = MinecraftClient.getInstance().currentServerEntry?.address?.lowercase()
        val ok = addr?.contains("hypixel.net") == true
        Debug.log("onHypixel=$ok addr=$addr")
        return ok
    }

    private fun requestLocraw(nowMs: Long) {
        if (!onServerWhereLocrawIsSafe()) {
            Debug.chat("Skip /locraw (not multiplayer or blocked server)")
            return
        }
        lastRequestMs = nowMs
        sendChatCommand("locraw")
    }

    private fun sendChatCommand(cmd: String) {
        val mc = MinecraftClient.getInstance() ?: return
        val player = mc.player ?: return
        val nh = player.networkHandler

        try {
            Debug.log("trying sendChatCommand(String)")
            nh.javaClass.getMethod("sendChatCommand", String::class.java).invoke(nh, cmd)
            Debug.chat("Sent /$cmd via sendChatCommand")
            return
        } catch (t: Throwable) {
            Debug.log("sendChatCommand failed: ${t.javaClass.simpleName}: ${t.message}")
        }
        try {
            Debug.log("trying sendChatMessage(String)")
            nh.javaClass.getMethod("sendChatMessage", String::class.java).invoke(nh, "/$cmd")
            Debug.chat("Sent /$cmd via sendChatMessage")
            return
        } catch (t: Throwable) {
            Debug.log("sendChatMessage failed: ${t.javaClass.simpleName}: ${t.message}")
        }
        try {
            Debug.log("trying player.sendCommand(String)")
            player.javaClass.getMethod("sendCommand", String::class.java).invoke(player, cmd)
            Debug.chat("Sent /$cmd via player.sendCommand")
        } catch (t: Throwable) {
            Debug.chat("§cAll /$cmd send methods failed (${t.javaClass.simpleName})")
            Debug.log("player.sendCommand failed: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun parseAndStore(line: String) {
        if (!line.startsWith("{") || !line.contains("\"gametype\"", true)) return

        val gametype = reGameType.find(line)?.groupValues?.getOrNull(1)?.uppercase()
        val map      = reMap.find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        if (gametype != null) {
            val skyblock = (gametype == "SKYBLOCK")
            if (skyblock && map.equals("Dwarven Mines", true)) {
                AreaState.markInMines(AreaState.Source.LOCRAW, "map=$map")
            } else if (skyblock) {
                AreaState.markNotInMines(AreaState.Source.LOCRAW, "map=$map")
            } else {
                AreaState.markNotInMines(AreaState.Source.LOCRAW, "gametype=$gametype")
            }
            lastSeenMs = System.currentTimeMillis()
            Debug.chat("locraw ➜ gametype=$gametype map='$map'")
        }

    }

    fun isInDwarvenMines(): Boolean {
        val age = System.currentTimeMillis() - lastSeenMs
        val fresh = age <= TTL_MS
        val ok = fresh && isSkyBlock && mapName.equals("Dwarven Mines", true)
        if (Debug.enabled) {
            Debug.log("isInDwarvenMines=$ok fresh=${fresh} ageMs=$age skyblock=$isSkyBlock map='$mapName'")
        }
        return ok
    }

    private fun onServerWhereLocrawIsSafe(): Boolean {
        val mc = MinecraftClient.getInstance()
        if (mc.isIntegratedServerRunning) return false

        val addr = mc.currentServerEntry?.address?.lowercase()
        // Many launchers show different forms; allow null/unknown but still avoid SP.
        val ok = addr == null || "hypixel.net" in addr || addr.endsWith(".hypixel.net")
        if (Debug.enabled) Debug.log("locraw server guard addr=$addr ok=$ok")
        return ok
    }
}
