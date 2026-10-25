package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatHarvestCropsGoal extends BaseRatHarvestGoal {
	private final TamedRat rat;
	@Nullable
	private Path targetPath;
	private final Map<BlockPos, Long> failedTargets = new HashMap<>();

	public RatHarvestCropsGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || !this.checkTheBasics(this.rat.getDepositPos().isPresent(), this.rat.getDepositPos().isPresent())) {
			return false;
		}
		this.resetTarget();
		return this.getTargetBlock() != null;
	}

	private void resetTarget() {
		this.setTargetBlock(null);
		this.targetPath = null;
		long now = this.rat.level().getGameTime();
		this.failedTargets.values().removeIf(retryAt -> retryAt <= now);
		List<BlockPos> allBlocks = new ArrayList<>();
		int RADIUS = this.rat.getRadius();
		for (BlockPos pos : BlockPos.betweenClosed(this.rat.getSearchCenter().offset(-RADIUS, -RADIUS, -RADIUS), this.rat.getSearchCenter().offset(RADIUS, RADIUS, RADIUS))) {
			BlockState state = this.rat.level().getBlockState(pos);
			if (!this.failedTargets.containsKey(pos) && this.canHarvestBlock(this.rat.level(), pos, state)) {
				allBlocks.add(pos.immutable());
			}
		}
		allBlocks.sort(this.getTargetSorter());
		for (BlockPos pos : allBlocks) {
			Path path = this.rat.getNavigation().createPath(pos, 1);
			if (path != null && path.canReach()) {
				this.setTargetBlock(pos);
				this.targetPath = path;
				return;
			}
		}
	}

	@Override
	public boolean canContinueToUse() {
		BlockPos target = this.getTargetBlock();
		if (!this.rat.isAlive() || !this.checkTheBasics(false, this.rat.getDepositPos().isPresent()) || target == null
				|| !this.canHarvestBlock(this.rat.level(), target, this.rat.level().getBlockState(target))) {
			return false;
		}
		if (!this.isWithinHarvestDistance(target) && (this.rat.getNavigation().isDone() || this.rat.getNavigation().isStuck())) {
			// Give another crop a turn before retrying a route that stopped short.
			this.failedTargets.put(target, this.rat.level().getGameTime() + 100);
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		super.start();
		this.rat.getNavigation().moveTo(this.targetPath, 1.25D);
	}

	@Override
	public void stop() {
		super.stop();
		this.targetPath = null;
	}

	private boolean isWithinHarvestDistance(BlockPos target) {
		return this.rat.getRatDistanceCenterSq(target.getX(), target.getY(), target.getZ()) < this.rat.getRatHarvestDistance(0.0D);
	}

	@Override
	public void tick() {
		if (this.getTargetBlock() != null) {
			BlockState block = this.rat.level().getBlockState(this.getTargetBlock());
			if (!this.canHarvestBlock(this.rat.level(), this.getTargetBlock(), block)) {
				this.stop();
				return;
			}
			if (this.isWithinHarvestDistance(this.getTargetBlock())) {
				if (block.is(BlockTags.CROPS) || block.is(Blocks.NETHER_WART)) {
					if (block.getBlock() instanceof CropBlock crop && !crop.isMaxAge(block)) {
						this.setTargetBlock(null);
						this.stop();
						return;
					}

					this.rat.level().destroyBlock(this.getTargetBlock(), true);
					if ((!RatConfig.ratsBreakBlockOnHarvest || RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_REPLANTER.get()))) {
						if (block.getBlock() instanceof BonemealableBlock) {
							this.rat.level().setBlockAndUpdate(this.getTargetBlock(), block.getBlock().defaultBlockState());
						}
					}
					this.stop();
				} else if (block.getBlock() instanceof SugarCaneBlock) {
					this.rat.level().destroyBlock(this.getTargetBlock(), true);
					this.stop();
				} else if (block.is(Blocks.SWEET_BERRY_BUSH)) {
					//[VanillaCopy] of Fox.FoxEatBerriesGoal
					int i = block.getValue(SweetBerryBushBlock.AGE);
					int j = 1 + this.rat.level().random.nextInt(2) + (i == 3 ? 1 : 0);
					ItemStack itemstack = this.rat.getItemBySlot(EquipmentSlot.MAINHAND);
					if (itemstack.isEmpty() && this.rat.getDepositPos().isPresent()) {
						this.rat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
						--j;
					}

					if (j > 0) {
						Block.popResource(this.rat.level(), this.getTargetBlock(), new ItemStack(Items.SWEET_BERRIES, j));
					}

					this.rat.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
					this.rat.level().setBlock(this.getTargetBlock(), block.setValue(SweetBerryBushBlock.AGE, 1), 2);
					this.stop();
				} else if (CaveVines.hasGlowBerries(block)) {
					CaveVines.use(this.rat, block, this.rat.level(), this.getTargetBlock());
					this.stop();
				}
			}
		}
	}

	private boolean canHarvestBlock(Level level, BlockPos pos, BlockState state) {
		if (!RatUtils.canRatBreakBlock(this.rat.level(), pos, this.rat)) {
			return false;
		}
		if (state.is(BlockTags.CROPS)) {
			if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) return false;
			if (!(state.getBlock() instanceof StemBlock) && !(state.getBlock() instanceof AttachedStemBlock)) {
				return true;
			}
		} else if (state.getBlock() instanceof SugarCaneBlock) {
			return level.getBlockState(pos.below()).getBlock() instanceof SugarCaneBlock;
		} else if (state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
			return true;
		}

		return (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) > 1) || CaveVines.hasGlowBerries(state);
	}
}
