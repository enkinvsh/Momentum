package com.akirahane.momentum.core.state;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.state.states.OriginalState;
import com.akirahane.momentum.core.state.states.air.AirborneState;
import com.akirahane.momentum.core.state.states.air.BreakFallReadyState;
import com.akirahane.momentum.core.state.states.ground.PowerJumpState;
import com.akirahane.momentum.core.state.states.ground.ProneState;
import com.akirahane.momentum.core.state.states.ground.SlideState;
import com.akirahane.momentum.core.state.states.ground.WalkState;
import com.akirahane.momentum.core.state.states.special.BreakFallState;
import com.akirahane.momentum.core.state.states.special.DodgeState;
import com.akirahane.momentum.core.state.states.wall.VaultInState;
import com.akirahane.momentum.core.state.states.wall.VaultUpState;
import com.akirahane.momentum.core.state.states.wall.WallClimbState;
import com.akirahane.momentum.core.state.states.wall.WallHangState;
import com.akirahane.momentum.core.state.states.wall.WallKickState;
import com.akirahane.momentum.core.state.states.wall.WallRunState;
import com.akirahane.momentum.core.state.states.wall.WallSlideState;
import com.akirahane.momentum.core.state.states.water.SwimDashState;
import com.akirahane.momentum.core.state.states.water.SwimState;
import com.akirahane.momentum.platform.PlatformServices;
import com.akirahane.momentum.platform.client.MovementHint;
import com.akirahane.momentum.platform.config.MomentumServerConfig;
import com.mojang.logging.LogUtils;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public abstract class BaseState {
	protected static final Logger LOGGER = LogUtils.getLogger();
	protected static final Identifier WALL_GRAVITY_ID = Identifier.fromNamespaceAndPath("momentum", "wall_gravity");
	protected static final Identifier DOLPHIN_GRAVITY_ID = Identifier.fromNamespaceAndPath("momentum", "dolphin_gravity");
	protected static final Identifier UP_SLOPE_ID = Identifier.fromNamespaceAndPath("momentum", "up_slope");
	public static String JUMP_LEFT = "jump_left";
	public static String JUMP_RIGHT = "jump_right";
	public static String BACK_JUMP_LEFT = "back_jump_left";
	public static String BACK_JUMP_RIGHT = "back_jump_right";
	public static String FALL = "fall";
	protected String IDLE = "idle";
	protected static List<BaseState.Transition> DEFAULT_CHAIN;

	public void onEnter(Player player, PlayerMovementContext context) {
		playStateAnimation(player, this.IDLE, context);
	}

	public void serverTick(Player player, PlayerMovementContext context) {
		if (MomentumServerConfig.MANEUVER_CONSUME_HUNGER.get()) {
			float modify = MomentumServerConfig.MANEUVER_CONSUME_HUNGER_AMOUNT.get().floatValue();
			if (context.isHasJetBooster()) {
				modify *= 1.0F - MomentumServerConfig.BOOSTER_STAMINA_REDUCTION.get().floatValue();
			}

			player.getFoodData().addExhaustion(modify);
		}
	}

	public void clientTick(Player player, PlayerMovementContext context) {
		this.serverTick(player, context);
		this.clientTickRemote(player, context);
	}

	public void clientTickRemote(Player player, PlayerMovementContext context) {
	}

	public void onExit(Player player, PlayerMovementContext context) {
		context.setTargetArmTransform(0.0F, 0.0F);
	}

	public BaseState evaluate(Player player, PlayerMovementContext context) {
		PlatformServices.client().clearHints();
		PlatformServices.client().addHint(MovementHint.ORIGINAL_STATE);
		PlatformServices.client().addHint(MovementHint.TOGGLE_HINT);
		if (DEFAULT_CHAIN == null) {
			DEFAULT_CHAIN = buildDefaultChain();
		}

		for (BaseState.Transition t : this.transitionChain()) {
			BaseState next = t.tryTransition(player, context);
			if (next != null) {
				return next;
			}
		}

		LOGGER.error("evaluate error! 有状态没有覆盖!");
		return StateType.ORIGINAL.getState();
	}

	private static List<BaseState.Transition> buildDefaultChain() {
		return List.of(
			new BaseState.Transition(StateType.ORIGINAL, OriginalState::canOriginal),
			new BaseState.Transition(StateType.DODGE, DodgeState::canDodge),
			new BaseState.Transition(StateType.SWIM_DASH, SwimDashState::canSwimDash),
			new BaseState.Transition(StateType.SLIDE, SlideState::canSlide),
			new BaseState.Transition(StateType.BREAK_FALL, BreakFallState::canBreakFall),
			new BaseState.Transition(StateType.VAULT_IN, VaultInState::canVaultIn),
			new BaseState.Transition(StateType.SWIM, SwimState::canSwim),
			new BaseState.Transition(StateType.PRONE, ProneState::canProne),
			new BaseState.Transition(StateType.POWER_JUMP, PowerJumpState::canPowerJump),
			new BaseState.Transition(StateType.WALL_KICK, WallKickState::canWallKick),
			new BaseState.Transition(StateType.WALL_RUN, WallRunState::canWallRun),
			new BaseState.Transition(StateType.VAULT_UP, VaultUpState::canVaultUp),
			new BaseState.Transition(StateType.WALL_HANG, WallHangState::canWallHang),
			new BaseState.Transition(StateType.WALL_CLIMB, WallClimbState::canWallClimb),
			new BaseState.Transition(StateType.WALL_SLIDE, WallSlideState::canWallSlide),
			new BaseState.Transition(StateType.BREAK_FALL_READY, BreakFallReadyState::canBreakFallReady),
			new BaseState.Transition(StateType.AIRBORNE, AirborneState::canAirborne),
			new BaseState.Transition(StateType.WALK, WalkState::canWalk)
		);
	}

	protected List<BaseState.Transition> transitionChain() {
		return DEFAULT_CHAIN;
	}

	protected static List<BaseState.Transition> without(List<BaseState.Transition> chain, StateType... targets) {
		List<BaseState.Transition> out = new ArrayList<>(chain);

		for (StateType t : targets) {
			out.removeIf(tr -> tr.target() == t);
		}

		return out;
	}

	protected static List<BaseState.Transition> withPredicate(List<BaseState.Transition> chain, StateType target, BiPredicate<Player, PlayerMovementContext> can) {
		List<BaseState.Transition> out = new ArrayList<>(chain);

		for (int i = 0; i < out.size(); i++) {
			if (out.get(i).target() == target) {
				out.set(i, new BaseState.Transition(target, can));
			}
		}

		return out;
	}

	protected static List<BaseState.Transition> moveAfter(
		List<BaseState.Transition> chain, StateType target, StateType anchor, BiPredicate<Player, PlayerMovementContext> can
	) {
		List<BaseState.Transition> out = new ArrayList<>(chain);
		out.removeIf(tr -> tr.target() == target);

		for (int i = 0; i < out.size(); i++) {
			if (out.get(i).target() == anchor) {
				out.add(i + 1, new BaseState.Transition(target, can));
				break;
			}
		}

		return out;
	}

	public abstract StateType getStateType();

	public static void playStateAnimation(Player player, @NotNull String animName, PlayerMovementContext context) {
		playStateAnimation(player, animName, context, 6, 1.0F);
	}

	public static void playStateAnimation(Player player, @NotNull String animName, PlayerMovementContext context, int fadeInTime, float speed) {
		if (player.level().isClientSide()) {
			if (context.getController() == null) {
				LOGGER.warn("播放动画失败, 没有找到玩家: {} 的动画控制器或控制器死了(什?)", player.getName().getString());
			} else {
				if (!Float.isFinite(speed) || speed <= 0.0F) {
					speed = 1.0F;
				}

				if (speed > 10.0F) {
					speed = 10.0F;
				}

				context.getController().setAnimationSpeed(speed);
				if (!ObjectUtils.isEmpty(animName) && !animName.equals(context.getCurrentAnimationName())) {
					context.getController().removeModifierIf(m -> m instanceof AbstractFadeModifier);
					context.getController()
						.replaceAnimationWithFade(
							AbstractFadeModifier.standardFadeIn(fadeInTime, EasingType.EASE_IN_OUT_SINE), Identifier.fromNamespaceAndPath("momentum", animName)
						);
					context.setCurrentAnimationName(animName);
				}
			}
		}
	}

	public static void stopAnimation(Player player, PlayerMovementContext context) {
		if (player.level().isClientSide() && context.getCurrentAnimationName() != null) {
			if (context.getController() == null) {
				LOGGER.warn("停止动画失败, 没有找到玩家: {} 的动画控制器或控制器死了(什?)", player.getName().getString());
			} else {
				context.getController().stop();
				context.setCurrentAnimationName(null);
			}
		}
	}

	protected record Transition(StateType target, BiPredicate<Player, PlayerMovementContext> can) {
		BaseState tryTransition(Player player, PlayerMovementContext context) {
			return this.can.test(player, context) ? this.target.getState() : null;
		}
	}
}
