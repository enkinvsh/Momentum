package com.akirahane.momentum.platform.config;

public final class MomentumClientConfig {
	public static final ConfigValue<Boolean> ENABLE_CAMERA_OFFSET = value(true);
	public static final ConfigValue<Double> FOV_BONUS_MAX = value(0.0);
	public static final ConfigValue<Boolean> ENABLE_DODGE_DIR_DOUBLE = value(true);
	public static final ConfigValue<Boolean> ENABLE_DODGE_SPRINT_CLICK = value(false);
	public static final ConfigValue<Boolean> ENABLE_DODGE_SPRINT_DOUBLE = value(false);
	// port default: подсказки по клавишам по умолчанию выключены
	public static final ConfigValue<Boolean> ENABLE_KEY_HINTS = value(false);
	public static final ConfigValue<Double> MIN_ALPHA_WHEN_MOVING = value(0.4);
	public static final ConfigValue<Double> MAX_ALPHA = value(1.0);
	public static final ConfigValue<Integer> FRESH_DURATION = value(80);
	public static final ConfigValue<Integer> IDLE_DELAY = value(100);
	public static final ConfigValue<Double> MOVE_THRESHOLD_SQR = value(0.005);
	public static final ConfigValue<Double> FADE_IN_SPEED = value(0.01);
	public static final ConfigValue<Double> FADE_OUT_SPEED = value(0.18);
	public static final ConfigValue<Boolean> ENABLE_PRONE = value(true);
	public static final ConfigValue<Boolean> ENABLE_SLIDE = value(true);
	public static final ConfigValue<Boolean> ENABLE_BREAK_FALL_READY = value(true);
	public static final ConfigValue<Boolean> ENABLE_BREAK_FALL = value(true);
	public static final ConfigValue<Boolean> ENABLE_DODGE = value(true);
	public static final ConfigValue<Boolean> ENABLE_WALL_CLIMB = value(true);
	public static final ConfigValue<Boolean> ENABLE_WALL_SLIDE = value(true);
	public static final ConfigValue<Boolean> ENABLE_WALL_RUN = value(true);
	public static final ConfigValue<Boolean> ENABLE_WALL_HANG = value(true);
	public static final ConfigValue<Boolean> ENABLE_POWER_JUMP = value(true);
	public static final ConfigValue<Boolean> ENABLE_WALL_KICK = value(true);
	public static final ConfigValue<Boolean> ENABLE_VAULT_UP = value(true);
	public static final ConfigValue<Boolean> ENABLE_VAULT_IN = value(true);
	public static final ConfigValue<Boolean> ENABLE_WATER_RUN = value(true);
	public static final ConfigValue<Boolean> ENABLE_WATER_PUSH = value(true);
	public static final ConfigValue<Boolean> ENABLE_FALL_SLOW = value(true);
	public static final ConfigValue<Boolean> ENABLE_AIR_JUMP = value(true);

	private MomentumClientConfig() {
	}

	private static <T> ConfigValue<T> value(T defaultValue) {
		return new ConfigValue<>(defaultValue);
	}
}
