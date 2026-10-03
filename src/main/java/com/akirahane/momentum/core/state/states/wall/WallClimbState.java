package com.akirahane.momentum.core.state.states.wall;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class WallClimbState extends BaseState {
	public static String WALL_CLIMB = "wall_climb";
	private static final int SOUND_TICK = 10;

	public static boolean canWallClimb(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_CLIMB.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_CLIMB.getAsBoolean()
			&& !context.getMovementInput().shift()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& context.isHasFaceWall()
			&& !Vec3.ZERO.equals(context.getInputVec())
			&& Mth.abs(context.getInputWallAngle()) < 30.0F
			&& Mth.abs(context.getLookWallAngle()) < 30.0F
			&& (context.getSpeed().y >= 0.0 || context.isHasJetBooster())
			&& checkKey(player, context);
	}

	public static boolean canWallClimbHold(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_CLIMB.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_CLIMB.getAsBoolean()
			&& !context.getMovementInput().shift()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& context.isHasFaceWall()
			&& !Vec3.ZERO.equals(context.getInputVec())
			&& Mth.abs(context.getInputWallAngle()) < 60.0F
			&& Mth.abs(context.getLookWallAngle()) < 60.0F
			&& (context.getSpeed().y >= 0.0 || context.isHasJetBooster())
			&& checkKey(player, context);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		if (!player.onClimbable()) {
			PlatformServices.client().addHint(MovementHint.WALL_CLIMB);
			return context.getMovementInput().jump();
		} else {
			PlatformServices.client().addHint(MovementHint.CLIMB_ACCELERATION);
			return true;
		}
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

		playStateAnimation(player, WALL_CLIMB, context);
		if (!player.onClimbable()) {
			context.addPermanentEffect(MomentumEffectType.LIMIT_ACCELERATION_SPEED, PlayerMovementContext.AIR_LIMIT_ACCELERATION);
			AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
			if (instance != null && instance.getModifier(WALL_GRAVITY_ID) == null) {
				instance.addOrReplacePermanentModifier(new AttributeModifier(WALL_GRAVITY_ID, -0.8, Operation.ADD_MULTIPLIED_BASE));
			}

			player.addDeltaMovement(new Vec3(0.0, player.getDeltaMovement().y * 0.2, 0.0));
		}

		context.setNeedSoundTick(10.0F);
		context.playWallSound(player, PlayerMovementContext.STEP, 0.15F, 1.0F);
		context.setTargetArmTransform(0.15F, -20.0F);
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		super.clientTick(player, context);
		if (context.isHasJetBooster() && !player.onClimbable()) {
			player.setDeltaMovement(player.getDeltaMovement().x, Math.max(0.3, player.getDeltaMovement().y), player.getDeltaMovement().z);
		}
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		float speed = (float)Math.min(context.getSpeed().y * 6.0, 5.0);
		playStateAnimation(player, WALL_CLIMB, context, 0, speed);
		context.setNeedSoundTick(context.getNeedSoundTick() - speed);
		if (context.getNeedSoundTick() <= 0.0F) {
			if (context.isHasJetBooster()) {
				PlatformServices.client().playSound(player, MomentumSound.JET3, 0.2F, 1.5F + player.getRandom().nextFloat() * 0.4F - 0.2F);
			}

			context.playWallSound(player, PlayerMovementContext.STEP, 0.15F, 1.0F);
			context.setNeedSoundTick(context.getNeedSoundTick() + 10.0F);
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
		return StateType.WALL_CLIMB;
	}
}
