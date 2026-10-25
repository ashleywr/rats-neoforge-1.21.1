package com.github.alexthe666.rats.gametest;

import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercise the registered resource through the normal loader's loot pipeline. */
@PrefixGameTestTemplate(false)
public class RatLootGameTests {
    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void plainToolsCanFindPlasticAndRetainGarbage(GameTestHelper helper) {
        for (ItemStack tool : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.DIAMOND_PICKAXE)}) {
            int plastic = plasticRolls(helper, tool, 1024);
            System.out.println("GARBAGE_LOOT_PROBE plain_tool=" + tool + " trials=1024 plastic=" + plastic);
            helper.assertTrue(plastic > 0 && plastic < 1024,
                    "Unenchanted tools must sometimes yield plastic and otherwise retain a reusable garbage pile; got " + plastic + "/1024 plastic");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void fortuneThreeAlwaysFindsOnePlastic(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE), 3);
        int plastic = plasticRolls(helper, tool, 32);
        System.out.println("GARBAGE_LOOT_PROBE fortune=3 trials=32 plastic=" + plastic);
        helper.assertTrue(plastic == 32, "Fortune III must guarantee exactly one Plastic Waste per pile; got " + plastic + "/32");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "ai_arena")
    public static void silkTouchPreservesThePileAndOverridesFortune(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH), 1);
        for (int fortune : new int[]{0, 3}) {
            if (fortune > 0) {
                // Commands can construct both enchantments; Silk Touch remains the first branch.
                tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), fortune);
            }
            int plastic = plasticRolls(helper, tool, 32);
            System.out.println("GARBAGE_LOOT_PROBE silk_touch=1 fortune=" + fortune + " trials=32 plastic=" + plastic);
            helper.assertTrue(plastic == 0, "Silk Touch must keep the garbage pile, including when Fortune is also present");
        }
        helper.succeed();
    }

    private static int plasticRolls(GameTestHelper helper, ItemStack tool, int trials) {
        var block = RatsBlockRegistry.GARBAGE_PILE.get();
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(block.getLootTable());
        var params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
                .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)))
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        int plastic = 0;
        RandomSource random = RandomSource.create(0x726174L);
        for (int trial = 0; trial < trials; trial++) {
            var drops = table.getRandomItems(params, random);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1,
                    "Each garbage-pile roll must conserve one item; got " + drops);
            ItemStack drop = drops.getFirst();
            helper.assertTrue(drop.is(RatsItemRegistry.PLASTIC_WASTE.get()) || drop.is(block.asItem()),
                    "Garbage must yield only Plastic Waste or the original pile; got " + drop);
            if (drop.is(RatsItemRegistry.PLASTIC_WASTE.get())) plastic++;
        }
        return plastic;
    }
}
