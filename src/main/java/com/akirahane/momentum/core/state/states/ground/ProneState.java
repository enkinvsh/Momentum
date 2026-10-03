package com.akirahane.momentum.core.state.states.ground;

import com.akirahane.momentum.core.MomentumUtils;
import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import java.util.List;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public class ProneState extends BaseState {
	public static boolean canProne(Player player, PlayerMovementContext context) {
		return player.getPose() == Pose.SWIMMING && !MomentumUtils.canPlayerFitAtPose(player, Pose.CROUCHING) || player.onGround() && checkKey(player, context);
	}

	public static boolean canProneHold(Player player, PlayerMovementContext context) {
		return player.getPose() == Pose.SWIMMING && !MomentumUtils.canPlayerFitAtPose(player, Pose.CROUCHING) || player.onGround() && checkKey(player, context);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		if (!SlideState.canSlideSpeedCheck(player, context) && !PlatformServices.client().containsHint(MovementHint.VAULT_IN_STAND)) {
			PlatformServices.client().addHint(MovementHint.PRONE);
		}

		return context.getMovementInput().lower();
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(without(DEFAULT_CHAIN, StateType.SLIDE), StateType.PRONE, ProneState::canProneHold);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		super.onEnter(player, context);
		PlatformServices.gameplay().setForcedPose(player, Pose.SWIMMING);
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		PlatformServices.gameplay().setForcedPose(player, null);
	}

	@Override
	public StateType getStateType() {
		return StateType.PRONE;
	}
}
