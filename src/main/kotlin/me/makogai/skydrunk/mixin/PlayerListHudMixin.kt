package me.makogai.skydrunk.mixin

import me.makogai.skydrunk.detect.LocationTrackerTab
import net.minecraft.client.gui.hud.PlayerListHud
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Hooks PlayerListHud#setHeaderAndFooter(Text header, Text footer)
 * and forwards the values to our LocationTrackerTab.
 */
@Mixin(PlayerListHud::class)
abstract class PlayerListHudMixin {

    // setHeaderAndFooter(Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;)V
    @Inject(
        method = ["setHeaderAndFooter(Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;)V"],
        at = [At("TAIL")]
    )
    private fun skydrunk_onHeaderFooter(header: Text?, footer: Text?, ci: CallbackInfo) {
        me.makogai.skydrunk.detect.LocationTrackerTab.onHeaderFooter(header, footer)
    }
}

