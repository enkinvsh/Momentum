package com.akirahane.momentum.core.state.states.water;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public class SwimState extends BaseState {
	public static boolean canSwim(Player player, PlayerMovementContext context) {
		if (player.isSwimming()) {
			return true;
		}

		if (player.isUnderWater()) {
			if (player.isSprinting()) {
				PlatformServices.client().addHint(MovementHint.SWIM_HOLD);
				return context.getMovementInput().up();
			} else {
				PlatformServices.client().addHint(MovementHint.SWIM);
				return false;
			}
		} else {
			if (!player.isInWater()) {
				return false;
			}

			PlatformServices.client().addHint(MovementHint.SWIM_ACTIVE);
			return context.getMovementInput().lower() && context.getMovementInput().up();
		}
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return without(DEFAULT_CHAIN, StateType.SLIDE);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		super.onEnter(player, context);
		player.setSwimming(true);
		player.setSprinting(true);
		LOGGER.trace("player.isSwimming(): {}", player.isSwimming());
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
	}

	@Override
	public StateType getStateType() {
		return StateType.SWIM;
	}
}
