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

assert_match() {
  local pattern="$1"
  shift

  if ! grep -qE "$pattern" "$@"; then
    echo "Expected a match for: $pattern" >&2
    exit 1
  fi
}

template_root="$(cd "$(dirname "$0")/../.." && pwd)"
tmp_root="${TMPDIR:-/tmp}/legacy-template-smoke.$$"
trap 'rm -rf "$tmp_root"' EXIT

smoke_side() {
  local side="$1"
  local target="$tmp_root/$side/SmokeMod-$side"
  local package_dir="com/github/smokeowner/smokemod_$side"
  local mod_id="smokemod_$side"

  echo "Testing init.sh --side=$side"
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

    init_args=(--side="$side")
    if [ "${TEMPLATE_SMOKE_SKIP_BUILD:-false}" = "true" ]; then
      init_args+=(--skip-format)
    fi
    ./init.sh "${init_args[@]}" "com.github.smokeowner.${mod_id}" "SmokeMod${side}" "$mod_id" SmokeOwner

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
    assert_path_exists "src/main/kotlin/${package_dir}/SmokeMod${side}.kt"
    assert_path_exists "src/main/java/${package_dir}/init/AutoDiscoveryMixinPlugin.java"
    assert_path_exists "src/main/resources/mixins.${mod_id}.json"
    assert_match '"plugin": "\$\{basePackage\}\.init\.AutoDiscoveryMixinPlugin"' "src/main/resources/mixins.${mod_id}.json"
    assert_no_match 'com\.example|examplemod|ExampleMod' src gradle.properties settings.gradle.kts README.md
    # Loom adds -XstartOnFirstThread to the client run on macOS, where it crashes the game.
    assert_match 'IS_OS_MAC_OSX' build.gradle.kts
    grep -qx "baseGroup = com.github.smokeowner.${mod_id}" gradle.properties
    grep -qx "modid = ${mod_id}" gradle.properties
    grep -qx "modSide = ${side}" gradle.properties
    grep -qx 'username = SmokeOwner' gradle.properties

    case "$side" in
      both)
        assert_path_exists "src/main/java/${package_dir}/mixin/MixinMinecraftServer.java"
        assert_path_exists "src/main/java/${package_dir}/mixin/client/MixinGuiMainMenu.java"
        assert_path_exists "src/main/java/${package_dir}/config/SmokeMod${side}Config.java"
        assert_path_exists "src/main/kotlin/${package_dir}/ClientSetup.kt"
        assert_path_exists src/main/resources/accesstransformer.cfg
        assert_match 'accessTransformer\(transformerFile\)' build.gradle.kts
        assert_match 'FMLAT' build.gradle.kts
        assert_no_match 'clientSideOnly|serverSideOnly' "src/main/kotlin/${package_dir}/SmokeMod${side}.kt"
        ;;
      server)
        assert_path_exists "src/main/java/${package_dir}/mixin/MixinMinecraftServer.java"
        assert_path_missing "src/main/java/${package_dir}/mixin/client"
        assert_path_missing "src/main/java/${package_dir}/config"
        assert_path_missing "src/main/kotlin/${package_dir}/ClientSetup.kt"
        assert_path_exists src/main/resources/accesstransformer.cfg
        assert_match 'serverSideOnly = true' "src/main/kotlin/${package_dir}/SmokeMod${side}.kt"
        assert_no_match 'moulconfig' build.gradle.kts
        ;;
      client)
        assert_path_missing "src/main/java/${package_dir}/mixin/MixinMinecraftServer.java"
        assert_path_exists "src/main/java/${package_dir}/mixin/client/MixinGuiMainMenu.java"
        assert_path_exists "src/main/java/${package_dir}/config/SmokeMod${side}Config.java"
        assert_path_missing src/main/resources/accesstransformer.cfg
        assert_no_match 'accessTransformer|FMLAT|accesstransformer' build.gradle.kts
        assert_match 'clientSideOnly = true' "src/main/kotlin/${package_dir}/SmokeMod${side}.kt"
        ;;
    esac

    # The server run configuration follows the side. Loom keeps the task of a removed run and
    # disables it (LoomTasks.registerRunTasks, architectury-loom 1.15.50), so the task's enabled
    # state is what tells the two apart; --dry-run marks every task SKIPPED either way.
    cat >"$tmp_root/run-server-state.gradle" <<'GRADLE'
gradle.projectsEvaluated {
    def task = gradle.rootProject.tasks.findByName('runServer')
    println "RUN_SERVER_ENABLED=" + (task != null && task.enabled)
}
GRADLE
    run_server_state=$(./gradlew --no-daemon --console=plain -q -I "$tmp_root/run-server-state.gradle" help | grep '^RUN_SERVER_ENABLED=' || true)
    if [ "$side" = "client" ]; then
      expected_state="RUN_SERVER_ENABLED=false"
    else
      expected_state="RUN_SERVER_ENABLED=true"
    fi
    if [ "$run_server_state" != "$expected_state" ]; then
      echo "Expected $expected_state for side $side, got: ${run_server_state:-nothing}" >&2
      exit 1
    fi

    if [ "${TEMPLATE_SMOKE_SKIP_BUILD:-false}" = "true" ]; then
      echo "Skipping the generated ${side} Gradle build."
    else
      ./gradlew --no-daemon --console=plain build
      assert_path_exists "build/libs/${mod_id}-1.0.0.jar"
    fi
  )
}

smoke_side both
smoke_side server
smoke_side client

echo "Template generation smoke test passed."
