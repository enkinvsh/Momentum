package com.akirahane.momentum.core.state.states.special;

import com.akirahane.momentum.core.MomentumUtils;
import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MomentumSound;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DodgeState extends BaseState {
	public static String DODGE_UP = "dodge_up";
	public static String DODGE_DOWN = "dodge_down";
	public static String DODGE_LEFT = "dodge_left";
	public static String DODGE_RIGHT = "dodge_right";

	public static boolean canDodge(Player player, PlayerMovementContext context) {
		if (!MomentumServerConfig.ENABLE_DODGE.getAsBoolean() || !MomentumClientConfig.ENABLE_DODGE.getAsBoolean()) {
			return false;
		}

		if (!player.onGround() && !context.isHasJetBooster() && !player.isSwimming()) {
			return false;
		}

		if (!player.isSwimming() && (player.getPose() != Pose.SWIMMING || MomentumUtils.canPlayerFitAtPose(player, Pose.CROUCHING))) {
			if (MomentumServerConfig.DODGE_COOLDOWN.get() * MomentumServerConfig.DODGE_STORAGE.get() - context.getDodgeCooldown()
				<= MomentumServerConfig.DODGE_COOLDOWN.get()) {
				return false;
			}

			boolean sprintDown = context.getMovementInput().sprint();
			boolean moveDown = context.getMovementInput().up()
				|| context.getMovementInput().left()
				|| context.getMovementInput().right()
				|| context.getMovementInput().down();
			boolean isTrue = false;
			if (MomentumClientConfig.ENABLE_DODGE_DIR_DOUBLE.getAsBoolean() && sprintDown) {
				PlatformServices.client().addHint(MovementHint.DODGE_DIR_DOUBLE);
				isTrue = context.isDoubleClickUp() || context.isDoubleClickDown() || context.isDoubleClickLeft() || context.isDoubleClickRight();
			}

			if (isTrue) {
				return true;
			}

			if (moveDown) {
				if (MomentumClientConfig.ENABLE_DODGE_SPRINT_DOUBLE.getAsBoolean()) {
					PlatformServices.client().addHint(MovementHint.DODGE_SPRINT_DOUBLE);
					isTrue = context.isDoubleClickSprint();
				}

				if (MomentumClientConfig.ENABLE_DODGE_SPRINT_CLICK.getAsBoolean()) {
					PlatformServices.client().addHint(MovementHint.DODGE_SPRINT_CLICK);
					isTrue = context.getInputBuffer()[context.getInputBufferIndex()].contains(5);
				}
			}

			return isTrue;
		} else {
			return false;
		}
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		float yRot = player.getYRot();
		String animationName;
		if (!player.level().isClientSide() || !PlatformServices.client().isLocalPlayer(player)) {
			int dir = context.getTransitionExtraData();
			switch (dir) {
				case 1:
					yRot += 180.0F;
					animationName = DODGE_DOWN;
					break;
				case 2:
					yRot -= 90.0F;
					animationName = DODGE_LEFT;
					break;
				case 3:
					yRot += 90.0F;
					animationName = DODGE_RIGHT;
					break;
				default:
					animationName = DODGE_UP;
			}
		} else if (context.getMovementInput().down()) {
			yRot += 180.0F;
			animationName = DODGE_DOWN;
			context.setTransitionExtraData(1);
		} else if (context.getMovementInput().left()) {
			yRot -= 90.0F;
			animationName = DODGE_LEFT;
			context.setTransitionExtraData(2);
		} else if (context.getMovementInput().right()) {
			yRot += 90.0F;
			animationName = DODGE_RIGHT;
			context.setTransitionExtraData(3);
		} else {
			animationName = DODGE_UP;
			context.setTransitionExtraData(0);
		}

		Vec3 direction = Vec3.directionFromRotation(4.0F, yRot);
		playStateAnimation(player, animationName, context, 4, 2.0F);
		if (!context.isHasJetBooster()) {
			player.setDeltaMovement(direction.x * 0.8, player.getDeltaMovement().y, direction.z * 0.8);
		} else {
			double currentSpeed = player.getDeltaMovement().horizontalDistance();
			double targetSpeed = direction.horizontalDistance() * 0.8;
			if (currentSpeed > targetSpeed) {
				Vec3 currentHorizontal = player.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
				Vec3 targetHorizontal = new Vec3(direction.x, 0.0, direction.z).normalize();
				double dot = currentHorizontal.dot(targetHorizontal);
				double factor = Math.max(0.0, dot);
				double finalSpeed = currentSpeed * factor;
				if (finalSpeed < targetSpeed) {
					finalSpeed = targetSpeed;
				}

				Vec3 normalized = new Vec3(direction.x, 0.3F, direction.z).normalize();
				player.setDeltaMovement(normalized.x * finalSpeed, normalized.y, normalized.z * finalSpeed);
			} else {
				player.setDeltaMovement(direction.x * 0.8, 0.3F, direction.z * 0.8);
			}

			player.fallDistance = 0.0;
		}

		context.setDodgeTimer(8);
		context.setDodgeCooldown(context.getDodgeCooldown() + MomentumServerConfig.DODGE_COOLDOWN.get());
		context.setNoJump(true);
		context.setNoMoveInput(true);
		context.addEffect(MomentumEffectType.BLOCK_FRICTION, context.DODGE_BLOCK_FRICTION, 3);
		if (context.isHasJetBooster()) {
			PlatformServices.client().playSound(player, MomentumSound.JET2, 1.0F, 1.0F + player.getRandom().nextFloat() * 0.4F - 0.2F);
		} else {
			player.playSound(SoundEvents.ARROW_SHOOT, 0.5F, 1.0F + player.getRandom().nextFloat() * 0.4F - 0.2F);
		}

		context.setMomentumRollIntensity(20.0F);
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		if (context.getDodgeTimer() < 6) {
			context.setMomentumRollIntensity(0.0F);
		}

		super.clientTick(player, context);
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		super.clientTickRemote(player, context);
		if (context.getDodgeTimer() == 6 && !player.onGround() && !player.isSwimming()) {
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
			LOGGER.info("context.getCurrentAnimationName(){}", context.getCurrentAnimationName());
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.setNoJump(false);
		context.setNoMoveInput(false);
		context.removeEffect(MomentumEffectType.BLOCK_FRICTION, context.DODGE_BLOCK_FRICTION);
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(DEFAULT_CHAIN, StateType.DODGE, (p, c) -> c.getDodgeTimer() > 0);
	}

	@Override
	public StateType getStateType() {
		return StateType.DODGE;
	}
}
