package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatFarmGoal extends BaseRatHarvestGoal {
	private final TamedRat rat;
	@Nullable
	private Path targetPath;
	private final Map<BlockPos, Long> failedTargets = new HashMap<>();

	public RatFarmGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || !this.checkTheBasics(this.rat.getDepositPos().isPresent(), false) || (!this.holdingSeeds() && !this.holdingBonemeal())) {
			return false;
		}
		this.resetTarget();
		return this.getTargetBlock() != null;
	}

	private boolean holdingSeeds() {
		ItemStack stack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
		return !stack.isEmpty() && stack.is(Tags.Items.SEEDS);
	}

	private boolean holdingBonemeal() {
		ItemStack stack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
		return !stack.isEmpty() && stack.is(Items.BONE_MEAL);
	}

	@Override
	public boolean canContinueToUse() {
		BlockPos target = this.getTargetBlock();
		if (!this.rat.isAlive() || !this.checkTheBasics(false, false) || target == null
				|| !this.isValidTarget(target, this.rat.level().getBlockState(target))) return false;
		double distance = this.rat.getRatDistanceCenterSq(target.getX(), target.getY(), target.getZ());
		double reach = this.holdingSeeds() ? this.rat.getRatHarvestDistance(0.0D) : 4.5D;
		if (distance >= reach && (this.rat.getNavigation().isDone() || this.rat.getNavigation().isStuck())) {
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

	@Override
	public void tick() {
		if (this.getTargetBlock() != null) {
			if (this.holdingSeeds()) {
				BlockState block = this.rat.level().getBlockState(this.getTargetBlock());
				if (block.getBlock().isFertile(block, this.rat.level(), this.getTargetBlock()) && this.rat.level().isEmptyBlock(this.getTargetBlock().above())) {
					double distance = this.rat.getRatDistanceCenterSq(this.getTargetBlock().getX(), this.getTargetBlock().getY(), this.getTargetBlock().getZ());
					if (distance < this.rat.getRatHarvestDistance(0.0F)) {
						if (this.holdingSeeds()) {
							ItemStack seedStack = this.rat.getItemInHand(InteractionHand.MAIN_HAND).copy();
							seedStack.setCount(1);
							this.rat.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
							if (seedStack.getItem() instanceof ItemNameBlockItem item) {
								this.rat.level().setBlockAndUpdate(this.getTargetBlock().above(), item.getBlock().defaultBlockState());
							}
						}
						this.stop();
					}
				} else {
					this.stop();
				}
			} else if (this.holdingBonemeal()) {
				BlockState block = this.rat.level().getBlockState(this.getTargetBlock());
				if (this.canPlantBeBonemealed(this.getTargetBlock(), block)) {
					double distance = this.rat.getRatDistanceCenterSq(this.getTargetBlock().getX(), this.getTargetBlock().getY(), this.getTargetBlock().getZ());
					if (distance < 4.5F) {
						if (this.holdingBonemeal()) {
							this.rat.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
							if (block.getBlock() instanceof BonemealableBlock bonemealable) {
								if (bonemealable.isValidBonemealTarget(this.rat.level(), this.getTargetBlock(), block)) {
									if (!this.rat.level().isClientSide()) {
										this.rat.level().levelEvent(2005, this.getTargetBlock(), 0);
										this.rat.level().playSound(null, this.getTargetBlock(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS);
										bonemealable.performBonemeal((ServerLevel) this.rat.level(), this.rat.level().getRandom(), this.getTargetBlock(), block);
									}
								}
							}
						}
						this.stop();
					}
				} else {
					this.stop();
				}
			}
		}
	}

	private void resetTarget() {
		this.setTargetBlock(null);
		this.targetPath = null;
		long now = this.rat.level().getGameTime();
		this.failedTargets.values().removeIf(retryAt -> retryAt <= now);
		int RADIUS = this.rat.getRadius();
		List<BlockPos> allBlocks = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(this.rat.getSearchCenter().offset(-RADIUS, -RADIUS, -RADIUS), this.rat.getSearchCenter().offset(RADIUS, RADIUS, RADIUS))) {
			BlockState state = this.rat.level().getBlockState(pos);
			if (!this.failedTargets.containsKey(pos) && this.isValidTarget(pos, state)
					&& (!this.holdingBonemeal() || this.canPlantBeBonemealed(pos, state))
					&& !RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
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

	private boolean isValidTarget(BlockPos pos, BlockState state) {
		if (this.holdingSeeds()) {
			return state.getBlock().isFertile(state, this.rat.level(), pos) && this.rat.level().isEmptyBlock(pos.above());
		}
		return this.holdingBonemeal() && state.is(BlockTags.BEE_GROWABLES) && state.getBlock() instanceof BonemealableBlock plant
				&& plant.isValidBonemealTarget(this.rat.level(), pos, state);
	}

	private boolean canPlantBeBonemealed(BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof BonemealableBlock bonemealable && state.is(BlockTags.BEE_GROWABLES)) {
			if (bonemealable.isValidBonemealTarget(this.rat.level(), pos, state)) {
				return bonemealable.isBonemealSuccess(this.rat.level(), this.rat.level().getRandom(), pos, state);
			}
		}
		return false;
	}
}
