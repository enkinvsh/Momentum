package com.akirahane.momentum.core.effect;

import lombok.Generated;
import net.minecraft.world.phys.Vec3;

public class MomentumEffect {
	private Vec3 value;
	private Vec3 modifyValue;
	private MomentumEffect.EffectType type;
	private int duration;
	private int elapsedDuration;

	public MomentumEffect() {
		this.init();
	}

	public void init() {
		this.value = Vec3.ZERO;
		this.modifyValue = Vec3.ZERO;
		this.type = MomentumEffect.EffectType.LOCAL_VALUE;
		this.duration = 0;
		this.elapsedDuration = 0;
	}

	public void tick() {
		if (!Vec3.ZERO.equals(this.modifyValue)) {
			this.value = this.value.add(this.modifyValue);
		}

		this.elapsedDuration++;
	}

	public MomentumEffect(Vec3 value, Vec3 modifyValue, MomentumEffect.EffectType type, int duration) {
		this.value = value;
		this.modifyValue = modifyValue;
		this.type = type;
		this.duration = duration;
	}

	public double applyTo(double number) {
		if (this.value == null) {
			return number;
		}

		return switch (this.type) {
			case REPLACE -> this.value.x;
			case BASE_MULTIPLIER, MULTIPLIER -> number * this.value.x;
			case LOCAL_VALUE -> number + this.value.x;
			case COMPOSE -> number + this.value.length();
		};
	}

	public Vec3 applyTo(Vec3 vec, float yRot) {
		switch (this.type) {
			case REPLACE:
				return this.value;
			case BASE_MULTIPLIER:
			case MULTIPLIER:
				return new Vec3(vec.x * this.value.x, vec.y * this.value.y, vec.z * this.value.z);
			case LOCAL_VALUE:
				double horizontalLength = vec.horizontalDistance();
				double forwardX;
				double forwardZ;
				if (horizontalLength > 1.0E-6) {
					forwardX = vec.x / horizontalLength;
					forwardZ = vec.z / horizontalLength;
				} else {
					double yRotRad = Math.toRadians(yRot);
					forwardX = -Math.sin(yRotRad);
					forwardZ = Math.cos(yRotRad);
				}

				double rightX = -forwardZ;
				double rightZ = forwardX;
				double addX = this.value.x * forwardX + this.value.z * rightX;
				double addY = this.value.y;
				double addZ = this.value.x * forwardZ + this.value.z * rightZ;
				return vec.add(addX, addY, addZ);
			case COMPOSE:
				return vec.add(this.value);
			default:
				return vec;
		}
	}

	@Generated
	public Vec3 getValue() {
		return this.value;
	}

	@Generated
	public Vec3 getModifyValue() {
		return this.modifyValue;
	}

	@Generated
	public MomentumEffect.EffectType getType() {
		return this.type;
	}

	@Generated
	public int getDuration() {
		return this.duration;
	}

	@Generated
	public int getElapsedDuration() {
		return this.elapsedDuration;
	}

	@Generated
	public MomentumEffect setValue(Vec3 value) {
		this.value = value;
		return this;
	}

	@Generated
	public MomentumEffect setModifyValue(Vec3 modifyValue) {
		this.modifyValue = modifyValue;
		return this;
	}

	@Generated
	public MomentumEffect setType(MomentumEffect.EffectType type) {
		this.type = type;
		return this;
	}

	@Generated
	public MomentumEffect setDuration(int duration) {
		this.duration = duration;
		return this;
	}

	@Generated
	public MomentumEffect setElapsedDuration(int elapsedDuration) {
		this.elapsedDuration = elapsedDuration;
		return this;
	}

	public enum EffectType {
		REPLACE,
		BASE_MULTIPLIER,
		LOCAL_VALUE,
		COMPOSE,
		MULTIPLIER;

		public int getPriority() {
			return switch (this) {
				case REPLACE -> 0;
				case BASE_MULTIPLIER -> 1;
				case LOCAL_VALUE -> 3;
				case COMPOSE -> 4;
				case MULTIPLIER -> 2;
			};
		}
	}
}
