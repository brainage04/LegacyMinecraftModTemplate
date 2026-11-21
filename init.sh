#!/usr/bin/env bash

set -euo pipefail

print_help() {
  printf 'Usage: %s [--side=both|server|client] [--skip-format] <new package name> <project class name> <modid> <owner>\n' "$0"
}

side="both"
format_sources="true"
positionals=()

while [ "$#" -gt 0 ]; do
  case "$1" in
    --side=*)
      side="${1#*=}"
      shift
      ;;
    --side)
      if [ "$#" -lt 2 ]; then
        print_help >&2
        exit 64
      fi
      side="$2"
      shift 2
      ;;
    --skip-format)
      format_sources="false"
      shift
      ;;
    -h | --help)
      print_help
      exit 0
      ;;
    --*)
      printf 'Unknown option: %s\n' "$1" >&2
      print_help >&2
      exit 64
      ;;
    *)
      positionals+=("$1")
      shift
      ;;
  esac
done

if [ "${#positionals[@]}" -ne 4 ]; then
  print_help >&2
  exit 64
fi

case "$side" in
  both | server | client) ;;
  *)
    printf 'Invalid side: %s\n' "$side" >&2
    print_help >&2
    exit 64
    ;;
esac

base=$(dirname "$(readlink -f "$0")")
package_name="${positionals[0]}"
project_name="${positionals[1]}"
modid="${positionals[2]}"
owner="${positionals[3]}"

if [[ ! "$package_name" =~ ^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)+$ ]]; then
  printf 'Invalid Java package: %s\n' "$package_name" >&2
  exit 64
fi
if [[ ! "$project_name" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  printf 'Invalid project class name: %s\n' "$project_name" >&2
  exit 64
fi
if [[ ! "$modid" =~ ^[a-z_][a-z0-9_]*$ ]]; then
  printf 'Invalid mod id: %s\n' "$modid" >&2
  exit 64
fi
if [[ ! "$owner" =~ ^[A-Za-z0-9-]+$ ]]; then
  printf 'Invalid GitHub owner: %s\n' "$owner" >&2
  exit 64
fi

package_dir=$(printf '%s' "$package_name" | tr '.' '/')

printf 'Updating %s\n' "$base"
printf 'Setting package name to %s\n' "$package_name"
printf 'Setting project name to %s\n' "$project_name"
printf 'Setting mod id to %s\n' "$modid"
printf 'Setting owner to %s\n' "$owner"
printf 'Setting side to %s\n' "$side"

find "$base/src/main" -type f -exec sed -i \
  -e "s|com\.example|$package_name|g" \
  -e "s|examplemod|$modid|g" \
  -e "s|ExampleMod|$project_name|g" {} +

sed -i \
  -e "s|com\.example|$package_name|g" \
  -e "s|examplemod|$modid|g" \
  -e "s|ExampleMod|$project_name|g" \
  -e "s|brainage04|$owner|g" \
  -e "s|^modSide = .*|modSide = $side|" "$base/gradle.properties"
sed -i "s|examplemod|$project_name|g" "$base/settings.gradle.kts"

move_package_tree() {
  local source="$1"
  local target="$2"
  [ -d "$source" ] || return 0
  mkdir -p "$(dirname "$target")"
  mv "$source" "$target"
}

move_package_tree "$base/src/main/java/com/example" "$base/src/main/java/$package_dir"
move_package_tree "$base/src/main/kotlin/com/example" "$base/src/main/kotlin/$package_dir"

if [ -f "$base/src/main/kotlin/$package_dir/ExampleMod.kt" ]; then
  mv "$base/src/main/kotlin/$package_dir/ExampleMod.kt" "$base/src/main/kotlin/$package_dir/$project_name.kt"
fi
if [ -f "$base/src/main/java/$package_dir/config/ExampleModConfig.java" ]; then
  mv "$base/src/main/java/$package_dir/config/ExampleModConfig.java" \
    "$base/src/main/java/$package_dir/config/${project_name}Config.java"
fi
mv "$base/src/main/resources/mixins.examplemod.json" "$base/src/main/resources/mixins.$modid.json"

java_dir="$base/src/main/java/$package_dir"
kotlin_dir="$base/src/main/kotlin/$package_dir"

case "$side" in
  both) ;;
  client)
    # A client-only mod never runs a dedicated server, so the @Mod annotation says so and the
    # build drops the server run configuration (modSide in gradle.properties).
    sed -i "s|@Mod(modid = ${project_name}.MOD_ID, useMetadata = true)|@Mod(modid = ${project_name}.MOD_ID, useMetadata = true, clientSideOnly = true)|" \
      "$kotlin_dir/${project_name}.kt"
    if ! grep -q 'clientSideOnly = true' "$kotlin_dir/${project_name}.kt"; then
      printf 'Could not mark the mod client-only: the @Mod line in %s is not the one this script rewrites.\nThe tree is already renamed, so reset it before trying again.\n' \
        "$kotlin_dir/${project_name}.kt" >&2
      exit 1
    fi

    # The server mixin and the access transformer exist to show both on a dedicated server.
    rm -f "$java_dir/mixin/MixinMinecraftServer.java" "$base/src/main/resources/accesstransformer.cfg"
    sed -i \
      -e '/If you don.t want access transformers, remove this line and accesstransformer.cfg/d' \
      -e '/^        accessTransformer(transformerFile)$/d' \
      -e '/If you don.t want access transformers, remove this line$/d' \
      -e '/^        this\["FMLAT"\] = /d' \
      -e '/^val transformerFile = /d' \
      "$base/build.gradle.kts"
    # ${modid} is Gradle's template expression in build.gradle.kts, not a shell variable.
    # shellcheck disable=SC2016
    sed -i 's|    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")||' "$base/build.gradle.kts"
    ;;
  server)
    # A server-only mod has no client: no keybinds, no config GUI, no client commands, no client mixin.
    sed -i "s|@Mod(modid = ${project_name}.MOD_ID, useMetadata = true)|@Mod(modid = ${project_name}.MOD_ID, useMetadata = true, serverSideOnly = true)|" \
      "$kotlin_dir/${project_name}.kt"
    if ! grep -q 'serverSideOnly = true' "$kotlin_dir/${project_name}.kt"; then
      printf 'Could not mark the mod server-only: the @Mod line in %s is not the one this script rewrites.\nThe tree is already renamed, so reset it before trying again.\n' \
        "$kotlin_dir/${project_name}.kt" >&2
      exit 1
    fi
    rm -rf "$java_dir/mixin/client" "$java_dir/config" "$java_dir/keybinds" "$java_dir/commands" \
      "$java_dir/utils" "$kotlin_dir/config" "$kotlin_dir/keybinds" "$kotlin_dir/commands" \
      "$kotlin_dir/utils" "$kotlin_dir/errors"
    rm -f "$kotlin_dir/ClientSetup.kt"

    # MoulConfig draws a client config screen, so a server-only mod does not ship it.
    sed -i \
      -e '/shadowModImpl("org.notenoughupdates.moulconfig/d' \
      -e '/moulconfig.tweaker.DevelopmentResourceTweaker/d' \
      -e '/relocate("io.github.notenoughupdates.moulconfig"/d' \
      "$base/build.gradle.kts"
    perl -0pi -e 's|\n// MoulConfig finds its Minecraft adapter.*?\ntasks\.named\("check"\) \{\n    dependsOn\(verifyMoulConfigService\)\n\}\n||s' \
      "$base/build.gradle.kts"

    cat >"$kotlin_dir/${project_name}.kt" <<EOF
