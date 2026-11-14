# Development

## Initialising

```shell
./init.sh <new-package-name> <project-class-name> <modid> <owner>
```

The initialization script rewrites the package and metadata, moves Java and Kotlin sources, renames the main class and Mixin configuration, and removes its one-shot scaffolding.

## Building

Run Gradle with Java 21 or newer:

```shell
./gradlew --no-daemon build
```

The build uses Gradle 9.3.1 and Essential Loom 1.15.50, while the produced Forge 1.8.9 mod remains Java 8 compatible.
It writes the mod jar to `build/libs/<modid>-<version>.jar`.

## Testing

`bash .github/scripts/smoke_template_generation.sh` initialises a copy of the template and builds it.
