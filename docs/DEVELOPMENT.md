# Development

## Initialising

```shell
./init.sh [--side=both|server|client] <new-package-name> <project-class-name> <modid> <owner>
```

The initialization script rewrites the package and metadata, moves Java and Kotlin sources, renames the main class and Mixin configuration, and removes its one-shot scaffolding.

`--side` (default `both`) decides what the generated mod keeps:

- `both` keeps the client and server halves: keybinds, the config GUI and client commands in `ClientSetup`, and a server mixin.
- `client` removes the server mixin and the access transformer, and marks the mod `clientSideOnly`, so the build has no server run configuration.
- `server` removes the client sources and MoulConfig, writes a server-only mod class, and marks the mod `serverSideOnly`.

## Mixins

Mixin classes are not listed in `mixins.<modid>.json`. `AutoDiscoveryMixinPlugin` returns every class in the `mixin` package from `getMixins()`, which Mixin calls once per config after it knows the side. Classes in `mixin.client` are returned only on a physical client, so a dedicated server never loads them.

## Access transformers

`src/main/resources/accesstransformer.cfg` widens members of the game jar. The build applies it to the Minecraft jar it compiles against, so an entry that names nothing fails the build, and ships it as `META-INF/<modid>_at.cfg` with the `FMLAT` manifest entry. Members use SRG names (`field_`/`func_`), as in Forge's own `forge_at.cfg`.

## Building

Run Gradle with Java 21 or newer:

```shell
./gradlew --no-daemon build
```

The build uses Gradle 9.3.1 and Essential Loom 1.15.50, while the produced Forge 1.8.9 mod remains Java 8 compatible.
It writes the mod jar to `build/libs/<modid>-<version>.jar`.

## Testing

`bash .github/scripts/smoke_template_generation.sh` initialises a copy of the template for each side and builds it.

`./gradlew runServer` starts a development server with the mod's mixins applied; it logs `[AutoDiscoveryMixinPlugin] side SERVER, mixins [...]` and `Server mixin applied`.
