# Momentum for Minecraft 26.2 (Fabric)

Unofficial port of [Momentum](https://github.com/AkiraHane/Momentum) by AkiraHane (MIT), a momentum-based
parkour mod (slide, wall run, wall climb, vault, dodge and more), to Minecraft 26.2 on Fabric.
Not affiliated with the original author.

## Download

See [Releases](https://github.com/enkinvsh/Momentum/releases). Requirements: Minecraft 26.2, Fabric Loader 0.18.6+,
Fabric API, [Player Animation Library 1.2.6 for 26.2](https://modrinth.com/mod/player-animation-library/version/Hq1CwB9w),
Java 25.

## Differences from upstream 1.3.0-beta

- **Jet Booster removed**: the `momentum:jet_booster` item, the `momentum:jet1..3` sounds and the Trinkets
  integration. Those were the mod's only vanilla registry entries, so a server running this port registers nothing
  new and players without the mod can still join. All maneuvers stay available
  (`ALLOW_MANEUVER_WITHOUT_BOOSTER` defaults to `true`); booster code paths remain as dead code.
- Toggle keys (enable mod, key hints; upstream Shift+M / Shift+N) are unbound by default.
  Bind them in Controls → Momentum.
- Key hints are off by default.

## Origin

Upstream publishes NeoForge sources only. This code is the Fabric jar from upstream release v1.3.0-beta
(`momentum-fabric-26.1.2-1.3.0-beta.jar`, sha256 `092f176447d01c9be7f8d1795cb79951a88c73172fd1a06c49e1da17e71fb9f1`)
decompiled with Vineflower 1.12.0 (`-dgs=1 -rsy=1`) and then ported. Resources are taken from the jar unchanged,
except for the removed Jet Booster files.

Every code change is marked with a `// 26.2 port:` or `// port default:` comment:

- `mixin/EntityMixin`: `Entity.collectColliders` was renamed to `collectCollidersIgnoringWorldBorder` in 26.2
  (same signature).
- `mixin/ItemInHandRendererMixin`: target `renderHandsWithItems` was renamed to `submitHandsWithItems`.
- `fabric/client/hud/FabricHintHud`: `Options.hideGui` is gone, uses `minecraft.gui.hud.isHidden()`.
- Mixins: decompiler output `(X)this` changed to `(X)(Object)this`.
- `fabric.mod.json`: `minecraft` dependency `~26.2`, `license` set to `MIT` (the upstream jar said
  "All Rights Reserved", while the upstream LICENSE, GitHub and Modrinth say MIT), contact links.
- `build.gradle`: compile-only Mocha (Molang engine) extracted from the Player Animation Library jar-in-jar,
  compile-only Lombok for leftover `@lombok.Generated`, separate `run-client` directory for the dev client.

## Build

```bash
./gradlew build   # JDK 25+, output: build/libs/momentum-fabric-26.2-1.3.0-beta+mk.26.2.jar
```

## License

MIT, see [LICENSE](LICENSE). Original work © 2026 AkiraHane.
