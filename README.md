# PIXY BOT PvP MOD — Stage 1 (spawning, names, skins, commands)

Fabric, Minecraft Java 1.21.11, Java 21, Mojang mappings.
Server-side only: bots are real `ServerPlayer`s (same technique as Carpet's /player), so they get
the real player model, skin, name tag, armor rendering, vanilla damage and native death messages.

## Build
1. Open https://fabricmc.net/develop , choose 1.21.11, and copy the exact Loader / Fabric API / Loom
   versions into `gradle.properties` (the ones in the zip are my best guess).
2. Install JDK 21.
3. `gradle wrapper` (once, needs Gradle installed) or import into IntelliJ, then:
   `./gradlew build`
4. JAR: `build/libs/pixy-bot-mod-0.1.0.jar` -> put in `mods/` with Fabric API.

## Commands (OP level 2)
- `/pixybot spawn [name] [skinUsername]` — at your exact position, yaw and pitch
- `/pixybot random [skinUsername]` — random name
- `/pixybot remove <name>`, `/pixybot removeall`, `/pixybot list`

## Troubleshooting (1.21.11 API is new; I couldn't compile this offline)
- Permission line fails: 1.21.11 reworked permissions. Try `.requires(src -> src.hasPermission(2))`
  or `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` -> whichever your IDE accepts.
- `server.services().profileResolver()` missing: use `SkullBlockEntity.fetchGameProfile(name)` instead.
- `snapTo` / `teleportTo` signature errors: use `moveTo` / the teleportTo overload your IDE shows.
- Paste any compiler error to Claude and it will be fixed.

## Not yet implemented (later stages)
Idle look-at AI, kits, horn + Hunter Mode, combat/pathfinding, GUI, persistence.
