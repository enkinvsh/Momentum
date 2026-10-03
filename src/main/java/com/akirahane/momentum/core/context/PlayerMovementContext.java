package com.akirahane.momentum.core.context;

import com.akirahane.momentum.client.animation.MomentumAnimationController;
import com.akirahane.momentum.core.MomentumUtils;
import com.akirahane.momentum.core.effect.MomentumEffect;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.akirahane.momentum.core.state.MovementStateMachine;
import com.akirahane.momentum.core.state.StateType;
import com.akirahane.momentum.core.state.states.ground.SlideState;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementInput;
import com.akirahane.momentum.platform.config.MomentumClientConfig;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import com.mojang.logging.LogUtils;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.AnimationController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Generated;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.value.ObjectValue;

public class PlayerMovementContext {
	protected static final Logger LOGGER = LogUtils.getLogger();
	public static final int UP = 0;
	public static final int DOWN = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final int JUMP = 4;
	public static final int SPRINT = 5;
	public static final int SHIFT = 6;
	public static final int SHIFT_HOLD = 7;
	public static final Function<SoundType, SoundEvent> STEP = SoundType::getStepSound;
	public static final Function<SoundType, SoundEvent> HIT = SoundType::getHitSound;
	public static final Function<SoundType, SoundEvent> BREAK = SoundType::getBreakSound;
	public static final Function<SoundType, SoundEvent> PLACE = SoundType::getPlaceSound;
	public static final Function<SoundType, SoundEvent> FALL = SoundType::getFallSound;
	MomentumAnimationController controller;
	MochaEngine<AnimationController> mocha;
	private boolean hasJetBooster = false;
	private boolean canMomentum = true;
	private boolean noMoveInput = false;
	private boolean noJump = false;
	private boolean hasLedge = false;
	private double ledgeOffsetY = 0.0;
	private boolean hasFaceWall = false;
	private boolean doubleClickUp = false;
	private boolean doubleClickDown = false;
	private boolean doubleClickLeft = false;
	private boolean doubleClickRight = false;
	private boolean doubleClickJump = false;
	private boolean doubleClickSprint = false;
	private boolean toBreakFallState = false;
	private boolean leftFootJump = true;
	private Vec3 speed = Vec3.ZERO;
	private Vec3 oldSpeed = Vec3.ZERO;
	private Vec3 oldDeltaMovement = Vec3.ZERO;
	private double blockStep = 0.0;
	private Vec3 slopeUnitVector = Vec3.ZERO;
	private double lastFallDistance = 0.0;
	private final int dodgeInvincible = 10;
	private Vec3 inputVec = Vec3.ZERO;
	private MovementInput movementInput = MovementInput.EMPTY;
	Vec3 wallNormal = Vec3.ZERO;
	Vec3 runWallNormal = Vec3.ZERO;
	float lookWallAngle = 360.0F;
	float inputWallAngle = 360.0F;
	private double jumpLimitSpeed = 0.0;
	private double jumpAcceleration = 0.0;
	private int jumpCooldown = 0;
	private float wallFriction = 0.6F;
	private float needSoundTick = 0.0F;
	private float gravityModify = 0.0F;
	private float jumpAnimationSpeed = 1.0F;
	private float bodyHeadAngleDiff = 0.0F;
	private float prevBodyHeadAngleDiff = 0.0F;
	private float prevHeadXRot = 0.0F;
	private int debugFrameCalls = 0;
	private int debugTickCounter = 0;
	private float targetCameraRoll = 0.0F;
	private float currentCameraRoll = 0.0F;
	private float prevCameraRoll = 0.0F;
	private float targetFovBonus = 0.0F;
	private float currentFovBonus = 0.0F;
	private float prevFovBonus = 0.0F;
	private float momentumRollIntensity = 0.0F;
	private float currentMomentumRoll = 0.0F;
	private float prevMomentumRoll = 0.0F;
	private float armOffsetY = 0.0F;
	private float prevArmOffsetY = 0.0F;
	private float targetArmOffsetY = 0.0F;
	private float armRotX = 0.0F;
	private float prevArmRotX = 0.0F;
	private float targetArmRotX = 0.0F;
	private int luckyNumber = 0;
	private String currentAnimationName = null;
	private int inputBufferIndex = 0;
	private final int inputBufferSize = 10;
	private final Map<MomentumEffectType, Set<MomentumEffect>> pendingEffectPool = new HashMap<>();
	private final HashSet<Integer>[] inputBuffer = new HashSet[10];
	private final List<BlockPos> wallBlocks = new ArrayList<>();
	private int jumpTimer = 0;
	private int slideCooldown = 0;
	private boolean slideJumpCooldownJustFinished = false;
	private int breakFallTimer = 0;
	private int dodgeTimer = 0;
	private int dodgeCooldown = 0;
	private int transitionExtraData = -1;
	private StateType previousStateType = StateType.ORIGINAL;
	private byte transitionWallData = -1;
	private int vaultTimer = 0;
	private int breakFallReadyCount = -1;
	private int wallJumpTimer = 0;
	private int wallRunReentryGraceTicks = 0;
	private int swimPushTimer = 0;
	private int wallKickCooldown = 0;
	private int airborneTimer = 0;
	public static MomentumEffect DEFAULT_FRICTION = new MomentumEffect(new Vec3(0.9, 0.0, 0.0), Vec3.ZERO, MomentumEffect.EffectType.MULTIPLIER, -1);
	public MomentumEffect BREAK_FALL_FRICTION = new MomentumEffect(new Vec3(0.5, 0.0, 0.0), Vec3.ZERO, MomentumEffect.EffectType.MULTIPLIER, 0);
	public MomentumEffect DODGE_BLOCK_FRICTION = new MomentumEffect(new Vec3(0.0, 0.0, 0.0), Vec3.ZERO, MomentumEffect.EffectType.MULTIPLIER, 0);
	public MomentumEffect SLIDE_FRICTION = new MomentumEffect(new Vec3(0.1, 0.0, 0.0), Vec3.ZERO, MomentumEffect.EffectType.MULTIPLIER, -1);
	public MomentumEffect SLIDE_BLOCK_FRICTION = new MomentumEffect(Vec3.ZERO, Vec3.ZERO, MomentumEffect.EffectType.MULTIPLIER, 0);
	public MomentumEffect SLIDE_ACCELERATION = new MomentumEffect(Vec3.ZERO, Vec3.ZERO, MomentumEffect.EffectType.COMPOSE, 0);
	public MomentumEffect AIR_ACCELERATION = new MomentumEffect(new Vec3(1.0, 1.0, 1.0), Vec3.ZERO, MomentumEffect.EffectType.MULTIPLIER, -1);
	public static MomentumEffect AIR_LIMIT_ACCELERATION = new MomentumEffect(new Vec3(0.3, Float.MAX_VALUE, 0.3), Vec3.ZERO, MomentumEffect.EffectType.COMPOSE, -1);
	public static MomentumEffect WALL_FRICTION = new MomentumEffect(new Vec3(0.4, 0.0, 0.0), Vec3.ZERO, MomentumEffect.EffectType.COMPOSE, -1);
	private static final Vec3[] WALL_CARDINALS = new Vec3[]{new Vec3(0.0, 0.0, -1.0), new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0), new Vec3(-1.0, 0.0, 0.0)};
	private static final Vec3[] WALL_DIAGONALS = new Vec3[]{
		new Vec3(1.0, 0.0, 1.0).normalize(), new Vec3(1.0, 0.0, -1.0).normalize(), new Vec3(-1.0, 0.0, 1.0).normalize(), new Vec3(-1.0, 0.0, -1.0).normalize()
	};
	private static final Vec3[] ALL_WALL_NORMALS = new Vec3[]{
		WALL_CARDINALS[0], WALL_CARDINALS[1], WALL_CARDINALS[2], WALL_CARDINALS[3], WALL_DIAGONALS[0], WALL_DIAGONALS[1], WALL_DIAGONALS[2], WALL_DIAGONALS[3]
	};

	public PlayerMovementContext(Player player) {
		for (MomentumEffectType type : MomentumEffectType.values()) {
			this.pendingEffectPool.put(type, new HashSet<>());
		}

		for (int i = 0; i < 10; i++) {
			this.inputBuffer[i] = new HashSet<>();
		}

		if (player.level().isClientSide()) {
			this.controller = (MomentumAnimationController)PlayerAnimationAccess.getPlayerAnimationLayer(
				player, Identifier.fromNamespaceAndPath("momentum", "movement_anim")
			);
			if (this.controller != null) {
				this.mocha = this.controller.getMolangRuntime();
				this.bindVariables();
			}
		}
	}

	private void bindVariables() {
		if (this.mocha.scope().get("math") instanceof ObjectValue math) {
			math.setFunction("clamp", Mth::clamp);
			math.setFunction("min", Math::min);
			math.setFunction("atan2", (y, x) -> (float)Math.toDegrees(Mth.atan2(y, x)));
		} else {
			LOGGER.warn("Failed to bind math.min_angle_180 to Mocha");
		}

		if (this.mocha.scope().get("variable") instanceof ObjectValue variable) {
			Minecraft mc = Minecraft.getInstance();
			variable.setFunction("get_movement_speed", () -> {
				float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
				return Mth.lerp(pt, (float)this.oldSpeed.horizontalDistance(), (float)this.getSpeed().horizontalDistance());
			});
			variable.setFunction("get_movement_y_speed", () -> {
				float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
				return Mth.lerp(pt, (float)this.oldSpeed.y, (float)this.getSpeed().y());
			});
			variable.setFunction("stable_body_head_angle", () -> {
				float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
				return Mth.lerp(pt, this.prevBodyHeadAngleDiff, this.bodyHeadAngleDiff);
			});
			variable.setFunction("smooth_head_x_rotation", () -> {
				if (this.controller != null && this.controller.getAvatar() instanceof Player player) {
					float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
					return Mth.lerp(pt, this.prevHeadXRot, player.getXRot());
				} else {
					return 0.0F;
				}
			});
			variable.setFunction("dolphin_tilt", () -> {
				float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
				Vec3 speed = this.getSpeed();
				Vec3 old = this.oldSpeed;
				float h = Mth.lerp(pt, (float)old.horizontalDistance(), (float)speed.horizontalDistance());
				float vy = Mth.lerp(pt, (float)old.y, (float)speed.y);
				float thetaDeg = (float)Math.toDegrees(Math.atan2(vy, Math.max(h, 0.001)));
				return -thetaDeg;
			});
		} else {
			LOGGER.warn("Failed to bind variable.get_movement_speed to Mocha");
		}
	}

	public void resetEffect() {
		for (MomentumEffectType type : MomentumEffectType.values()) {
			this.pendingEffectPool.get(type).clear();
		}
	}

	public void serverTick(Player player) {
		if (this.jumpTimer > 0) {
			this.jumpTimer--;
		}

		if (this.slideCooldown > 0) {
			this.slideCooldown--;
		}

		if (this.breakFallTimer > 0) {
			this.breakFallTimer--;
		}

		if (this.dodgeTimer > 0) {
			this.dodgeTimer--;
		}

		if (this.dodgeCooldown > 0) {
			this.dodgeCooldown--;
		}

		if (this.vaultTimer > 0) {
			this.vaultTimer--;
		}

		if (this.jumpCooldown > 0) {
			this.jumpCooldown--;
		}

		if (this.breakFallReadyCount > 0) {
			this.breakFallReadyCount--;
		}

		if (this.wallJumpTimer > 0) {
			this.wallJumpTimer--;
		}

		if (this.wallRunReentryGraceTicks > 0) {
			this.wallRunReentryGraceTicks--;
		}

		if (this.swimPushTimer > 0) {
			this.swimPushTimer--;
		}

		if (this.wallKickCooldown > 0) {
			this.wallKickCooldown--;
		}

		boolean newHasJetBooster = PlatformServices.gameplay().hasJetBooster(player);
		if (newHasJetBooster != this.hasJetBooster) {
			this.hasJetBooster = newHasJetBooster;
			MomentumUtils.applyBoosterAttributes(player, newHasJetBooster);
		}

		this.canMomentum = this.checkMomentum(this.hasJetBooster);
		if (player.fallDistance > 0.0) {
			this.lastFallDistance = player.fallDistance;
		}
	}

	public void clientTick(Player player) {
		this.captureMovementInput(player);
		if (this.slideCooldown == 1 && !SlideState.SLIDE.equals(this.currentAnimationName)) {
			player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.3F, 1.5F);
		}

		this.serverTick(player);
		this.prevBodyHeadAngleDiff = this.bodyHeadAngleDiff;
		this.prevHeadXRot = player.getXRot();
		this.doubleClickUp = this.isDoubleClick(0);
		this.doubleClickDown = this.isDoubleClick(1);
		this.doubleClickLeft = this.isDoubleClick(2);
		this.doubleClickRight = this.isDoubleClick(3);
		this.doubleClickJump = this.isDoubleClick(4);
		this.doubleClickSprint = this.isDoubleClick(5);
		double moveSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double jumpStrength = player.getJumpBoostPower();
		this.jumpLimitSpeed = moveSpeed * (1.0 + jumpStrength) * 3.0;
		double baseJumpAccel = moveSpeed * (1.0 + jumpStrength) * 1.2;
		double jumpRatio = this.speed.horizontalDistance() / Math.max(this.jumpLimitSpeed, 0.001);
		this.jumpAcceleration = baseJumpAccel * Math.max(0.0, 1.0 - jumpRatio * jumpRatio);
		this.setWorldInputVec(player);
		this.detectWall(player);
		this.bodyHeadAngleDiff = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
		this.tickCameraRoll();
		this.computeTargetFovBonus(player);
		this.tickFovBonus();
		this.tickMomentumRoll(player);
		this.tickArmOffset();
	}

	public void clientTickRemote(Player player) {
		this.prevBodyHeadAngleDiff = this.bodyHeadAngleDiff;
		this.prevHeadXRot = player.getXRot();
		this.bodyHeadAngleDiff = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
		if (this.jumpTimer > 0) {
			this.jumpTimer--;
		}

		if (this.slideCooldown > 0) {
			this.slideCooldown--;
		}

		if (this.breakFallTimer > 0) {
			this.breakFallTimer--;
		}

		if (this.dodgeTimer > 0) {
			this.dodgeTimer--;
		}

		if (this.dodgeCooldown > 0) {
			this.dodgeCooldown--;
		}

		if (this.vaultTimer > 0) {
			this.vaultTimer--;
		}

		if (this.jumpCooldown > 0) {
			this.jumpCooldown--;
		}

		if (this.breakFallReadyCount > 0) {
			this.breakFallReadyCount--;
		}

		if (this.wallJumpTimer > 0) {
			this.wallJumpTimer--;
		}

		if (this.wallRunReentryGraceTicks > 0) {
			this.wallRunReentryGraceTicks--;
		}

		if (this.swimPushTimer > 0) {
			this.swimPushTimer--;
		}

		if (this.wallKickCooldown > 0) {
			this.wallKickCooldown--;
		}

		this.remoteDetectWall(player);
	}

	public void tickArmOffset() {
		this.prevArmOffsetY = this.armOffsetY;
		this.prevArmRotX = this.armRotX;
		this.armOffsetY = Mth.lerp(0.2F, this.armOffsetY, this.targetArmOffsetY);
		this.armRotX = Mth.lerp(0.2F, this.armRotX, this.targetArmRotX);
	}

	public float getRenderArmOffsetY(float partialTick) {
		return Mth.lerp(partialTick, this.prevArmOffsetY, this.armOffsetY);
	}

	public float getRenderArmRotX(float partialTick) {
		return Mth.lerp(partialTick, this.prevArmRotX, this.armRotX);
	}

	public void setTargetArmTransform(float offsetY, float rotX) {
		this.targetArmOffsetY = offsetY;
		this.targetArmRotX = rotX;
	}

	public void setLuckyNumber(Player player) {
		this.luckyNumber = player.getRandom().nextInt(100);
	}

	public static float computeAnimSpeedFromVelocityAngle(Vec3 curSpeed, Vec3 prevSpeed) {
		float hCur = (float)curSpeed.horizontalDistance();
		float hPrev = (float)prevSpeed.horizontalDistance();
		double thetaCur = Math.atan2(curSpeed.y, Math.max(hCur, 0.001));
		double thetaPrev = Math.atan2(prevSpeed.y, Math.max(hPrev, 0.001));
		double deltaDeg = Math.toDegrees(Math.abs(thetaCur - thetaPrev));
		return (float)(0.4 + Math.clamp(deltaDeg * 0.14, 0.0, 2.5));
	}

	public static float computeAnimSpeedByAngleTracking(
		Vec3 velocity, Vec3 prevVelocity, float animTickSec, float animLengthSec, float angleTop, float angleBottom, float kP, float kF
	) {
		float h = (float)velocity.horizontalDistance();
		float thetaDeg = (float)Math.toDegrees(Math.atan2(velocity.y * 0.2F, Math.max(h, 0.001)));
		float thetaClamped = Mth.clamp(thetaDeg, angleBottom, angleTop);
		float hPrev = (float)prevVelocity.horizontalDistance();
		float thetaPrevDeg = (float)Math.toDegrees(Math.atan2(prevVelocity.y * 0.2F, Math.max(hPrev, 0.001)));
		float thetaPrevClamped = Mth.clamp(thetaPrevDeg, angleBottom, angleTop);
		float range = angleTop - angleBottom;
		if (range <= 1.0E-6F) {
			return 1.0F;
		}

		float fraction = (angleTop - thetaClamped) / range;
		float targetPos = fraction * animLengthSec;
		float angleDelta = thetaPrevClamped - thetaClamped;
		float feedForward = angleDelta / range * animLengthSec * 20.0F * kF;
		float error = targetPos - animTickSec;
		float pCorrection = kP * error;
		float speed = feedForward + pCorrection;
		return Mth.clamp(speed, 0.0F, 5.0F);
	}

	public void tickCameraRoll() {
		this.prevCameraRoll = this.currentCameraRoll;
		float smoothing = Math.abs(this.targetCameraRoll) > Math.abs(this.currentCameraRoll) ? 0.24F : 0.5F;
		float diff = this.targetCameraRoll - this.currentCameraRoll;
		this.currentCameraRoll += diff * smoothing;
		if (Math.abs(this.currentCameraRoll - this.targetCameraRoll) < 0.01F) {
			this.currentCameraRoll = this.targetCameraRoll;
		}
	}

	public void tickFovBonus() {
		this.prevFovBonus = this.currentFovBonus;
		float diff = this.targetFovBonus - this.currentFovBonus;
		this.currentFovBonus += diff * 0.15F;
	}

	private void computeTargetFovBonus(Player player) {
		MovementStateMachine sm = PlatformServices.gameplay().movementState(player);
		if (sm.getCurrentState().getStateType() == StateType.ORIGINAL) {
			this.targetFovBonus = 0.0F;
		} else if (!MomentumClientConfig.ENABLE_CAMERA_OFFSET.get()) {
			this.targetFovBonus = 0.0F;
		} else {
			double speedPerSec = this.getSpeed().horizontalDistance() * 20.0;
			double minSpeed = 5.0;
			double maxSpeed = 21.0;
			float speedFactor = (float)Mth.clamp((speedPerSec - minSpeed) / (maxSpeed - minSpeed), 0.0, 1.0);
			float fovEffectScale = PlatformServices.client().fovEffectScale();
			this.targetFovBonus = speedFactor * (float)MomentumClientConfig.FOV_BONUS_MAX.get().doubleValue() * fovEffectScale;
		}
	}

	public void tickMomentumRoll(Player player) {
		this.prevMomentumRoll = this.currentMomentumRoll;
		float targetRoll = 0.0F;
		if (this.momentumRollIntensity != 0.0F) {
			Vec3 velocity = player.getDeltaMovement();
			double horizontalSpeed = velocity.horizontalDistance();
			double speedPerSec = horizontalSpeed * 20.0;
			double minSpeed = 5.0;
			double maxSpeed = 21.0;
			if (speedPerSec > minSpeed) {
				Vec3 vDir = new Vec3(velocity.x, 0.0, velocity.z).normalize();
				float yaw = player.getYRot();
				Vec3 lookDir = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw)));
				double crossY = vDir.x * lookDir.z - vDir.z * lookDir.x;
				double angleThreshold = 0.26;
				if (Math.abs(crossY) > angleThreshold) {
					double sign = Math.signum(crossY);
					double mappedCross = (Math.abs(crossY) - angleThreshold) / (1.0 - angleThreshold);
					mappedCross = Math.min(1.0, mappedCross) * sign;
					float speedFactor = (float)Mth.clamp((speedPerSec - minSpeed) / (maxSpeed - minSpeed), 0.0, 1.0);
					targetRoll = (float)mappedCross * this.momentumRollIntensity * speedFactor;
				}
			}
		}

		float smoothing = 0.2F;
		this.currentMomentumRoll = Mth.lerp(smoothing, this.currentMomentumRoll, targetRoll);
		if (Math.abs(this.currentMomentumRoll) < 0.01F && targetRoll == 0.0F) {
			this.currentMomentumRoll = 0.0F;
		}
	}

	public float getRenderMomentumRoll(float partialTick) {
		return Mth.lerp(partialTick, this.prevMomentumRoll, this.currentMomentumRoll);
	}

	public float getRenderCameraRoll(float partialTick) {
		return Mth.lerp(partialTick, this.prevCameraRoll, this.currentCameraRoll);
	}

	public float getRenderFovBonus(float partialTick) {
		return Mth.lerp(partialTick, this.prevFovBonus, this.currentFovBonus);
	}

	public boolean isDoubleClick(int key) {
		int len = this.inputBuffer.length;
		int current = this.inputBufferIndex;
		if (!this.inputBuffer[current].contains(key)) {
			return false;
		}

		boolean foundFirst = false;
		int idx = current;

		for (int i = 1; i < len; i++) {
			int lastIdx = idx;
			idx = (current + i) % len;
			if (this.inputBuffer[idx].contains(key) && !foundFirst) {
				foundFirst = true;
			} else if (foundFirst) {
				for (int clickKey : this.inputBuffer[idx]) {
					if (!this.inputBuffer[lastIdx].contains(clickKey) && clickKey != 4 && clickKey != key) {
						foundFirst = false;
					}
				}
			}
		}

		return foundFirst;
	}

	public boolean wasKeyPressedRecently(int key, int offset) {
		int len = this.inputBuffer.length;
		int current = this.inputBufferIndex;

		for (int i = 1; i <= Math.min(offset, this.inputBuffer.length - 1); i++) {
			int idx = (current - i + len) % len;
			if (this.inputBuffer[idx].contains(key)) {
				return true;
			}
		}

		return false;
	}

	private boolean checkMomentum(boolean hasJetBooster) {
		return MomentumServerConfig.ALLOW_MANEUVER_WITHOUT_BOOSTER.get() ? true : hasJetBooster;
	}

	public static int encodeWallNormal(Vec3 normal) {
		if (normal != null && !(normal.lengthSqr() < 0.001)) {
			for (int i = 0; i < ALL_WALL_NORMALS.length; i++) {
				if (ALL_WALL_NORMALS[i].distanceToSqr(normal) < 0.001) {
					return i;
				}
			}

			return -1;
		} else {
			return -1;
		}
	}

	public static Vec3 decodeWallNormal(int index) {
		return index >= 0 && index < ALL_WALL_NORMALS.length ? ALL_WALL_NORMALS[index] : Vec3.ZERO;
	}

	public void detectWall(Player player) {
		AABB box = player.getBoundingBox();
		Level level = player.level();
		double reach = 0.5;
		float yawRad = (float)Math.toRadians(player.getYRot());
		Vec3 lookVec = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
		Vec3 inputVec = this.inputVec;
		boolean hasInput = inputVec.lengthSqr() > 0.001;
		Vec3 inputNorm = hasInput ? inputVec.normalize() : lookVec;
		List<PlayerMovementContext.WallCandidate> cardCands = this.collectWallCandidates(
			player, level, box, WALL_CARDINALS, lookVec, inputVec, inputNorm, hasInput, reach
		);
		PlayerMovementContext.WallCandidate best = this.pickByPriority(cardCands, hasInput);
		if (best == null) {
			List<PlayerMovementContext.WallCandidate> diagCands = this.collectWallCandidates(
				player, level, box, WALL_DIAGONALS, lookVec, inputVec, inputNorm, hasInput, reach
			);
			best = this.pickByPriority(diagCands, hasInput);
		}

		if (best == null) {
			this.hasFaceWall = false;
			this.hasLedge = false;
			this.ledgeOffsetY = 0.0;
			this.wallNormal = Vec3.ZERO;
			this.lookWallAngle = 360.0F;
			this.inputWallAngle = 360.0F;
			this.wallBlocks.clear();
		} else {
			this.wallBlocks.clear();
			BlockPos.betweenClosedStream(best.expanded).forEach(pos -> {
				BlockState state = level.getBlockState(pos);
				if (!state.isAir()) {
					this.wallBlocks.add(pos.immutable());
				}
			});
			this.hasFaceWall = best.hasFaceWall;
			this.hasLedge = best.hasLedge;
			this.ledgeOffsetY = best.ledgeOffsetY;
			this.wallNormal = best.normal;
			this.lookWallAngle = best.lookAngle;
			this.inputWallAngle = hasInput ? best.inputAngle : 360.0F;
		}
	}

	private List<PlayerMovementContext.WallCandidate> collectWallCandidates(
		Player player, Level level, AABB box, Vec3[] normals, Vec3 lookVec, Vec3 inputVec, Vec3 inputNorm, boolean hasInput, double reach
	) {
		List<PlayerMovementContext.WallCandidate> result = new ArrayList<>();
		double eyeY = player.getEyeY();
		double topY = box.maxY;
		double headHalf = topY - eyeY;
		double chinY = eyeY - headHalf * 2.0;

		for (Vec3 normal : normals) {
			AABB expanded = box.expandTowards(normal.x * reach, 0.0, normal.z * reach);
			if (!level.noCollision(player, expanded)) {
				PlayerMovementContext.WallCandidate c = new PlayerMovementContext.WallCandidate();
				c.normal = normal;
				c.expanded = expanded;
				double lookDot = lookVec.x * normal.x + lookVec.z * normal.z;
				double lookCross = lookVec.x * normal.z - lookVec.z * normal.x;
				c.lookAngle = (float)Math.toDegrees(Math.atan2(lookCross, lookDot));
				c.lookAngleDiff = (float)Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, lookDot))));
				if (hasInput) {
					double inputDot = inputVec.x * normal.x + inputVec.z * normal.z;
					double inputCross = inputVec.x * normal.z - inputVec.z * normal.x;
					c.inputAngle = (float)Math.toDegrees(Math.atan2(inputCross, inputDot));
					double normDot = inputNorm.x * normal.x + inputNorm.z * normal.z;
					c.inputAngleDiff = (float)Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, normDot))));
				} else {
					c.inputAngle = 360.0F;
					c.inputAngleDiff = 360.0F;
				}

				AABB ledgeBox = new AABB(box.minX + normal.x * reach, eyeY, box.minZ + normal.z * reach, box.maxX + normal.x * reach, topY, box.maxZ + normal.z * reach);
				AABB chinBox = new AABB(box.minX + normal.x * reach, chinY, box.minZ + normal.z * reach, box.maxX + normal.x * reach, eyeY, box.maxZ + normal.z * reach);
				c.hasFaceWall = !level.noCollision(player, chinBox);
				c.hasLedge = level.noCollision(player, ledgeBox) && c.hasFaceWall;
				if (!c.hasLedge && c.hasFaceWall && player.getDeltaMovement().y < -0.4) {
					double fallDist = Math.abs(player.getDeltaMovement().y);

					for (double yOff = 0.2; yOff <= fallDist + 0.5; yOff += 0.2) {
						AABB sweepLedgeBox = new AABB(
							box.minX + normal.x * reach, eyeY + yOff, box.minZ + normal.z * reach, box.maxX + normal.x * reach, topY + yOff, box.maxZ + normal.z * reach
						);
						AABB sweepChinBox = new AABB(
							box.minX + normal.x * reach, chinY + yOff, box.minZ + normal.z * reach, box.maxX + normal.x * reach, eyeY + yOff, box.maxZ + normal.z * reach
						);
						if (level.noCollision(player, sweepLedgeBox) && !level.noCollision(player, sweepChinBox)) {
							c.hasLedge = true;
							c.ledgeOffsetY = yOff;
							break;
						}
					}
				}

				result.add(c);
			}
		}

		return result;
	}

	private PlayerMovementContext.WallCandidate pickByPriority(List<PlayerMovementContext.WallCandidate> candidates, boolean hasInput) {
		if (candidates.isEmpty()) {
			return null;
		}

		List<PlayerMovementContext.WallCandidate> ledged = new ArrayList<>();

		for (PlayerMovementContext.WallCandidate c : candidates) {
			if (c.hasLedge) {
				ledged.add(c);
			}
		}

		List<PlayerMovementContext.WallCandidate> pool = !ledged.isEmpty() ? ledged : candidates;
		PlayerMovementContext.WallCandidate best = null;
		float bestDiff = Float.MAX_VALUE;

		for (PlayerMovementContext.WallCandidate c : pool) {
			float diff = hasInput ? c.inputAngleDiff : c.lookAngleDiff;
			if (diff < bestDiff) {
				bestDiff = diff;
				best = c;
			}
		}

		return best;
	}

	public void setWorldInputVec(Player player) {
		float forward = this.movementInput.forward();
		float strafe = this.movementInput.strafe();
		float yaw = player.getYRot();
		double rad = Math.toRadians(yaw);
		double sin = Math.sin(rad);
		double cos = Math.cos(rad);
		double x = strafe * cos - forward * sin;
		double z = forward * cos + strafe * sin;
		this.inputVec = new Vec3(x, 0.0, z);
	}

	private void captureMovementInput(Player player) {
		MovementInput current = PlatformServices.client().captureMovementInput(player);
		MovementInput previous = this.movementInput;
		this.inputBufferIndex = (this.inputBufferIndex + 1) % 10;
		Set<Integer> pressedThisTick = this.inputBuffer[this.inputBufferIndex];
		pressedThisTick.clear();
		if (current.up() && !previous.up()) {
			pressedThisTick.add(0);
		}

		if (current.down() && !previous.down()) {
			pressedThisTick.add(1);
		}

		if (current.left() && !previous.left()) {
			pressedThisTick.add(2);
		}

		if (current.right() && !previous.right()) {
			pressedThisTick.add(3);
		}

		if (current.jump() && !previous.jump()) {
			pressedThisTick.add(4);
		}

		if (current.sprint() && !previous.sprint()) {
			pressedThisTick.add(5);
		}

		if (current.shift() && !previous.shift()) {
			pressedThisTick.add(6);
		}

		if (current.shift()) {
			pressedThisTick.add(7);
		}

		this.movementInput = current;
	}

	public void remoteDetectWall(Player player) {
		AABB box = player.getBoundingBox();
		Level level = player.level();
		double reach = 0.5;
		float yawRad = (float)Math.toRadians(player.getYRot());
		Vec3 lookVec = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
		Vec3 velocity = this.speed;
		Vec3 inputVec = new Vec3(velocity.x, 0.0, velocity.z);
		boolean hasInput = inputVec.horizontalDistanceSqr() > 1.0E-6;
		Vec3 inputNorm = hasInput ? inputVec.normalize() : lookVec;
		List<PlayerMovementContext.WallCandidate> cardCands = this.collectWallCandidates(
			player, level, box, WALL_CARDINALS, lookVec, inputVec, inputNorm, hasInput, reach
		);
		PlayerMovementContext.WallCandidate best = this.pickByPriority(cardCands, hasInput);
		if (best == null) {
			List<PlayerMovementContext.WallCandidate> diagCands = this.collectWallCandidates(
				player, level, box, WALL_DIAGONALS, lookVec, inputVec, inputNorm, hasInput, reach
			);
			best = this.pickByPriority(diagCands, hasInput);
		}

		if (best == null) {
			if (this.transitionWallData >= 0) {
				this.applySyncedWallData(player);
			} else {
				this.setLookWallAngle(360.0F);
				this.setWallNormal(Vec3.ZERO);
				this.setInputWallAngle(360.0F);
				this.setHasFaceWall(false);
				this.setHasLedge(false);
				this.setLedgeOffsetY(0.0);
			}
		} else {
			this.setWallNormal(best.normal);
			this.setLookWallAngle(best.lookAngle);
			this.setInputWallAngle(hasInput ? best.inputAngle : 360.0F);
			this.setHasFaceWall(best.hasFaceWall);
			this.setHasLedge(best.hasLedge);
			this.setLedgeOffsetY(best.ledgeOffsetY);
		}
	}

	private void applySyncedWallData(Player player) {
		int wallIndex = this.transitionWallData & 7;
		Vec3 wallNormal = decodeWallNormal(wallIndex);
		if (Vec3.ZERO.equals(wallNormal)) {
			this.setWallNormal(Vec3.ZERO);
			this.setLookWallAngle(360.0F);
			this.setInputWallAngle(360.0F);
			this.setHasFaceWall(false);
			this.setHasLedge(false);
			this.setLedgeOffsetY(0.0);
		} else {
			AABB box = player.getBoundingBox();
			Level level = player.level();
			double reach = 0.5;
			float yawRad = (float)Math.toRadians(player.getYRot());
			Vec3 lookVec = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
			double lookDot = lookVec.x * wallNormal.x + lookVec.z * wallNormal.z;
			double lookCross = lookVec.x * wallNormal.z - lookVec.z * wallNormal.x;
			float lookAngle = (float)Math.toDegrees(Math.atan2(lookCross, lookDot));
			Vec3 velocity = player.getDeltaMovement();
			Vec3 inputVec = new Vec3(velocity.x, 0.0, velocity.z);
			boolean hasInput = inputVec.horizontalDistanceSqr() > 1.0E-6;
			float inputAngle = 360.0F;
			if (hasInput) {
				double inputDot = inputVec.x * wallNormal.x + inputVec.z * wallNormal.z;
				double inputCross = inputVec.x * wallNormal.z - inputVec.z * wallNormal.x;
				inputAngle = (float)Math.toDegrees(Math.atan2(inputCross, inputDot));
			}

			AABB expanded = box.expandTowards(wallNormal.x * reach, 0.0, wallNormal.z * reach);
			double eyeY = player.getEyeY();
			double topY = box.maxY;
			double headHalf = topY - eyeY;
			double chinY = eyeY - headHalf * 2.0;
			AABB chinBox = new AABB(
				box.minX + wallNormal.x * reach, chinY, box.minZ + wallNormal.z * reach, box.maxX + wallNormal.x * reach, eyeY, box.maxZ + wallNormal.z * reach
			);
			AABB ledgeBox = new AABB(
				box.minX + wallNormal.x * reach, eyeY, box.minZ + wallNormal.z * reach, box.maxX + wallNormal.x * reach, topY, box.maxZ + wallNormal.z * reach
			);
			boolean hasFaceWall = !level.noCollision(player, chinBox);
			boolean hasLedge = level.noCollision(player, ledgeBox) && hasFaceWall;
			this.setWallNormal(wallNormal);
			this.setLookWallAngle(lookAngle);
			this.setInputWallAngle(hasInput ? inputAngle : 360.0F);
			this.setHasFaceWall(hasFaceWall);
			this.setHasLedge(hasLedge);
			this.setLedgeOffsetY(0.0);
			this.wallBlocks.clear();
			BlockPos.betweenClosedStream(expanded).forEach(pos -> {
				BlockState state = level.getBlockState(pos);
				if (!state.isAir()) {
					this.wallBlocks.add(pos.immutable());
				}
			});
		}
	}

	public void playWallSound(Player player, Function<SoundType, SoundEvent> kind, float volumeMultiplier, float pitchMultiplier) {
		if (!this.wallBlocks.isEmpty()) {
			Level level = player.level();
			BlockPos pos = this.wallBlocks.get(level.getRandom().nextInt(this.wallBlocks.size()));
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				SoundType soundType = PlatformServices.gameplay().soundType(state, level, pos, player);
				player.playSound(kind.apply(soundType), soundType.getVolume() * volumeMultiplier, soundType.getPitch() * pitchMultiplier);
			}
		}
	}

	public void addPermanentEffect(MomentumEffectType type, MomentumEffect effect) {
		effect.setDuration(-1);
		effect.setElapsedDuration(0);
		this.pendingEffectPool.get(type).add(effect);
	}

	public void addEffect(MomentumEffectType type, MomentumEffect effect, int duration) {
		effect.setDuration(duration);
		effect.setElapsedDuration(0);
		this.pendingEffectPool.get(type).add(effect);
	}

	public void removeEffect(MomentumEffectType type, MomentumEffect effect) {
		effect.setElapsedDuration(0);
		this.pendingEffectPool.get(type).remove(effect);
	}

	public void setJumpAnimationSpeed(float jumpAnimationSpeed) {
		if (jumpAnimationSpeed > 10.0F) {
			jumpAnimationSpeed = 10.0F;
		}

		this.jumpAnimationSpeed = jumpAnimationSpeed;
	}

	@Generated
	public MomentumAnimationController getController() {
		return this.controller;
	}

	@Generated
	public MochaEngine<AnimationController> getMocha() {
		return this.mocha;
	}

	@Generated
	public boolean isHasJetBooster() {
		return this.hasJetBooster;
	}

	@Generated
	public boolean isCanMomentum() {
		return this.canMomentum;
	}

	@Generated
	public boolean isNoMoveInput() {
		return this.noMoveInput;
	}

	@Generated
	public boolean isNoJump() {
		return this.noJump;
	}

	@Generated
	public boolean isHasLedge() {
		return this.hasLedge;
	}

	@Generated
	public double getLedgeOffsetY() {
		return this.ledgeOffsetY;
	}

	@Generated
	public boolean isHasFaceWall() {
		return this.hasFaceWall;
	}

	@Generated
	public boolean isDoubleClickUp() {
		return this.doubleClickUp;
	}

	@Generated
	public boolean isDoubleClickDown() {
		return this.doubleClickDown;
	}

	@Generated
	public boolean isDoubleClickLeft() {
		return this.doubleClickLeft;
	}

	@Generated
	public boolean isDoubleClickRight() {
		return this.doubleClickRight;
	}

	@Generated
	public boolean isDoubleClickJump() {
		return this.doubleClickJump;
	}

	@Generated
	public boolean isDoubleClickSprint() {
		return this.doubleClickSprint;
	}

	@Generated
	public boolean isToBreakFallState() {
		return this.toBreakFallState;
	}

	@Generated
	public boolean isLeftFootJump() {
		return this.leftFootJump;
	}

	@Generated
	public Vec3 getSpeed() {
		return this.speed;
	}

	@Generated
	public Vec3 getOldSpeed() {
		return this.oldSpeed;
	}

	@Generated
	public Vec3 getOldDeltaMovement() {
		return this.oldDeltaMovement;
	}

	@Generated
	public double getBlockStep() {
		return this.blockStep;
	}

	@Generated
	public Vec3 getSlopeUnitVector() {
		return this.slopeUnitVector;
	}

	@Generated
	public double getLastFallDistance() {
		return this.lastFallDistance;
	}

	@Generated
	public int getDodgeInvincible() {
		return 10;
	}

	@Generated
	public Vec3 getInputVec() {
		return this.inputVec;
	}

	@Generated
	public MovementInput getMovementInput() {
		return this.movementInput;
	}

	@Generated
	public Vec3 getWallNormal() {
		return this.wallNormal;
	}

	@Generated
	public Vec3 getRunWallNormal() {
		return this.runWallNormal;
	}

	@Generated
	public float getLookWallAngle() {
		return this.lookWallAngle;
	}

	@Generated
	public float getInputWallAngle() {
		return this.inputWallAngle;
	}

	@Generated
	public double getJumpLimitSpeed() {
		return this.jumpLimitSpeed;
	}

	@Generated
	public double getJumpAcceleration() {
		return this.jumpAcceleration;
	}

	@Generated
	public int getJumpCooldown() {
		return this.jumpCooldown;
	}

	@Generated
	public float getWallFriction() {
		return this.wallFriction;
	}

	@Generated
	public float getNeedSoundTick() {
		return this.needSoundTick;
	}

	@Generated
	public float getGravityModify() {
		return this.gravityModify;
	}

	@Generated
	public float getJumpAnimationSpeed() {
		return this.jumpAnimationSpeed;
	}

	@Generated
	public float getBodyHeadAngleDiff() {
		return this.bodyHeadAngleDiff;
	}

	@Generated
	public float getPrevBodyHeadAngleDiff() {
		return this.prevBodyHeadAngleDiff;
	}

	@Generated
	public float getPrevHeadXRot() {
		return this.prevHeadXRot;
	}

	@Generated
	public int getDebugFrameCalls() {
		return this.debugFrameCalls;
	}

	@Generated
	public int getDebugTickCounter() {
		return this.debugTickCounter;
	}

	@Generated
	public float getTargetCameraRoll() {
		return this.targetCameraRoll;
	}

	@Generated
	public float getCurrentCameraRoll() {
		return this.currentCameraRoll;
	}

	@Generated
	public float getPrevCameraRoll() {
		return this.prevCameraRoll;
	}

	@Generated
	public float getTargetFovBonus() {
		return this.targetFovBonus;
	}

	@Generated
	public float getCurrentFovBonus() {
		return this.currentFovBonus;
	}

	@Generated
	public float getPrevFovBonus() {
		return this.prevFovBonus;
	}

	@Generated
	public float getMomentumRollIntensity() {
		return this.momentumRollIntensity;
	}

	@Generated
	public float getCurrentMomentumRoll() {
		return this.currentMomentumRoll;
	}

	@Generated
	public float getPrevMomentumRoll() {
		return this.prevMomentumRoll;
	}

	@Generated
	public float getArmOffsetY() {
		return this.armOffsetY;
	}

	@Generated
	public float getPrevArmOffsetY() {
		return this.prevArmOffsetY;
	}

	@Generated
	public float getTargetArmOffsetY() {
		return this.targetArmOffsetY;
	}

	@Generated
	public float getArmRotX() {
		return this.armRotX;
	}

	@Generated
	public float getPrevArmRotX() {
		return this.prevArmRotX;
	}

	@Generated
	public float getTargetArmRotX() {
		return this.targetArmRotX;
	}

	@Generated
	public int getLuckyNumber() {
		return this.luckyNumber;
	}

	@Generated
	public String getCurrentAnimationName() {
		return this.currentAnimationName;
	}

	@Generated
	public int getInputBufferIndex() {
		return this.inputBufferIndex;
	}

	@Generated
	public int getInputBufferSize() {
		return 10;
	}

	@Generated
	public Map<MomentumEffectType, Set<MomentumEffect>> getPendingEffectPool() {
		return this.pendingEffectPool;
	}

	@Generated
	public HashSet<Integer>[] getInputBuffer() {
		return this.inputBuffer;
	}

	@Generated
	public List<BlockPos> getWallBlocks() {
		return this.wallBlocks;
	}

	@Generated
	public int getJumpTimer() {
		return this.jumpTimer;
	}

	@Generated
	public int getSlideCooldown() {
		return this.slideCooldown;
	}

	@Generated
	public boolean isSlideJumpCooldownJustFinished() {
		return this.slideJumpCooldownJustFinished;
	}

	@Generated
	public int getBreakFallTimer() {
		return this.breakFallTimer;
	}

	@Generated
	public int getDodgeTimer() {
		return this.dodgeTimer;
	}

	@Generated
	public int getDodgeCooldown() {
		return this.dodgeCooldown;
	}

	@Generated
	public int getTransitionExtraData() {
		return this.transitionExtraData;
	}

	@Generated
	public StateType getPreviousStateType() {
		return this.previousStateType;
	}

	@Generated
	public byte getTransitionWallData() {
		return this.transitionWallData;
	}

	@Generated
	public int getVaultTimer() {
		return this.vaultTimer;
	}

	@Generated
	public int getBreakFallReadyCount() {
		return this.breakFallReadyCount;
	}

	@Generated
	public int getWallJumpTimer() {
		return this.wallJumpTimer;
	}

	@Generated
	public int getWallRunReentryGraceTicks() {
		return this.wallRunReentryGraceTicks;
	}

	@Generated
	public int getSwimPushTimer() {
		return this.swimPushTimer;
	}

	@Generated
	public int getWallKickCooldown() {
		return this.wallKickCooldown;
	}

	@Generated
	public int getAirborneTimer() {
		return this.airborneTimer;
	}

	@Generated
	public MomentumEffect getBREAK_FALL_FRICTION() {
		return this.BREAK_FALL_FRICTION;
	}

	@Generated
	public MomentumEffect getDODGE_BLOCK_FRICTION() {
		return this.DODGE_BLOCK_FRICTION;
	}

	@Generated
	public MomentumEffect getSLIDE_FRICTION() {
		return this.SLIDE_FRICTION;
	}

	@Generated
	public MomentumEffect getSLIDE_BLOCK_FRICTION() {
		return this.SLIDE_BLOCK_FRICTION;
	}

	@Generated
	public MomentumEffect getSLIDE_ACCELERATION() {
		return this.SLIDE_ACCELERATION;
	}

	@Generated
	public MomentumEffect getAIR_ACCELERATION() {
		return this.AIR_ACCELERATION;
	}

	@Generated
	public void setController(MomentumAnimationController controller) {
		this.controller = controller;
	}

	@Generated
	public void setMocha(MochaEngine<AnimationController> mocha) {
		this.mocha = mocha;
	}

	@Generated
	public void setHasJetBooster(boolean hasJetBooster) {
		this.hasJetBooster = hasJetBooster;
	}

	@Generated
	public void setCanMomentum(boolean canMomentum) {
		this.canMomentum = canMomentum;
	}

	@Generated
	public void setNoMoveInput(boolean noMoveInput) {
		this.noMoveInput = noMoveInput;
	}

	@Generated
	public void setNoJump(boolean noJump) {
		this.noJump = noJump;
	}

	@Generated
	public void setHasLedge(boolean hasLedge) {
		this.hasLedge = hasLedge;
	}

	@Generated
	public void setLedgeOffsetY(double ledgeOffsetY) {
		this.ledgeOffsetY = ledgeOffsetY;
	}

	@Generated
	public void setHasFaceWall(boolean hasFaceWall) {
		this.hasFaceWall = hasFaceWall;
	}

	@Generated
	public void setDoubleClickUp(boolean doubleClickUp) {
		this.doubleClickUp = doubleClickUp;
	}

	@Generated
	public void setDoubleClickDown(boolean doubleClickDown) {
		this.doubleClickDown = doubleClickDown;
	}

	@Generated
	public void setDoubleClickLeft(boolean doubleClickLeft) {
		this.doubleClickLeft = doubleClickLeft;
	}

	@Generated
	public void setDoubleClickRight(boolean doubleClickRight) {
		this.doubleClickRight = doubleClickRight;
	}

	@Generated
	public void setDoubleClickJump(boolean doubleClickJump) {
		this.doubleClickJump = doubleClickJump;
	}

	@Generated
	public void setDoubleClickSprint(boolean doubleClickSprint) {
		this.doubleClickSprint = doubleClickSprint;
	}

	@Generated
	public void setToBreakFallState(boolean toBreakFallState) {
		this.toBreakFallState = toBreakFallState;
	}

	@Generated
	public void setLeftFootJump(boolean leftFootJump) {
		this.leftFootJump = leftFootJump;
	}

	@Generated
	public void setSpeed(Vec3 speed) {
		this.speed = speed;
	}

	@Generated
	public void setOldSpeed(Vec3 oldSpeed) {
		this.oldSpeed = oldSpeed;
	}

	@Generated
	public void setOldDeltaMovement(Vec3 oldDeltaMovement) {
		this.oldDeltaMovement = oldDeltaMovement;
	}

	@Generated
	public void setBlockStep(double blockStep) {
		this.blockStep = blockStep;
	}

	@Generated
	public void setSlopeUnitVector(Vec3 slopeUnitVector) {
		this.slopeUnitVector = slopeUnitVector;
	}

	@Generated
	public void setLastFallDistance(double lastFallDistance) {
		this.lastFallDistance = lastFallDistance;
	}

	@Generated
	public void setInputVec(Vec3 inputVec) {
		this.inputVec = inputVec;
	}

	@Generated
	public void setMovementInput(MovementInput movementInput) {
		this.movementInput = movementInput;
	}

	@Generated
	public void setWallNormal(Vec3 wallNormal) {
		this.wallNormal = wallNormal;
	}

	@Generated
	public void setRunWallNormal(Vec3 runWallNormal) {
		this.runWallNormal = runWallNormal;
	}

	@Generated
	public void setLookWallAngle(float lookWallAngle) {
		this.lookWallAngle = lookWallAngle;
	}

	@Generated
	public void setInputWallAngle(float inputWallAngle) {
		this.inputWallAngle = inputWallAngle;
	}

	@Generated
	public void setJumpLimitSpeed(double jumpLimitSpeed) {
		this.jumpLimitSpeed = jumpLimitSpeed;
	}

	@Generated
	public void setJumpAcceleration(double jumpAcceleration) {
		this.jumpAcceleration = jumpAcceleration;
	}

	@Generated
	public void setJumpCooldown(int jumpCooldown) {
		this.jumpCooldown = jumpCooldown;
	}

	@Generated
	public void setWallFriction(float wallFriction) {
		this.wallFriction = wallFriction;
	}

	@Generated
	public void setNeedSoundTick(float needSoundTick) {
		this.needSoundTick = needSoundTick;
	}

	@Generated
	public void setGravityModify(float gravityModify) {
		this.gravityModify = gravityModify;
	}

	@Generated
	public void setBodyHeadAngleDiff(float bodyHeadAngleDiff) {
		this.bodyHeadAngleDiff = bodyHeadAngleDiff;
	}

	@Generated
	public void setPrevBodyHeadAngleDiff(float prevBodyHeadAngleDiff) {
		this.prevBodyHeadAngleDiff = prevBodyHeadAngleDiff;
	}

	@Generated
	public void setPrevHeadXRot(float prevHeadXRot) {
		this.prevHeadXRot = prevHeadXRot;
	}

	@Generated
	public void setDebugFrameCalls(int debugFrameCalls) {
		this.debugFrameCalls = debugFrameCalls;
	}

	@Generated
	public void setDebugTickCounter(int debugTickCounter) {
		this.debugTickCounter = debugTickCounter;
	}

	@Generated
	public void setTargetCameraRoll(float targetCameraRoll) {
		this.targetCameraRoll = targetCameraRoll;
	}

	@Generated
	public void setCurrentCameraRoll(float currentCameraRoll) {
		this.currentCameraRoll = currentCameraRoll;
	}

	@Generated
	public void setPrevCameraRoll(float prevCameraRoll) {
		this.prevCameraRoll = prevCameraRoll;
	}

	@Generated
	public void setTargetFovBonus(float targetFovBonus) {
		this.targetFovBonus = targetFovBonus;
	}

	@Generated
	public void setCurrentFovBonus(float currentFovBonus) {
		this.currentFovBonus = currentFovBonus;
	}

	@Generated
	public void setPrevFovBonus(float prevFovBonus) {
		this.prevFovBonus = prevFovBonus;
	}

	@Generated
	public void setMomentumRollIntensity(float momentumRollIntensity) {
		this.momentumRollIntensity = momentumRollIntensity;
	}

	@Generated
	public void setCurrentMomentumRoll(float currentMomentumRoll) {
		this.currentMomentumRoll = currentMomentumRoll;
	}

	@Generated
	public void setPrevMomentumRoll(float prevMomentumRoll) {
		this.prevMomentumRoll = prevMomentumRoll;
	}

	@Generated
	public void setArmOffsetY(float armOffsetY) {
		this.armOffsetY = armOffsetY;
	}

	@Generated
	public void setPrevArmOffsetY(float prevArmOffsetY) {
		this.prevArmOffsetY = prevArmOffsetY;
	}

	@Generated
	public void setTargetArmOffsetY(float targetArmOffsetY) {
		this.targetArmOffsetY = targetArmOffsetY;
	}

	@Generated
	public void setArmRotX(float armRotX) {
		this.armRotX = armRotX;
	}

	@Generated
	public void setPrevArmRotX(float prevArmRotX) {
		this.prevArmRotX = prevArmRotX;
	}

	@Generated
	public void setTargetArmRotX(float targetArmRotX) {
		this.targetArmRotX = targetArmRotX;
	}

	@Generated
	public void setCurrentAnimationName(String currentAnimationName) {
		this.currentAnimationName = currentAnimationName;
	}

	@Generated
	public void setInputBufferIndex(int inputBufferIndex) {
		this.inputBufferIndex = inputBufferIndex;
	}

	@Generated
	public void setJumpTimer(int jumpTimer) {
		this.jumpTimer = jumpTimer;
	}

	@Generated
	public void setSlideCooldown(int slideCooldown) {
		this.slideCooldown = slideCooldown;
	}

	@Generated
	public void setSlideJumpCooldownJustFinished(boolean slideJumpCooldownJustFinished) {
		this.slideJumpCooldownJustFinished = slideJumpCooldownJustFinished;
	}

	@Generated
	public void setBreakFallTimer(int breakFallTimer) {
		this.breakFallTimer = breakFallTimer;
	}

	@Generated
	public void setDodgeTimer(int dodgeTimer) {
		this.dodgeTimer = dodgeTimer;
	}

	@Generated
	public void setDodgeCooldown(int dodgeCooldown) {
		this.dodgeCooldown = dodgeCooldown;
	}

	@Generated
	public void setTransitionExtraData(int transitionExtraData) {
		this.transitionExtraData = transitionExtraData;
	}

	@Generated
	public void setPreviousStateType(StateType previousStateType) {
		this.previousStateType = previousStateType;
	}

	@Generated
	public void setTransitionWallData(byte transitionWallData) {
		this.transitionWallData = transitionWallData;
	}

	@Generated
	public void setVaultTimer(int vaultTimer) {
		this.vaultTimer = vaultTimer;
	}

	@Generated
	public void setBreakFallReadyCount(int breakFallReadyCount) {
		this.breakFallReadyCount = breakFallReadyCount;
	}

	@Generated
	public void setWallJumpTimer(int wallJumpTimer) {
		this.wallJumpTimer = wallJumpTimer;
	}

	@Generated
	public void setWallRunReentryGraceTicks(int wallRunReentryGraceTicks) {
		this.wallRunReentryGraceTicks = wallRunReentryGraceTicks;
	}

	@Generated
	public void setSwimPushTimer(int swimPushTimer) {
		this.swimPushTimer = swimPushTimer;
	}

	@Generated
	public void setWallKickCooldown(int wallKickCooldown) {
		this.wallKickCooldown = wallKickCooldown;
	}

	@Generated
	public void setAirborneTimer(int airborneTimer) {
		this.airborneTimer = airborneTimer;
	}

	@Generated
	public void setBREAK_FALL_FRICTION(MomentumEffect BREAK_FALL_FRICTION) {
		this.BREAK_FALL_FRICTION = BREAK_FALL_FRICTION;
	}

	@Generated
	public void setDODGE_BLOCK_FRICTION(MomentumEffect DODGE_BLOCK_FRICTION) {
		this.DODGE_BLOCK_FRICTION = DODGE_BLOCK_FRICTION;
	}

	@Generated
	public void setSLIDE_FRICTION(MomentumEffect SLIDE_FRICTION) {
		this.SLIDE_FRICTION = SLIDE_FRICTION;
	}

	@Generated
	public void setSLIDE_BLOCK_FRICTION(MomentumEffect SLIDE_BLOCK_FRICTION) {
		this.SLIDE_BLOCK_FRICTION = SLIDE_BLOCK_FRICTION;
	}

	@Generated
	public void setSLIDE_ACCELERATION(MomentumEffect SLIDE_ACCELERATION) {
		this.SLIDE_ACCELERATION = SLIDE_ACCELERATION;
	}

	@Generated
	public void setAIR_ACCELERATION(MomentumEffect AIR_ACCELERATION) {
		this.AIR_ACCELERATION = AIR_ACCELERATION;
	}

	private static class WallCandidate {
		Vec3 normal;
		AABB expanded;
		boolean hasFaceWall;
		boolean hasLedge;
		double ledgeOffsetY = 0.0;
		float lookAngle;
		float inputAngle;
		float lookAngleDiff;
		float inputAngleDiff;
	}
}
