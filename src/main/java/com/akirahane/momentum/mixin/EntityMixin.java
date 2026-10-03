package com.akirahane.momentum.mixin;

import com.akirahane.momentum.core.MomentumUtils;
import com.akirahane.momentum.core.state.MovementStateMachine;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.platform.PlatformServices;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	private Level level;
	@Unique
	private static final ImmutableList<@NotNull Axis> XZY_AXIS_ORDER = ImmutableList.of(Axis.X, Axis.Z, Axis.Y);
	@Unique
	private static final ImmutableList<@NotNull Axis> ZXY_AXIS_ORDER = ImmutableList.of(Axis.Z, Axis.X, Axis.Y);

	// 26.2 port: Entity.collectColliders(Entity, Level, List, AABB) переименован в collectCollidersIgnoringWorldBorder (та же сигнатура: сущности + блоки + граница мира)
	@Shadow
	private static List<VoxelShape> collectCollidersIgnoringWorldBorder(@Nullable Entity source, Level level, List<VoxelShape> entityColliders, AABB boundingBox) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Shadow
	public abstract float maxUpStep();

	@Shadow
	public abstract boolean onGround();

	@Shadow
	private static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	protected EntityMixin(EntityType<?> type, Level level) {
	}

	@ModifyVariable(method = "collide", at = @At(value = "STORE", ordinal = 0), name = "stepUpAABB")
	private AABB momentum$modifyStepUpAABB(AABB original, Vec3 movement) {
		Entity self = (Entity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			if (!player.level().isClientSide()) {
				return original;
			}

			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			return stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)
				? original
				: original.expandTowards(0.0, Math.ceil(Math.max(Math.abs(movement.x), Math.abs(movement.z))), 0.0);
		} else {
			return original;
		}
	}

	@WrapOperation(
		method = "collide",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F")
	)
	private float[] momentum$wrapCollectCandidateStepUpHeights(
		AABB boundingBox,
		List<VoxelShape> colliders,
		float maxStepHeight,
		float stepHeightToSkip,
		Operation<float[]> original,
		@Local(name = "movement", argsOnly = true) Vec3 movement,
		@Local(name = "movementStep") Vec3 movementStep
	) {
		Entity self = (Entity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (!(self instanceof Player player)) {
			return (float[])original.call(new Object[]{boundingBox, colliders, maxStepHeight, stepHeightToSkip});
		} else {
			if (!player.level().isClientSide()) {
				return (float[])original.call(new Object[]{boundingBox, colliders, maxStepHeight, stepHeightToSkip});
			}

			MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
			if (stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
				return (float[])original.call(new Object[]{boundingBox, colliders, maxStepHeight, stepHeightToSkip});
			}

			float customMaxStep = (float)(maxStepHeight + Math.ceil(Math.max(Math.abs(movement.x), Math.abs(movement.z))));
			if (!colliders.isEmpty()) {
				stateMachine.getContext().setSlopeUnitVector(this.momentum$getSlopeDirection(player, colliders.getFirst()));
			}

			AABB movementRange = boundingBox.expandTowards(movement.x, 0.0, movement.z);
			FloatSet candidates = new FloatArraySet(4);

			for (VoxelShape collider : colliders) {
				collider.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
					AABB subBoxFlat = new AABB(minX, movementRange.minY, minZ, maxX, movementRange.maxY, maxZ);
					if (movementRange.intersects(subBoxFlat)) {
						float relativeCoord = (float)(maxY - boundingBox.minY);
						if (!(relativeCoord < 0.0F) && relativeCoord != stepHeightToSkip) {
							if (!(relativeCoord > customMaxStep)) {
								candidates.add(relativeCoord);
							}
						}
					}
				});
			}

			float[] result = candidates.toFloatArray();
			FloatArrays.unstableSort(result);
			FloatList filtered = new FloatArrayList();
			float lastHeight = 0.0F;

			for (float current : result) {
				if (current - lastHeight > this.maxUpStep()) {
					break;
				}

				Vec3 stepFromGround = collideWithShapes(new Vec3(movement.x, current, movement.z), boundingBox, colliders);
				if (!(stepFromGround.horizontalDistanceSqr() <= movementStep.horizontalDistanceSqr())) {
					if (!momentum$hasGroundUnder(boundingBox.move(stepFromGround), colliders)) {
						break;
					}

					filtered.add(current);
					lastHeight = current;
				}
			}

			result = filtered.toFloatArray();
			int i = 0;

			for (int j = result.length - 1; i < j; j--) {
				float tmp = result[i];
				result[i] = result[j];
				result[j] = tmp;
				i++;
			}

			return result;
		}
	}

	@Inject(method = "collide", at = @At("RETURN"), cancellable = true)
	private void addStepDown(
		Vec3 movement,
		CallbackInfoReturnable<Vec3> cir,
		@Local(name = "aabb") AABB aabb,
		@Local(name = "entityColliders") List<VoxelShape> entityColliders,
		@Local(name = "movementStep") Vec3 movementStep
	) {
		Entity self = (Entity)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		if (self instanceof Player player) {
			if (player.level().isClientSide()) {
				if (!player.isInLiquid()) {
					MovementStateMachine stateMachine = PlatformServices.gameplay().movementState(player);
					if (!stateMachine.getCurrentState().getStateType().equals(StateType.ORIGINAL)) {
						if (((Vec3)cir.getReturnValue()).y > 0.0) {
							MomentumUtils.setSlideAcceleration(movement, ((Vec3)cir.getReturnValue()).y, stateMachine, player);
						} else {
							movementStep = movement.lengthSqr() == 0.0 ? movement : momentum$collideBoundingBoxDown(self, movement, aabb, player.level(), entityColliders);
							if (!(movementStep.y >= 0.0)) {
								boolean yCollision = movement.y != movementStep.y;
								boolean onGroundAfterCollision = yCollision && movement.y < 0.0;
								float maxDownStep = (float)(this.maxUpStep() + Math.ceil(Math.max(Math.abs(movement.x), Math.abs(movement.z))));
								if (maxDownStep > 0.0F && !onGroundAfterCollision && this.onGround()) {
									entityColliders = this.level.getEntityCollisions(self, aabb.expandTowards(movement.subtract(0.0, maxDownStep, 0.0)));
									AABB stepDownAABB = aabb.expandTowards(movement.x, -maxDownStep, movement.z);
									stepDownAABB = stepDownAABB.expandTowards(0.0, -player.getBoundingBox().getYsize() - 1.0E-5F, 0.0);
									List<VoxelShape> colliders = collectCollidersIgnoringWorldBorder(self, this.level, entityColliders, stepDownAABB); // 26.2 port: новое имя collectColliders
									float stepHeightToSkip = (float)movementStep.y;
									float[] candidateStepDownHeights = momentum$collectCandidateStepDownHeights(aabb, colliders, -maxDownStep, stepHeightToSkip, this.maxUpStep());

									for (float candidateStepDHeight : candidateStepDownHeights) {
										if (candidateStepDHeight != 0.0F) {
											Vec3 stepFromGround = momentum$collideWithShapesDown(new Vec3(movement.x, candidateStepDHeight, movement.z), aabb, colliders);
											if (!(stepFromGround.horizontalDistanceSqr() <= 0.0)) {
												VoxelShape supportingCollider = momentum$findSupportingCollider(aabb.move(stepFromGround), colliders);
												if (supportingCollider != null && stepFromGround.horizontalDistanceSqr() > 0.0) {
													cir.setReturnValue(stepFromGround);
													if (!stateMachine.getCurrentState().getStateType().equals(StateType.SLIDE)) {
														return;
													}

													stateMachine.getContext().setSlopeUnitVector(this.momentum$getSlopeDirection(player, supportingCollider));
													MomentumUtils.setSlideAcceleration(movement, stepFromGround.y, stateMachine, player);
													return;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Unique
	private static boolean momentum$hasGroundUnder(AABB movedAABB, List<VoxelShape> colliders) {
		return momentum$findSupportingCollider(movedAABB, colliders) != null;
	}

	@Unique
	private static VoxelShape momentum$findSupportingCollider(AABB movedAABB, List<VoxelShape> colliders) {
		double epsilon = 1.0E-5;
		VoxelShape best = null;
		double bestContactArea = 0.0;

		for (VoxelShape collider : colliders) {
			double[] contactArea = new double[]{0.0};
			collider.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
				if (!(Math.abs(movedAABB.minY - maxY) > epsilon)) {
					double overlapX = Math.min(movedAABB.maxX, maxX) - Math.max(movedAABB.minX, minX);
					double overlapZ = Math.min(movedAABB.maxZ, maxZ) - Math.max(movedAABB.minZ, minZ);
					if (overlapX > 0.0 && overlapZ > 0.0) {
						contactArea[0] += overlapX * overlapZ;
					}
				}
			});
			if (contactArea[0] > bestContactArea) {
				bestContactArea = contactArea[0];
				best = collider;
			}
		}

		return best;
	}

	@Unique
	private static float[] momentum$collectCandidateStepDownHeights(
		AABB boundingBox, List<VoxelShape> colliders, float maxStepHeight, float stepHeightToSkip, float original_StepHeight
	) {
		FloatSet candidates = new FloatArraySet(4);

		for (VoxelShape collider : colliders) {
			DoubleList coords = collider.getCoords(Axis.Y);

			for (int i = coords.size() - 1; i >= 0; i--) {
				float relativeCoord = (float)(coords.getDouble(i) - boundingBox.minY);
				if (!(relativeCoord > 0.0F) && relativeCoord != stepHeightToSkip) {
					if (relativeCoord < maxStepHeight) {
						break;
					}

					candidates.add(relativeCoord);
				}
			}
		}

		float[] sortedCandidates = candidates.toFloatArray();
		FloatArrays.unstableSort(sortedCandidates);
		FloatList filtered = new FloatArrayList();
		float lastHeight = 0.0F;

		for (int i = sortedCandidates.length - 1; i >= 0; i--) {
			float current = sortedCandidates[i];
			if (!(Math.abs(current - lastHeight) <= original_StepHeight)) {
				break;
			}

			filtered.add(current);
			lastHeight = current;
		}

		float[] result = filtered.toFloatArray();
		FloatArrays.unstableSort(result);
		return result;
	}

	@Unique
	public Vec3 momentum$getSlopeDirection(Player player, VoxelShape collider) {
		if (collider.isEmpty()) {
			return Vec3.ZERO;
		}

		AABB bounds = collider.bounds();
		BlockPos feet = BlockPos.containing((bounds.minX + bounds.maxX) / 2.0, bounds.maxY, (bounds.minZ + bounds.maxZ) / 2.0);
		BlockGetter level = player.level();
		float countX = 0.0F;
		float countZ = 0.0F;
		boolean north = this.momentum$isHigher(level, feet, 0, -1);
		boolean south = this.momentum$isHigher(level, feet, 0, 1);
		boolean west = this.momentum$isHigher(level, feet, -1, 0);
		boolean east = this.momentum$isHigher(level, feet, 1, 0);
		if (north) {
			countZ++;
		}

		if (south) {
			countZ--;
		}

		if (west) {
			countX++;
		}

		if (east) {
			countX--;
		}

		if (countX == 0.0F && countZ == 0.0F) {
			boolean nw = this.momentum$isHigher(level, feet, -1, -1);
			boolean ne = this.momentum$isHigher(level, feet, 1, -1);
			boolean sw = this.momentum$isHigher(level, feet, -1, 1);
			boolean se = this.momentum$isHigher(level, feet, 1, 1);
			float diag = 0.707F;
			if (nw) {
				countX += diag;
				countZ += diag;
			}

			if (ne) {
				countX -= diag;
				countZ += diag;
			}

			if (sw) {
				countX += diag;
				countZ -= diag;
			}

			if (se) {
				countX -= diag;
				countZ -= diag;
			}
		}

		return countX == 0.0F && countZ == 0.0F ? Vec3.ZERO : new Vec3(countX, 0.0, countZ).normalize();
	}

	@Unique
	private boolean momentum$isHigher(BlockGetter level, BlockPos feet, int dx, int dz) {
		BlockPos check = feet.offset(dx, 0, dz);
		return !level.getBlockState(check).getCollisionShape(level, check).isEmpty();
	}

	@Unique
	private static Vec3 momentum$collideBoundingBoxDown(@Nullable Entity source, Vec3 movement, AABB boundingBox, Level level, List<VoxelShape> entityColliders) {
		List<VoxelShape> colliders = collectCollidersIgnoringWorldBorder(source, level, entityColliders, boundingBox.expandTowards(movement)); // 26.2 port: новое имя collectColliders
		return momentum$collideWithShapesDown(movement, boundingBox, colliders);
	}

	@Unique
	private static Vec3 momentum$collideWithShapesDown(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
		if (shapes.isEmpty()) {
			return movement;
		}

		Vec3 resolvedMovement = Vec3.ZERO;
		UnmodifiableIterator var4 = momentum$axisStepOrderDown(movement).iterator();

		while (var4.hasNext()) {
			Axis axis = (Axis)var4.next();
			double axisMovement = movement.get(axis);
			if (axisMovement != 0.0) {
				double collision = Shapes.collide(axis, boundingBox.move(resolvedMovement), shapes, axisMovement);
				resolvedMovement = resolvedMovement.with(axis, collision);
			}
		}

		return resolvedMovement;
	}

	@Unique
	private static ImmutableList<@NotNull Axis> momentum$axisStepOrderDown(Vec3 movement) {
		return Math.abs(movement.x) < Math.abs(movement.z) ? XZY_AXIS_ORDER : ZXY_AXIS_ORDER;
	}

	@Unique
	private record MomentumStepDownCandidate(float height, AABB box) {
	}
}
