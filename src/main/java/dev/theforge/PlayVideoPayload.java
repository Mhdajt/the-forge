package dev.theforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: "play this video" (name = file in the_forge/<name>.fgv, or "stop"). */
public record PlayVideoPayload(String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayVideoPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("the_forge", "play_video"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayVideoPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PlayVideoPayload::name, PlayVideoPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
