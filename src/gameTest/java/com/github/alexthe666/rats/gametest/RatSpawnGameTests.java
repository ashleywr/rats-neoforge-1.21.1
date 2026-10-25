package com.github.alexthe666.rats.gametest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.github.alexthe666.rats.RatConfig.GarbageRatSpawnLightRule.*;

@PrefixGameTestTemplate(false)
public class RatSpawnGameTests {
    private static final BlockPos PILE = new BlockPos(40, 0, 40);

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void darkGarbagePreservesDaylightAndTorchRestrictions(GameTestHelper helper) {
        prepare(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, DARK, 6000, true, true, true, false, false) == 0,
                    "The default DARK policy must preserve daylight restrictions");
            helper.assertTrue(attempt(helper, DARK, 18000, true, true, true, false, false) == 0,
                    "The default DARK policy must still reject actual torchlight at night");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void nightGarbageAllowsTorchlightOnlyAtNight(GameTestHelper helper) {
        prepare(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, NIGHT, 6000, true, true, true, false, false) == 0,
                    "NIGHT policy must reject a lit pile during daytime");
            helper.assertTrue(attempt(helper, NIGHT, 18000, true, true, true, false, false) == 1,
                    "NIGHT policy must allow an unobstructed lit pile at night");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void anyGarbageAllowsLitDaylight(GameTestHelper helper) {
        prepare(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, ANY, 6000, true, true, true, false, false) == 1,
                    "ANY policy must allow an unobstructed lit pile in daylight");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void anyGarbageStillRespectsSpawningGamerules(GameTestHelper helper) {
        prepare(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, ANY, 6000, false, true, true, false, false) == 0,
                    "ANY policy must respect doMobSpawning=false");
            helper.assertTrue(attempt(helper, ANY, 6000, true, false, true, false, false) == 0,
                    "ANY policy must respect doRatSpawning=false");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void anyGarbageStillRejectsCoveredPiles(GameTestHelper helper) {
        prepare(helper);
        helper.setBlock(PILE.above(), Blocks.STONE);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, ANY, 6000, true, true, true, true, false) == 0,
                    "Removing lighting restrictions must not bypass a covered or obstructed spawn");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void anyGarbageLeavesNaturalDaylightRestrictionsIntact(GameTestHelper helper) {
        prepare(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, ANY, 6000, true, true, true, false, true) == 1,
                    "Only the garbage spawn must succeed while natural spawning rejects daylight");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void passiveModeKeepsItsExistingGarbageLightBehavior(GameTestHelper helper) {
        prepare(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, DARK, 6000, true, true, false, false, false) == 1,
                    "Rats Spawn Like Monsters=false must retain its existing unrestricted garbage lighting");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "rats", template = "wild_ai_arena", timeoutTicks = 80)
    public static void piedGarbageKeepsItsDarknessRestrictions(GameTestHelper helper) {
        prepare(helper);
        helper.setBlock(PILE, RatsBlockRegistry.PIED_GARBAGE.get());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(attempt(helper, ANY, 6000, true, true, true, false, false) == 0,
                    "ANY rat lighting must not enable Pied Garbage in daylight");
            helper.assertTrue(attempt(helper, ANY, 18000, true, true, true, false, false) == 0,
                    "ANY rat lighting must not enable Pied Garbage beside a torch at night");
            helper.succeed();
        });
    }

    private static void prepare(GameTestHelper helper) {
        for (BlockPos floor : BlockPos.betweenClosed(38, 0, 38, 42, 0, 42)) helper.setBlock(floor, Blocks.BEDROCK);
        helper.setBlock(PILE, RatsBlockRegistry.GARBAGE_PILE.get());
        helper.setBlock(PILE.above(), Blocks.AIR);
        helper.setBlock(PILE.offset(1, 1, 0), Blocks.TORCH);
    }

    private static int attempt(GameTestHelper helper, RatConfig.GarbageRatSpawnLightRule policy,
                               long time, boolean mobs, boolean ratsEnabled, boolean monsters,
                               boolean covered, boolean checkNatural) {
        ServerLevel level = helper.getLevel();
        BlockPos pile = helper.absolutePos(PILE);
        AABB spawnArea = new AABB(pile.above()).inflate(0.1);
        double oldRate = RatConfig.garbageSpawnRate;
        var oldPolicy = RatConfig.garbageRatSpawnLightRule;
        boolean oldMonsters = RatConfig.ratsSpawnLikeMonsters;
        int oldDecrease = RatConfig.ratSpawnDecrease;
        int oldTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        boolean oldMobs = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        boolean oldRats = level.getGameRules().getBoolean(RatsMod.SPAWN_RATS);
        long oldTime = level.getDayTime();
        try {
            RatConfig.garbageSpawnRate = 1.0;
            RatConfig.garbageRatSpawnLightRule = policy;
            RatConfig.ratsSpawnLikeMonsters = monsters;
            RatConfig.ratSpawnDecrease = 0;
            level.setDayTime(time);
            level.updateSkyBrightness();
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(mobs, level.getServer());
            level.getGameRules().getRule(RatsMod.SPAWN_RATS).set(ratsEnabled, level.getServer());
            helper.assertTrue(level.isNight() == (time == 18000), "Fixture must establish the requested day/night state");
            if (!covered) {
                helper.assertTrue(level.getBrightness(LightLayer.BLOCK, pile.above()) > 0,
                        "The test pile must actually receive torchlight");
                if (time == 6000) helper.assertTrue(level.getMaxLocalRawBrightness(pile.above()) > 7,
                        "The test pile must actually receive daylight");
            }
            if (checkNatural) {
                Rat rat = RatsEntityRegistry.RAT.get().create(level);
                helper.assertTrue(rat != null, "Natural-spawn probe must construct a real rat");
                rat.moveTo(pile.getX() + 0.5, pile.getY() + 1, pile.getZ() + 0.5, 0, 0);
                helper.assertTrue(!rat.checkSpawnRules(level, MobSpawnType.NATURAL),
                        "ANY garbage lighting must not change the natural rat daylight predicate");
            }
            level.getEntitiesOfClass(PathfinderMob.class, spawnArea).forEach(PathfinderMob::discard);
            level.getBlockState(pile).randomTick(level, pile, RandomSource.create(20261011));
            var spawned = level.getEntitiesOfClass(PathfinderMob.class, spawnArea);
            System.out.println("SPAWN_LIGHT_PROBE policy=" + policy + " time=" + time
                    + " block_light=" + level.getBrightness(LightLayer.BLOCK, pile.above())
                    + " mobs=" + mobs + " rats=" + ratsEnabled + " monsters=" + monsters
                    + " covered=" + covered + " spawned=" + spawned.size());
            int count = spawned.size();
            spawned.forEach(PathfinderMob::discard);
            return count;
        } finally {
            RatConfig.garbageSpawnRate = oldRate;
            RatConfig.garbageRatSpawnLightRule = oldPolicy;
            RatConfig.ratsSpawnLikeMonsters = oldMonsters;
            RatConfig.ratSpawnDecrease = oldDecrease;
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldTicks, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(oldMobs, level.getServer());
            level.getGameRules().getRule(RatsMod.SPAWN_RATS).set(oldRats, level.getServer());
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
    }
}
