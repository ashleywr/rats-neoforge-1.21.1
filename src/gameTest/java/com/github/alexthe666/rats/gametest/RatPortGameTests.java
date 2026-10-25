package com.github.alexthe666.rats.gametest;

import com.github.alexthe666.rats.registry.RatlantisItemRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.TickRatUpgrade;
import com.github.alexthe666.rats.server.message.SyncThrownBlockPacket;
import com.github.alexthe666.rats.server.message.UpdateRatFluidPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@PrefixGameTestTemplate(false)
public class RatPortGameTests {
    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void fluidUpdateAcceptsClientboundPackets(GameTestHelper helper) {
        helper.assertTrue(NetworkRegistry.getCodec(UpdateRatFluidPacket.TYPE.id(),
                ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND) != null,
                "Fluid updates sent by the server must decode on clients");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void thrownBlockAcceptsClientboundPackets(GameTestHelper helper) {
        helper.assertTrue(NetworkRegistry.getCodec(SyncThrownBlockPacket.TYPE.id(),
                ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND) != null,
                "Thrown-block updates sent by the server must decode on clients");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void fluidUpdateRoundTripsFilledAndEmpty(GameTestHelper helper) {
        for (FluidStack fluid : new FluidStack[]{new FluidStack(Fluids.WATER, 1000), FluidStack.EMPTY}) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                UpdateRatFluidPacket.STREAM_CODEC.encode(buffer, new UpdateRatFluidPacket(42, fluid));
                UpdateRatFluidPacket decoded = UpdateRatFluidPacket.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.ratId() == 42 && decoded.fluid().getAmount() == fluid.getAmount()
                        && decoded.fluid().getFluid() == fluid.getFluid(),
                        "Both filled and completely drained tanks must round-trip without losing entity or fluid state");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void ratlantisSwordHasMainhandAttributes(GameTestHelper helper) {
        ItemStack sword = new ItemStack(RatlantisItemRegistry.RATLANTIS_SWORD.get());
        double[] amounts = new double[2];
        sword.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) amounts[0] += modifier.amount();
            if (attribute.equals(Attributes.ATTACK_SPEED)) amounts[1] += modifier.amount();
        });
        helper.assertTrue(Math.abs(amounts[0] - 10.0) < 0.00001 && Math.abs(amounts[1] + 2.4) < 0.00001,
                "The registered Ratlantis sword must supply +10 attack damage and -2.4 attack speed in the main hand");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void chefCompletesAndDropsExactlyOneOutput(GameTestHelper helper) {
        TamedRat rat = helper.spawn(RatsEntityRegistry.TAMED_RAT.get(), new BlockPos(3, 1, 3));
        rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.POTATO, 3));
        rat.cookingProgress = 99;
        ((TickRatUpgrade) RatsItemRegistry.RAT_UPGRADE_CHEF.get()).tick(rat);
        helper.assertTrue(rat.getMainHandItem().is(Items.POTATO) && rat.getMainHandItem().getCount() == 2,
                "Server cooking must consume exactly one input from a stack");
        int output = helper.getLevel().getEntitiesOfClass(ItemEntity.class, rat.getBoundingBox().inflate(1))
                .stream().filter(item -> item.getItem().is(Items.BAKED_POTATO)).mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(output == 1 && rat.cookingProgress == 0,
                "Cooking without a chest must drop one output and restart the processing timer");
        rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.POTATO));
        rat.cookingProgress = 99;
        ((TickRatUpgrade) RatsItemRegistry.RAT_UPGRADE_CHEF.get()).tick(rat);
        helper.assertTrue(rat.getMainHandItem().is(Items.BAKED_POTATO) && rat.getMainHandItem().getCount() == 1,
                "The final input must be replaced with its cooked output on the server");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void disenchanterCompletesOnServer(GameTestHelper helper) {
        TamedRat rat = helper.spawn(RatsEntityRegistry.TAMED_RAT.get(), new BlockPos(3, 1, 3));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS), 2);
        rat.setItemSlot(EquipmentSlot.MAINHAND, sword);
        rat.cookingProgress = 999;
        ((TickRatUpgrade) RatsItemRegistry.RAT_UPGRADE_DISENCHANTER.get()).tick(rat);
        helper.assertTrue(rat.getMainHandItem().is(Items.DIAMOND_SWORD) && !rat.getMainHandItem().isEnchanted()
                        && rat.getMainHandItem().getCount() == 1 && rat.cookingProgress == 0,
                "Server disenchanting must preserve the item and remove its enchantments");
        helper.succeed();
    }
}
