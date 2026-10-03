package com.akirahane.momentum.core.state.states.wall;

import com.akirahane.momentum.core.context.PlayerMovementContext;
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

public class WallRunState extends BaseState {
	public static String WALL_RUN_LEFT = "wall_run_left";
	public static String WALL_RUN_RIGHT = "wall_run_right";
	private static final int SOUND_TICK = 10;
	private static final double WALL_PUSH_STRENGTH = 0.05;
	private static final double VERTICAL_SPEED_RETAINED_PER_TICK = 0.8;
	private static final double VERTICAL_SPEED_EPSILON = 0.01;

	public static boolean canWallRun(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_RUN.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_RUN.getAsBoolean()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& !Vec3.ZERO.equals(context.getInputVec())
			&& context.isHasFaceWall()
			&& isLookAndSpeedSameSide(player, context)
			&& Mth.abs(context.getLookWallAngle()) >= 30.0F
			&& Mth.abs(context.getInputWallAngle()) >= 30.0F
			&& Mth.abs(context.getInputWallAngle()) < 100.0F
			&& canWallRunSpeedCheck(player, context)
			&& checkKey(player, context)
			&& !player.onGround();
	}

	public static boolean canWallRunHold(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_RUN.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_RUN.getAsBoolean()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& context.getWallNormal().equals(context.getRunWallNormal())
			&& isLookAndSpeedSameSide(player, context)
			&& Mth.abs(context.getLookWallAngle()) >= 30.0F
			&& canWallRunSpeedCheck(player, context)
			&& checkKeyHold(player, context)
			&& !player.onGround();
	}

