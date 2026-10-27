# Rats — Features and Fixes for NeoForge 1.21.1

This is my feature and maintenance fork of an existing **unofficial NeoForge 1.21.1 port** of [Rats](https://github.com/AlexModGuy/Rats). I build on the [repaired port maintained by eba67](https://github.com/eba67/Rats-Fix-Port--1.21.1), whose README credits [DearDanielr](https://github.com/DearDanielr) for the unofficial port. I did not create the original mod or that port.

My work adds behavior improvements, compatibility changes and repairs on top of that port. This is more than a loader conversion, and it is not an official Rats release.

## My changes

- Repairs to rat AI, harvesting, transport, movement and upgrade behavior.
- Dedicated-server and client packet handling fixes.
- Loot repairs, including the pet-shop chest fallback used with CTOV.
- Configurable garbage encounter lighting policies; the default remains dark spawning.
- Selected quarry and flight improvements from the [newer unofficial release line](https://github.com/gabriellj67/Rats-Unofficial-1.21.1NEO), while preserving the existing local repairs.

This fork does not include every feature from the newer unofficial line. Square portals and its new backpack integration have not been imported. The [historical port notes](docs/UPSTREAM_PORT_NOTES.md) are kept separately so their fixes are not mistaken for my work.

## Downloads and requirements

Download the matching runtime JAR from [my releases](https://github.com/ashleywr/rats-neoforge-1.21.1/releases).

- Minecraft **1.21.1**, **NeoForge**, and **Java 21**.
- **Citadel 2.7.0 or newer** for the matching Minecraft/loader version; the current build was tested with 2.7.1.
- JEI and Jade are optional client integrations. See the chosen release for its exact dependency ranges.

Install the matching build on the server and clients. Close Minecraft before replacing its JAR and keep only one Rats build installed. Existing mod and registry IDs remain `rats`; the repository rename does not rename saved-world content.

## Building and validation

Use Java 21 and the checked-in wrapper:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

The current maintenance build passed all **55 required dedicated-server GameTests**. The suite covers selected gameplay, AI, loot and port regressions; it does not establish that every feature or modpack combination works. Test sources and their disposable world are separate from the runtime JAR.

For problems introduced by this fork, use [this repository's issue tracker](https://github.com/ashleywr/rats-neoforge-1.21.1/issues) and include the release filename, loader/dependency versions, logs and a reproduction. Do not send fork-specific support requests to the original authors.

## Credits and license

The original Rats mod is by AlexModGuy/AlexThe666, GizmoTheMoonPig and its contributors. The earlier unofficial port and repair work retain their own author credits, including DearDanielr and eba67. Selected newer backports are credited to the gabriellj67 release line.

The fork remains under **LGPL-3.0**, with the existing license, copyright notices and asset attributions retained. My changes do not imply endorsement by the original authors or port maintainers. Builds are provided as is; maintenance is best effort.
