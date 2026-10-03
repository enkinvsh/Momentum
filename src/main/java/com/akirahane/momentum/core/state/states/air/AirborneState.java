package com.akirahane.momentum.core.state.states.air;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.core.state.states.ground.SlideState;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class AirborneState extends BaseState {
	public static boolean canAirborne(Player player, PlayerMovementContext context) {
		return !player.onGround();
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		context.setLuckyNumber(player);
		context.addPermanentEffect(MomentumEffectType.LIMIT_ACCELERATION_SPEED, PlayerMovementContext.AIR_LIMIT_ACCELERATION);
		double airXz = MomentumServerConfig.AIR_ACCELERATION_XZ.get();
		context.AIR_ACCELERATION.setValue(new Vec3(airXz, 1.0, airXz));
		context.addPermanentEffect(MomentumEffectType.ACCELERATION, context.AIR_ACCELERATION);
		context.setJumpAnimationSpeed(1.0F);
		context.setMomentumRollIntensity(8.0F);
		if (!BaseState.JUMP_RIGHT.equals(context.getCurrentAnimationName())
			&& !BaseState.JUMP_LEFT.equals(context.getCurrentAnimationName())
			&& !BaseState.BACK_JUMP_RIGHT.equals(context.getCurrentAnimationName())
			&& !BaseState.BACK_JUMP_LEFT.equals(context.getCurrentAnimationName())
			&& !BaseState.FALL.equals(context.getCurrentAnimationName())) {
			if (!SlideState.SLIDE.equals(context.getCurrentAnimationName()) || !(context.getSpeed().y < 0.0)) {
				if (context.getSpeed().horizontalDistance() > 0.1F) {
					float yaw = player.getYRot();
					Vec3 lookVec = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw))).normalize();
					Vec3 motionDirection = context.getSpeed().normalize();
					boolean isBackwardJump = lookVec.x * motionDirection.x + lookVec.z * motionDirection.z < 0.0;
					if (context.isLeftFootJump()) {
						playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_LEFT : BaseState.JUMP_LEFT, context);
					} else {
						playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_RIGHT : BaseState.JUMP_RIGHT, context);
					}

					context.setLeftFootJump(!context.isLeftFootJump());
				} else {
					super.onEnter(player, context);
				}
			}
		}
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		if (player.isInWater()) {
			playStateAnimation(player, this.IDLE, context);
		} else if (context.getSpeed().y < -1.0) {
			float speed = (float)(Math.clamp(-context.getSpeed().y / 2.5, 0.0, 1.5) + 0.5);
			playStateAnimation(player, FALL, context, 20, speed);
		} else {
			if (context.getSpeed().y > 0.0
				&& context.getSpeed().horizontalDistance() > 0.1F
				&& !BaseState.JUMP_RIGHT.equals(context.getCurrentAnimationName())
				&& !BaseState.JUMP_LEFT.equals(context.getCurrentAnimationName())
				&& !BaseState.BACK_JUMP_RIGHT.equals(context.getCurrentAnimationName())
				&& !BaseState.BACK_JUMP_LEFT.equals(context.getCurrentAnimationName())) {
				float yaw = player.getYRot();
				Vec3 lookVec = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw))).normalize();
				Vec3 motionDirection = context.getSpeed().normalize();
				boolean isBackwardJump = lookVec.x * motionDirection.x + lookVec.z * motionDirection.z < 0.0;
				if (context.isLeftFootJump()) {
					playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_LEFT : BaseState.JUMP_LEFT, context);
				} else {
					playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_RIGHT : BaseState.JUMP_RIGHT, context);
				}

				context.setLeftFootJump(!context.isLeftFootJump());
				return;
			}

			if (FALL.equals(context.getCurrentAnimationName())) {
				playStateAnimation(player, this.IDLE, context);
			}
		}

		if (BaseState.JUMP_RIGHT.equals(context.getCurrentAnimationName())
			|| BaseState.JUMP_LEFT.equals(context.getCurrentAnimationName())
			|| BaseState.BACK_JUMP_RIGHT.equals(context.getCurrentAnimationName())
			|| BaseState.BACK_JUMP_LEFT.equals(context.getCurrentAnimationName())) {
			if (context.getSpeed().y > 0.0) {
				context.setJumpAnimationSpeed(context.getJumpAnimationSpeed() * 0.9F);
			} else {
				context.setJumpAnimationSpeed(context.getJumpAnimationSpeed() * 1.1F);
			}

			playStateAnimation(player, context.getCurrentAnimationName(), context, 0, context.getJumpAnimationSpeed());
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.setMomentumRollIntensity(0.0F);
		context.setJumpAnimationSpeed(1.0F);
		context.removeEffect(MomentumEffectType.LIMIT_ACCELERATION_SPEED, PlayerMovementContext.AIR_LIMIT_ACCELERATION);
		context.removeEffect(MomentumEffectType.ACCELERATION, context.AIR_ACCELERATION);
	}

	@Override
	public StateType getStateType() {
		return StateType.AIRBORNE;
	}
}
