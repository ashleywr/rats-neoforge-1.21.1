package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.server.entity.RatMount;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;

public class RatFollowOwnerGoal extends FollowOwnerGoal {
	private final TamedRat rat;
	private final double speedModifier;
	private final float stopDistance;

	public RatFollowOwnerGoal(TamedRat rat, double speedModifier, float startDist, float stopDist) {
		super(rat, speedModifier, startDist, stopDist);
		this.rat = rat;
		this.speedModifier = speedModifier;
		this.stopDistance = stopDist;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		// 1.21: FollowOwnerGoal.unableToMove() was inlined; we replicate the equivalent gating here.
		if (this.rat.isInCage() || this.rat.isOrderedToSit() || this.rat.isLeashed() || this.rat.getVehicle() instanceof Player) {
			return false;
		}
		return this.rat.getVehicle() instanceof RatMount ? this.rat.isFollowing() : this.rat.canMove() && this.rat.isFollowing() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity owner = this.rat.getOwner();
		// Vanilla captures the constructor's navigator, which Rats replaces on mode changes.
		return owner != null && this.rat.isAlive() && this.rat.isFollowing() && !this.rat.isInCage()
				&& !this.rat.isOrderedToSit() && !this.rat.isLeashed() && !(this.rat.getVehicle() instanceof Player)
				&& (this.rat.getVehicle() instanceof RatMount || this.rat.canMove())
				&& !this.rat.getNavigation().isDone() && this.rat.distanceToSqr(owner) > this.stopDistance * this.stopDistance;
	}

	@Override
	public void stop() {
		super.stop();
		this.rat.getNavigation().stop();
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public void tick() {
		LivingEntity owner = this.rat.getOwner();
		if (owner == null) {
			return;
		}
		double distanceToOwnerSq = this.rat.distanceToSqr(owner);
		if (this.rat.getVehicle() instanceof RatMount mount && mount.shouldTeleportWhenFarAway() && distanceToOwnerSq > 144.0D && this.maybeTeleportMount(owner)) {
			return;
		}
		if (this.rat.getVehicle() instanceof RatMount) {
			if (distanceToOwnerSq > 16.0D) {
				this.rat.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), this.speedModifier);
			}
		} else if (distanceToOwnerSq > 36.0D) {
			this.rat.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), this.speedModifier);
		} else {
			this.rat.getNavigation().stop();
		}
	}

	private boolean maybeTeleportMount(LivingEntity owner) {
		BlockPos anchor = owner.blockPosition();
		for (int i = 0; i < 10; i++) {
			int dx = this.randomIntInclusive(-3, 3);
			int dy = this.randomIntInclusive(-1, 1);
			int dz = this.randomIntInclusive(-3, 3);
			if (this.attemptTeleportEntity(this.rat.getVehicle(), anchor.getX() + dx, anchor.getY() + dy, anchor.getZ() + dz)) {
				return true;
			}
		}
		return false;
	}

	private boolean attemptTeleportEntity(Entity mount, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!this.canTeleportTo(pos)) return false;
		mount.moveTo(x + 0.5D, y, z + 0.5D, mount.getYRot(), mount.getXRot());
		this.rat.getNavigation().stop();
		return true;
	}

	private boolean canTeleportTo(BlockPos pos) {
		PathType type = WalkNodeEvaluator.getPathTypeStatic(this.rat, pos);
		if (type != PathType.WALKABLE) return false;
		BlockPos below = pos.below();
		net.minecraft.world.level.block.state.BlockState belowState = this.rat.level().getBlockState(below);
		if (!belowState.isFaceSturdy(this.rat.level(), below, net.minecraft.core.Direction.UP)) return false;
		if (belowState.is(BlockTags.LEAVES)) return false;
		return this.rat.level().noCollision(this.rat, this.rat.getBoundingBox().move(pos.subtract(this.rat.blockPosition())));
	}

	private int randomIntInclusive(int min, int max) {
		return this.rat.getRandom().nextInt(max - min + 1) + min;
	}
}
