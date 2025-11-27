package me.makogai.skydrunk.config

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.io.path.readText

object McManaged {
    lateinit var managed: ManagedConfig<McRoot>
        private set

    private var lastSavedJson: String = ""
    private var autoSaveTickCounter = 0

    fun init() {
        // Ensure the Fabric config directory exists and create the managed config file if missing
        val cfgPath = FabricLoader.getInstance().configDir
        try {
            Files.createDirectories(cfgPath)
        } catch (e: Exception) {
            println("[Skydrunk] Failed to create config dir: ${'$'}{e.message}")
        }
        val file = cfgPath.resolve("skydrunk-moul.json").toFile()
        managed = ManagedConfig.create(file, McRoot::class.java) { /* no-op */ }
        managed.injectIntoInstance()

        // After injection, attempt to load persistent values we may have stored previously
        try { loadPersistentFallback() } catch (e: Exception) { println("[Skydrunk] loadPersistentFallback failed: ${'$'}{e.message}") }

        // Read any existing fallback file content into lastSavedJson so autosave has a baseline
        try {
            val path = FabricLoader.getInstance().configDir.resolve("skydrunk-moul-fallback.json")
            if (Files.exists(path)) {
                val content = Files.readString(path).trim()
                if (content.isNotBlank()) lastSavedJson = content
            }
        } catch (_: Throwable) {}

        // Persist the default/initialized config immediately so the file exists for later sharing
        try {
            save()
        } catch (e: Exception) {
            println("[Skydrunk] Initial config save failed: ${'$'}{e.message}")
        }
    }

    fun open() {
        managed.openConfigGui()
    }

    fun data(): McRoot = managed.instance

    /**
     * Attempt to persist the managed config. Tries common library methods via reflection
     * then falls back to writing the instance as JSON to the managed file.
     */
    fun save() {
        // Try to find and call any suitable save/write method reflectively
        try {
            val clazz = managed::class.java
            val methods = (clazz.methods + clazz.declaredMethods).distinctBy { it.name + it.parameterCount }
            // Prefer no-arg methods whose name suggests saving/serializing
            val candidateNoArg = methods.firstOrNull { m ->
                m.parameterCount == 0 && Regex("(?i).*(save|write|serialize|tojson|export).*" ).matches(m.name)
            }
            if (candidateNoArg != null) {
                candidateNoArg.isAccessible = true
                val res = candidateNoArg.invoke(managed)
                // if method returned a String that looks like JSON, write it
                if (res is String && res.trim().startsWith("{")) {
                    try {
                        // Parse and skip empty JSON returned by library (e.g. "{}")
                        try {
                            val elem = Json.parseToJsonElement(res)
                            if (elem is kotlinx.serialization.json.JsonObject && elem.isEmpty()) {
                                // empty result => ignore and fallthrough to fallback save
                            } else {
                                val path = FabricLoader.getInstance().configDir.resolve("skydrunk-moul.json")
                                Files.createDirectories(path.parent)
                                path.writeText(res)
                                lastSavedJson = res
                                return
                            }
                        } catch (_: Throwable) {
                            // if parsing fails, still write res if it's non-empty
                            val path = FabricLoader.getInstance().configDir.resolve("skydrunk-moul.json")
                            Files.createDirectories(path.parent)
                            path.writeText(res)
                            lastSavedJson = res
                            return
                        }
                    } catch (_: Throwable) {}
                }
                // If method returned/ran but didn't write a file, verify the file exists; if not, continue to fallback
                try {
                    val path = FabricLoader.getInstance().configDir.resolve("skydrunk-moul.json")
                    if (Files.exists(path) && Files.readString(path).isNotBlank()) return
                } catch (_: Throwable) { /* fallthrough to fallback */ }
            }

            // Try single-arg methods that accept File/Path/String
            val candidateOneArg = methods.firstOrNull { m ->
                m.parameterCount == 1 && Regex("(?i).*(save|write|serialize|export|tojson).*" ).matches(m.name)
            }
            if (candidateOneArg != null) {
                try {
                    candidateOneArg.isAccessible = true
                    val paramType = candidateOneArg.parameterTypes[0]
                    val cfgDir = FabricLoader.getInstance().configDir
                    val file = cfgDir.resolve("skydrunk-moul.json").toFile()
                    val arg: Any = when {
                        paramType.isAssignableFrom(File::class.java) -> file
                        paramType.isAssignableFrom(java.nio.file.Path::class.java) -> cfgDir.resolve("skydrunk-moul.json")
                        paramType.isAssignableFrom(String::class.java) -> file.absolutePath
                        else -> file
                    }
                    candidateOneArg.invoke(managed, arg)
                    // verify file exists and isn't just empty-object; if it does and valid, we're done
                    val path = cfgDir.resolve("skydrunk-moul.json")
                    if (Files.exists(path)) {
                        val content = Files.readString(path).trim()
                        if (content.isNotBlank() && content != "{}") {
                            lastSavedJson = content
                            return
                        }
                    }
                } catch (_: Throwable) { /* fallthrough to fallback */ }
            }
        } catch (e: Exception) {
            println("[Skydrunk] ManagedConfig reflection attempts failed: ${'$'}{e.message}")
        }

        // Try a toJson method on the managed instance
        try {
            val toJson = try { managed::class.java.getMethod("toJson") } catch (_: Exception) { null }
            if (toJson != null) {
                val json = toJson.invoke(managed) as? String
                if (!json.isNullOrBlank()) {
                    // skip empty-object
                    if (json.trim() != "{}") {
                        // do not overwrite the library's file; write our fallback file instead
                        val fpath = FabricLoader.getInstance().configDir.resolve("skydrunk-moul-fallback.json")
                        Files.createDirectories(fpath.parent)
                        fpath.writeText(json)
                        lastSavedJson = json
                        return
                    }
                }
            }
        } catch (_: Throwable) {}

        //Fallback serializer: reflectively serialize the instance into JSON-like text so the file is not empty
        try {
            val written = persistFallbackJson()
            if (written) return
        } catch (e: Exception) {
            println("[Skydrunk] Failed to write fallback managed config: ${'$'}{e.message}")
        }
    }

