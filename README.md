# LegacyMinecraftModTemplate

A maintained Forge 1.8.9 mod template based on [hannibal002's template](https://github.com/hannibal002/Example-1.8.9-Mod), with Kotlin, Mixin, MoulConfig, and reusable configuration utilities.

## Features

See above.

## Getting started

Create a repository with GitHub's **Use this template** button. The `Initialize Template Repo` workflow derives a Java-safe package, project class, and mod id from the new repository name.

For local initialization, run `./init.sh <new-package-name> <project-class-name> <modid> <owner>` (see [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)).

## Commands

- `/examplemod` - opens the config GUI.
- `Enter` - opens the config GUI (rebindable under Controls).

## Configuration

The example MoulConfig config is saved to `config/examplemod/config.json`.

## Compatibility

No known incompatibilities.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - initialising, building and the smoke test.
