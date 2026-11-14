#!/usr/bin/env bash

set -euo pipefail

assert_path_exists() {
  if [ ! -e "$1" ]; then
    echo "Expected path to exist: $1" >&2
    exit 1
  fi
}

assert_path_missing() {
  if [ -e "$1" ]; then
    echo "Expected path to be absent: $1" >&2
    exit 1
  fi
}

assert_no_match() {
  local pattern="$1"
  shift

  if grep -rnE "$pattern" "$@" >&2; then
    echo "Expected no match for: $pattern" >&2
    exit 1
  fi
}

template_root="$(cd "$(dirname "$0")/../.." && pwd)"
tmp_root="${TMPDIR:-/tmp}/legacy-template-smoke.$$"
trap 'rm -rf "$tmp_root"' EXIT

target="$tmp_root/SmokeMod"
package_dir="com/github/smokeowner/smokemod"
mkdir -p "$target"
tar \
  --exclude=.git \
  --exclude=.gradle \
  --exclude=build \
  --exclude=run \
  -C "$template_root" \
  -cf - . |
  tar -C "$target" -xf -

(
  cd "$target"

  ./init.sh com.github.smokeowner.smokemod SmokeMod smokemod SmokeOwner

  assert_path_missing init.sh
  # init.sh keeps the workflow under GitHub Actions, because a push made with the
  # workflow's GITHUB_TOKEN cannot update workflow files.
  if [ -z "${GITHUB_ACTIONS:-}" ]; then
    assert_path_missing .github/workflows/init.yml
  else
    assert_path_exists .github/workflows/init.yml
  fi
  assert_path_missing .github/scripts/smoke_template_generation.sh
  assert_path_missing docs/DEVELOPMENT.md
  assert_path_missing src/main/java/com/example
  assert_path_missing src/main/kotlin/com/example
  assert_path_exists "src/main/kotlin/${package_dir}/SmokeMod.kt"
  assert_path_exists "src/main/java/${package_dir}/config/SmokeModConfig.java"
  assert_path_exists "src/main/java/${package_dir}/mixin/MixinGuiMainMenu.java"
  assert_path_exists src/main/resources/mixins.smokemod.json
  assert_no_match 'com\.example|examplemod|ExampleMod' src gradle.properties settings.gradle.kts README.md
  grep -qx 'baseGroup = com.github.smokeowner.smokemod' gradle.properties
  grep -qx 'modid = smokemod' gradle.properties
  grep -qx 'username = SmokeOwner' gradle.properties

  if [ "${TEMPLATE_SMOKE_SKIP_BUILD:-false}" = "true" ]; then
    echo "Skipping the generated Gradle build."
  else
    ./gradlew --no-daemon --console=plain build
    assert_path_exists build/libs/smokemod-1.0.0.jar
  fi
)

echo "Template generation smoke test passed."
