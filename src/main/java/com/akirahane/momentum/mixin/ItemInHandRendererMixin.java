package com.akirahane.momentum.mixin;

import com.akirahane.momentum.client.ClientVisualEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(
		method = "submitHandsWithItems", // 26.2 port: ItemInHandRenderer.renderHandsWithItems переименован в submitHandsWithItems (та же сигнатура)
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 1, shift = Shift.AFTER)
	)
	private void momentum$transformHands(
		float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int light, CallbackInfo ci
	) {
		ClientVisualEffects.transformHands(player, poseStack, partialTick);
	}
}
