#!/usr/bin/env bash

set -euo pipefail

print_help() {
  printf 'Usage: %s [--skip-format] <new package name> <project class name> <modid> <owner>\n' "$0"
}

format_sources="true"
positionals=()
for argument in "$@"; do
  case "$argument" in
    --skip-format) format_sources="false" ;;
    *) positionals+=("$argument") ;;
  esac
done

if [ "${#positionals[@]}" -ne 4 ]; then
  print_help >&2
  exit 64
fi

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

find "$base/src/main" -type f -exec sed -i \
  -e "s|com\.example|$package_name|g" \
  -e "s|examplemod|$modid|g" \
  -e "s|ExampleMod|$project_name|g" {} +

sed -i \
  -e "s|com\.example|$package_name|g" \
  -e "s|examplemod|$modid|g" \
  -e "s|ExampleMod|$project_name|g" \
  -e "s|brainage04|$owner|g" "$base/gradle.properties"
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