    /** Called every client tick from the client; will autosave every ~20 ticks when the persisted JSON differs. */
    fun autoSaveTick() {
        autoSaveTickCounter = (autoSaveTickCounter + 1) % 5
        if (autoSaveTickCounter != 0) return
        try {
            val cur = computePersistJson()
            if (cur != lastSavedJson) {
                // write directly to our fallback file and also to the primary moul file so it's not empty
                val cfgDir = FabricLoader.getInstance().configDir
                val fpath = cfgDir.resolve("skydrunk-moul-fallback.json")
                val main = cfgDir.resolve("skydrunk-moul.json")
                Files.createDirectories(fpath.parent)
                fpath.writeText(cur)
                try { Files.createDirectories(main.parent); main.writeText(cur) } catch (_: Throwable) {}
                lastSavedJson = cur
                try { println("[Skydrunk] autosave persisted config (fallback)") } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }

    //Fallback persistence using kotlinx.serialization
    @Serializable private data class PersistOverlay(val showOverlay: Boolean, val opacity: Double, val scale: Double, val posX: Float, val posY: Float)
    @Serializable private data class PersistMilestone(val enabled: Boolean, val targetAmount: Double, val accumulated: Double, val shardsAccumulated: Long, val showInstaSell: Boolean, val opacity: Double, val scale: Double, val posX: Float, val posY: Float)
    @Serializable private data class PersistViewmodel(val enabled: Boolean, val offX: Float, val offY: Float, val offZ: Float, val scale: Float, val swingSpeed: Float, val equipSpeed: Float)
    @Serializable private data class PersistRoot(val generalUiEnabled: Boolean, val overlay: PersistOverlay, val milestone: PersistMilestone, val viewmodel: PersistViewmodel)

    private val jsonSer = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun persistFallbackJson(): Boolean {
        val out = computePersistJson()
        try {
            val cfgDir = FabricLoader.getInstance().configDir
            val fpath = cfgDir.resolve("skydrunk-moul-fallback.json")
            val main = cfgDir.resolve("skydrunk-moul.json")
            Files.createDirectories(fpath.parent)
            fpath.writeText(out)
            try { Files.createDirectories(main.parent); main.writeText(out) } catch (_: Throwable) {}
            lastSavedJson = out
            return true
        } catch (e: Throwable) {
            println("[Skydrunk] persistFallbackJson failed: ${'$'}{e.message}")
            return false
        }
    }

    private fun computePersistJson(): String {
        val cfg = managed.instance
        val overlay = cfg.hunting.overlay
        val ms = cfg.hunting.milestone
        val vm = cfg.viewmodel
        val p = PersistRoot(
            generalUiEnabled = cfg.general.uiEnabled,
            overlay = PersistOverlay(overlay.showOverlay, overlay.opacity.toDouble(), overlay.scale.toDouble(), overlay.posX, overlay.posY),
            milestone = PersistMilestone(ms.enabled, ms.targetAmount, ms.accumulated, ms.shardsAccumulated, ms.showInstaSell, ms.opacity.toDouble(), ms.scale.toDouble(), ms.posX, ms.posY),
            viewmodel = PersistViewmodel(vm.enabled, vm.offX, vm.offY, vm.offZ, vm.scale, vm.swingSpeed, vm.equipSpeed)
        )
        return jsonSer.encodeToString(p)
    }

    private fun loadPersistentFallback() {
        val path = FabricLoader.getInstance().configDir.resolve("skydrunk-moul-fallback.json")
        if (!Files.exists(path)) return
        val txt = path.readText().trim()
        if (txt.isEmpty()) return
        try {
            val p = jsonSer.decodeFromString<PersistRoot>(txt)
            applyPersistedToManaged(p)
        } catch (_: Exception) {
            // not our fallback format; ignore
        }
    }

    private fun applyPersistedToManaged(p: PersistRoot) {
        try {
            val root = managed.instance
            root.general.uiEnabled = p.generalUiEnabled
            root.hunting.overlay.showOverlay = p.overlay.showOverlay
            root.hunting.overlay.opacity = p.overlay.opacity.toFloat()
            root.hunting.overlay.scale = p.overlay.scale.toFloat()
            root.hunting.overlay.posX = p.overlay.posX
            root.hunting.overlay.posY = p.overlay.posY

            val ms = root.hunting.milestone
            ms.enabled = p.milestone.enabled
            ms.targetAmount = p.milestone.targetAmount
            ms.accumulated = p.milestone.accumulated
            ms.shardsAccumulated = p.milestone.shardsAccumulated
            ms.showInstaSell = p.milestone.showInstaSell
            ms.opacity = p.milestone.opacity.toFloat()
            ms.scale = p.milestone.scale.toFloat()
            ms.posX = p.milestone.posX
            ms.posY = p.milestone.posY

            val vm = root.viewmodel
            vm.enabled = p.viewmodel.enabled
            vm.offX = p.viewmodel.offX
            vm.offY = p.viewmodel.offY
            vm.offZ = p.viewmodel.offZ
            vm.scale = p.viewmodel.scale
            vm.swingSpeed = p.viewmodel.swingSpeed
            vm.equipSpeed = p.viewmodel.equipSpeed
        } catch (e: Exception) {
            println("[Skydrunk] applyPersistedToManaged failed: ${'$'}{e.message}")
        }
    }
}
