package com.akirahane.momentum.core.state;

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
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import org.slf4j.Logger;

public enum StateType {
	ORIGINAL(new OriginalState(), "state.momentum.original"),
	WALK(new WalkState(), "state.momentum.walk"),
	PRONE(new ProneState(), "state.momentum.prone"),
	SLIDE(new SlideState(), "state.momentum.slide"),
	AIRBORNE(new AirborneState(), "state.momentum.airborne"),
	BREAK_FALL_READY(new BreakFallReadyState(), "state.momentum.break_fall_ready"),
	SWIM(new SwimState(), "state.momentum.swim"),
	BREAK_FALL(new BreakFallState(), "state.momentum.break_fall"),
	DODGE(new DodgeState(), "state.momentum.dodge"),
	WALL_CLIMB(new WallClimbState(), "state.momentum.wall_climb"),
	WALL_SLIDE(new WallSlideState(), "state.momentum.wall_slide"),
	WALL_RUN(new WallRunState(), "state.momentum.wall_run"),
	WALL_HANG(new WallHangState(), "state.momentum.wall_hang"),
	POWER_JUMP(new PowerJumpState(), "state.momentum.power_jump"),
	WALL_KICK(new WallKickState(), "state.momentum.wall_kick"),
	VAULT_UP(new VaultUpState(), "state.momentum.vault_up"),
	VAULT_IN(new VaultInState(), "state.momentum.vault_in"),
	SWIM_DASH(new SwimDashState(), "state.momentum.swim_dash");

	static final Logger LOGGER = LogUtils.getLogger();
	private final int id;
	private static final StateType[] BY_ID = new StateType[values().length];
	private static final Map<Class<? extends BaseState>, StateType> CLASS_MAP = new HashMap<>();
	private final BaseState state;
	private final String i18nKey;

	StateType(BaseState state, String i18nKey) {
		this.i18nKey = i18nKey;
		this.id = this.ordinal();
		this.state = state;
	}

	public static StateType fromId(int id) {
		if (id >= 0 && id < BY_ID.length) {
			return BY_ID[id];
		}

		LOGGER.error("Invalid state id: {}", id);
		throw new IllegalArgumentException("Invalid state id: " + id);
	}

	@Generated
	public int getId() {
		return this.id;
	}

	@Generated
	public BaseState getState() {
		return this.state;
	}

	@Generated
	public String getI18nKey() {
		return this.i18nKey;
	}

	static {
		for (StateType type : values()) {
			BY_ID[type.id] = type;
			CLASS_MAP.put((Class<? extends BaseState>)type.state.getClass(), type);
		}
	}
}
