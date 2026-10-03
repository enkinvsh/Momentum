package com.akirahane.momentum.client.animation;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.AnimationController.AnimationStateHandler;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import team.unnamed.mocha.MochaEngine;

public class MomentumAnimationController extends PlayerAnimationController {
	private final SpeedModifier speedModifier = new SpeedModifier(1.0F);
	public static final float MAX_SAFE_SPEED = 10.0F;

	public MomentumAnimationController(Avatar avatar, AnimationStateHandler animationHandler) {
		super(avatar, animationHandler);
		if (avatar instanceof Player player) {
			this.addModifierBefore(new ArmConditionModifier(player));
		}

		this.addModifierLast(this.speedModifier);
	}

	public MochaEngine<AnimationController> getMolangRuntime() {
		return this.molangRuntime;
	}

	public void setAnimationSpeed(float speed) {
		if (!Float.isFinite(speed) || speed <= 0.0F) {
			speed = 1.0F;
		}

		this.speedModifier.speed = speed;
	}

	@Override
	public void tick(AnimationData state) {
		if (!Float.isFinite(this.speedModifier.speed) || this.speedModifier.speed > 10.0F) {
			this.speedModifier.speed = 1.0F;
		}

		super.tick(state);
	}

	public int getAnimationTick() {
		return this.tick;
	}
}
