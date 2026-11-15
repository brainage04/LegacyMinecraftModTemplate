# LegacyMinecraftModTemplate

A maintained Forge 1.8.9 mod template based on [hannibal002's template](https://github.com/hannibal002/Example-1.8.9-Mod), with Kotlin, Mixin, MoulConfig, and reusable configuration utilities.

## Features

See above.

## Getting started

Create a repository with GitHub's **Use this template** button. The `Initialize Template Repo` workflow derives a Java-safe package, project class, and mod id from the new repository name.

For local initialization, run `./init.sh [--side=both|server|client] [--skip-format] <new-package-name> <project-class-name> <modid> <owner>` (see [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)). `--side` drops the half the mod does not need: `client` removes the server mixin and the access transformer, `server` removes the client sources and MoulConfig.

## Commands

- `/examplemod` - opens the config GUI.
- `Enter` - opens the config GUI (rebindable under Controls).

## Configuration

The example MoulConfig config is saved to `config/examplemod/config.json`.

## Compatibility

No known incompatibilities.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - initialising, mixins, access transformers, building and the smoke test.
