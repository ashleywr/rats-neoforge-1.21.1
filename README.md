Unofficial Port and Fix of Rats Mod by AlexModGuy

Original mod by : https://github.com/AlexModGuy
Unofficial Port by : https://github.com/DearDanielr
__________________________________________________

FIXED Crash when try to trade with plague doctor

FIXED Crash when render mobs in ratlantis

FIXED Crash when curdling station rendering fluids

FIXED Crash when using curdling station

FIXED someone methods relationed with nbt and loot

Optimized nbt files

FIXED structures natural generation 

FIXED some crashes/bugs caused by incorrectly named folders and files

FIXED plague doctor spamming invisibility potions non-stop at night

FIXED server attempting to import ClientLevel

Added Earthworks Compat

FIXED crash when trying to summon dutchrat

FIXED crash when rat manipulates fluids

FIXED crafts with empty tag ( ex: c:string )  

FIXED Attempt to load the client class (CreativeTabClientHelper) on the server

FIXED The rat follows the player with excessive teleporting

FIXED Natural spawn of Pied Piper and rats

FIXED remaining empty tags
c:dusts/prismarine
c:gravel
c:gunpowder
c:stone
c:cobblestone/mossy

FIXED SyncThrownBlockPacket being sent incorrectly during the Neo-Ratlantean fight

Change in the generation of rats from 3 at a time to 2.

FIXED anvil enchantment on mod equipments

FIXED Ratlantis sword deals no damage

FIXED Rats do not spawn in Garbage ( they would spawn, but it was extremely rare )

FIXED enchantment by the enchanting table on mod equipments

FIXED Upgrades models don't have effects in singleplayer before player quit and re-enters in world

FIXED Basic speed upgrade is excessively fast

FIXED :The flying upgrade wasn't working

FIXED :Rendering crash when using the buccaneer upgrade

FIXED :Flight error in other upgrades
-Bee Upgrade
-Dragon Upgrade
-Biplane Mount

FIXED :Rats don't drop items
raw_rat: 100% chance
rat_pelt: 25% chance
rat_skull: 5% chance
rat_paw: 10% chance
chunky_cheese_token: 0.01% chance (ratlantis)
plague_essence: 5% chance (if have plague)
plague_tome: 0.35% chance (if have plague)

FIXED :The rat cannot move with other mount upgrades
-Golem Mount Upgrade
-Strider Mount Upgrade
-Chicken Mount Upgrade
-Beast Mount Upgrade

FIXED :With the automaton upgrade, the rats couldn't walk

FIXED :Rats and Pied Piper don't Spawn in garbage pile

FIXED petshops loot table (#4)
- desert.nbt
- plains.nbt
- savanna.nbt
- taiga.nbt
- snowy.nbt
- legacy_pet_shop.nbt
- zombie_desert.nbt
- zombie_plains.nbt
- zombie_savanna.nbt
- zombie_snowy.nbt
- zombie_taiga.nbt

FIXED mod banners doesn't exist
- Cheese Banner Pattern
- Rat and Crossbones Banner Pattern
- Rat and Sickle Banner Pattern
- Rat Banner Pattern

FIXED rat pathfinding in quarry platform (#6)

Modified the deposit rat system in quarry

FIXED crash when using mob filter upgrade with Re:Avaritia Mod (#10)

<!-- ashley-release-information -->

## Ashley's builds

This is an unofficial maintenance fork. Original authors retain ownership and credit. See the [upstream project](https://github.com/eba67/Rats-Fix-Port--1.21.1) for the original mod. Local changes do not imply upstream endorsement.

Licensing: **LGPL-3.0**. Existing license files, copyright notices, attribution and any separate asset terms remain in force. This fork does not relicense upstream work.

Provided **AS IS**, without warranty of any kind. Maintenance, updates and support are best effort; compatibility with every modpack is not guaranteed.

### Downloads and installation

Download a runtime `.jar` from [GitHub Releases](https://github.com/ashleywr/Rats-Fix-Port--1.21.1/releases). Use the Minecraft version and loader named in that release and install its required dependencies. Do not install `-sources`, `-dev`, `-slim`, `-common` or test JARs as the mod.

Close Minecraft, back up your world, and place the matching runtime JAR in the instance's `mods/` directory. Replace older versions of this same mod rather than loading duplicates. For content mods, install matching versions on the server and its clients unless the release explicitly says client-only or server-only.

### Building and reporting problems

Use Java 21 and the checked-in Gradle wrapper:

```powershell
.\gradlew.bat build
```

Build outputs are under `build/libs/` or each loader subproject's `build/libs/`. Some projects require the local development dependencies described above; those dependencies are not included in the download.

For a problem report, include the exact release filename, Minecraft and loader versions, relevant mod versions, `logs/latest.log`, and a crash report if one was written. Remove private information before sharing logs. Remove the JAR to uninstall; worlds containing this mod's blocks, entities or world generation may require restoring a backup.
