#!/usr/bin/env bash

function join_by {
   local d=${1-} f=${2-}
   if shift 2; then
     printf %s "$f" "${@/#/$d}"
   fi
}

if [[ $* == *--rebuild* ]] || [ ! -d "target" ]; then
  echo "Rebuilding the project..."
  mvn clean package
fi

cd target
module_jars=(lib/*)
module_path=$(join_by ":" ${module_jars[@]})
# Include the classes, along with all libs.
module_path="./classes:$module_path"
echo "Module path: $module_path"
echo "Running jpackage..."
jpackage \
  --name "Ace of Shades Launcher" \
  --app-version "1.0.0" \
  --description "Launcher app for Ace of Shades, a voxel-based first-person shooter." \
  --icon ../icon.png \
  --linux-shortcut \
  --linux-deb-maintainer "andrewlalisofficial@gmail.com" \
  --linux-menu-group "Game" \
  --linux-app-category "Game" \
  --module-path "$module_path" \
  --module aos2_launcher/nl.andrewl.aos2_launcher.Launcher \
  --add-modules jdk.crypto.cryptoki

