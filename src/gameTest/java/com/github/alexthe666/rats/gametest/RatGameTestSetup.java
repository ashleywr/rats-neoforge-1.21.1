package com.github.alexthe666.rats.gametest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Development-only registration and optional evidence output for the local runner. */
@EventBusSubscriber(modid = "rats", bus = EventBusSubscriber.Bus.MOD)
public class RatGameTestSetup {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        String glob = System.getProperty("mctest.filter", "*");
        Pattern filter = Pattern.compile(Arrays.stream(glob.split("\\*", -1))
                .map(Pattern::quote).collect(Collectors.joining(".*")), Pattern.CASE_INSENSITIVE);
        int selected = 0;
        if (glob.equals("__reporter_self_check__")) {
            event.register(ReporterSelfCheck.class);
            selected = 1;
        }
        for (Class<?> testClass : List.of(RatAiGameTests.class, WildRatGameTests.class, RatPortGameTests.class, RatSpawnGameTests.class, RatLootGameTests.class)) {
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GameTest.class) &&
                        (filter.matcher(testClass.getSimpleName() + "." + method.getName()).matches()
                                || filter.matcher(method.getName()).matches())) {
                    event.register(method);
                    selected++;
                }
            }
        }
        if (selected == 0) throw new IllegalArgumentException("No Rats GameTests match " + glob);
        String report = System.getProperty("mctest.report");
        if (report != null) {
            EvidenceReporter reporter = new EvidenceReporter(Path.of(report));
            reporter.write(Map.of("event", "registered", "count", selected, "filter", glob));
            GlobalTestReporter.replaceWith(reporter);
        }
    }

    /** Registered only by the reserved explicit filter; never included in the suite. */
    @PrefixGameTestTemplate(false)
    public static class ReporterSelfCheck {
        @GameTest(templateNamespace = "rats", template = "ai_arena")
        public static void evidenceReporterSelfCheck(GameTestHelper helper) {
            for (BlockPos floor : BlockPos.betweenClosed(0, 0, 0, 15, 0, 15)) helper.setBlock(floor, Blocks.BEDROCK);
            var rat = helper.spawn(RatsEntityRegistry.RAT.get(), new BlockPos(2, 1, 2));
            rat.setNoAi(true);
            rat.setOnGround(true);
            rat.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
            Vec3 target = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(6, 1, 2)));
            rat.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
            helper.assertTrue(false, "Intentional evidence-reporter self-check");
        }
    }

    private static final class EvidenceReporter implements TestReporter {
        private final Path output;
        private final Gson gson = new GsonBuilder().serializeNulls().create();
        private final LogTestReporter log = new LogTestReporter();

        EvidenceReporter(Path output) { this.output = output; }

        void write(Map<String, ?> entry) {
            try {
                Files.createDirectories(this.output.toAbsolutePath().getParent());
                Files.writeString(this.output, this.gson.toJson(entry) + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException error) {
                throw new IllegalStateException("Cannot save GameTest evidence", error);
            }
        }

        @Override
        public void onTestFailed(GameTestInfo test) {
            this.log.onTestFailed(test);
            Map<String, Object> result = result(test, "failed");
            result.put("error", test.getError().toString());
            var level = test.getLevel();
            result.put("fixture", Map.of("structure", test.getStructureName(), "position", test.getStructureBlockPos().toShortString(),
                    "day_time", level.getDayTime(), "random_ticks", level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING),
                    "mob_spawning", level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING),
                    "mob_griefing", level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)));
            result.put("rats_config", Map.of("break_crops", RatConfig.ratsBreakCrops, "steal_items", RatConfig.ratsStealItems,
                    "update_delay", RatConfig.ratUpdateDelay, "garbage_chance", RatConfig.garbageSpawnRate,
                    "garbage_light_rule", RatConfig.garbageRatSpawnLightRule.name()));
            result.put("mobs", level.getEntitiesOfClass(Mob.class, test.getStructureBounds().inflate(1)).stream().limit(16).map(mob -> {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
                state.put("uuid", mob.getUUID().toString());
                state.put("position", List.of(mob.getX(), mob.getY(), mob.getZ()));
                Vec3 relative = mob.position().subtract(Vec3.atLowerCornerOf(test.getStructureBlockPos()));
                state.put("relative_position", List.of(relative.x, relative.y, relative.z));
                state.put("no_ai", mob.isNoAi());
                state.put("on_ground", mob.onGround());
                state.put("hand", mob.getMainHandItem().toString());
                state.put("navigation", mob.getNavigation().getClass().getSimpleName());
                state.put("path_done", mob.getNavigation().isDone());
                state.put("path_stuck", mob.getNavigation().isStuck());
                var path = mob.getNavigation().getPath();
                state.put("path", path == null ? null : Map.of("nodes", path.getNodeCount(), "can_reach", path.canReach(), "target", path.getTarget().toShortString()));
                state.put("running_goals", mob.goalSelector.getAvailableGoals().stream().filter(goal -> goal.isRunning())
                        .map(goal -> Map.of("priority", goal.getPriority(), "class", goal.getGoal().getClass().getSimpleName())).toList());
                if (mob instanceof AbstractRat rat) {
                    state.put("can_move", rat.canMove());
                    state.put("raid_cooldown", rat.raidCooldown);
                }
                if (mob instanceof TamedRat rat) {
                    state.put("command", rat.getCommand().name());
                    state.put("working", rat.isCurrentlyWorking);
                }
                return state;
            }).toList());
            result.put("dropped_items", level.getEntitiesOfClass(ItemEntity.class, test.getStructureBounds().inflate(1)).stream().limit(16)
                    .map(item -> Map.of("stack", item.getItem().toString(), "position", List.of(item.getX(), item.getY(), item.getZ()),
                            "on_ground", item.onGround(), "no_gravity", item.isNoGravity())).toList());
            this.write(result);
        }

        private static Map<String, Object> result(GameTestInfo test, String outcome) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("event", "test");
            result.put("name", test.getTestName());
            result.put("outcome", outcome);
            result.put("required", test.isRequired());
            result.put("runtime_ms", test.getRunTime());
            return result;
        }

        @Override
        public void onTestSuccess(GameTestInfo test) {
            this.log.onTestSuccess(test);
            this.write(result(test, "passed"));
        }

        @Override
        public void finish() {
            this.log.finish();
            this.write(Map.of("event", "finished"));
        }
    }
}
