package com.akirahane.momentum.core.state.states.wall;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.BaseState;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class WallKickState extends BaseState {
	private static final int WALL_KICK_STATE_TICKS = 5;
	private static final int WALL_RUN_REENTRY_GRACE_TICKS = 8;
	private static final double HORIZONTAL_EPSILON = 1.0E-6;
	private static final double WALL_RUN_KICK_NORMAL_MULTIPLIER = 1.0;

	public static boolean canWallKick(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_KICK.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_KICK.getAsBoolean()
			&& !player.onGround()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& context.getInputVec().horizontalDistance() > 0.01
			&& Mth.abs(context.getInputWallAngle()) >= 120.0F
			&& checkKey(player, context);
	}

	public static boolean canWallKickRun(Player player, PlayerMovementContext context) {
		return MomentumServerConfig.ENABLE_WALL_KICK.getAsBoolean()
			&& MomentumClientConfig.ENABLE_WALL_KICK.getAsBoolean()
			&& !Vec3.ZERO.equals(context.getWallNormal())
			&& checkKey(player, context);
	}

	public static boolean checkKey(Player player, PlayerMovementContext context) {
		PlatformServices.client().addHint(MovementHint.WALL_KICK);
		return context.getInputBuffer()[context.getInputBufferIndex()].contains(4);
	}

	@Override
	protected List<BaseState.Transition> transitionChain() {
		return withPredicate(DEFAULT_CHAIN, StateType.WALL_KICK, (p, c) -> c.getWallJumpTimer() > 0);
	}

	@Override
	public void onEnter(Player player, PlayerMovementContext context) {
		float yaw = player.getYRot();
		Vec3 lookVec = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw))).normalize();
		Vec3 motionDirection = context.getSpeed().normalize();
		boolean isBackwardJump = lookVec.x * motionDirection.x + lookVec.z * motionDirection.z < 0.0;
		if (context.isLeftFootJump()) {
			playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_LEFT : BaseState.JUMP_LEFT, context);
		} else {
			playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_RIGHT : BaseState.JUMP_RIGHT, context);
		}

		context.setLeftFootJump(!context.isLeftFootJump());
		float jumpPower = PlatformServices.gameplay().jumpPower(player);
		boolean fromWallRun = context.getPreviousStateType() == StateType.WALL_RUN;
		boolean inputDirected = Mth.abs(context.getInputWallAngle()) >= 120.0F;
		if (fromWallRun) {
			if (inputDirected) {
				applyInputDirectedKick(player, context, jumpPower);
			} else {
				applyWallRunKick(player, context, jumpPower);
			}

			context.setWallRunReentryGraceTicks(8);
			if (inputDirected) {
				player.fallDistance = 0.0;
			}
		} else if (inputDirected) {
			applyInputDirectedKick(player, context, jumpPower);
			player.fallDistance = 0.0;
		} else {
			player.addDeltaMovement(new Vec3(-context.getWallNormal().x * jumpPower, 0.0, -context.getWallNormal().z * jumpPower));
		}

		context.playWallSound(player, PlayerMovementContext.FALL, 0.15F, 1.0F);
		player.playSound(SoundEvents.ARROW_SHOOT, 0.5F, 1.0F + player.getRandom().nextFloat() * 0.4F - 0.2F);
		context.setWallJumpTimer(5);
	}

	private static void applyWallRunKick(Player player, PlayerMovementContext context, float jumpPower) {
		Vec3 normal = context.getRunWallNormal();
		if (normal.horizontalDistanceSqr() < 1.0E-6) {
			normal = context.getWallNormal();
		}

		normal = new Vec3(normal.x, 0.0, normal.z).normalize();
		Vec3 current = player.getDeltaMovement();
		Vec3 currentHorizontal = new Vec3(current.x, 0.0, current.z);
		Vec3 awayFromWall = normal.scale(-jumpPower * 1.0);
		double ySpeed = Mth.abs(context.getInputWallAngle()) >= 100.0F ? jumpPower * 1.5 : current.y;
		setMomentumPreservingHorizontalVelocity(player, context, currentHorizontal.add(awayFromWall), ySpeed, jumpPower);
	}

	private static void applyInputDirectedKick(Player player, PlayerMovementContext context, float jumpPower) {
		Vec3 current = player.getDeltaMovement();
		Vec3 currentHorizontal = new Vec3(current.x, 0.0, current.z);
		Vec3 input = new Vec3(context.getInputVec().x, 0.0, context.getInputVec().z);
		if (input.horizontalDistance() > 1.0) {
			input = input.normalize();
		}

		Vec3 desiredHorizontal = currentHorizontal.add(input.scale(jumpPower * 0.5));
		setMomentumPreservingHorizontalVelocity(player, context, desiredHorizontal, jumpPower * 1.5, jumpPower);
	}

	private static void setMomentumPreservingHorizontalVelocity(
		Player player, PlayerMovementContext context, Vec3 desiredHorizontal, double ySpeed, float jumpPower
	) {
		Vec3 current = player.getDeltaMovement();
		Vec3 currentHorizontal = new Vec3(current.x, 0.0, current.z);
		double currentSpeed = currentHorizontal.horizontalDistance();
		double desiredSpeed = desiredHorizontal.horizontalDistance();
		Vec3 direction;
		if (desiredSpeed > 1.0E-6) {
			direction = desiredHorizontal.scale(1.0 / desiredSpeed);
		} else if (currentSpeed > 1.0E-6) {
			direction = currentHorizontal.scale(1.0 / currentSpeed);
		} else {
			direction = Vec3.ZERO;
		}

		double wallKickSpeedLimit = context.getJumpLimitSpeed() + jumpPower * 0.5;
		double acceleration = context.getWallKickCooldown() == 0
			? Math.min(Math.max(0.0, desiredSpeed - currentSpeed), Math.max(0.0, wallKickSpeedLimit - currentSpeed))
			: 0.0;
		double targetSpeed = currentSpeed + acceleration;
		player.setDeltaMovement(direction.x * targetSpeed, ySpeed, direction.z * targetSpeed);
	}

	@Override
	public void clientTickRemote(Player player, PlayerMovementContext context) {
		super.clientTickRemote(player, context);
		float yaw = player.getYRot();
		Vec3 lookVec = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw))).normalize();
		Vec3 motionDirection = context.getSpeed().normalize();
		boolean isBackwardJump = lookVec.x * motionDirection.x + lookVec.z * motionDirection.z < 0.0;
		if (context.isLeftFootJump()) {
			playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_LEFT : BaseState.JUMP_LEFT, context);
		} else {
			playStateAnimation(player, isBackwardJump ? BaseState.BACK_JUMP_RIGHT : BaseState.JUMP_RIGHT, context);
		}
	}

	@Override
	public void onExit(Player player, PlayerMovementContext context) {
		super.onExit(player, context);
		context.setWallKickCooldown(MomentumServerConfig.WALL_KICK_ACCELERATION_COOLDOWN.getAsInt());
	}

	@Override
	public StateType getStateType() {
		return StateType.WALL_KICK;
	}
}
