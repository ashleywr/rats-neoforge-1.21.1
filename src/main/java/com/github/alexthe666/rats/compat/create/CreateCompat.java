package com.github.alexthe666.rats.compat.create;

import com.github.alexthe666.rats.RatsMod;

// Soft compat with Create. Most of our cross-mod usefulness is already free: rats and rat blocks
// expose standard NeoForge IItemHandler / IFluidHandler capabilities, so Create's funnels, belts,
// chutes and depots already pull from / push to rat tubes and rat-cage inventories without any
// further glue.
//
// The bootstrap invokes this class only after NeoForge has confirmed that Create is present.
// Keep that mod-id boundary as the whole compatibility contract until this class has a real
// Create API integration. Probing implementation classes adds class-loading risk without adding
// behaviour: NeoForge capabilities already provide the intended interoperability.
public final class CreateCompat {

	private CreateCompat() {}

	public static void init() {
		// Future hooks (rat-on-contraption survival, depot pickup-target prioritisation, etc) plug
		// in here. The init path is currently no-op because rats already work with Create via the
		// standard NeoForge capability surface.
		RatsMod.LOGGER.debug("Rats/Create: standard NeoForge capability integration enabled");
	}
}