package $package_name

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import org.apache.logging.log4j.LogManager

@Mod(modid = ${project_name}.MOD_ID, useMetadata = true, serverSideOnly = true)
class ${project_name} {
    @Mod.EventHandler
    fun serverStarting(event: FMLServerStartingEvent) {
        event.registerServerCommand(object : CommandBase() {
            override fun getCommandName() = MOD_ID

            override fun getCommandUsage(sender: ICommandSender) = "/\$MOD_ID"

            override fun processCommand(sender: ICommandSender, args: Array<String>) {
                LOGGER.info("{} ran on the server", MOD_ID)
            }
        })
    }

    companion object {
        const val MOD_ID = "$modid"
        const val MOD_NAME = "$project_name"
        val LOGGER = LogManager.getLogger(MOD_ID)
    }
}
EOF
    ;;
esac

cat >"$base/README.md" <<EOF
# $project_name

A Forge 1.8.9 mod generated from brainage04/LegacyMinecraftModTemplate.

Run Gradle with Java 21 or newer:

\`\`\`shell
./gradlew --no-daemon build
\`\`\`

The build writes the mod jar to \`build/libs/$modid-<version>.jar\`.
The produced mod targets Java 8 for Minecraft 1.8.9.
EOF

if [ -z "${GITHUB_ACTIONS:-}" ]; then
  rm -f "$base/.github/workflows/init.yml"
fi
rm -f "$base/.github/scripts/smoke_template_generation.sh"
rmdir "$base/.github/scripts" 2>/dev/null || true
rm -f "$base/docs/DEVELOPMENT.md"
rm -f "$(readlink -f "$0")"

# Renaming changes line lengths, so google-java-format can wrap the renamed sources differently,
# and spotlessCheck fails the generated mod's build on any unformatted line.
if [ "$format_sources" = "true" ]; then
  printf 'Formatting the renamed sources with Spotless\n'
  (cd "$base" && ./gradlew --no-daemon --quiet spotlessApply)
fi

printf 'Initialization complete.\n'
