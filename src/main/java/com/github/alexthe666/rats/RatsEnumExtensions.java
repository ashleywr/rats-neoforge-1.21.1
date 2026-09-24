package com.github.alexthe666.rats;

import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

/** Parameters for NeoForge's early enum-extension loader. */
public final class RatsEnumExtensions {
	public static final EnumProxy<Rarity> RATLANTIS_SPECIAL = new EnumProxy<>(
			Rarity.class, -1, "rats:ratlantis_special", ChatFormatting.GREEN);

	public static final EnumProxy<Raid.RaiderType> PIED_PIPER = new EnumProxy<>(
			Raid.RaiderType.class,
			(Supplier<EntityType<? extends Raider>>) () -> RatsEntityRegistry.PIED_PIPER.get(),
			new int[]{0, 0, 1, 0, 0, 1, 1, 2});

	private RatsEnumExtensions() {
	}
}
