package me.makogai.skydrunk.client

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.*
import me.makogai.skydrunk.bazaar.BazaarPriceFetcher
import me.makogai.skydrunk.config.McManaged
import me.makogai.skydrunk.detect.AreaState
import me.makogai.skydrunk.detect.ChatDropListener
import me.makogai.skydrunk.detect.LocrawDetector
import me.makogai.skydrunk.dungeons.TripwireHighlighter
import me.makogai.skydrunk.hud.DragScreen
import me.makogai.skydrunk.hud.ShardHud
import me.makogai.skydrunk.hud.ShardSession
import me.makogai.skydrunk.ui.InventoryResetButton
import me.makogai.skydrunk.update.UpdateChecker
import me.makogai.skydrunk.util.Debug
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
import com.mojang.brigadier.arguments.IntegerArgumentType.*
import com.mojang.brigadier.arguments.IntegerArgumentType
import kotlin.math.abs


class SkydrunkClient : ClientModInitializer {

    private lateinit var openConfigKey: KeyBinding

    private fun fmtLong(n: Long): String {
        val absN = abs(n.toDouble())
        return when {
            absN >= 1_000_000_000 -> String.format("%.1fb", n / 1_000_000_000.0)
            absN >= 1_000_000     -> String.format("%.1fm", n / 1_000_000.0)
            absN >= 1_000         -> String.format("%.1fk", n / 1_000.0)
            else -> "%,d".format(n)
        }
    }

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
            // Attempt to open milestone popup if requested; the opener will retry a few times
            me.makogai.skydrunk.hud.MilestonePopupOpener.tick(client)
            // Attempt to open viewmodel editor if requested
            me.makogai.skydrunk.viewmodel.ViewmodelEditorOpener.tick(client)
            // Attempt to open dragger GUI if requested
            me.makogai.skydrunk.hud.DragScreenOpener.tick(client)

            // If user toggled the in-config 'Open Milestone Popup', consume it here and request the opener
            try {
                val ms = McManaged.data().hunting.milestone
                if (ms.openPopup) {
                    ms.openPopup = false
                    try { McManaged.save() } catch (_: Throwable) {}
                    me.makogai.skydrunk.hud.MilestonePopupOpener.request()
                }
            } catch (_: Throwable) {}

            // Autosave managed config fallback if anything changed (runs ~once per second)
            try { McManaged.autoSaveTick() } catch (_: Throwable) {}
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
                val name = ShardSession.currentNameOrNull() ?: return null
                val cfg = McManaged.data()
                return cfg.priceOverrides[name] ?: BazaarPriceFetcher.guessIdFromShardName(name)
            }

            fun register(name: String) {
                dispatcher.register(
                    literal(name)
                        .executes { openConfigCmd() }
                        .then(literal("open").executes { openConfigCmd() })
                        .then(literal("drag").executes { ctx ->
                            me.makogai.skydrunk.hud.DragScreenOpener.request()
                            ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Opening dragger…"))
                            1
                        })
                        .then(literal("gui").executes { ctx ->
                            me.makogai.skydrunk.hud.DragScreenOpener.request()
                            ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Opening dragger…"))
                            1
                        })
                        .then(literal("reset").executes { ShardSession.reset(); 1 })
                        .then(literal("addshard").executes { ShardSession.addShards(1); 1 })
                        .then(literal("update")
                            .then(literal("check").executes {
                                me.makogai.skydrunk.update.UpdateChecker.checkNow(announceUpToDate = true)
                                1
                            })
                        )
                        .then(literal("vmedit").executes { ctx ->
                            // Open the managed MoulConfig editor so users get the real sliders and live updates.
                            me.makogai.skydrunk.client.ConfigOpener.requestOpen(2)
                            ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Opening Viewmodel settings in MoulConfig…"))
                            1
                        })
                        .then(literal("overlay")
                            .then(literal("on").executes { McManaged.data().hunting.overlay.showOverlay = true; 1 })
                            .then(literal("off").executes { McManaged.data().hunting.overlay.showOverlay = false; 1 })
                            .then(literal("status").executes {
                                val on = McManaged.data().hunting.overlay.showOverlay
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
                                    "§7$id §8➜ §aInsta-Sell: §f%,d §7| §eSell-Order: §f%,d"
                                        .format(snap.instaSell, snap.sellOrder)
                                }
                                mc.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] $msg"))
                                1
                            })
                            .then(literal("id").then(
                                argument("bzid", word()).executes { ctx ->
                                    val id = StringArgumentType.getString(ctx, "bzid")
                                    val snap = BazaarPriceFetcher.get(id)
                                    val msg = if (snap == null) "§cNo price for §f$id"
                                    else "§7$id §8➜ §aInsta-Sell: §f%,d §7| §eSell-Order: §f%,d"
                                        .format(snap.instaSell, snap.sellOrder)
                                    ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] $msg"))
                                    1
                                }
                            ))
                        )
                        .then(literal("whereami").executes {
                            val s = if (AreaState.isInDwarvenMines()) "§aDwarven Mines" else "§7Unknown/Other"
                            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Area: $s"))
                            1
                        })
                        .then(literal("locraw").executes { LocrawDetector.debugRequestNow(); 1 })
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
                        .then(literal("milestone")
                            .then(literal("set").then(
                                argument("amount", IntegerArgumentType.integer(1)).executes { ctx ->
                                    val amt = IntegerArgumentType.getInteger(ctx, "amount")
                                    val cfgNow = McManaged.data()
                                    // start a fresh milestone: clear accumulators + notifications
                                    cfgNow.hunting.milestone.accumulated = 0.0
                                    cfgNow.hunting.milestone.shardsAccumulated = 0L
                                    cfgNow.hunting.milestone.notifiedSellOrder = false
                                    cfgNow.hunting.milestone.notifiedInstaSell = false
                                    cfgNow.hunting.milestone.targetAmount = amt.toDouble()
                                    cfgNow.hunting.milestone.enabled = amt > 0
                                    try { McManaged.save() } catch (_: Throwable) {}
                                    ctx.source.client.inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Milestone set to §f${fmtLong(amt.toLong())}§b."))
                                    1
                                }
                            ))
                            .then(literal("reset").executes {
                                val cfgNow = McManaged.data()
                                cfgNow.hunting.milestone.accumulated = 0.0
                                cfgNow.hunting.milestone.shardsAccumulated = 0L
                                cfgNow.hunting.milestone.targetAmount = 0.0
                                cfgNow.hunting.milestone.enabled = false
                                cfgNow.hunting.milestone.notifiedSellOrder = false
                                cfgNow.hunting.milestone.notifiedInstaSell = false
                                try { McManaged.save() } catch (_: Throwable) {}
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Milestone reset."))
                                1
                            })
                            .then(literal("open").executes {
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Opening milestone popup…"))
                                me.makogai.skydrunk.hud.MilestonePopupOpener.request()
                                1
                            })
                            .executes {
                                // Fallback: show current milestone status in chat
                                val cfgNow = McManaged.data()
                                val tgt = cfgNow.hunting.milestone.targetAmount.toLong()
                                val acc = cfgNow.hunting.milestone.accumulated.toLong()
                                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("§b[Skydrunk] Milestone: §f${fmtLong(acc)} §7/ §6${fmtLong(tgt)}"))
                                1
                            }
                        )
                )
            }

            register("skydrunk")
            register("sd") // alias
        }
    }
}
