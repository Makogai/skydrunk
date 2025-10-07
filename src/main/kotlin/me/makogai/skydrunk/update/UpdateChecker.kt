package me.makogai.skydrunk.update

import com.google.gson.JsonParser
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.util.Debug
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object UpdateChecker {
    private const val REPO_OWNER = "makogai"
    private const val REPO_NAME  = "SkyDrunk"
    private const val RELEASES_PAGE = "github.com/$REPO_OWNER/$REPO_NAME/releases"

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    @Volatile private var didStartupCheck = false

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (didStartupCheck) return@EndTick
            val cfg = McManaged.data().general
            if (!cfg.autoCheckUpdates) {
                didStartupCheck = true
                return@EndTick
            }
            if (client.world == null) return@EndTick
            didStartupCheck = true
            checkNow(announceUpToDate = false)
        })
    }

    /** Public: you can call UpdateChecker.checkNow(true) from menus. */
    fun checkNow(announceUpToDate: Boolean = true) {
        val currentRaw = FabricLoader.getInstance()
            .getModContainer("skydrunk")
            .map { it.metadata.version.friendlyString }
            .orElse("dev")

        val latestPair = fetchLatestRelease()
        if (latestPair == null) {
            chat( chatPrefix().append(gray("Update check failed.")) )
            return
        }

        val latestRaw = latestPair.first
        val latest = norm(latestRaw)
        val current = norm(currentRaw)

        if (isNewer(latest, current)) {
            val msg: MutableText = chatPrefix()
                .append(gray("New version available: "))
                .append(boldGreen(pretty(latest)))              // vX.Y.Z
                .append(gray("  (you have "))
                .append(white(pretty(current)))                 // vX.Y.Z
                .append(gray(").  "))
                .append(yellow("Go to "))
                .append(white(RELEASES_PAGE))
            chat(msg)
        } else if (announceUpToDate) {
            chat(
                chatPrefix()
                    .append(gray("You’re up to date (latest: "))
                    .append(white(pretty(latest)))
                    .append(gray(")."))
            )
        }
    }

    private fun fetchLatestRelease(): Pair<String, String>? = try {
        val req = HttpRequest.newBuilder(
            URI.create("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
        )
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "SkyDrunk-Mod-UpdateChecker")
            .GET()
            .build()

        val body = http.send(req, HttpResponse.BodyHandlers.ofString()).body()
        val json = JsonParser.parseString(body).asJsonObject
        val tag = json["tag_name"]?.asString ?: return null
        val url = json["html_url"]?.asString ?: return null
        tag to url
    } catch (t: Throwable) {
        Debug.warn("Update check failed: ${t.javaClass.simpleName} ${t.message}")
        null
    }

    // --- Version helpers -----------------------------------------------------

    private fun norm(ver: String): String = ver.trim().lowercase().removePrefix("v")
    private fun pretty(ver: String): String = if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"

    /** Compare normalized semantic-ish versions. */
    private fun isNewer(latest: String, current: String): Boolean {
        if (current == "dev") return true
        fun parts(s: String) = s.split(Regex("[^0-9a-z]+")).filter { it.isNotEmpty() }
        val a = parts(current)
        val b = parts(latest)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val ai = a.getOrNull(i) ?: "0"
            val bi = b.getOrNull(i) ?: "0"
            val ani = ai.toIntOrNull()
            val bni = bi.toIntOrNull()
            if (ani != null && bni != null) {
                if (ani != bni) return bni > ani
            } else if (ai != bi) return bi > ai
        }
        return false
    }

    // --- Chat helpers --------------------------------------------------------

    private fun chatPrefix() = Text.literal("[SkyDrunk] ")
        .formatted(Formatting.AQUA)
        .styled { it.withBold(true) }

    private fun gray(s: String)  = Text.literal(s).formatted(Formatting.GRAY)
    private fun white(s: String) = Text.literal(s).formatted(Formatting.WHITE)
    private fun yellow(s: String)= Text.literal(s).formatted(Formatting.YELLOW)
    private fun boldGreen(s: String) =
        Text.literal(s).formatted(Formatting.GREEN).styled { it.withBold(true) }

    private fun chat(t: Text) {
        val mc = MinecraftClient.getInstance()
        mc.execute { mc.inGameHud?.chatHud?.addMessage(t) }
    }
}
