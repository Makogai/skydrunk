package me.makogai.skydrunk.util

import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object Debug {
    @Volatile var enabled = false

    fun chat(msg: String) {
        if (!enabled) return
        MinecraftClient.getInstance()?.inGameHud?.chatHud?.addMessage(Text.of("§8[SD-D] §7$msg"))
    }

    fun log(msg: String) {
        if (!enabled) return
        System.out.println("[Skydrunk/DEBUG] $msg")
    }

    private fun chatRaw(msg: String) {
        MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of(msg))
    }

    fun info(msg: String)  { if (enabled) chatRaw("§b[Skydrunk] §7$msg") }
    fun warn(msg: String)  { if (enabled) chatRaw("§e[Skydrunk] §6$msg") }
    fun error(msg: String) {             chatRaw("§c[Skydrunk] §c$msg") }
}
