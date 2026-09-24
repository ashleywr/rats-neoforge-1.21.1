package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class WildRatRaidContainerGoal extends Goal {
	private final AbstractRat rat;
	private BlockPos targetContainer;
	protected int nextStartTick;

	public WildRatRaidContainerGoal(AbstractRat rat) {
		this.rat = rat;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!this.rat.canMove() || this.rat.getOwner() != null || this.rat.isEating()) {
			return false;
		}
		if (this.nextStartTick > 0) {
			this.nextStartTick--;
			return false;
		}
		this.nextStartTick = this.adjustedTickDelay(RatConfig.ratUpdateDelay);
		List<BlockPos> containers = this.findContainersWithFood();
		if (containers.isEmpty()) {
			return false;
		}
		this.targetContainer = containers.getFirst();
		return true;
	}

	private List<BlockPos> findContainersWithFood() {
		List<BlockPos> containers = new ArrayList<>();
		int searchRadius = 16;
		BlockPos ratPos = this.rat.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(ratPos.offset(-searchRadius, -1, -searchRadius), ratPos.offset(searchRadius, 1, searchRadius))) {
			BlockEntity blockEntity = this.rat.level().getBlockEntity(pos);
			if (blockEntity instanceof Container container && this.containerHasFood(container)) {
				containers.add(pos.immutable());
			}
		}
		containers.sort(Comparator.comparingDouble(pos -> this.rat.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)));
		return containers;
	}

	private boolean containerHasFood(Container container) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty() && RatUtils.isRatFood(stack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		if (!this.rat.isAlive() || this.targetContainer == null) {
			return false;
		}
		BlockEntity blockEntity = this.rat.level().getBlockEntity(this.targetContainer);
		return blockEntity instanceof Container container && this.containerHasFood(container);
	}

	@Override
	public void start() {
		if (this.targetContainer != null) {
			this.rat.getNavigation().moveTo(this.targetContainer.getX() + 0.5D, this.targetContainer.getY() + 0.5D, this.targetContainer.getZ() + 0.5D, 1.2D);
		}
	}

	@Override
	public void tick() {
		if (this.targetContainer != null && this.rat.distanceToSqr(this.targetContainer.getCenter()) < 3.0D) {
			BlockEntity blockEntity = this.rat.level().getBlockEntity(this.targetContainer);
			if (blockEntity instanceof Container container) {
				ItemStack foodStack = this.findFoodInContainer(container);
				if (!foodStack.isEmpty() && this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
					this.rat.setItemInHand(InteractionHand.MAIN_HAND, foodStack.split(1));
					if (this.rat instanceof Rat wildRat) {
						wildRat.setFleePos(this.targetContainer);
					}
					if (blockEntity instanceof ChestBlockEntity chest) {
						chest.setChanged();
					}
				}
			}
			this.stop();
		}
	}

	private ItemStack findFoodInContainer(Container container) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty() && RatUtils.isRatFood(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void stop() {
		this.targetContainer = null;
		this.rat.getNavigation().stop();
	}
}
