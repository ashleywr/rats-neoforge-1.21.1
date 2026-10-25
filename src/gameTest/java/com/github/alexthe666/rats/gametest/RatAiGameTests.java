package com.github.alexthe666.rats.gametest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.ai.goal.WildRatTargetFoodGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatTargetItemsGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatPickupGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatDepositGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatFollowOwnerGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.harvest.RatFarmGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.harvest.RatHarvestCropsGoal;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatCageMoveControl;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatMoveControl;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.EnumSet;
import java.util.UUID;

@PrefixGameTestTemplate(false)
public class RatAiGameTests {
    private static final String ARENA = "ai_arena";

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void removedCropReleasesWork(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setNoAi(true);
        BlockPos crop = new BlockPos(6, 1, 2);
        wheat(helper, crop);
        RatHarvestCropsGoal goal = new RatHarvestCropsGoal(rat);
        helper.assertTrue(goal.canUse(), "Mature crop must start harvesting");
        goal.start();
        helper.setBlock(crop, Blocks.AIR);
        helper.assertTrue(!goal.canContinueToUse(), "Removed crop must release the harvest goal");
        goal.stop();
        helper.assertTrue(!rat.isCurrentlyWorking, "Rat must release its working state");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void unreachableCropIsSkipped(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 5));
        rat.setNoAi(true);
        BlockPos enclosed = new BlockPos(5, 1, 5);
        for (BlockPos pos : BlockPos.betweenClosed(4, 1, 4, 6, 4, 6)) {
            if (pos.getX() != 5 || pos.getZ() != 5 || pos.getY() == 4) {
                helper.setBlock(pos, Blocks.BEDROCK);
            }
        }
        wheat(helper, enclosed);
        BlockPos reachable = new BlockPos(2, 1, 11);
        wheat(helper, reachable);
        RatHarvestCropsGoal goal = new RatHarvestCropsGoal(rat);
        helper.assertTrue(goal.canUse(), "An accessible crop must be found");
        helper.assertTrue(helper.absolutePos(reachable).equals(goal.getTargetBlock()),
                "The enclosed nearer crop must not starve an accessible crop");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 260)
    public static void harvestWithHeldBerriesAndNoChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
        BlockPos crop = new BlockPos(9, 1, 2);
        wheat(helper, crop);
        helper.succeedWhen(() -> helper.assertTrue(!isMatureWheat(helper, crop),
                "A speed-upgraded Harvest rat holding berries must reach the crop without a deposit chest"));
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void heldBerriesDoNotInterruptHarvestWithoutChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setNoAi(true);
        rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
        wheat(helper, new BlockPos(9, 1, 2));
        RatHarvestCropsGoal goal = new RatHarvestCropsGoal(rat);
        helper.assertTrue(goal.canUse(), "A crop must be selected with no deposit chest");
        goal.start();
        helper.assertTrue(goal.canContinueToUse(), "A held berry must not interrupt the journey to that crop");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 260)
    public static void harvestTwoBerryBushesWithoutChest(GameTestHelper helper) {
        floor(helper);
        harvester(helper, new BlockPos(2, 1, 2));
        BlockPos first = new BlockPos(3, 1, 2);
        BlockPos second = new BlockPos(9, 1, 2);
        helper.setBlock(first.below(), Blocks.DIRT);
        helper.setBlock(second.below(), Blocks.DIRT);
        helper.setBlock(first, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        helper.setBlock(second, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(first, SweetBerryBushBlock.AGE, 1);
            helper.assertBlockProperty(second, SweetBerryBushBlock.AGE, 1);
        });
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 160)
    public static void navigatorSurvivesPeriodicCheck(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setNoAi(true);
        rat.setRespawnCountdown(180);
        var navigation = rat.getNavigation();
        BlockPos destination = helper.absolutePos(new BlockPos(9, 1, 2));
        helper.assertTrue(navigation.moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, 1.25D),
                "A path must be active before the periodic check");
        var path = navigation.getPath();
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(rat.getNavigation() == navigation,
                    "Periodic checks must retain the navigator and its path when the mode has not changed");
            helper.assertTrue(navigation.getPath() == path, "The active path must survive the periodic check");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 160)
    public static void leavingCageRestoresGroundMovement(GameTestHelper helper) {
        floor(helper);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, RatsBlockRegistry.RAT_CAGE.get());
        TamedRat rat = harvester(helper, pos);
        rat.setNoAi(true);
        helper.assertTrue(rat.getMoveControl() instanceof RatCageMoveControl, "Caged rat must use cage movement");
        helper.setBlock(pos, Blocks.AIR);
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(rat.getMoveControl() instanceof RatMoveControl, "Leaving a cage must restore ground movement");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void stoppedCropPathYieldsToAnotherCrop(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setNoAi(true);
        wheat(helper, new BlockPos(5, 1, 2));
        BlockPos other = new BlockPos(2, 1, 8);
        wheat(helper, other);
        RatHarvestCropsGoal goal = new RatHarvestCropsGoal(rat);
        helper.assertTrue(goal.canUse(), "First crop must be selected");
        goal.start();
        rat.getNavigation().stop();
        helper.assertTrue(!goal.canContinueToUse(), "A route that stopped short must end the harvest attempt");
        goal.stop();
        boolean started = false;
        for (int i = 0; i < 30 && !started; i++) started = goal.canUse();
        helper.assertTrue(started && helper.absolutePos(other).equals(goal.getTargetBlock()),
                "Another crop must get a turn before retrying the failed route");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void renamedStatUpgradesDoNotCollide(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        ItemStack health = new ItemStack(RatsItemRegistry.RAT_UPGRADE_HEALTH.get());
        ItemStack warrior = new ItemStack(RatsItemRegistry.RAT_UPGRADE_WARRIOR.get());
        health.set(DataComponents.CUSTOM_NAME, Component.literal("Same name"));
        warrior.set(DataComponents.CUSTOM_NAME, Component.literal("Same name"));
        rat.setItemSlot(EquipmentSlot.LEGS, health);
        rat.setItemSlot(EquipmentSlot.FEET, warrior);
        rat.onUpgradeChanged();
        double expected = 8.0D + 20.0D + RatConfig.warriorHealthUpgrade;
        helper.assertTrue(Math.abs(rat.getAttributeValue(Attributes.MAX_HEALTH) - expected) < 0.001D,
                "Both renamed upgrades must contribute to the same attribute");
        rat.onUpgradeChanged();
        helper.assertTrue(Math.abs(rat.getAttributeValue(Attributes.MAX_HEALTH) - expected) < 0.001D,
                "Reapplying upgrades must not accumulate modifiers");
        rat.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        rat.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        rat.onUpgradeChanged();
        helper.assertTrue(rat.getAttributeValue(Attributes.MAX_HEALTH) == 8.0D,
                "Removing upgrades must remove their modifiers");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 180)
    public static void cheesePickupOwnsMovement(GameTestHelper helper) {
        floor(helper);
        Rat rat = helper.spawn(RatsEntityRegistry.RAT.get(), new BlockPos(2, 1, 2));
        rat.setOnGround(true);
        rat.setPersistenceRequired();
        ItemEntity cheese = cheese(helper, new BlockPos(9, 1, 2));
        // Deterministic substitute for a low-priority wander goal: it steers away every tick.
        rat.goalSelector.addGoal(10, new Goal() {
            {
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                return true;
            }

            @Override
            public void tick() {
                BlockPos away = helper.absolutePos(new BlockPos(2, 1, 13));
                rat.getNavigation().moveTo(away.getX() + 0.5D, away.getY(), away.getZ() + 0.5D, 1.25D);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(cheese.isRemoved() || cheese.getItem().getCount() < 2,
                "Cheese pickup must reach the item despite a competing low-priority movement goal"));
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void cheesePickupStopsNavigation(GameTestHelper helper) {
        floor(helper);
        Rat rat = helper.spawn(RatsEntityRegistry.RAT.get(), new BlockPos(2, 1, 2));
        rat.setOnGround(true);
        rat.setNoAi(true);
        cheese(helper, new BlockPos(9, 1, 2));
        WildRatTargetFoodGoal goal = new WildRatTargetFoodGoal(rat);
        helper.assertTrue(goal.canUse(), "Reachable cheese must be selected");
        goal.start();
        helper.assertTrue(!rat.getNavigation().isDone(), "Pickup must start a path");
        goal.stop();
        helper.assertTrue(rat.getNavigation().isDone(), "Interrupted pickup must release its old path");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void cheesePickupTracksDisplacedItem(GameTestHelper helper) {
        floor(helper);
        Rat rat = helper.spawn(RatsEntityRegistry.RAT.get(), new BlockPos(2, 1, 2));
        rat.setOnGround(true);
        rat.setNoAi(true);
        ItemEntity item = cheese(helper, new BlockPos(9, 1, 2));
        WildRatTargetFoodGoal goal = new WildRatTargetFoodGoal(rat);
        helper.assertTrue(goal.canUse(), "Reachable cheese must be selected");
        goal.start();
        Vec3 displaced = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(9, 1, 9)));
        item.setPos(displaced);
        for (int i = 0; i < 20; i++) goal.tick();
        helper.assertTrue(item.blockPosition().equals(rat.getNavigation().getTargetPos()),
                "Moving cheese must update the path destination");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 220)
    public static void gatherDeliversDroppedCheeseToChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setCommand(RatCommand.GATHER);
        BlockPos chest = new BlockPos(2, 1, 8);
        helper.setBlock(chest, Blocks.CHEST);
        rat.setDepositPos(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(chest)));
        cheese(helper, new BlockPos(9, 1, 2));
        helper.succeedWhen(() -> helper.assertContainerContains(chest, RatsItemRegistry.CHEESE.get()));
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void airborneHarvestStartsAndStopsMovement(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = flyingHarvester(helper);
        rat.setRespawnCountdown(1000);
        rat.setNoAi(true);
        wheat(helper, new BlockPos(9, 1, 2));
        RatHarvestCropsGoal goal = new RatHarvestCropsGoal(rat);
        helper.assertTrue(goal.canUse(), "An airborne rat must find a crop");
        goal.start();
        rat.getNavigation().tick();
        helper.assertTrue(rat.getMoveControl().hasWanted() && goal.canContinueToUse(),
                "A retained flight path must steer the rat and keep harvesting active");
        goal.stop();
        helper.assertTrue(rat.getNavigation().isDone() && !rat.getMoveControl().hasWanted(),
                "Stopping flight harvest must cancel both the path and flight acceleration");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 260)
    public static void airborneHarvestReachesCrop(GameTestHelper helper) {
        floor(helper);
        flyingHarvester(helper);
        BlockPos crop = new BlockPos(9, 1, 2);
        wheat(helper, crop);
        helper.succeedWhen(() -> helper.assertTrue(!isMatureWheat(helper, crop),
                "A flying Harvest rat must travel to and harvest the crop"));
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void failedFlightPathCancelsMovement(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = flyingHarvester(helper);
        rat.setRespawnCountdown(1000);
        rat.setNoAi(true);
        BlockPos target = helper.absolutePos(new BlockPos(9, 3, 2));
        helper.assertTrue(rat.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.25D),
                "Flight navigation must start a reachable route");
        rat.getNavigation().tick();
        helper.assertTrue(rat.getMoveControl().hasWanted(), "The route must steer flight movement");
        helper.assertTrue(!rat.getNavigation().moveTo((net.minecraft.world.level.pathfinder.Path) null, 1.25D)
                        && rat.getNavigation().isDone() && !rat.getMoveControl().hasWanted(),
                "A failed replacement path must cancel the previous flight acceleration");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void airborneGatherTracksAndStopsMovement(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = flyingHarvester(helper);
        rat.setRespawnCountdown(1000);
        rat.setNoAi(true);
        rat.setCommand(RatCommand.GATHER);
        BlockPos chest = new BlockPos(2, 1, 8);
        helper.setBlock(chest, Blocks.CHEST);
        rat.setDepositPos(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(chest)));
        ItemEntity item = cheese(helper, new BlockPos(9, 1, 2));
        RatTargetItemsGoal goal = new RatTargetItemsGoal(rat);
        helper.assertTrue(goal.canUse(), "An airborne Gather rat must select dropped cheese");
        goal.start();
        rat.getNavigation().tick();
        helper.assertTrue(rat.getMoveControl().hasWanted() && goal.canContinueToUse(),
                "The pickup path must drive flight movement");
        item.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(9, 1, 9))));
        for (int i = 0; i < 20; i++) goal.tick();
        helper.assertTrue(item.blockPosition().equals(rat.getNavigation().getTargetPos()),
                "Moving cheese must update the flight destination");
        goal.stop();
        helper.assertTrue(rat.getNavigation().isDone() && !rat.getMoveControl().hasWanted(),
                "Interrupted pickup must cancel flight movement");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 300)
    public static void flyingGatherDeliversToChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = flyingHarvester(helper);
        rat.setCommand(RatCommand.GATHER);
        BlockPos chest = new BlockPos(2, 1, 8);
        helper.setBlock(chest, Blocks.CHEST);
        rat.setDepositPos(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(chest)));
        cheese(helper, new BlockPos(9, 1, 2));
        helper.succeedWhen(() -> helper.assertContainerContains(chest, RatsItemRegistry.CHEESE.get()));
    }

    private static TamedRat flyingHarvester(GameTestHelper helper) {
        TamedRat rat = harvester(helper, new BlockPos(2, 3, 2));
        rat.setItemSlot(EquipmentSlot.FEET, new ItemStack(RatsItemRegistry.RAT_UPGRADE_FLIGHT.get()));
        rat.onUpgradeChanged();
        rat.setOnGround(false);
        rat.setFlying(true);
        rat.setNoGravity(true);
        return rat;
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void plantingSkipsInaccessibleSoil(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = farmer(helper, new BlockPos(2, 1, 5), Items.WHEAT_SEEDS);
        rat.setRespawnCountdown(1000);
        rat.setNoAi(true);
        BlockPos enclosed = new BlockPos(5, 0, 5);
        for (BlockPos pos : BlockPos.betweenClosed(4, 1, 4, 6, 4, 6)) {
            if (pos.getX() != 5 || pos.getZ() != 5 || pos.getY() == 4) helper.setBlock(pos, Blocks.BEDROCK);
        }
        helper.setBlock(enclosed, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        BlockPos reachable = new BlockPos(2, 0, 11);
        helper.setBlock(reachable, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        RatFarmGoal goal = new RatFarmGoal(rat);
        helper.assertTrue(goal.canUse(), "Farmer must find planting soil");
        goal.start();
        helper.assertTrue(helper.absolutePos(reachable).equals(goal.getTargetBlock())
                        && rat.getNavigation().getPath().canReach() && goal.canContinueToUse(),
                "Planting must skip inaccessible nearer soil and retain the reachable path");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 260)
    public static void farmerPlantsWithoutChest(GameTestHelper helper) {
        floor(helper);
        farmer(helper, new BlockPos(2, 1, 2), Items.WHEAT_SEEDS);
        BlockPos soil = new BlockPos(9, 0, 2);
        helper.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WHEAT, soil.above()));
    }

    @GameTest(templateNamespace = "rats", template = ARENA, timeoutTicks = 260)
    public static void farmerBonemealsWithoutChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = farmer(helper, new BlockPos(2, 1, 2), Items.BONE_MEAL);
        BlockPos crop = new BlockPos(9, 1, 2);
        wheat(helper, crop);
        helper.setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(0));
        helper.succeedWhen(() -> helper.assertTrue(rat.getMainHandItem().getCount() < 4,
                "A Farmer must travel to the crop and use bonemeal without a chest"));
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void stoppedPlantingPathYieldsToAnotherPlot(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = farmer(helper, new BlockPos(2, 1, 2), Items.WHEAT_SEEDS);
        rat.setRespawnCountdown(1000);
        rat.setNoAi(true);
        helper.setBlock(new BlockPos(5, 0, 2), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        BlockPos other = new BlockPos(2, 0, 9);
        helper.setBlock(other, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        RatFarmGoal goal = new RatFarmGoal(rat);
        helper.assertTrue(goal.canUse(), "Farmer must find the first plot");
        goal.start();
        rat.getNavigation().stop();
        helper.assertTrue(!goal.canContinueToUse(), "A stopped route must release planting");
        goal.stop();
        boolean started = false;
        for (int i = 0; i < 30 && !started; i++) started = goal.canUse();
        helper.assertTrue(started && helper.absolutePos(other).equals(goal.getTargetBlock()),
                "Another plot must get a turn before retrying the failed route");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void pickupReleasesRemovedChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setCommand(RatCommand.TRANSPORT);
        BlockPos chest = new BlockPos(9, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        rat.setPickupPos(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(chest)));
        RatPickupGoal goal = new RatPickupGoal(rat, RatPickupGoal.PickupType.INVENTORY);
        helper.assertTrue(goal.canUse(), "Transport pickup must start");
        goal.start();
        goal.tick();
        helper.setBlock(chest, Blocks.AIR);
        helper.assertTrue(!goal.canContinueToUse(), "Removing a pickup chest must end the work goal");
        goal.tick();
        helper.assertTrue(!rat.isCurrentlyWorking && rat.getNavigation().isDone() && !goal.canUse(),
                "A removed chest must release work and movement until it is replaced");
        helper.setBlock(chest, Blocks.CHEST);
        helper.assertTrue(goal.canUse(), "Replacing the assigned chest must allow pickup again");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void depositReleasesRemovedChest(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setCommand(RatCommand.GATHER);
        BlockPos chest = new BlockPos(9, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        rat.setDepositPos(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(chest)));
        rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(RatsItemRegistry.CHEESE.get()));
        RatDepositGoal goal = new RatDepositGoal(rat, RatDepositGoal.DepositType.INVENTORY);
        helper.assertTrue(goal.canUse(), "Deposit must start");
        goal.start();
        goal.tick();
        helper.setBlock(chest, Blocks.AIR);
        helper.assertTrue(!goal.canContinueToUse(), "Removing a deposit chest must end the work goal");
        goal.tick();
        helper.assertTrue(!rat.isCurrentlyWorking && rat.getNavigation().isDone(),
                "A removed deposit chest must release work and movement");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void followUsesChangedNavigator(GameTestHelper helper) {
        floor(helper);
        OwnerRat rat = new OwnerRat(helper.getLevel());
        rat.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 1, 2))));
        rat.setTame(true, false);
        rat.setCommand(RatCommand.FOLLOW);
        rat.setOnGround(true);
        helper.getLevel().addFreshEntity(rat);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(13, 1, 2))));
        rat.owner = player;
        RatFollowOwnerGoal goal = new RatFollowOwnerGoal(rat, 1.25D, 10.0F, 3.0F);
        rat.switchNavigator(2);
        rat.switchNavigator(0);
        helper.assertTrue(goal.canUse(), "Distant owner must activate Follow");
        goal.start();
        goal.tick();
        helper.assertTrue(!rat.getNavigation().isDone() && goal.canContinueToUse(),
                "Follow must continue using the current navigator after a mode change");
        goal.stop();
        helper.assertTrue(rat.getNavigation().isDone(), "Follow must stop the current navigator");
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void energyTransportUsesEnergyForContinuation(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setCommand(RatCommand.TRANSPORT);
        rat.setItemSlot(EquipmentSlot.LEGS, new ItemStack(RatsItemRegistry.RAT_UPGRADE_BASIC_ENERGY.get()));
        rat.onUpgradeChanged();
        BlockPos wheel = new BlockPos(9, 1, 2);
        helper.setBlock(wheel, RatsBlockRegistry.RAT_CAGE_WHEEL.get());
        GlobalPos assigned = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(wheel));
        rat.setPickupPos(assigned);
        rat.setDepositPos(assigned);
        RatPickupGoal pickup = new RatPickupGoal(rat, RatPickupGoal.PickupType.ENERGY);
        helper.assertTrue(pickup.canUse(), "Energy pickup must start below capacity");
        pickup.start();
        helper.assertTrue(pickup.canContinueToUse(), "Empty energy storage must continue pickup");
        rat.setHeldRF(rat.getRFTransferRate());
        helper.assertTrue(!pickup.canContinueToUse(), "Full energy storage must end pickup");
        pickup.stop();
        RatDepositGoal deposit = new RatDepositGoal(rat, RatDepositGoal.DepositType.ENERGY);
        helper.assertTrue(deposit.canUse(), "Energy deposit must start with stored energy");
        deposit.start();
        helper.assertTrue(rat.getMainHandItem().isEmpty() && deposit.canContinueToUse(),
                "Energy deposit must continue with an empty item hand");
        rat.setHeldRF(0);
        helper.assertTrue(!deposit.canContinueToUse(), "Empty energy storage must end deposit");
        deposit.stop();
        helper.succeed();
    }

    @GameTest(templateNamespace = "rats", template = ARENA)
    public static void fluidTransportUsesFluidForContinuation(GameTestHelper helper) {
        floor(helper);
        TamedRat rat = harvester(helper, new BlockPos(2, 1, 2));
        rat.setCommand(RatCommand.TRANSPORT);
        rat.setItemSlot(EquipmentSlot.LEGS, new ItemStack(RatsItemRegistry.RAT_UPGRADE_BUCKET.get()));
        rat.onUpgradeChanged();
        BlockPos curdler = new BlockPos(9, 1, 2);
        helper.setBlock(curdler, RatsBlockRegistry.AUTO_CURDLER.get());
        GlobalPos assigned = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(curdler));
        rat.setPickupPos(assigned);
        rat.setDepositPos(assigned);
        RatPickupGoal pickup = new RatPickupGoal(rat, RatPickupGoal.PickupType.FLUID);
        helper.assertTrue(pickup.canUse(), "Fluid pickup must start below capacity");
        pickup.start();
        helper.assertTrue(pickup.canContinueToUse(), "An empty tank must continue pickup");
        rat.transportingFluid = new FluidStack(NeoForgeMod.MILK.get(), rat.getMBTransferRate());
        helper.assertTrue(!pickup.canContinueToUse(), "A full tank must end pickup");
        pickup.stop();
        RatDepositGoal deposit = new RatDepositGoal(rat, RatDepositGoal.DepositType.FLUID);
        helper.assertTrue(deposit.canUse(), "Fluid deposit must start with stored milk");
        deposit.start();
        helper.assertTrue(rat.getMainHandItem().isEmpty() && deposit.canContinueToUse(),
                "Fluid deposit must continue with an empty item hand");
        rat.transportingFluid = FluidStack.EMPTY;
        helper.assertTrue(!deposit.canContinueToUse(), "An empty tank must end deposit");
        deposit.stop();
        helper.succeed();
    }

    private static TamedRat farmer(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item heldItem) {
        TamedRat rat = harvester(helper, pos);
        rat.setItemSlot(EquipmentSlot.LEGS, new ItemStack(RatsItemRegistry.RAT_UPGRADE_FARMER.get()));
        rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(heldItem, 4));
        rat.onUpgradeChanged();
        return rat;
    }

    private static class OwnerRat extends TamedRat {
        Player owner;

        OwnerRat(Level level) {
            super(RatsEntityRegistry.TAMED_RAT.get(), level);
        }

        @Override
        public Player getOwner() {
            return this.owner;
        }
    }

    private static TamedRat harvester(GameTestHelper helper, BlockPos pos) {
        TamedRat rat = helper.spawn(RatsEntityRegistry.TAMED_RAT.get(), pos);
        rat.setTame(true, false);
        // Work commands must also function while the owner is offline.
        rat.setOwnerUUID(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        rat.setPersistenceRequired();
        rat.setOnGround(true);
        rat.setCommand(RatCommand.HARVEST);
        rat.setRadiusCenter(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(7, 1, 7))));
        rat.setRadius(8);
        rat.setItemSlot(EquipmentSlot.CHEST, new ItemStack(RatsItemRegistry.RAT_UPGRADE_SPEED.get()));
        rat.onUpgradeChanged();
        return rat;
    }

    private static ItemEntity cheese(GameTestHelper helper, BlockPos pos) {
        Vec3 absolute = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        ItemEntity item = new ItemEntity(helper.getLevel(), absolute.x, absolute.y, absolute.z,
                new ItemStack(RatsItemRegistry.CHEESE.get(), 2));
        item.setDeltaMovement(Vec3.ZERO);
        item.setOnGround(true);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    private static void wheat(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
    }

    private static boolean isMatureWheat(GameTestHelper helper, BlockPos pos) {
        var state = helper.getBlockState(pos);
        return state.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).isMaxAge(state);
    }

    private static void floor(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 15, 0, 15)) {
            helper.setBlock(pos, Blocks.BEDROCK);
            helper.setBlock(pos.above(5), Blocks.SEA_LANTERN);
        }
    }
}
