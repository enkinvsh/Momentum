package com.akirahane.momentum.platform;

import com.akirahane.momentum.core.state.MovementStateMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public interface GameplayPlatform {
	GameplayPlatform NOOP = new GameplayPlatform() {
		@Override
		public boolean hasJetBooster(Player player) {
			return false;
		}

		@Override
		public float jumpPower(Player player) {
			return 0.0F;
		}

		@Override
		public MovementStateMachine movementState(Player player) {
			throw new IllegalStateException("Gameplay platform is not installed");
		}

		@Override
		public boolean hasMovementState(Player player) {
			return false;
		}

		@Override
		public boolean isMomentumEnabled(Player player) {
			return false;
		}

		@Override
		public void setMomentumEnabled(Player player, boolean enabled) {
			throw new IllegalStateException("Gameplay platform is not installed");
		}

		@Override
		public void setForcedPose(Player player, Pose pose) {
			throw new IllegalStateException("Gameplay platform is not installed");
		}

		@Override
		public SoundType soundType(BlockState state, Level level, BlockPos pos, Player player) {
			throw new IllegalStateException("Gameplay platform is not installed");
		}

		@Override
		public float blockFriction(BlockState state, Level level, BlockPos pos, Player player) {
			throw new IllegalStateException("Gameplay platform is not installed");
		}
	};

	boolean hasJetBooster(Player var1);

	float jumpPower(Player var1);

	MovementStateMachine movementState(Player var1);

	boolean hasMovementState(Player var1);

	boolean isMomentumEnabled(Player var1);

	void setMomentumEnabled(Player var1, boolean var2);

	void setForcedPose(Player var1, Pose var2);

	SoundType soundType(BlockState var1, Level var2, BlockPos var3, Player var4);

	float blockFriction(BlockState var1, Level var2, BlockPos var3, Player var4);
}
