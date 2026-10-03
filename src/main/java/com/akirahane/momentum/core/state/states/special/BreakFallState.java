package com.akirahane.momentum.core.state.states.special;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BreakFallState extends BaseState {
	public static String BREAK_FALL = "break_fall";

	public static boolean canBreakFall(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_BREAK_FALL.getAsBoolean() && MomentumClientConfig.ENABLE_BREAK_FALL.getAsBoolean() && context.isToBreakFallState();
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		context.setBreakFallTimer(12);
		PlatformServices.gameplay().setForcedPose(player, Pose.SWIMMING);
		context.setNoJump(true);
		context.setNoMoveInput(true);
		playStateAnimation(player, BREAK_FALL, context, 0, 2.0F);
		Vec3 direction = new Vec3(context.getSpeed().x, 0.0, context.getSpeed().z).normalize();
		if (Vec3.ZERO.equals(direction)) {
			direction = Vec3.directionFromRotation(0.0F, player.getYRot());
		}

		player.setDeltaMovement(direction.x, 0.0, direction.z);
		context.addEffect(MomentumEffectType.FRICTION, context.BREAK_FALL_FRICTION, 6);
		BlockPos below = player.blockPosition().below();
		BlockState state = player.level().getBlockState(below);
		if (!state.isAir()) {
			SoundType soundType = PlatformServices.gameplay().soundType(state, player.level(), below, player);
			player.playSound(soundType.getBreakSound(), soundType.getVolume(), soundType.getPitch() * 0.8F);
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.setNoJump(false);
		context.setNoMoveInput(false);
		PlatformServices.gameplay().setForcedPose(player, null);
		context.removeEffect(MomentumEffectType.FRICTION, context.BREAK_FALL_FRICTION);
		context.setBreakFallTimer(0);
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return moveAfter(DEFAULT_CHAIN, StateType.BREAK_FALL, StateType.DODGE, (p, c) -> c.getBreakFallTimer() > 0);
	}

	@Override
	public StateType getStateType() {
		return StateType.BREAK_FALL;
	}
}
