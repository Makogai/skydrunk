package me.makogai.skydrunk.mixin;

import me.makogai.skydrunk.config.McManaged;
import me.makogai.skydrunk.config.McRoot;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void skydrunk$head(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                               float swingProgress, ItemStack stack, float equipProgress,
                               MatrixStack matrices, VertexConsumerProvider providers, int light,
                               CallbackInfo ci) {
        if (hand == Hand.MAIN_HAND) {
            System.out.println("[Skydrunk] HeldItemRenderer HEAD fired: " + stack);
        }
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(" +
                            "Lnet/minecraft/entity/LivingEntity;" +
                            "Lnet/minecraft/item/ItemStack;" +
                            "Lnet/minecraft/item/ItemDisplayContext;" +
                            "Lnet/minecraft/client/util/math/MatrixStack;" +
                            "Lnet/minecraft/client/render/VertexConsumerProvider;" +
                            "I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void skydrunk$apply(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                float swingProgress, ItemStack stack, float equipProgress,
                                MatrixStack matrices, VertexConsumerProvider providers, int light,
                                CallbackInfo ci) {

        McRoot root = McManaged.INSTANCE.data();
        var vm = root.getViewmodel();

        if (!vm.getEnabled()) return;
        if (hand != Hand.MAIN_HAND && !vm.getAffectOffHand()) return;

        if (vm.getRotX() != 0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(vm.getRotX()));
        if (vm.getRotY() != 0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(vm.getRotY()));
        if (vm.getRotZ() != 0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(vm.getRotZ()));

        float s = Math.max(0.01f, vm.getScale());
        if (s != 1f) matrices.scale(s, s, s);

        if (vm.getOffX() != 0f || vm.getOffY() != 0f || vm.getOffZ() != 0f) {
            matrices.translate(vm.getOffX(), vm.getOffY(), vm.getOffZ());
        }
    }

    // Equip animation speed: scales the 0.4F step constant in updateHeldItems
    private static boolean SKD_loggedEquip = false;

    // Equip animation speed: scales the 0.4F step constant in updateHeldItems
    @ModifyConstant(method = "updateHeldItems", constant = @Constant(floatValue = 0.4F))
    private float skydrunk$scaleEquipStep(float base) {
        var vm = McManaged.INSTANCE.data().getViewmodel();
        float factor = Math.max(0.01f, vm.getEquipSpeed()); // was 0.05f; now allow 0.01f
        return base * factor;
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float skydrunk$scaleSwingAmplitude(float swingProgress) {
        var vm = McManaged.INSTANCE.data().getViewmodel();
        float amp = vm.getSwingAmplitude();
        if (amp == 1f) return swingProgress;
        float v = swingProgress * Math.max(0f, amp);
        return Math.max(0f, Math.min(1f, v));
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float skydrunk$scaleEquipAmplitude(float equipProgress) {
        var vm = McManaged.INSTANCE.data().getViewmodel();
        float amp = vm.getEquipAmplitude();
        if (amp == 1f) return equipProgress;
        float v = equipProgress * Math.max(0f, amp);
        return Math.max(0f, Math.min(1f, v));
    }

//    // Swing animation speed (visual): scale swingProgress parameter
//    // Parameters of type float in this method are: tickDelta(0), pitch(1), swingProgress(2), equipProgress(3)
//    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
//    private float skydrunk$scaleSwingParam(float swingProgress) {
//        var vm = McManaged.INSTANCE.data().getViewmodel();
//        float factor = Math.max(0.05f, vm.getSwingSpeed());
//        if (factor == 1f) return swingProgress;
//
//        float out = swingProgress * factor; // >1 speeds up visually, <1 slows
//        if (out > 1f) out = 1f;
//        if (out < 0f) out = 0f;
//        return out;
//    }
}
