#!/usr/bin/env bash
# Checks the layering rules of the Android applications (see "Android Application Architecture" in README.md):
# - the domain layer depends neither on Android, Timber or the dependency injection framework, nor on the data, ui
#   and di layers of the application;
# - the data layer logs through the Logger port: Timber is only used by its implementation (LoggerImpl).
#
# Usage: check-android-architecture.sh <project directory>
set -euo pipefail

project_dir="${1:?Usage: $0 <project directory>}"
violations=0

report() {
  local rule="$1" matches="$2"
  if [[ -n "$matches" ]]; then
    echo "::error::$rule"
    echo "$matches"
    violations=$((violations + 1))
  fi
}

mapfile -t domain_dirs < <(find "$project_dir" -type d -name domain -path '*/src/*' -not -path '*/build/*')
mapfile -t data_dirs < <(find "$project_dir" -type d -name data -path '*/src/*' -not -path '*/build/*')

for dir in "${domain_dirs[@]}"; do
  report "Domain layer must not depend on Android, Timber or dependency injection ($dir)" \
    "$(grep -rnE '^import (android|androidx|timber|dagger|javax\.inject)\.' "$dir" --include='*.kt' || true)"
  report "Domain layer must not depend on the data, ui or di layers ($dir)" \
    "$(grep -rnE '^import org\.calypsonet\.keyple\.demo\.[a-z.]+\.(data|ui|di)\.' "$dir" --include='*.kt' \
      | grep -v 'org\.calypsonet\.keyple\.demo\.common\.' || true)"
done

for dir in "${data_dirs[@]}"; do
  report "Data layer must log through the Logger port, Timber is reserved to LoggerImpl ($dir)" \
    "$(grep -rnE '^import timber\.' "$dir" --include='*.kt' | grep -v '/LoggerImpl\.kt:' || true)"
done

if [[ $violations -gt 0 ]]; then
  echo "$violations architecture rule violation(s) found in $project_dir"
  exit 1
fi
echo "Architecture rules checked in $project_dir: ${#domain_dirs[@]} domain and ${#data_dirs[@]} data directories, no violation"
