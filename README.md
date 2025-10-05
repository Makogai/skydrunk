# SkyDrunk

Modern Fabric client mod for Hypixel Skyblock
- Shard tracking (dynamic for all shard names)
- Live price fetch (Hypixel bazaar v1 public API)
- Clean, movable HUD + MoulConfig UI
- Optional dungeon tripwire highlighting

## Requirements
- Minecraft 1.21.5
- Fabric Loader ≥ 0.15.11
- Fabric API

## Install
1. Grab the latest jar from **Releases**.
2. Put it in `mods/`.

## Commands
- `/sd` — open settings
- `/sd reset` — reset shard session
- `/sd drag` — move HUD
- `/sd shard set "<Name>"` — pick shard to track
- `/sd price now` — check bazaar price for current shard
- `/sd price set "<Name>" <BZ_ID>` — override bazaar id

## Build
```bash
./gradlew build remapJar
# Output: build/libs/skydrunk-<version>.jar
