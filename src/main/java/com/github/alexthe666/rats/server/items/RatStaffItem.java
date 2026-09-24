package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

public class RatStaffItem extends LoreTagItem {
	public RatStaffItem(Properties properties) {
		super(properties, 2, false);
	}

	public int getStaff(ItemStack stack) {
		if (stack.is(RatsItemRegistry.PATROL_STICK.get())) return 2;
		if (stack.is(RatsItemRegistry.RADIUS_STICK.get())) return 1;
		return 0;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		if (FMLEnvironment.dist.isClient()) {
			ClientOnly.appendSelectedRat(tooltip);
		}
	}

	/** Keeps Minecraft and its LocalPlayer field out of this common item class on dedicated servers. */
	private static final class ClientOnly {
		private static void appendSelectedRat(List<Component> tooltip) {
			var minecraft = net.minecraft.client.Minecraft.getInstance();
			if (minecraft.level == null || minecraft.player == null) return;

			TamedRat rat = com.github.alexthe666.rats.server.capability.SelectedRat.get(minecraft.player);
			if (rat != null) {
				tooltip.add(Component.translatable(RatsLangConstants.CHEESE_STAFF_SELECTED, rat.getDisplayName(), rat.getUUID().toString()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			}
		}
	}
}
