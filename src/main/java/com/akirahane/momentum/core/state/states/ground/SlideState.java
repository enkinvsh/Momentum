package com.akirahane.momentum.core.state.states.ground;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffect;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

public class SlideState extends BaseState {
	public static String SLIDE = "slide";
	protected int JUMP_DECELERATION_WINDOW = 5;

	public static boolean canSlide(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_SLIDE.getAsBoolean()
			&& MomentumClientConfig.ENABLE_SLIDE.getAsBoolean()
			&& player.onGround()
			&& canSlideSpeedCheck(player, context)
			&& checkKey(player, context);
	}

	public static boolean canSlideSpeedCheck(Player player, PlayerMovementContext context) {
		return context.getSpeed().horizontalDistance() * 20.0
				> (player.isSprinting() ? MomentumServerConfig.MIN_SLIDE_SPEED.get() : MomentumServerConfig.MIN_SLIDE_SPEED.get() * 2.0)
			&& (
				context.getPendingEffectPool().get(MomentumEffectType.ACCELERATION).contains(context.SLIDE_ACCELERATION)
					|| context.getOldSpeed().horizontalDistance() >= -context.getOldSpeed().y
			);
	}

	public static boolean canSlideHold(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_SLIDE.getAsBoolean()
			&& MomentumClientConfig.ENABLE_SLIDE.getAsBoolean()
			&& player.onGround()
			&& canSlideHoldSpeedCheck(player, context)
			&& checkKey(player, context);
	}

	public static boolean canSlideHoldSpeedCheck(Player player, PlayerMovementContext context) {
		return context.getSpeed().horizontalDistance() * 20.0 > MomentumServerConfig.MIN_SLIDE_SPEED.get()
			&& (
				context.getPendingEffectPool().get(MomentumEffectType.ACCELERATION).contains(context.SLIDE_ACCELERATION)
					|| context.getOldSpeed().horizontalDistance() >= -context.getOldSpeed().y
			);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.SLIDE);
		return context.getMovementInput().lower();
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		PlatformServices.gameplay().setForcedPose(player, Pose.SWIMMING);
		context.setNoMoveInput(true);
		context.addPermanentEffect(MomentumEffectType.FRICTION, context.SLIDE_FRICTION);
		context.addEffect(MomentumEffectType.BLOCK_FRICTION, context.SLIDE_BLOCK_FRICTION, this.JUMP_DECELERATION_WINDOW);
		Vec3 velocity = player.getDeltaMovement();
		float jumpPower = PlatformServices.gameplay().jumpPower(player);
		LOGGER.trace("player.getJumpPower() {}", jumpPower);
		int slideAccelerationCooldown = MomentumServerConfig.SLIDE_ACCELERATION_COOLDOWN.get();
		if (context.getSlideCooldown() != 0) {
			jumpPower *= (float)(slideAccelerationCooldown - context.getSlideCooldown()) / slideAccelerationCooldown;
		}

		if (context.getSpeed().horizontalDistance() >= context.getJumpLimitSpeed()) {
			jumpPower = 0.0F;
		}

		player.addDeltaMovement(new Vec3(velocity.x * jumpPower / velocity.horizontalDistance(), 0.0, velocity.z * jumpPower / velocity.horizontalDistance()));
		playStateAnimation(player, SLIDE, context, 6, 1.0F);
		context.setMomentumRollIntensity(15.0F);
		context.setTargetArmTransform(0.15F, -15.0F);
		context.setSlideCooldown(slideAccelerationCooldown);
		player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.3F, 0.5F);
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		super.clientTick(player, context);
		if (!context.isSlideJumpCooldownJustFinished()
			&& this.JUMP_DECELERATION_WINDOW < MomentumServerConfig.SLIDE_ACCELERATION_COOLDOWN.get() - context.getSlideCooldown()) {
			context.setTargetArmTransform(0.35F, -30.0F);
			context.setSlideJumpCooldownJustFinished(true);
		}
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		if (player.tickCount % 2 == 0) {
			player.playSound(SoundType.GRASS.getStepSound(), 0.05F, 1.0F);
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		Vec3 slopeDir = context.getSlopeUnitVector();
		if (Vec3.ZERO.equals(slopeDir) && context.getBlockStep() > 0.0) {
			double riseHeight = context.getBlockStep();
			float deceleration = (float)(0.12F * Math.min(riseHeight * riseHeight, 1.0));
			context.SLIDE_ACCELERATION
				.setValue(new Vec3(slopeDir.x * deceleration, context.getBlockStep() * player.getDeltaMovement().horizontalDistance() * 0.5, slopeDir.z * deceleration));
			context.addEffect(MomentumEffectType.ACCELERATION, context.SLIDE_ACCELERATION, 1);
			context.setBlockStep(0.0);
		}

		if (this.JUMP_DECELERATION_WINDOW >= MomentumServerConfig.SLIDE_ACCELERATION_COOLDOWN.get() - context.getSlideCooldown()) {
			context.addEffect(MomentumEffectType.FRICTION, new MomentumEffect(new Vec3(0.05, 0.0, 0.0), Vec3.ZERO, MomentumEffect.EffectType.LOCAL_VALUE, 5), 5);
		}

		PlatformServices.gameplay().setForcedPose(player, null);
		context.setNoMoveInput(false);
		context.removeEffect(MomentumEffectType.FRICTION, context.SLIDE_FRICTION);
		context.removeEffect(MomentumEffectType.BLOCK_FRICTION, context.SLIDE_BLOCK_FRICTION);
		context.setSlopeUnitVector(Vec3.ZERO);
		context.setSlideCooldown(MomentumServerConfig.SLIDE_ACCELERATION_COOLDOWN.get());
		context.setMomentumRollIntensity(0.0F);
		context.setSlideJumpCooldownJustFinished(false);
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(DEFAULT_CHAIN, StateType.SLIDE, SlideState::canSlideHold);
	}

	@Override
	public StateType getStateType() {
		return StateType.SLIDE;
	}
}
