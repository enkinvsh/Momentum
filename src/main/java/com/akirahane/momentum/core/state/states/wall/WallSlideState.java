package com.akirahane.momentum.core.state.states.wall;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

public class WallSlideState extends BaseState {
	public static String WALL_SLIDE = "wall_slide";

	public static boolean canWallSlide(Player player, PlayerMovementContext context) {
		if (MomentumServerConfig.ENABLE_WALL_SLIDE.getAsBoolean() && MomentumClientConfig.ENABLE_WALL_SLIDE.getAsBoolean()) {
			if (player.onClimbable()) {
				PlatformServices.client().addHint(MovementHint.CLIMB_ACCELERATION);
			}

			return !player.onGround()
				&& !Vec3.ZERO.equals(context.getWallNormal())
				&& context.getSpeed().y < 0.0
				&& Mth.abs(context.getLookWallAngle()) < 30.0F
				&& (player.onClimbable() || !Vec3.ZERO.equals(context.getInputVec()) && Mth.abs(context.getInputWallAngle()) < 30.0F || checkKey(player, context));
		} else {
			return false;
		}
	}

	public static boolean canWallSlideHold(Player player, PlayerMovementContext context) {
		if (MomentumServerConfig.ENABLE_WALL_SLIDE.getAsBoolean() && MomentumClientConfig.ENABLE_WALL_SLIDE.getAsBoolean()) {
			if (player.onClimbable()) {
				PlatformServices.client().addHint(MovementHint.CLIMB_ACCELERATION);
			}

			return !player.onGround()
				&& !Vec3.ZERO.equals(context.getWallNormal())
				&& Mth.abs(context.getLookWallAngle()) < 60.0F
				&& (player.onClimbable() || !Vec3.ZERO.equals(context.getInputVec()) && Mth.abs(context.getInputWallAngle()) < 60.0F || checkKey(player, context));
		} else {
			return false;
		}
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.WALL_SLIDE);
		return context.getMovementInput().jump();
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(
			withPredicate(DEFAULT_CHAIN, StateType.WALL_CLIMB, WallClimbState::canWallClimbHold), StateType.WALL_SLIDE, WallSlideState::canWallSlideHold
		);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		if (player.level().isClientSide() && PlatformServices.client().isLocalPlayer(player)) {
			int wallIndex = PlayerMovementContext.encodeWallNormal(context.getWallNormal());
			context.setTransitionWallData((byte)(wallIndex >= 0 ? wallIndex : -1));
		}

		context.addPermanentEffect(MomentumEffectType.LIMIT_ACCELERATION_SPEED, PlayerMovementContext.AIR_LIMIT_ACCELERATION);
		playStateAnimation(player, WALL_SLIDE, context, 4, 1.0F);
		context.setTargetArmTransform(-0.15F, 5.0F);
	}

	@Override
	public void serverTick(Player player, PlayerMovementContext context) {
		super.serverTick(player, context);
		player.fallDistance *= 0.9;
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		super.clientTick(player, context);
		player.fallDistance *= 0.9;
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (context.getSpeed().y > 0.0 && instance != null) {
			instance.removeModifier(WALL_GRAVITY_ID);
		} else if (instance != null && instance.getModifier(WALL_GRAVITY_ID) == null) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(WALL_GRAVITY_ID, -0.9, Operation.ADD_MULTIPLIED_BASE));
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
		context.removeEffect(MomentumEffectType.LIMIT_ACCELERATION_SPEED, PlayerMovementContext.AIR_LIMIT_ACCELERATION);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			instance.removeModifier(WALL_GRAVITY_ID);
		}
	}

	@Override
	public StateType getStateType() {
		return StateType.WALL_SLIDE;
	}
}
