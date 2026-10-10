# Cannons

A server-side Fabric mod for Minecraft 1.20.1. Vanilla clients can join without installing the mod.
The project targets Java 17 and uses Yarn `1.20.1+build.10`, Fabric Loader `0.19.5`, and Fabric API
`0.92.12+1.20.1`.

## Build and install

```sh
./gradlew test build
```

Copy `build/libs/cannons-1.0.0.jar` to the dedicated server's `mods/` directory, along with
Fabric API. The server must run Java 17 or newer. No client jar or custom resource pack is needed.

The JUnit suite covers the numerical and state-management logic. Mapped Minecraft sources for the
pinned version were generated and checked while implementing the mixins.

## Configuration

The server creates `config/cannons.json` on first start. Invalid values are clamped and logged.

| Setting | Default | Meaning |
| --- | ---: | --- |
| `projectileSpeedBlocksPerSecond` | 30 | Cannonball travel speed; 30 blocks/s is 1.5 blocks per tick. |
| `refusalRadiusBlocks` | 20 | Radius around the cannon in which an obstructed launch is refused. |
| `offsetRadiusBlocks` | 8 | Maximum random target offset; sampled uniformly through the sphere's volume. |
| `chunkRetentionSeconds` | 90 | How far ahead a firing cannon keeps its target corridor ticketed. |
| `explosionPower` | 4 | Power of the final TNT explosion. |
| `lookRangeBlocks` | 5 | Maximum command raycast distance to select a cannon. |
| `maxRpm` | 60 | Maximum emerald-count firing rate. |
| `arcHeightRatio` | 0.25 | Arc rise as a fraction of straight-line distance. |
| `collisionSubstepBlocks` | 0.25 | Maximum projectile collision-sweep step; values above 0.98 are clamped to prevent tunneling through one-block obstacles. |
| `chunkWaitTimeoutTicks` | 200 | Maximum wait for an unloaded corridor chunk before abandoning a shot. |
| `noticeThrottleSeconds` | 1 | Minimum interval for grouped blocked-shot action-bar notices. |
| `batteryNameMaxLength` | 16 | Maximum length of a battery name. |

## Cannon behavior

Crafting the shaped recipe in `data/cannons/recipes/cannon.json` produces a specially named vanilla
barrel item. Placing that item registers the barrel and adds a centered, non-see-through `cannon`
text label. The barrel still uses the vanilla block and inventory; no custom blocks, entities,
items, packets, or registries are added. The actual crafting result is the custom cannon item, but
the vanilla recipe-book ghost displays a plain barrel.

The cannon inventory is a 9-by-3 grid:

- Slots 0–17 accept TNT.
- Slots 18–25 accept any item with a custom name. A stack named exactly like a player's username,
  including case, binds that player to the cannon.
- Slot 26 accepts emeralds; the emerald count sets RPM, clamped to 1 through `maxRpm`.
- Hoppers and droppers may insert TNT into slots 0–17 and extract from those slots only.

Any player can open or break a cannon. Breaking it drops a fresh cannon item (except for creative
players) and the barrel spills its contents using vanilla behavior. Battery membership is removed
when the binding is removed, and all membership is removed when the cannon is removed.

## Commands

Coordinates are absolute doubles. They must be finite, within the world border, and inside the
world's build-height range. Target-all coordinates are interpreted in the command player's dimension.

| Command | Behavior |
| --- | --- |
| `/cannontarget x y z` or `/ct x y z` | Set the target of the bound cannon being looked at. |
| `/cannontargetall x y z` or `/cta x y z` | Set the target on loaded, bound cannons in all dimensions; report skipped dimensions, chunks, and bindings. |
| `/cannontargetall battery x y z` or `/cta battery x y z` | Set the target for that battery's loaded, bound cannons in the player's dimension. |
| `/cannonfire` or `/cf` | Start or stop the bound cannon being looked at. |
| `/cannonfireall [battery]` or `/cfa [battery]` | Start selected cannons, or stop the selected cannons that are currently firing. Unloaded members receive asynchronous chunk tickets and are rechecked when loaded. |
| `/cannonbattery list` or `/cb list` | List the player's batteries and member counts. |
| `/cannonbattery add name` or `/cb add name` | Add the bound cannon being looked at to a battery; creates the battery if needed. |
| `/cannonbattery remove name` or `/cb remove name` | Remove the bound cannon being looked at; empty batteries are deleted. |
| `/cannongive [player]` | Give a cannon to yourself or the selected player. Requires permission level 2. |

All commands other than `/cannongive` require a player. Battery names must match `[A-Za-z_]{1,16}`.
When a cannon runs out of TNT, its starter receives a private chat message. For a single cannon the
message includes its block coordinates and dimension so ammunition can be restored there. Blocked
shots are grouped into action-bar notices.

## Firing and persistence

The firing rate uses integer progress against 1,200 units per shot, avoiding fractional drift. A
shot uses a parabolic arc, sweeps for solid-block collisions in substeps, and refuses the launch if
the path intersects solid blocks within the configured radius around the cannon. Fluids are ignored.
The cannon consumes one TNT only after a launch passes that preflight check. The projectile is a
tagged vanilla TNT entity with gravity and its regular fuse disabled. It travels along the arc,
detonates on collision or at the target, and does not damage blocks until the final explosion.

The mod uses a non-expiring, per-dimension reference-counted chunk ticket at the same ticket level
as vanilla `/forceload`; it never synchronously loads chunks. If a needed chunk remains unavailable
for the configured timeout, the shot is abandoned without consuming TNT or exploding.

Registered cannon positions are stored per world. Battery membership is stored in the Overworld's
persistent state. Label entities are reconciled through entity load/unload events. Firing targets,
cadence, active firing, projectiles, and chunk tickets are runtime-only and are not saved.
Tagged projectiles found in saved entity data are discarded on load.

## Open questions

The prompt did not assign the three custom death messages—`X experienced ballistics.`,
`X was hammered.`, and `X failed to look up.`—to specific damage scenarios. The mod currently
chooses one randomly for each death caused by a cannon projectile.
