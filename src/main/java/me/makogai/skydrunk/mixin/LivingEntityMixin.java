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

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void skydrunk$scaleSwingDuration(CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        var mc = MinecraftClient.getInstance();

        // only the local client player
        if (!(self instanceof AbstractClientPlayerEntity) || mc.player != self) return;

        McRoot root = McManaged.INSTANCE.data();
        var vm = root.getViewmodel();
        float speed = Math.max(0.01f, vm.getSwingSpeed()); // <1 = slower, >1 = faster
        if (speed == 1f) return;

        int base = cir.getReturnValueI();
        cir.setReturnValue(Math.max(1, Math.round(base * (1f / speed))));
    }
}
