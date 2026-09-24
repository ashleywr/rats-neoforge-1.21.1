package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.RatsClientPayloadHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateSelectedRatPacket(int entityId, int ratId) implements CustomPacketPayload {

    public static final Type<UpdateSelectedRatPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "update_selected_rat"));

    public static final StreamCodec<FriendlyByteBuf, UpdateSelectedRatPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UpdateSelectedRatPacket::entityId,
                ByteBufCodecs.VAR_INT, UpdateSelectedRatPacket::ratId,
                UpdateSelectedRatPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateSelectedRatPacket packet, IPayloadContext context) {
        RatsClientPayloadHandler.handle(packet, context);
    }
}
