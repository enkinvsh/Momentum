package com.akirahane.momentum.core.state;

import com.akirahane.momentum.core.context.PlayerMovementContext;
import com.akirahane.momentum.core.effect.MomentumEffect;
import com.akirahane.momentum.core.effect.MomentumEffectType;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import lombok.Generated;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class MovementStateMachine {
	protected static final Logger LOGGER = LogUtils.getLogger();
	private BaseState currentState;
	private final PlayerMovementContext context;
	private boolean dirty = false;

	public MovementStateMachine(Player player) {
		this.context = new PlayerMovementContext(player);
		this.currentState = StateType.ORIGINAL.getState();
		LOGGER.trace("[MovementStateMachine] init");
	}

	public BaseState clientTick(Player player) {
		this.handleEffect();
		BaseState org = this.currentState;
		this.context.clientTick(player);
		this.currentState.clientTick(player, this.context);
		BaseState next = this.currentState.evaluate(player, this.context);
		this.transition(next, player);
		if (!StateType.SLIDE.equals(this.currentState.getStateType())) {
			this.context.setSlopeUnitVector(Vec3.ZERO);
		}

		return !org.equals(next) ? next : null;
	}

	public void serverTick(Player player) {
		this.context.serverTick(player);
		this.currentState.serverTick(player, this.context);
	}

	public void clientTickRemote(Player player) {
		this.context.clientTickRemote(player);
		this.currentState.clientTickRemote(player, this.context);
	}

	private void transition(BaseState next, Player player) {
		if (!next.equals(this.currentState)) {
			LOGGER.trace("[MovementStateMachine] {} to {}", this.currentState.getStateType(), next.getStateType());
			this.context.setPreviousStateType(this.currentState.getStateType());
			this.currentState.onExit(player, this.context);
			this.currentState = next;
			this.currentState.onEnter(player, this.context);
		}
	}

	public void setStateFromClient(StateType newStateType, Player player) {
		this.transition(newStateType.getState(), player);
		this.dirty = true;
	}

	public void handleEffect() {
		for (Entry<MomentumEffectType, Set<MomentumEffect>> entry : this.context.getPendingEffectPool().entrySet()) {
			Iterator<MomentumEffect> iterator = entry.getValue().iterator();

			while (iterator.hasNext()) {
				MomentumEffect momentumEffect = iterator.next();
				if (momentumEffect.getDuration() >= 0 && momentumEffect.getElapsedDuration() >= momentumEffect.getDuration()) {
					momentumEffect.setElapsedDuration(0);
					iterator.remove();
				} else {
					momentumEffect.tick();
				}
			}
		}
	}

	public double applyEffect(double number, MomentumEffectType type) {
		Set<MomentumEffect> effects = this.context.getPendingEffectPool().get(type);
		if (effects != null && !effects.isEmpty()) {
			double origin = number;
			MomentumEffect[] sorted = effects.toArray(MomentumEffect[]::new);
			Arrays.sort(sorted, (a, b) -> Integer.compare(a.getType().getPriority(), b.getType().getPriority()));

			for (MomentumEffect effect : sorted) {
				number = effect.applyTo(number);
			}

			return Double.isFinite(number) ? number : origin;
		} else {
			return number;
		}
	}

	public Vec3 applyEffect(Vec3 vec, float yRot, MomentumEffectType type) {
		Set<MomentumEffect> effects = this.context.getPendingEffectPool().get(type);
		if (effects != null && !effects.isEmpty()) {
			Vec3 origin = vec;
			MomentumEffect[] sorted = effects.toArray(MomentumEffect[]::new);
			Arrays.sort(sorted, (a, b) -> Integer.compare(a.getType().getPriority(), b.getType().getPriority()));

			for (MomentumEffect effect : sorted) {
				vec = effect.applyTo(vec, yRot);
				if (vec == null) {
					return origin;
				}
			}

			return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z) ? vec : origin;
		} else {
			return vec;
		}
	}

	@Generated
	public BaseState getCurrentState() {
		return this.currentState;
	}

	@Generated
	public PlayerMovementContext getContext() {
		return this.context;
	}

	@Generated
	public boolean isDirty() {
		return this.dirty;
	}

	@Generated
	public void setCurrentState(BaseState currentState) {
		this.currentState = currentState;
	}

	@Generated
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
}
