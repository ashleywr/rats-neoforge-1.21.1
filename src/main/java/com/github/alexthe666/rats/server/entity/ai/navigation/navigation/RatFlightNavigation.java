package com.github.alexthe666.rats.server.entity.ai.navigation.navigation;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatFlightMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

public class RatFlightNavigation extends FlyingPathNavigation {

	public final TamedRat rat;

	public RatFlightNavigation(TamedRat rat, Level level) {
		super(rat, level);
		this.rat = rat;
	}

	@Override
	public boolean moveTo(Path path, double speed) {
		// FlyingPathNavigation consumes the path even when the rat is airborne.
		boolean started = super.moveTo(path, speed);
		if (started) {
			this.rat.setFlying(true);
		} else {
			this.stop();
		}
		return started;
	}

	@Override
	public void stop() {
		super.stop();
		if (this.rat.getMoveControl() instanceof RatFlightMoveControl control) {
			control.stop();
		}
	}
}
