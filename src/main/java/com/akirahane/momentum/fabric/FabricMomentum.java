package com.akirahane.momentum.fabric;

import com.akirahane.momentum.core.MovementDamageHooks;
import com.akirahane.momentum.fabric.config.FabricServerConfig;
import com.akirahane.momentum.fabric.init.FabricAttachments;
import com.akirahane.momentum.fabric.network.FabricNetwork;
import com.akirahane.momentum.fabric.platform.FabricGameplayPlatform;
import com.akirahane.momentum.platform.PlatformServices;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

public final class FabricMomentum implements ModInitializer {
	@Override
	public void onInitialize() {
		FabricServerConfig.load();
		FabricAttachments.register();
		// 26.2 port: Jet Booster вырезан (нет записей в реестрах → пускаем клиентов без мода) — предмет, звуки jet1..3 и вкладка креатива больше не регистрируются
		PlatformServices.installGameplay(new FabricGameplayPlatform());
		FabricNetwork.register();
		registerServerTicks();
		registerDamageHooks();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> FabricServerConfig.load());
	}

	private static void registerServerTicks() {
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlatformServices.gameplay().movementState(player).serverTick(player);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(FabricNetwork::flushDirtyStates);
	}

	private static void registerDamageHooks() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && MovementDamageHooks.shouldCancelDamage(player)) {
				player.fallDistance = 0.0;
				return false;
			} else {
				return true;
			}
		});
	}
}
