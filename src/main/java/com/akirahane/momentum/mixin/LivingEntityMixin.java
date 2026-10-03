package com.akirahane.momentum.mixin;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.MovementStateMachine;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable, WaypointTransmitter {
	@Shadow
	public float yBodyRot;
	@Shadow
	public float yHeadRot;

	protected LivingEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@ModifyVariable(method = "travelInWater", at = @At("STORE"), name = "slowDown")
	private float slowDown(float original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			return stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)
				? original
				: Math.clamp(1.0F - (float)stateMachine.applyEffect(1.0F - Math.max(0.0F, original), MomentumEffectType.FRICTION), 0.0F, 1.0F);
		} else {
			return original;
		}
	}

	@ModifyVariable(method = "travelInWater", at = @At(value = "STORE", ordinal = 0), name = "waterWalker")
	private float momentum$adjustWaterWalker(float original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (stateMachine.getCurrentState().getStateType() == StateType.ORIGINAL) {
				return original;
			}

			double submergedRatio = Math.min(1.0, self.getFluidHeight(FluidTags.WATER) / self.getBbHeight());
			float airRatio = (float)(1.0 - submergedRatio);
			return Math.max(original, airRatio);
		} else {
			return original;
		}
	}

	@ModifyVariable(method = "travelInAir", at = @At("HEAD"), argsOnly = true, name = "input")
	private Vec3 modifyTravelInput(Vec3 original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				return original;
			} else {
				return stateMachine.getContext().isNoMoveInput() ? Vec3.ZERO : original;
			}
		} else {
			return original;
		}
	}

	@ModifyVariable(method = "travelInAir", at = @At("STORE"), name = "blockFriction")
	private float blockFriction(float original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			return stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)
				? original
				: Math.clamp(1.0F - (float)stateMachine.applyEffect(1.0F - Math.max(0.0F, original), MomentumEffectType.BLOCK_FRICTION), 0.0F, 1.0F);
		} else {
			return original;
		}
	}

	@ModifyVariable(method = "travelInAir", at = @At("STORE"), name = "friction")
	private float friction(float original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			return stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)
				? original
				: Math.clamp(1.0F - (float)stateMachine.applyEffect(1.0F - Math.max(0.0F, original), MomentumEffectType.FRICTION), 0.0F, 1.0F);
		} else {
			return original;
		}
	}

	@ModifyConstant(method = "travelInAir", constant = @Constant(floatValue = 0.91F))
	public float modifyAirFriction(float original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			return stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL) ? original : MomentumServerConfig.AIR_FRICTION.get().floatValue();
		} else {
			return original;
		}
	}

	@ModifyVariable(method = "travelInAir", at = @At("STORE"), name = "movement")
	private Vec3 movement(Vec3 original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				return original;
			}

			original = stateMachine.applyEffect(original.subtract(stateMachine.getContext().getOldDeltaMovement()), player.getYRot(), MomentumEffectType.ACCELERATION)
				.add(stateMachine.getContext().getOldDeltaMovement());
			Vec3 limitSpeed = stateMachine.applyEffect(Vec3.ZERO, player.getYRot(), MomentumEffectType.LIMIT_ACCELERATION_SPEED);
			if (limitSpeed.length() != 0.0) {
				original = new Vec3(
					Math.abs(original.x) > Math.abs(limitSpeed.x)
						? (Math.abs(original.x) < Math.abs(stateMachine.getContext().getOldDeltaMovement().x) ? original.x : stateMachine.getContext().getOldDeltaMovement().x)
						: original.x,
					Math.abs(original.y) > Math.abs(limitSpeed.y)
						? (Math.abs(original.y) < Math.abs(stateMachine.getContext().getOldDeltaMovement().y) ? original.y : stateMachine.getContext().getOldDeltaMovement().y)
						: original.y,
					Math.abs(original.z) > Math.abs(limitSpeed.z)
						? (Math.abs(original.z) < Math.abs(stateMachine.getContext().getOldDeltaMovement().z) ? original.z : stateMachine.getContext().getOldDeltaMovement().z)
						: original.z
				);
			}

			return original;
		} else {
			return original;
		}
	}

	@WrapOperation(
		method = "jumpFromGround",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
	)
	private void wrapSprintBoost(LivingEntity self, Vec3 originalBoost, Operation<Void> original) {
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				original.call(new Object[]{self, originalBoost});
			} else {
				Vec3 current = self.getDeltaMovement();
				float angle = self.getYRot() * (float) (Math.PI / 180.0);
				Vec3 acceleration = new Vec3(
					-Mth.sin(angle) * stateMachine.getContext().getJumpAcceleration(), 0.0, Mth.cos(angle) * stateMachine.getContext().getJumpAcceleration()
				);
				Vec3 boosted = current.add(acceleration);
				double currentH = current.horizontalDistance();
				double boostedH = boosted.horizontalDistance();
				if (boostedH > stateMachine.getContext().getJumpLimitSpeed() && currentH < stateMachine.getContext().getJumpLimitSpeed()) {
					Vec3 clamped = new Vec3(
						boosted.x * stateMachine.getContext().getJumpLimitSpeed() / boostedH, 0.0, boosted.z * stateMachine.getContext().getJumpLimitSpeed() / boostedH
					);
					Vec3 diff = clamped.subtract(current.x, 0.0, current.z);
					original.call(new Object[]{self, new Vec3(diff.x, 0.0, diff.z)});
				} else if (currentH < stateMachine.getContext().getJumpLimitSpeed()) {
					original.call(new Object[]{self, acceleration});
				} else {
					original.call(new Object[]{self, Vec3.ZERO});
				}
			}
		} else {
			original.call(new Object[]{self, originalBoost});
		}
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void onJumpFromGround(CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (!stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				if (stateMachine.getContext().isNoJump()) {
					ci.cancel();
				}
			}
		}
	}

	@Inject(method = "tickHeadTurn", at = @At("HEAD"), cancellable = true)
	private void momentum$lockBodyRotation(float yBodyRotT, CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (!stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				StateType state = stateMachine.getCurrentState().getStateType();
				if (StateType.WALL_CLIMB.equals(state) || StateType.WALL_RUN.equals(state) || StateType.WALL_SLIDE.equals(state) || StateType.WALL_HANG.equals(state)) {
					PlayerMovementContext context = stateMachine.getContext();
					Vec3 wallNormal = context.getWallNormal();
					if (wallNormal.lengthSqr() > 0.001) {
						float targetBodyRot = (float)Math.toDegrees(Math.atan2(-wallNormal.x, wallNormal.z));
						float diff = Mth.wrapDegrees(targetBodyRot - this.yBodyRot);
						this.yBodyRot += diff * 0.3F;
						if (StateType.WALL_HANG.equals(state) && context.getSpeed().horizontalDistance() * 20.0 > 0.05) {
							float MAX_HEAD_DIFF = 60.0F;
							float headDiff = Mth.wrapDegrees(this.yHeadRot - this.yBodyRot);
							if (Math.abs(headDiff) > MAX_HEAD_DIFF) {
								this.yHeadRot = this.yBodyRot + Math.signum(headDiff) * MAX_HEAD_DIFF;
							}
						}
					}

					ci.cancel();
				}
			}
		}
	}

	@ModifyReturnValue(method = "handleOnClimbable", at = @At("RETURN"))
	private Vec3 momentum$boostClimbSpeed(Vec3 original) {
		LivingEntity self = (LivingEntity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			if (!self.level().isClientSide()) {
				return original;
			}

			if (!self.onClimbable()) {
				return original;
			}

			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (!stateMachine.getContext().getMovementInput().sprint()) {
				return original;
			}

			if (stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				return original;
			}

			if (!stateMachine.getCurrentState().getStateType().equals(StateType.WALL_CLIMB)
				&& !stateMachine.getCurrentState().getStateType().equals(StateType.WALL_SLIDE)) {
				return original;
			}

			Double multiplier = MomentumServerConfig.CLIMB_BOOST_MULTIPLIER.get();
			if (!(original.y > 0.0) || stateMachine.getContext().getMovementInput().up() && this.horizontalCollision) {
				double boostedY = original.y * multiplier;
				if (boostedY > 0.0) {
					boostedY = Math.min(boostedY, 0.2 * multiplier);
				}

				return new Vec3(original.x, boostedY, original.z);
			} else {
				return original;
			}
		} else {
			return original;
		}
	}
}
