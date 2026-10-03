package com.akirahane.momentum.core;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.MovementStateMachine;
import com.akirahane.momentum.core.state.StateType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class MomentumUtils {
	protected static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier BOOSTER_SPEED_ID = Identifier.fromNamespaceAndPath("momentum", "booster_speed");
	private static final Identifier BOOSTER_JUMP_ID = Identifier.fromNamespaceAndPath("momentum", "booster_jump");
	private static final Identifier BOOSTER_STEP_ID = Identifier.fromNamespaceAndPath("momentum", "booster_step");
	public static final double MAX_SLIDE_SPEED = 0.98;
	public static final float DOWNHILL_ACCEL_FACTOR = 0.08F;
	public static final float UPHILL_DECEL_FACTOR = 0.12F;

	public static void setSlideAcceleration(Vec3 movement, double movementStepY, MovementStateMachine stateMachine, Player player) {
		if (stateMachine.getCurrentState().getStateType().equals(StateType.SLIDE)) {
			double currentSpeed = movement.horizontalDistance();
			stateMachine.getContext().setBlockStep((movementStepY + stateMachine.getContext().getBlockStep()) / 2.0);
			int duration = 2;
			Vec3 slopeDir = stateMachine.getContext().getSlopeUnitVector();
			if (movementStepY < 0.0) {
				double dropHeight = Math.abs(movementStepY);
				double speedRatio = Math.min(1.0, currentSpeed / 0.98);
				double headroom = 1.0 - speedRatio * speedRatio;
				double matchRatio = Math.min(currentSpeed, dropHeight) / Math.max(Math.max(currentSpeed, dropHeight), 0.001);
				float acceleration = (float)(0.08F * dropHeight * 3.0 * headroom * matchRatio);
				stateMachine.getContext().SLIDE_ACCELERATION.setValue(new Vec3(slopeDir.x * acceleration, 0.0, slopeDir.z * acceleration));
				stateMachine.getContext().addEffect(MomentumEffectType.ACCELERATION, stateMachine.getContext().SLIDE_ACCELERATION, duration);
			} else if (movementStepY > 0.0) {
				double riseHeight = Math.abs(movementStepY);
				float deceleration = (float)(0.12F * Math.min(riseHeight * riseHeight, 1.0));
				stateMachine.getContext().SLIDE_ACCELERATION.setValue(new Vec3(slopeDir.x * deceleration, 0.0, slopeDir.z * deceleration));
				stateMachine.getContext().addEffect(MomentumEffectType.ACCELERATION, stateMachine.getContext().SLIDE_ACCELERATION, duration);
			}
		}
	}

	public static void applyBoosterAttributes(Player player, boolean apply) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
		if (speed != null && jump != null && step != null) {
			speed.removeModifier(BOOSTER_SPEED_ID);
			jump.removeModifier(BOOSTER_JUMP_ID);
			step.removeModifier(BOOSTER_STEP_ID);
			if (apply) {
				speed.addOrReplacePermanentModifier(new AttributeModifier(BOOSTER_SPEED_ID, 0.04, Operation.ADD_VALUE));
				jump.addOrReplacePermanentModifier(new AttributeModifier(BOOSTER_JUMP_ID, 0.2, Operation.ADD_VALUE));
				step.addOrReplacePermanentModifier(new AttributeModifier(BOOSTER_STEP_ID, 0.4, Operation.ADD_VALUE));
			}
		}
	}

	public static boolean isDivingEdge(Player player, PlayerMovementContext context) {
		Vec3 movement = player.getDeltaMovement();
		int dx = (int)Math.signum(movement.x);
		int dz = (int)Math.signum(movement.z);
		if (dx == 0 && dz == 0) {
			return false;
		}

		BlockPos center = player.blockPosition().offset(dx * 2, 0, dz * 2);
		Level level = player.level();

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = -1; y >= -3; y--) {
					BlockPos pos = center.offset(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (!state.getCollisionShape(level, pos).isEmpty()) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public static boolean canPlayerFitAtPose(Player player, Pose pose) {
		EntityDimensions dimensions = player.getDimensions(pose);
		AABB boundingBox = dimensions.makeBoundingBox(player.position());
		return player.level().noCollision(player, boundingBox);
	}

	public static float approach(float current, float target, float alpha, float epsilon) {
		float next = current + (target - current) * alpha;
		return Math.abs(next - target) <= epsilon ? target : next;
	}
}
