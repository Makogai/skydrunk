package me.makogai.skydrunk.mixin;

import me.makogai.skydrunk.config.McManaged;
import me.makogai.skydrunk.config.McRoot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static boolean SKD_loggedOnce = false;

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void skydrunk$scaleSwingDuration(CallbackInfoReturnable<Integer> cir) {
        // Only affect the local client player to avoid touching mobs/NPCs
        LivingEntity self = (LivingEntity) (Object) this;
        var mc = MinecraftClient.getInstance();
        if (!(self instanceof AbstractClientPlayerEntity) || mc.player != self) return;

        McRoot root = McManaged.INSTANCE.data();
        var vm = root.getViewmodel();

        float speed = Math.max(0.01f, vm.getSwingSpeed()); // allow VERY slow (down to 1% speed)
        if (speed == 1f) return;

        int base = cir.getReturnValueI();
        // Slower speed -> larger duration
        int newDur = Math.max(1, Math.round(base * (1f / speed)));
        cir.setReturnValue(newDur);

        if (!SKD_loggedOnce) {
            System.out.println("[Skydrunk] Swing duration " + base + " -> " + newDur + " (speed=" + speed + ")");
            SKD_loggedOnce = true;
        }
    }
}
