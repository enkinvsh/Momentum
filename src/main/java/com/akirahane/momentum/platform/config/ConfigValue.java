package com.akirahane.momentum.platform.config;

import java.util.Objects;
import java.util.function.Supplier;

public final class ConfigValue<T> implements Supplier<T> {
	private volatile T value;

	public ConfigValue(T defaultValue) {
		this.value = Objects.requireNonNull(defaultValue);
	}

	@Override
	public T get() {
		return this.value;
	}

	public boolean getAsBoolean() {
		return (Boolean)this.value;
	}

	public int getAsInt() {
		return ((Number)this.value).intValue();
	}

	public void updateFromPlatform(T value) {
		this.value = Objects.requireNonNull(value);
	}
}
