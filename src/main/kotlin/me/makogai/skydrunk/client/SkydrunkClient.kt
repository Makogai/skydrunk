package me.makogai.skydrunk.client

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.*
import me.makogai.skydrunk.bazaar.BazaarPriceFetcher
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.config.PriceSource
import me.makogai.skydrunk.detect.AreaState
import me.makogai.skydrunk.detect.ChatDropListener
import me.makogai.skydrunk.detect.LocrawDetector
import me.makogai.skydrunk.hud.DragScreen
import me.makogai.skydrunk.hud.ShardHud
import me.makogai.skydrunk.hud.ShardSession
import me.makogai.skydrunk.ui.InventoryResetButton
import me.makogai.skydrunk.util.Debug
import me.makogai.skydrunk.dungeons.TripwireHighlighter
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import me.makogai.skydrunk.update.UpdateChecker


class SkydrunkClient : ClientModInitializer {

    private lateinit var openConfigKey: KeyBinding

    override fun onInitializeClient() {
        // init config + bazaar + ui
        ConfigOpener.init()
        McManaged.init()
        UpdateChecker.init()
        BazaarPriceFetcher.start()
        InventoryResetButton.init()

        // detectors
        LocrawDetector.init()
        ChatDropListener.init()
        TripwireHighlighter.init()
        ShardSession.init()

        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, _ ->
            AreaState.onJoin()
        })

        // HUD
        HudRenderCallback.EVENT.register(HudRenderCallback { ctx, tickCounter ->
            ShardHud.render(ctx, tickCounter)
        })

        // Keybind: K to open Skydrunk (Moul screen)
        openConfigKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.skydrunk.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.skydrunk"
            )
        )
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            while (openConfigKey.wasPressed()) {
                client.execute { McManaged.open() }
            }
        })

        // Client commands
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            fun openConfigCmd(): Int {
                val mc = MinecraftClient.getInstance()
                mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Opening settings…"))
                ConfigOpener.requestOpen(2)
                return 1
            }

            fun currentShardBazaarIdOrGuess(): String? {
                val name = ShardSession.currentName()
                if (name == "—") return null
                val cfg = McManaged.data()
                return cfg.priceOverrides[name] ?: BazaarPriceFetcher.guessIdFromShardName(name)
            }

            fun register(name: String) {
                dispatcher.register(
                    literal(name)
                        .executes { openConfigCmd() }
                        .then(literal("open").executes { openConfigCmd() })
                        .then(literal("drag").executes { DragScreen.open(); 1 })
                        .then(literal("reset").executes { ShardSession.reset(); 1 })
                        .then(literal("addshard").executes { ShardSession.addShards(1); 1 })
                        .then(literal("update")
                            .then(literal("check").executes {
                                me.makogai.skydrunk.update.UpdateChecker.checkNow(announceUpToDate = true)
                                1
                            })
                        )
                        .then(literal("vmedit").executes {
                            val mc = MinecraftClient.getInstance()
                            mc.execute { me.makogai.skydrunk.viewmodel.ViewmodelEditor.open() }
                            1
                        })



                        .then(literal("overlay")
                            .then(literal("on").executes {
                                McManaged.data().overlay.showOverlay = true; 1
                            })
                            .then(literal("off").executes {
                                McManaged.data().overlay.showOverlay = false; 1
                            })
                            .then(literal("status").executes {
                                val on = McManaged.data().overlay.showOverlay
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(
                                    Text.of("§b[Skydrunk] Overlay: ${if (on) "§aON" else "§cOFF"}")
                                ); 1
                            })
                        )

                        .then(literal("price")
                            .then(literal("now").executes { ctx ->
                                val id = currentShardBazaarIdOrGuess()
                                val mc = ctx.source.client
                                if (id == null) {
                                    mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] §7No active shard. Use §f/sd shard set \"Name\""))
                                    return@executes 1
                                }
                                val snap = BazaarPriceFetcher.get(id)
                                val msg = if (snap == null) {
                                    "§cNo price for §f$id§c. ${BazaarPriceFetcher.lastError ?: ""}"
                                } else {
                                    "§7$id §8➜ §aInsta-Sell: §f%,d §7| §eSell Order: §f%,d"
                                        .format(snap.instaSell, snap.sellOrder)
                                }
                                mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] $msg"))
                                1
                            })
                            .then(literal("use-sell").executes { ctx ->
                                val d = McManaged.data()
                                d.hunting.priceSource = PriceSource.BAZAAR_INSTA_SELL
                                val id = currentShardBazaarIdOrGuess()
                                val snap = id?.let { BazaarPriceFetcher.get(it) }
                                if (snap != null) d.hunting.coinsPerShard = snap.instaSell.toDouble()
                                ShardSession.onPriceRefresh()
                                ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Using §aInsta-Sell§7 per-shard price."))
                                1
                            })
                            .then(literal("use-buy").executes { ctx ->
                                val d = McManaged.data()
                                d.hunting.priceSource = PriceSource.BAZAAR_SELL_ORDER
                                val id = currentShardBazaarIdOrGuess()
                                val snap = id?.let { BazaarPriceFetcher.get(it) }
                                if (snap != null) d.hunting.coinsPerShard = snap.sellOrder.toDouble()
                                ShardSession.onPriceRefresh()
                                ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Using §eSell Order§7 per-shard price."))
                                1
                            })
                            .then(literal("id").then(
                                argument("bzid", word()).executes { ctx ->
                                    val id = StringArgumentType.getString(ctx, "bzid")
                                    val snap = BazaarPriceFetcher.get(id)
                                    val msg = if (snap == null) "§cNo price for §f$id"
                                    else "§7$id §8➜ §aInsta-Sell: §f%,d §7| §eSell Order: §f%,d"
                                        .format(snap.instaSell, snap.sellOrder)
                                    ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] $msg"))
                                    1
                                }
                            ))
                            .then(literal("set").then(
                                argument("name", greedyString()).then(
                                    argument("bzid", word()).executes { ctx ->
                                        val nameArg = StringArgumentType.getString(ctx, "name").trim()
                                        val idArg = StringArgumentType.getString(ctx, "bzid")
                                        McManaged.data().priceOverrides[nameArg] = idArg
                                        ctx.source.client.inGameHud.chatHud.addMessage(
                                            Text.of("§b[Skydrunk] §7Override §f$nameArg §7→ §f$idArg")
                                        )
                                        1
                                    }
                                )
                            ))
                        )

                        .then(literal("debug")
                            .then(literal("on").executes {
                                Debug.enabled = true
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Debug: §aON")); 1
                            })
                            .then(literal("off").executes {
                                Debug.enabled = false
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Debug: §cOFF")); 1
                            })
                            .then(literal("status").executes {
                                val s = if (Debug.enabled) "§aON" else "§cOFF"
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Debug: $s")); 1
                            })
                        )

                        .then(literal("whereami").executes {
                            val s = if (AreaState.isInDwarvenMines()) "§aDwarven Mines" else "§7Unknown/Other"
                            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Area: $s"))
                            1
                        })

                        .then(literal("locraw").executes {
                            LocrawDetector.debugRequestNow(); 1
                        })

                        .then(literal("shard")
                            .then(literal("set").then(
                                argument("name", greedyString()).executes { ctx ->
                                    val nameArg = StringArgumentType.getString(ctx, "name").trim()
                                    ShardSession.touch(nameArg)
                                    ctx.source.client.inGameHud.chatHud.addMessage(
                                        Text.of("§b[Skydrunk] Now tracking §f$nameArg §7shards.")
                                    )
                                    1
                                }
                            ))
                        )
                )
            }

            register("skydrunk")
            register("sd") // alias
        }
    }
}
