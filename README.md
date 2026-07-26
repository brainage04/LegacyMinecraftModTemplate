# LegacyMinecraftModTemplate

A maintained Forge 1.8.9 mod template based on [hannibal002's template](https://github.com/hannibal002/Example-1.8.9-Mod), with Kotlin, Mixin, MoulConfig, and reusable configuration utilities.

## Use the template

Create a repository with GitHub's **Use this template** button. The `Initialize Template Repo` workflow derives a Java-safe package, project class, and mod id from the new repository name.

For local initialization:

```shell
./make-my-own.sh <new-package-name> <project-class-name> <modid> <owner>
```

The initialization script rewrites the package and metadata, moves Java and Kotlin sources, renames the main class and Mixin configuration, and removes its one-shot scaffolding.

## Build

Run Gradle with Java 21 or newer:

```shell
./gradlew --no-daemon build
```

The build uses Gradle 9.3.1 and Essential Loom 1.15.50, while the produced Forge 1.8.9 mod remains Java 8 compatible.
