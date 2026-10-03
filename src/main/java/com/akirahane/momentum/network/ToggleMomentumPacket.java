package com.akirahane.momentum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ToggleMomentumPacket() implements CustomPacketPayload {
	public static final ToggleMomentumPacket INSTANCE = new ToggleMomentumPacket();
	public static final Type<@NotNull ToggleMomentumPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "toggle_momentum"));
	public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull ToggleMomentumPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
		return TYPE;
	}
}
