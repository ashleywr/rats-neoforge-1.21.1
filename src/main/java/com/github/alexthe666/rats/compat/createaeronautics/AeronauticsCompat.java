package com.github.alexthe666.rats.compat.createaeronautics;

import com.github.alexthe666.rats.RatsMod;

// Soft compat with Create: Aeronautics. The bootstrap invokes this only after NeoForge has
// confirmed the optional mod is present. There are no direct Aeronautics API calls yet, so a
// class probe would only create unnecessary sensitivity to its internal package layout.
public final class AeronauticsCompat {

	private AeronauticsCompat() {}

	public static void init() {
		RatsMod.LOGGER.debug("Rats/Aeronautics: standard NeoForge capability integration enabled");
	}
}
