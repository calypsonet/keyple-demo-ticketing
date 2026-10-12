#!/usr/bin/env bash
# Checks the layering rules of the Android applications (see "Android Application Architecture" in README.md):
# - the domain layer, meant to be transposable to another language, only imports the Kotlin standard library, the
#   java.time date types, the Keypop APIs, the Keyple utilities, the common library and its own packages (allowlist):
#   no Android, Timber, dependency injection, coroutines or JVM-specific API, and no data, ui or di layer;
# - the domain layer is synchronous: no suspend function;
# - the data layer logs through the Logger port: Timber is only used by its implementation (TimberLogger);
# - the ui layer neither accesses the data layer nor handles the Keypop card types.
#
# Compatible with bash 3.2 (default shell of the macOS runners): no mapfile, no arrays.
#
# Usage: check-android-architecture.sh <project directory>
set -euo pipefail

project_dir="${1:?Usage: $0 <project directory>}"
violations=0
domain_count=0
data_count=0
ui_count=0

report() {
  local rule="$1" matches="$2"
  if [[ -n "$matches" ]]; then
    echo "::error::$rule"
    echo "$matches"
    violations=$((violations + 1))
  fi
}

while IFS= read -r dir; do
  domain_count=$((domain_count + 1))
  report "Domain layer may only depend on the Kotlin standard library, java.time, the Keypop APIs, the Keyple utilities, the common library and itself ($dir)" \
    "$(grep -rnE --include='*.kt' '^import ' "$dir" \
      | grep -vE ':import (kotlin\.|java\.time\.|org\.eclipse\.keypop\.|org\.eclipse\.keyple\.core\.util\.|org\.calypsonet\.keyple\.demo\.common\.|org\.calypsonet\.keyple\.demo\.[a-z.]+\.domain\.)' || true)"
  report "Domain layer must be synchronous: no suspend function, the threads being chosen by the UI ($dir)" \
    "$(grep -rnE --include='*.kt' '(^|[^[:alnum:]_])suspend (fun|\()' "$dir" \
      | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' || true)"
done < <(find "$project_dir" -type d -name domain -path '*/src/*' -not -path '*/build/*')

while IFS= read -r dir; do
  data_count=$((data_count + 1))
  report "Data layer must log through the Logger port, Timber is reserved to TimberLogger ($dir)" \
    "$(grep -rnE --include='*.kt' '^import timber\.' "$dir" | grep -v '/TimberLogger\.kt:' || true)"
done < <(find "$project_dir" -type d -name data -path '*/src/*' -not -path '*/build/*')

while IFS= read -r dir; do
  ui_count=$((ui_count + 1))
  report "UI layer must not access the data layer ($dir)" \
    "$(grep -rnE --include='*.kt' '^import org\.calypsonet\.keyple\.demo\.[a-z.]+\.data\.' "$dir" \
      | grep -v 'org\.calypsonet\.keyple\.demo\.common\.' || true)"
  report "UI layer must not handle the Keypop card types ($dir)" \
    "$(grep -rnE --include='*.kt' '^import org\.eclipse\.keypop\.(calypso|storagecard)\.' "$dir" || true)"
done < <(find "$project_dir" -type d -name ui -path '*/src/*' -not -path '*/build/*')

if [[ $violations -gt 0 ]]; then
  echo "$violations architecture rule violation(s) found in $project_dir"
  exit 1
fi
echo "Architecture rules checked in $project_dir: $domain_count domain, $data_count data and $ui_count ui directories, no violation"
