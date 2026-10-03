package com.akirahane.momentum.core.state.states.wall;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class VaultUpState extends BaseState {
	public static boolean canVaultUp(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_VAULT_UP.getAsBoolean()
			&& MomentumClientConfig.ENABLE_VAULT_UP.getAsBoolean()
			&& context.isHasLedge()
			&& checkKey(player, context);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.VAULT_UP);
		return context.getInputBuffer()[context.getInputBufferIndex()].contains(4);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		super.onEnter(player, context);
		context.setVaultTimer(6);
		player.setDeltaMovement(0.0, 0.6, 0.0);
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		PlatformServices.gameplay().setForcedPose(player, null);
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return moveAfter(without(DEFAULT_CHAIN, StateType.VAULT_IN), StateType.VAULT_UP, StateType.SWIM_DASH, (p, c) -> c.getVaultTimer() > 0);
	}

	@Override
	public StateType getStateType() {
		return StateType.VAULT_UP;
	}
}
