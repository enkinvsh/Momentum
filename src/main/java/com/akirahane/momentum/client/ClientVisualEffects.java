package com.akirahane.momentum.client;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;

public final class ClientVisualEffects {
	private ClientVisualEffects() {
	}

	public static float cameraRoll(LocalPlayer player, float partialTick) {
		if (MomentumClientConfig.ENABLE_CAMERA_OFFSET.get() && isMovementVisualActive(player)) {
			PlayerMovementContext context = PlatformServices.gameplay().movementState(player).getContext();
			return context.getRenderCameraRoll(partialTick) + context.getRenderMomentumRoll(partialTick);
		} else {
			return 0.0F;
		}
	}

	public static float fovBonus(LocalPlayer player, float partialTick) {
		return MomentumClientConfig.ENABLE_CAMERA_OFFSET.get() && isMovementVisualActive(player)
			? PlatformServices.gameplay().movementState(player).getContext().getRenderFovBonus(partialTick)
			: 0.0F;
	}

	public static void transformHands(LocalPlayer player, PoseStack poseStack, float partialTick) {
		if (isMovementVisualActive(player)) {
			PlayerMovementContext context = PlatformServices.gameplay().movementState(player).getContext();
			poseStack.translate(0.0, context.getRenderArmOffsetY(partialTick), 0.0);
			poseStack.mulPose(Axis.XP.rotationDegrees(context.getRenderArmRotX(partialTick)));
		}
	}

	private static boolean isMovementVisualActive(LocalPlayer player) {
		return player != null && PlatformServices.gameplay().hasMovementState(player)
			? PlatformServices.gameplay().movementState(player).getCurrentState().getStateType() != StateType.ORIGINAL
			: false;
	}
}