	public static boolean isLookAndSpeedSameSide(Player player, PlayerMovementContext context) {
		Vec3 wallNormal = context.getWallNormal();
		if (Vec3.ZERO.equals(wallNormal)) {
			return false;
		}

		Vec3 speed = player.getDeltaMovement();
		if (speed.horizontalDistanceSqr() < 1.0E-4) {
			return true;
		}

		float yaw = player.getYRot();
		Vec3 lookVec = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw)));
		Vec3 tangent = new Vec3(-wallNormal.z, 0.0, wallNormal.x);
		double speedDot = speed.x * tangent.x + speed.z * tangent.z;
		double lookDot = lookVec.x * tangent.x + lookVec.z * tangent.z;
		double speedTangentNorm = speedDot / Math.max(speed.horizontalDistance(), 0.001);
		return speedTangentNorm * lookDot > -0.4;
	}

	public static boolean canWallRunSpeedCheck(Player player, PlayerMovementContext context) {
		boolean hasReentryGrace = context.getWallRunReentryGraceTicks() > 0;
		double horizontalSpeed = context.getSpeed().horizontalDistance();
		if (hasReentryGrace) {
			horizontalSpeed = Math.max(horizontalSpeed, player.getDeltaMovement().horizontalDistance());
		}

		double minimumSpeed = MomentumServerConfig.MIN_WALL_RUN_SPEED.get() * (hasReentryGrace ? 0.75 : 1.0);
		return horizontalSpeed * 20.0 > minimumSpeed && (hasReentryGrace || context.isHasJetBooster() || horizontalSpeed > (float)(-context.getSpeed().y));
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.WALL_RUN);
		return context.getMovementInput().up() && context.getMovementInput().jump();
	}

	public static boolean checkKeyHold(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.WALL_RUN_HOLD);
		return context.getMovementInput().up();
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(withPredicate(DEFAULT_CHAIN, StateType.WALL_KICK, WallKickState::canWallKickRun), StateType.WALL_RUN, WallRunState::canWallRunHold);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		Vec3 wallNormal = context.getWallNormal();
		context.setRunWallNormal(wallNormal);
		float inputWallAngle = context.getInputWallAngle();
		Vec3 currentMovement = player.getDeltaMovement();
		context.setNoMoveInput(true);
		Vec3 tangent = new Vec3(-wallNormal.z, 0.0, wallNormal.x);
		double dot = currentMovement.x * tangent.x + currentMovement.z * tangent.z;
		if (dot < 0.0) {
			tangent = tangent.scale(-1.0);
		}

		boolean useLeft;
		if (player.level().isClientSide() && PlatformServices.client().isLocalPlayer(player)) {
			useLeft = inputWallAngle > 0.0F;
			context.setTransitionExtraData(useLeft ? 1 : 0);
			int wallIndex = PlayerMovementContext.encodeWallNormal(wallNormal);
			context.setTransitionWallData((byte)(wallIndex >= 0 ? wallIndex : -1));
		} else {
			int extra = context.getTransitionExtraData();
			useLeft = extra >= 0 ? extra == 1 : inputWallAngle > 0.0F;
		}

		playStateAnimation(player, useLeft ? WALL_RUN_LEFT : WALL_RUN_RIGHT, context);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		double ySpeed = currentMovement.y > 0.0 ? currentMovement.y : 0.0;
		if (context.isHasJetBooster()) {
			ySpeed = dampVerticalSpeed(currentMovement.y);
			PlatformServices.client().playSound(player, MomentumSound.JET2, 1.0F, 1.0F + player.getRandom().nextFloat() * 0.4F - 0.2F);
			player.fallDistance = 0.0;
		}

		double wallRunSpeed = Math.max(currentMovement.horizontalDistance(), context.getJumpLimitSpeed());
		if (instance != null) {
			if (context.isHasLedge()) {
				instance.addOrReplacePermanentModifier(new AttributeModifier(WALL_GRAVITY_ID, -1.0, Operation.ADD_MULTIPLIED_BASE));
				context.setGravityModify(-1.0F);
				player.setDeltaMovement(tangent.x * wallRunSpeed + wallNormal.x * 0.05, 0.0, tangent.z * wallRunSpeed + wallNormal.z * 0.05);
				player.fallDistance = 0.0;
			} else {
				float gravityModify = context.isHasJetBooster() ? -1.0F : -0.6F;
				instance.addOrReplacePermanentModifier(new AttributeModifier(WALL_GRAVITY_ID, gravityModify, Operation.ADD_MULTIPLIED_BASE));
				context.setGravityModify(gravityModify);
				player.setDeltaMovement(tangent.x * wallRunSpeed + wallNormal.x * 0.05, ySpeed, tangent.z * wallRunSpeed + wallNormal.z * 0.05);
			}
		}

		context.setNeedSoundTick(10.0F);
		context.playWallSound(player, PlayerMovementContext.STEP, 0.15F, 1.0F);
		context.setTargetCameraRoll(inputWallAngle > 0.0F ? -15.0F : 15.0F);
	}

	@Override
	public void clientTick(Player player, PlayerMovementContext context) {
		super.clientTick(player, context);
		Vec3 wallNormal = context.getRunWallNormal();
		Vec3 currentMovement = player.getDeltaMovement();
		context.setNoMoveInput(true);
		Vec3 tangent = new Vec3(-wallNormal.z, 0.0, wallNormal.x);
		double dot = currentMovement.x * tangent.x + currentMovement.z * tangent.z;
		if (dot < 0.0) {
			tangent = tangent.scale(-1.0);
		}

		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			boolean stopAtLedge = context.isHasLedge();
			boolean dampWithBooster = !stopAtLedge && context.isHasJetBooster();
			float gravityModify = !stopAtLedge && !dampWithBooster ? -0.6F : -1.0F;
			if (context.getGravityModify() != gravityModify) {
				instance.addOrReplacePermanentModifier(new AttributeModifier(WALL_GRAVITY_ID, gravityModify, Operation.ADD_MULTIPLIED_BASE));
				context.setGravityModify(gravityModify);
			}

			double tangentSpeed = Math.abs(currentMovement.dot(tangent));
			double wallRunSpeed = Math.max(tangentSpeed, context.getJumpLimitSpeed());
			double ySpeed = stopAtLedge ? 0.0 : (dampWithBooster ? dampVerticalSpeed(currentMovement.y) : currentMovement.y);
			player.setDeltaMovement(tangent.x * wallRunSpeed + wallNormal.x * 0.05, ySpeed, tangent.z * wallRunSpeed + wallNormal.z * 0.05);
		}
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		float speed = (float)Math.min(context.getSpeed().length() * 5.0, 5.0);
		playStateAnimation(player, context.getCurrentAnimationName(), context, 0, speed);
		context.setNeedSoundTick(context.getNeedSoundTick() - speed);
		if (context.getNeedSoundTick() <= 0.0F) {
			context.playWallSound(player, PlayerMovementContext.STEP, 0.15F, 1.0F);
			context.setNeedSoundTick(context.getNeedSoundTick() + 10.0F);
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.setTargetCameraRoll(0.0F);
		AttributeInstance instance = player.getAttribute(Attributes.GRAVITY);
		if (instance != null) {
			instance.removeModifier(WALL_GRAVITY_ID);
		}

		context.setNoMoveInput(false);
	}

	@Override
	public StateType getStateType() {
		return StateType.WALL_RUN;
	}

	private static double dampVerticalSpeed(double speed) {
		double dampedSpeed = speed * 0.8;
		return Math.abs(dampedSpeed) < 0.01 ? 0.0 : dampedSpeed;
	}
}
