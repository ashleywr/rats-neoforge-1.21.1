package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.RatsClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCurdlerFluidPacket(long blockPos, FluidStack fluid) implements CustomPacketPayload {

	public static final Type<UpdateCurdlerFluidPacket> TYPE =
		new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "update_curdler_fluid"));


	public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> SAFE_FLUID_CODEC =
		StreamCodec.of(
			(buf, fluid) -> {
				buf.writeBoolean(!fluid.isEmpty());
				if (!fluid.isEmpty()) {
					FluidStack.STREAM_CODEC.encode(buf, fluid);
				}
			},
			buf -> {
				if (buf.readBoolean()) {
					return FluidStack.STREAM_CODEC.decode(buf);
				}
				return FluidStack.EMPTY;
			}
		);


	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCurdlerFluidPacket> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, UpdateCurdlerFluidPacket::blockPos,
			SAFE_FLUID_CODEC, UpdateCurdlerFluidPacket::fluid,
			UpdateCurdlerFluidPacket::new
		);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(UpdateCurdlerFluidPacket packet, IPayloadContext context) {
		RatsClientPayloadHandler.handle(packet, context);
	}
}
