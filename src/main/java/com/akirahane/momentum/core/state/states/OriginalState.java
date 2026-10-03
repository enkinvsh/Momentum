package com.akirahane.momentum.core.state.states;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class OriginalState extends BaseState {
	public static boolean canOriginal(Player player, PlayerMovementContext context) {
		return !PlatformServices.gameplay().isMomentumEnabled(player)
			|| !context.isCanMomentum()
			|| player.getAbilities().flying
			|| player.isFallFlying()
			|| player.isPassenger()
			|| player.isSleeping()
			|| player.isSpectator()
			|| player.isAutoSpinAttack()
			|| player.isDeadOrDying()
			|| MomentumServerConfig.MANEUVER_CONSUME_HUNGER.get() && player.getFoodData().getFoodLevel() <= 6.0F;
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		context.resetEffect();
		playStateAnimation(player, this.IDLE, context);
		stopAnimation(player, context);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			instance.removeModifier(WALL_GRAVITY_ID);
		}

		instance = player.getAttribute(Attributes.STEP_HEIGHT);
		if (instance != null) {
			instance.removeModifier(UP_SLOPE_ID);
		}
	}

	@Override
	public void serverTick(Player player, PlayerMovementContext context) {
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.resetEffect();
		stopAnimation(player, context);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			instance.removeModifier(WALL_GRAVITY_ID);
		}

		instance = player.getAttribute(Attributes.STEP_HEIGHT);
		if (instance != null) {
			instance.removeModifier(UP_SLOPE_ID);
		}

		context.addPermanentEffect(MomentumEffectType.FRICTION, PlayerMovementContext.DEFAULT_FRICTION);
	}

	@Override
	public StateType getStateType() {
		return StateType.ORIGINAL;
	}
}
