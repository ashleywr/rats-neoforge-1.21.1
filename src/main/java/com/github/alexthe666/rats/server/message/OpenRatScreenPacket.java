package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.RatsClientPayloadHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRatScreenPacket(int containerId, int entityId) implements CustomPacketPayload {

    public static final Type<OpenRatScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "open_rat_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenRatScreenPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenRatScreenPacket::containerId,
                ByteBufCodecs.VAR_INT, OpenRatScreenPacket::entityId,
                OpenRatScreenPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenRatScreenPacket packet, IPayloadContext context) {
        RatsClientPayloadHandler.handle(packet, context);
    }
}
