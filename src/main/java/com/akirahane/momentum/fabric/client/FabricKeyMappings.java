package com.akirahane.momentum.fabric.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;

public final class FabricKeyMappings {
	private static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath("momentum", "category"));
	public static final KeyMapping LOWER_CENTER = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.lower_center", Type.KEYSYM, 67, CATEGORY));
	// port default: вкл/выкл мода и подсказок по умолчанию без клавиши (было Shift+M / Shift+N) — M и N часто
	// заняты другими модами (например, Simple Voice Chat), а в 26.2 срабатывают все привязки одной клавиши.
	// Назначить можно в Настройки → Управление → Momentum.
	public static final KeyMapping TOGGLE_MOMENTUM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.change_momentum", Type.KEYSYM, -1, CATEGORY));
	public static final KeyMapping TOGGLE_HINTS = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.toggle_hint", Type.KEYSYM, -1, CATEGORY));

	private FabricKeyMappings() {
	}

	public static void register() {
	}
}
