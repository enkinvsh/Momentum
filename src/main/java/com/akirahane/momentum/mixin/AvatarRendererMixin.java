package com.akirahane.momentum.mixin;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.MovementStateMachine;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	public void momentum$modifyRenderState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		if (entity instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			PlayerMovementContext context = stateMachine.getContext();
			StateType currentState = stateMachine.getCurrentState().getStateType();
			if (!StateType.SWIM.equals(currentState)) {
				if (!StateType.PRONE.equals(currentState)) {
					if (!StateType.VAULT_IN.equals(currentState)) {
						if (StateType.SWIM_DASH.equals(currentState)) {
							if (context.getSpeed().y > 0.05) {
								state.isInWater = true;
							}
						} else {
							if (!StateType.ORIGINAL.equals(currentState)) {
								state.swimAmount = 0.0F;
							}
						}
					}
				}
			}
		}
	}
}
