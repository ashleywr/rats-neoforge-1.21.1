package com.github.alexthe666.rats.client;

import com.github.alexthe666.rats.client.gui.CheeseStaffScreen;
import com.github.alexthe666.rats.client.gui.PatrolStaffScreen;
import com.github.alexthe666.rats.client.gui.RadiusStaffScreen;
import com.github.alexthe666.rats.client.gui.RatScreen;
import com.github.alexthe666.rats.client.util.RatRecordSoundInstance;
import com.github.alexthe666.rats.server.block.entity.AutoCurdlerBlockEntity;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.OpenRatScreenPacket;
import com.github.alexthe666.rats.server.message.SyncPlaguePacket;
import com.github.alexthe666.rats.server.message.UpdateCurdlerFluidPacket;
import com.github.alexthe666.rats.server.message.UpdateRatMusicPacket;
import com.github.alexthe666.rats.server.message.UpdateSelectedRatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client packet work kept out of common payload classes so dedicated servers can register them. */
public final class RatsClientPayloadHandler {
    private RatsClientPayloadHandler() {}

    public static void handle(ManageRatStaffPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (packet.clear()) {
                SelectedRat.clear(minecraft.player);
                return;
            }
            Entity entity = minecraft.player.level().getEntity(packet.entityId());
            if (entity instanceof TamedRat rat && packet.openGUI()) {
                switch (packet.staffToOpen()) {
                    case 1 -> minecraft.setScreen(new RadiusStaffScreen(rat, packet.pos()));
                    case 2 -> minecraft.setScreen(new PatrolStaffScreen(rat, packet.pos()));
                    default -> minecraft.setScreen(new CheeseStaffScreen(rat, packet.pos(), Direction.values()[packet.dirOrd()]));
                }
            }
        });
    }

    public static void handle(OpenRatScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) return;
            Entity entity = minecraft.level.getEntity(packet.entityId());
            if (!(entity instanceof TamedRat rat)) return;
            LocalPlayer player = minecraft.player;
            RatMenu menu;
            if (player.containerMenu instanceof RatMenu existing && existing.containerId == packet.containerId()) {
                menu = existing;
            } else {
                menu = new RatMenu(packet.containerId(), new SimpleContainer(6), player.getInventory());
                player.containerMenu = menu;
            }
            minecraft.setScreen(new RatScreen(menu, player.getInventory(), rat));
        });
    }

    public static void handle(SyncPlaguePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity entity = level.getEntity(packet.entityId());
            if (entity instanceof LivingEntity living) {
                Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(packet.effectId()).orElse(null);
                if (effect == null) return;
                if (packet.duration() == 0) {
                    living.removeEffect(effect);
                } else {
                    living.forceAddEffect(new MobEffectInstance(
                            effect, packet.duration(), packet.amplifier(), packet.isEffectAmbient(),
                            packet.isEffectVisible(), packet.effectShowsIcon()), null);
                }
            }
        });
    }

    public static void handle(UpdateCurdlerFluidPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos pos = BlockPos.of(packet.blockPos());
            Level level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(pos) instanceof AutoCurdlerBlockEntity curdler) {
                curdler.getTank().setFluid(packet.fluid());
            }
        });
    }

    public static void handle(UpdateRatMusicPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;
            Entity entity = minecraft.level.getEntity(packet.id());
            if (entity instanceof TamedRat rat) {
                minecraft.getSoundManager().queueTickingSound(new RatRecordSoundInstance(rat, packet.record().value()));
                minecraft.gui.setNowPlaying(packet.record().value().getDescription());
            }
        });
    }

    public static void handle(UpdateSelectedRatPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity entity = level.getEntity(packet.entityId());
            if (entity instanceof LivingEntity living) {
                SelectedRat.setLocal(living, packet.ratId());
            }
        });
    }
}
