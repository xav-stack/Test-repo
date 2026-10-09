---
name: Minecraft smoke tests
description: Version-specific notes for testing the Fabric 1.20.1 mod server with an offline Mineflayer client.
---

Keep offline-mode test servers bound to `127.0.0.1`; offline authentication accepts arbitrary usernames.

**Why:** Offline mode disables username authentication and is unsafe on a publicly reachable server.

**How to apply:** Bind the test server to loopback and stop it after the local Mineflayer test.

For a fresh Fabric 1.20.1 superflat world, provide explicit flat generator settings alongside `level-type=minecraft:flat`. Empty generator settings can log `No key layers in MapLike[{}]` even if fallback terrain looks flat.

**Why:** The server's flat-settings codec expects a layer definition; an explicit bedrock/dirt/grass layer configuration avoids the startup error.

**How to apply:** Configure the desired biome, layers, feature/lake flags, and structure overrides before generating the world.

Mineflayer's `Item.displayName` can show the vanilla registry label (for example, “Barrel”) instead of an item's NBT custom name.

**Why:** Mineflayer's registry label is not proof that the server-side custom name or marker tag is missing.

**How to apply:** Inspect item NBT or verify the mod's actual binding and command behavior rather than relying on `displayName`.
