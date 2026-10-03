package com.akirahane.momentum.core.state.states.wall;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class VaultInState extends BaseState {
	public static final String VAULT_IN = "vault_in";

	public static boolean canVaultIn(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_VAULT_IN.getAsBoolean()
			&& MomentumClientConfig.ENABLE_VAULT_IN.getAsBoolean()
			&& (context.isHasLedge() || player.onGround() && !context.isHasFaceWall())
			&& !Vec3.ZERO.equals(context.getInputVec())
			&& Mth.abs(context.getInputWallAngle()) < 90.0F
			&& checkKey(player, context);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		if (player.onGround() && !context.isHasFaceWall()) {
			PlatformServices.client().addHint(MovementHint.VAULT_IN_STAND);
			return context.getMovementInput().up() && context.getMovementInput().lower();
		} else {
			PlatformServices.client().addHint(MovementHint.VAULT_IN);
			return context.getMovementInput().lower() && context.getMovementInput().jump();
		}
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		PlatformServices.gameplay().setForcedPose(player, Pose.SWIMMING);
		context.setVaultTimer(10);
		context.setNoJump(true);
		if (player.onGround()) {
			AttributeInstance instance = player.getAttribute(Attributes.STEP_HEIGHT);
			if (instance != null && instance.getModifier(UP_SLOPE_ID) == null) {
				instance.addOrReplacePermanentModifier(new AttributeModifier(UP_SLOPE_ID, 0.6, Operation.ADD_VALUE));
			}
		} else {
			player.setDeltaMovement(player.getDeltaMovement().x, 0.6, player.getDeltaMovement().z);
		}

		playStateAnimation(player, "vault_in", context, 2, 1.5F);
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		AttributeInstance instance = player.getAttribute(Attributes.STEP_HEIGHT);
		if (instance != null) {
			instance.removeModifier(UP_SLOPE_ID);
		}

		PlatformServices.gameplay().setForcedPose(player, null);
		context.setNoJump(false);
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return moveAfter(without(DEFAULT_CHAIN, StateType.VAULT_UP), StateType.VAULT_IN, StateType.SWIM_DASH, (p, c) -> c.getVaultTimer() > 0);
	}

	@Override
	public StateType getStateType() {
		return StateType.VAULT_IN;
	}
}
