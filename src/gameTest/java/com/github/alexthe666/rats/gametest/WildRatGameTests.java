package com.github.alexthe666.rats.gametest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.RatTrapBlock;
import com.github.alexthe666.rats.server.block.entity.RatTrapBlockEntity;
import com.github.alexthe666.rats.server.entity.ai.goal.RatRaidChestsGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatRaidCropsGoal;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.misc.RatPathingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class WildRatGameTests {
    @GameTest(templateNamespace = "rats", template = "wild_ai_arena")
    public static void originalChestGoalFindsOpenFoodChest(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.setNoAi(true);
        BlockPos chest = pos(6, 1, 2);
        foodChest(helper, chest);
        boolean visible = RatPathingHelper.canSeeOrDigToBlock(rat, helper.absolutePos(chest));
        boolean starts = new RatRaidChestsGoal(rat).canUse();
        helper.assertTrue(visible, "The food chest itself must not block the sight check");
        helper.assertTrue(starts, "The original chest raid goal must find an unobstructed food chest");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 260)
    public static void enclosedCropDoesNotStarveDroppedCheese(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 5));
        rat.raidCooldown = 1200;
        BlockPos crop = pos(5, 1, 5);
        for (BlockPos pos : BlockPos.betweenClosed(pos(4, 1, 4), pos(6, 4, 6))) {
            if (pos.getX() != crop.getX() || pos.getZ() != crop.getZ() || pos.getY() == 4) helper.setBlock(pos, Blocks.BEDROCK);
        }
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        Vec3 cheesePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos(2, 1, 11)));
        ItemEntity cheese = new ItemEntity(helper.getLevel(), cheesePos.x, cheesePos.y, cheesePos.z, new ItemStack(RatsItemRegistry.CHEESE.get()));
        cheese.setDeltaMovement(Vec3.ZERO);
        cheese.setOnGround(true);
        helper.getLevel().addFreshEntity(cheese);
        helper.succeedWhen(() -> helper.assertTrue(cheese.isRemoved() && rat.getMainHandItem().is(RatsItemRegistry.CHEESE.get()), "An enclosed crop must not prevent this rat from collecting accessible cheese"));
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 260)
    public static void wildRatRaidsAccessibleFoodChest(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.raidCooldown = 0;
        Container chest = foodChest(helper, pos(9, 1, 2));
        helper.succeedWhen(() -> helper.assertTrue(chest.getItem(0).getCount() < 8, "A wild rat must reach and steal from an accessible food chest"));
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 260)
    public static void wildRatRaidsAccessibleMatureCrop(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.raidCooldown = 0;
        BlockPos crop = pos(9, 1, 2);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.succeedWhen(() -> helper.assertTrue(!helper.getBlockState(crop).is(Blocks.WHEAT) && rat.getMainHandItem().is(Items.WHEAT), "A wild rat must reach and steal from an accessible mature crop"));
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena")
    public static void originalCropGoalRaidsCarrots(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.setNoAi(true);
        rat.raidCooldown = 0;
        BlockPos crop = pos(9, 1, 2);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7));
        RatRaidCropsGoal goal = new RatRaidCropsGoal(rat);
        helper.assertTrue(goal.canUse(), "Fixture must find the mature carrot crop");
        goal.start();
        rat.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(crop)));
        try {
            goal.tick();
        } catch (RuntimeException error) {
            helper.assertTrue(false, "Crop raid threw " + error.getClass().getSimpleName());
        }
        helper.assertTrue(!helper.getBlockState(crop).is(Blocks.CARROTS) && rat.getMainHandItem().is(Items.CARROT), "A carrot raid must remove the crop and give the rat food");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena")
    public static void disabledRaidSettingsPreventRegisteredRaids(GameTestHelper helper) {
        Rat rat = raidFixture(helper);
        boolean oldCrops = RatConfig.ratsBreakCrops;
        boolean oldChests = RatConfig.ratsStealItems;
        try {
            RatConfig.ratsBreakCrops = false;
            RatConfig.ratsStealItems = false;
            helper.assertTrue(!anyRaidCanStart(rat), "Registered raid goals must respect disabled crop and chest raiding");
        } finally {
            RatConfig.ratsBreakCrops = oldCrops;
            RatConfig.ratsStealItems = oldChests;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena")
    public static void mobGriefingRulePreventsRegisteredRaids(GameTestHelper helper) {
        Rat rat = raidFixture(helper);
        var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean original = rule.get();
        try {
            rule.set(false, helper.getLevel().getServer());
            helper.assertTrue(!anyRaidCanStart(rat), "Registered raid goals must respect mobGriefing=false");
        } finally {
            rule.set(original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena")
    public static void solidWallStillBlocksChestRaids(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.setNoAi(true);
        BlockPos chest = pos(6, 1, 2);
        foodChest(helper, chest);
        for (BlockPos wall : BlockPos.betweenClosed(pos(4, 1, 1), pos(4, 3, 3))) helper.setBlock(wall, Blocks.BEDROCK);
        helper.assertTrue(!RatPathingHelper.canSeeOrDigToBlock(rat, helper.absolutePos(chest)), "A solid wall must block sight to a food chest");
        helper.assertTrue(!new RatRaidChestsGoal(rat).canUse(), "The chest itself may be visible, but a wall may not be ignored");
        helper.succeed();
    }

    private static Rat raidFixture(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.setNoAi(true);
        rat.raidCooldown = 0;
        foodChest(helper, pos(6, 1, 2));
        helper.setBlock(pos(4, 0, 4), Blocks.FARMLAND);
        helper.setBlock(pos(4, 1, 4), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        return rat;
    }

    private static boolean anyRaidCanStart(Rat rat) {
        return rat.goalSelector.getAvailableGoals().stream().anyMatch(goal -> {
            String name = goal.getGoal().getClass().getSimpleName();
            return (name.contains("Crops") || name.contains("Chests") || name.contains("Container")) && goal.canUse();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 35)
    public static void catCanInterruptAnActiveCropRaid(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        rat.raidCooldown = 0;
        helper.setBlock(pos(13, 0, 2), Blocks.FARMLAND);
        helper.setBlock(pos(13, 1, 2), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.runAfterDelay(3, () -> {
            boolean raiding = rat.goalSelector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning).anyMatch(goal -> goal.getGoal().getClass().getSimpleName().contains("Crop"));
            helper.assertTrue(raiding, "Fixture must begin with an active crop raid");
            Cat cat = EntityType.CAT.create(helper.getLevel());
            cat.setNoAi(true);
            cat.setOnGround(true);
            cat.setPos(rat.position().add(1, 0, 0));
            helper.getLevel().addFreshEntity(cat);
            helper.succeedWhen(() -> {
                boolean avoiding = rat.goalSelector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning).anyMatch(goal -> goal.getGoal().getClass().getSimpleName().equals("AvoidEntityGoal"));
                helper.assertTrue(avoiding, "A cat must preempt an active crop raid, as in the original goal priorities");
            });
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 260)
    public static void baitedTrapCatchesWildRat(GameTestHelper helper) {
        Rat rat = rat(helper, pos(2, 1, 2));
        BlockPos trapPos = pos(9, 1, 2);
        helper.setBlock(trapPos, RatsBlockRegistry.RAT_TRAP.get().defaultBlockState().setValue(RatTrapBlock.SHUT, false));
        RatTrapBlockEntity trap = (RatTrapBlockEntity) helper.getBlockEntity(trapPos);
        trap.setBaitStack(new ItemStack(RatsItemRegistry.CHEESE.get()));
        helper.succeedWhen(() -> helper.assertTrue(rat.isDeadInTrap(), "A baited trap must attract and catch a wild rat"));
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x + 32, y, z + 32);
    }

    private static Rat rat(GameTestHelper helper, BlockPos pos) {
        for (BlockPos floor : BlockPos.betweenClosed(16, 0, 16, 63, 0, 63)) {
            helper.setBlock(floor, Blocks.BEDROCK);
            helper.setBlock(floor.above(5), Blocks.SEA_LANTERN);
        }
        Rat rat = RatsEntityRegistry.RAT.get().create(helper.getLevel());
        rat.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(pos)));
        rat.setOnGround(true);
        helper.getLevel().addFreshEntity(rat);
        return rat;
    }

    private static Container foodChest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        Container chest = (Container) helper.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(Items.APPLE, 8));
        return chest;
    }

}
