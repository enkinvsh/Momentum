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
import net.minecraft.world.phys.Vec3;

public class WallHangState extends BaseState {
	private static final int SOUND_TICK = 10;
	public static final String WALL_HANG = "wall_hang";
	public static final String WALL_HANG_LOOK_RIGHT = "wall_hang_look_right";
	public static final String WALL_HANG_LOOK_LEFT = "wall_hang_look_left";
	public static final String WALL_HANG_RIGHT = "wall_hang_right";
	public static final String WALL_HANG_LEFT = "wall_hang_left";

	public static boolean canWallHang(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_HANG.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_HANG.getAsBoolean()
			&& !player.onGround()
			&& context.isHasLedge()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& (!context.getMovementInput().lower() || Mth.abs(context.getInputWallAngle()) < 90.0F)
			&& player.fallDistance <= player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) * 2.0
			&& player.getDeltaMovement().y <= 0.1
			&& !checkKey(player, context);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.WALL_HANG);
		return context.getMovementInput().shift();
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(
			DEFAULT_CHAIN,
			StateType.WALL_HANG,
			(p, c) -> (!c.getMovementInput().lower() || Mth.abs(c.getInputWallAngle()) < 90.0F)
				&& c.isHasLedge()
				&& !Vec3.ZERO.equals(c.getWallNormal())
				&& !checkKey(p, c)
		);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		if (player.level().isClientSide() && PlatformServices.client().isLocalPlayer(player)) {
			int wallIndex = PlayerMovementContext.encodeWallNormal(context.getWallNormal());
			context.setTransitionWallData((byte)(wallIndex >= 0 ? wallIndex : -1));
		}

		playStateAnimation(player, "wall_hang", context, 1, 1.0F);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null && instance.getModifier(WALL_GRAVITY_ID) == null) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(WALL_GRAVITY_ID, -1.0, Operation.ADD_MULTIPLIED_BASE));
		}

		if (context.getLedgeOffsetY() > 0.0) {
			player.setPos(player.getX(), player.getY() + context.getLedgeOffsetY(), player.getZ());
			context.setLedgeOffsetY(0.0);
		}

		player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
		player.fallDistance = 0.0;
		context.addPermanentEffect(MomentumEffectType.FRICTION, PlayerMovementContext.WALL_FRICTION);
		context.setNeedSoundTick(10.0F);
		context.playWallSound(player, PlayerMovementContext.STEP, 0.15F, 1.0F);
		context.setTargetArmTransform(0.15F, -10.0F);
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		super.clientTick(player, context);
		if (!context.getMovementInput().down()
			&& (!(Mth.abs(context.getInputWallAngle()) > 150.0F) || !(Mth.abs(context.getInputWallAngle()) < 360.0F) || !(Mth.abs(context.getLookWallAngle()) > 150.0F))
			)
		 {
			Vec3 wallNormal = context.getWallNormal();
			Vec3 currentMovement = player.getDeltaMovement();
			double pushStrength = 0.05;
			player.setDeltaMovement(currentMovement.x + wallNormal.x * pushStrength, currentMovement.y, currentMovement.z + wallNormal.z * pushStrength);
		}
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		float speed = (float)context.getSpeed().horizontalDistance() * 20.0F;
		if (speed > 0.05) {
			if (context.getInputWallAngle() < 0.0F) {
				playStateAnimation(player, "wall_hang_right", context, 2, speed);
			} else {
				playStateAnimation(player, "wall_hang_left", context, 2, speed);
			}

			context.setNeedSoundTick(context.getNeedSoundTick() - speed);
			if (context.getNeedSoundTick() <= 0.0F) {
				context.playWallSound(player, PlayerMovementContext.STEP, 0.15F, 1.0F);
				context.setNeedSoundTick(context.getNeedSoundTick() + 10.0F);
			}
		} else {
			context.setNeedSoundTick(10.0F);
			if (Mth.abs(context.getLookWallAngle()) < 45.0F) {
				playStateAnimation(player, "wall_hang", context, 2, 1.0F);
			} else if (context.getLookWallAngle() < -70.0F) {
				if ("wall_hang_look_left".equals(context.getCurrentAnimationName())) {
					playStateAnimation(player, "wall_hang", context, 4, 1.0F);
				} else {
					playStateAnimation(player, "wall_hang_look_right", context, 4, 1.0F);
				}
			} else if (context.getLookWallAngle() > 70.0F) {
				if ("wall_hang_look_right".equals(context.getCurrentAnimationName())) {
					playStateAnimation(player, "wall_hang", context, 4, 1.0F);
				} else {
					playStateAnimation(player, "wall_hang_look_left", context, 4, 1.0F);
				}
			}
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			instance.removeModifier(WALL_GRAVITY_ID);
		}

		context.removeEffect(MomentumEffectType.FRICTION, PlayerMovementContext.WALL_FRICTION);
	}

	@Override
	public StateType getStateType() {
		return StateType.WALL_HANG;
	}
}
