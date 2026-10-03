package com.akirahane.momentum.fabric.mixin;

import com.akirahane.momentum.core.MovementDamageHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Player.class)
public abstract class PlayerFallDamageMixin {
	@ModifyArgs(
		method = "causeFallDamage",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Avatar;causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z")
	)
	private void momentum$adjustFallDamage(Args args) {
		Player player = (Player)(Object)this; // 26.2 port: декомпиляция потеряла (Object)-каст this в миксине
		MovementDamageHooks.FallDamage adjusted = MovementDamageHooks.adjustFallDamage(player, (Double)args.get(0), (Float)args.get(1));
		args.set(0, adjusted.distance());
		args.set(1, adjusted.multiplier());
	}
}
