# Historical unofficial-port notes

These notes were inherited from the earlier unofficial port. They preserve its author and porter credits; they are not my changelog or a claim that I independently retested every historical entry. See the current [README](../README.md) for my changes and validation.

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
