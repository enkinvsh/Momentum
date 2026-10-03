package com.akirahane.momentum.network;

import com.akirahane.momentum.core.state.StateType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record StateTransitionPacket(StateType stateType, int extraData, byte wallData) implements CustomPacketPayload {
	public static final Type<@NotNull StateTransitionPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "sync_momentum_state"));
	public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull StateTransitionPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		pkt -> pkt.stateType.getId(),
		ByteBufCodecs.VAR_INT,
		StateTransitionPacket::extraData,
		ByteBufCodecs.BYTE,
		StateTransitionPacket::wallData,
		(state, extra, wall) -> new StateTransitionPacket(StateType.fromId(state), extra, wall)
	);

	@Override
	public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
		return TYPE;
	}
}
