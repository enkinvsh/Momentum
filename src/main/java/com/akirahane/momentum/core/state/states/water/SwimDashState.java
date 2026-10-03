package com.akirahane.momentum.core.state.states.water;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.core.state.states.wall.WallClimbState;
import com.akirahane.momentum.core.state.states.wall.WallSlideState;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MomentumSound;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SwimDashState extends BaseState {
	public static String DOLPHIN_JUMP = "dolphin_jump";

	public static boolean canSwimDash(Player player, PlayerMovementContext context) {
		if (!MomentumServerConfig.ENABLE_WATER_PUSH.getAsBoolean() || !MomentumClientConfig.ENABLE_WATER_PUSH.getAsBoolean()) {
			return false;
		} else if (!player.isUnderWater()) {
			return false;
		} else if (MomentumServerConfig.DODGE_COOLDOWN.get() * MomentumServerConfig.DODGE_STORAGE.get() - context.getDodgeCooldown()
			<= MomentumServerConfig.DODGE_COOLDOWN.get()) {
			return false;
		} else if (player.isSwimming()) {
			PlatformServices.client().addHint(MovementHint.PUSH);
			return context.getInputBuffer()[context.getInputBufferIndex()].contains(5);
		} else {
			PlatformServices.client().addHint(MovementHint.PUSH_UP);
			return context.getMovementInput().up() && context.getInputBuffer()[context.getInputBufferIndex()].contains(5);
		}
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		List<BaseState.Transition> chain = withPredicate(
			DEFAULT_CHAIN, StateType.SWIM_DASH, (p, c) -> c.getSwimPushTimer() > 0 && (p.isUnderWater() || !p.onGround())
		);
		chain = moveAfter(chain, StateType.WALL_SLIDE, StateType.DODGE, WallSlideState::canWallSlide);
		return moveAfter(chain, StateType.WALL_CLIMB, StateType.DODGE, WallClimbState::canWallClimb);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		player.setSwimming(true);
		player.setSprinting(true);
		float yRot = player.getYRot();
		float xRot = player.getXRot();
		Vec3 direction = Vec3.directionFromRotation(xRot, yRot);
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

			Vec3 normalized = new Vec3(direction.x, !player.isSwimming() ? 0.3F : direction.y, direction.z).normalize();
			player.setDeltaMovement(normalized.x * finalSpeed, normalized.y, normalized.z * finalSpeed);
		} else {
			player.setDeltaMovement(direction.x * 0.8, !player.isSwimming() ? 0.3F : direction.y, direction.z * 0.8);
		}

		if (context.isHasJetBooster()) {
			PlatformServices.client().playSound(player, MomentumSound.JET2, 1.0F, 1.0F + player.getRandom().nextFloat() * 0.4F - 0.2F);
		}

		context.setSwimPushTimer(10);
		context.setDodgeCooldown(context.getDodgeCooldown() + MomentumServerConfig.DODGE_COOLDOWN.get());
		context.setNoJump(true);
		context.setNoMoveInput(true);
		player.playSound(SoundEvents.PLAYER_SWIM, 0.5F, 1.0F + player.getRandom().nextFloat() * 0.4F - 0.2F);
		context.setMomentumRollIntensity(15.0F);
		PlatformServices.gameplay().setForcedPose(player, Pose.SWIMMING);
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		if (!player.isInWater()) {
			context.setSwimPushTimer(10);
			AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
			if (instance != null && instance.getModifier(DOLPHIN_GRAVITY_ID) == null) {
				instance.addOrReplacePermanentModifier(new AttributeModifier(DOLPHIN_GRAVITY_ID, -0.4, Operation.ADD_MULTIPLIED_BASE));
			}
		}

		super.clientTick(player, context);
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		if (!player.isInWater()) {
			playStateAnimation(player, DOLPHIN_JUMP, context);
		} else if (!this.IDLE.equals(context.getCurrentAnimationName())) {
			playStateAnimation(player, this.IDLE, context);
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.setNoJump(false);
		context.setNoMoveInput(false);
		PlatformServices.gameplay().setForcedPose(player, null);
		player.setSwimming(false);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			instance.removeModifier(DOLPHIN_GRAVITY_ID);
		}
	}

	@Override
	public StateType getStateType() {
		return StateType.SWIM_DASH;
	}
}
