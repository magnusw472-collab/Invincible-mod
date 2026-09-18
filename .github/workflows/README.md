# Invincible (Last Stand) — Fabric mod for 1.21.11

A Fabric mod (not a Bukkit/Spigot plugin — Fabric servers use mods, and mods for
1.21.11 need mixins rather than simple event listeners) that gives a toggleable
"Last Stand" mode per player:

- Normal damage still applies exactly like vanilla — armor, potions, knockback,
  hunger, fall damage, fire, all untouched.
- Totems of Undying still work exactly like vanilla — this mod doesn't touch
  totem logic at all; it only fires after vanilla has already decided a totem
  didn't save the player.
- The only change: if a hit is fatal and no totem saved them, the death is
  cancelled and their health is set to 1.0 (half a heart) instead.

## Why a mixin, and why hook `die()` instead of the damage calculation

Fabric API doesn't expose an event that lets you *modify* the final damage
amount (only allow/deny it outright), and armor/potion/enchantment modifiers
are applied deep inside vanilla's damage pipeline — reimplementing that math
in the mod risks drifting from vanilla behavior over time.

Instead, this hooks `LivingEntity#die(DamageSource)`, which vanilla only calls
once it has already applied all damage modifiers, checked for a Totem of
Undying, and determined the entity should actually die. That makes it a
narrow, stable interception point: everything upstream of it (armor, potions,
totems) is exactly vanilla.

## Project layout

```
build.gradle
settings.gradle
gradle.properties
src/main/resources/fabric.mod.json
src/main/resources/invincible.mixins.json
src/main/java/com/mag/invincible/InvincibleMod.java       <- command registration
src/main/java/com/mag/invincible/mixin/LivingEntityMixin.java   <- the die() hook
```

## Build

This project has no Gradle wrapper jar included (I don't have network access
in my build environment to download one). To build it:

1. Open the folder in IntelliJ IDEA with the Gradle + Minecraft Development
   plugins (easiest — it'll generate the wrapper and sync automatically), **or**
2. Run `gradle wrapper --gradle-version 8.10` once yourself if you have Gradle
   installed, then `./gradlew build`.

Either way, once synced: `./gradlew build` produces the mod jar under
`build/libs/invincible-fabric-1.0.0.jar`. Drop that into your server's `mods/`
folder alongside **Fabric API** (required — this mod depends on it for
command registration) and Fabric Loader.

### Version pins used (current for 1.21.11 as of writing)

| Component | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.18.4 |
| Yarn mappings | 1.21.11+build.4 |
| Fabric Loom | 1.14 |
| Fabric API | 0.141.3+1.21.11 |
| Java | 21 (required — 1.21.11 is the last version that still runs on Java 21) |

If any of these have moved by the time you build (Fabric API in particular
ships frequent point releases), bump the version in `gradle.properties` — the
rest of the project doesn't need to change.

## Usage

- `/invincible` — toggle Last Stand for yourself (requires permission level 2, i.e. op)
- `/invincible <player>` — toggle Last Stand for another player (also requires op)
- `/god` — alias for the same command

Permission is currently hardcoded to "must be op" (`hasPermissionLevel(2)`).
If you're using a permissions mod (e.g. LuckPerms via Fabric Permissions API)
and want a finer-grained permission node instead of op-only, let me know and
I'll wire that in.

## Known caveats

- **Doesn't persist** across server restarts — the enabled/disabled set is
  in-memory only. Can add saving to a file if you want it to survive restarts.
- **No cooldown / unlimited saves** — per your spec, this triggers every
  single time, indefinitely, with no charge limit or cooldown.
- **Repeated damage sources** (fire, poison, void): the player will be saved
  each time it would kill them, but if they're still in the damage source
  they'll immediately take another lethal hit next tick — so they survive,
  but won't recover until removed from the hazard. Can add an automatic
  teleport-to-safety on void damage if useful.
- I wasn't able to compile/test this in my current environment (no network
  access to pull Minecraft/Fabric dependencies), so treat this as a solid
  first pass to build and test on your end. The one part most likely to need
  a small tweak if it doesn't compile out of the box is the
  `CommandRegistrationCallback` signature in `InvincibleMod.java` — Fabric API
  has adjusted that a couple of times across versions, so if you get a compile
  error there, paste it back to me and I'll fix it immediately.
