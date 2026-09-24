package com.github.alexthe666.rats.server.message;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.RatsClientPayloadHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ManageRatStaffPacket(int entityId, BlockPos pos, int dirOrd, boolean clear, boolean openGUI, int staffToOpen) implements CustomPacketPayload {

    public static final Type<ManageRatStaffPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "manage_rat_staff"));

    public static final StreamCodec<FriendlyByteBuf, ManageRatStaffPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ManageRatStaffPacket::entityId,
                BlockPos.STREAM_CODEC, ManageRatStaffPacket::pos,
                ByteBufCodecs.VAR_INT, ManageRatStaffPacket::dirOrd,
                ByteBufCodecs.BOOL, ManageRatStaffPacket::clear,
                ByteBufCodecs.BOOL, ManageRatStaffPacket::openGUI,
                ByteBufCodecs.VAR_INT, ManageRatStaffPacket::staffToOpen,
                ManageRatStaffPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManageRatStaffPacket packet, IPayloadContext context) {
        RatsClientPayloadHandler.handle(packet, context);
    }
}
