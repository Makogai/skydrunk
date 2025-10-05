package me.makogai.skydrunk.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString   // ✅
import kotlinx.serialization.encodeToString   // ✅
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

@Serializable data class HuntingShards(
    var enabled: Boolean = true,
    var trackGlacite: Boolean = true,
    var autoResetOnWarp: Boolean = false,
    var coinsPerShard: Long = 150_000,
    var autoUpdatePrice: Boolean = true,
    var priceSource: String = "BAZAAR_INSTA_SELL"
)


@Serializable data class OverlayStyle(
    var showOverlay: Boolean = true,
    var opacity: Double = 0.9,
    var scale: Double = 1.0,
    var posX: Int = 16,
    var posY: Int = 16
)


@Serializable data class CrystalHollows(
    var showOwned: Boolean = true,
    var highlightMissing: Boolean = true
)

@Serializable data class SkydrunkConfigModel(
    var uiEnabled: Boolean = true,
    var hunting: HuntingShards = HuntingShards(),
    var overlay: OverlayStyle = OverlayStyle(),
    var hollows: CrystalHollows = CrystalHollows()
)

object SkydrunkConfig {
    private val path = FabricLoader.getInstance().configDir.resolve("skydrunk.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    var data: SkydrunkConfigModel = SkydrunkConfigModel()

    fun load() {
        data = try {
            if (Files.exists(path)) json.decodeFromString<SkydrunkConfigModel>(Files.readString(path))
            else { save(); SkydrunkConfigModel() }
        } catch (_: Exception) { SkydrunkConfigModel() }
    }

    fun save() {
        Files.createDirectories(path.parent)
        Files.writeString(path, json.encodeToString(data))
    }
}
