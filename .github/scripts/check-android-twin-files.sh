#!/usr/bin/env bash
# Checks that the twin files of the Android applications are still identical (see "Android Application Architecture"
# in README.md): each application is standalone, the duplication of some technical files between the applications
# being assumed. The files listed below must stay identical, apart from the license header and the package name of
# the application; a change must be reported in each application.
#
# Compatible with bash 3.2 (default shell of the macOS runners): no mapfile, no arrays.
#
# Usage: check-android-twin-files.sh (from the root of the repository)
set -euo pipefail

# Kotlin source root of each application
app_root() {
  case "$1" in
    control) echo "src/control/app/src/main/kotlin/org/calypsonet/keyple/demo/control" ;;
    validation) echo "src/validation/app/src/main/kotlin/org/calypsonet/keyple/demo/validation" ;;
    reload) echo "src/reloading-remote/client/keyple-mobile-android/app/src/main/kotlin/org/calypsonet/keyple/demo/reload/remote" ;;
    *) echo "Unknown application: $1" >&2; exit 2 ;;
  esac
}

# Content of a file without its license header, with a neutral package name and without carriage returns
normalize() {
  tr -d '\r' < "$1" \
    | awk 'header_done { print; next } /^ \*+ \*\/$/ { header_done = 1 }' \
    | sed -E 's/org\.calypsonet\.keyple\.demo\.(control|validation|reload\.remote)/org.calypsonet.keyple.demo.APP/g'
}

violations=0
checked=0

# Twin files: path relative to the source root, followed by the applications sharing it
while read -r path apps; do
  [[ -z "$path" || "$path" == \#* ]] && continue
  reference_app=""
  for app in $apps; do
    file="$(app_root "$app")/$path"
    if [[ ! -f "$file" ]]; then
      echo "::error::Twin file not found: $file"
      violations=$((violations + 1))
      continue
    fi
    if [[ -z "$reference_app" ]]; then
      reference_app="$app"
      reference_file="$file"
      continue
    fi
    if ! diff -u <(normalize "$reference_file") <(normalize "$file") > /dev/null; then
      echo "::error::Twin files differ: $reference_file and $file"
      diff -u <(normalize "$reference_file") <(normalize "$file") || true
      violations=$((violations + 1))
    fi
  done
  checked=$((checked + 1))
done <<'EOF'
# Shared by the three applications
DemoApplication.kt                                      control validation reload
data/DemoMifareClassicKeyProvider.kt                    control validation reload
data/JavaTimeClock.kt                                   control validation reload
data/TimberLogger.kt                                    control validation reload
di/ClockModule.kt                                       control validation reload
di/KeypopApiModule.kt                                   control validation reload
di/LoggerModule.kt                                      control validation reload
domain/spi/Clock.kt                                     control validation reload
domain/spi/Logger.kt                                    control validation reload
domain/spi/UiContext.kt                                 control validation reload
ui/ActivityUiContext.kt                                 control validation reload
# Shared by the control and validation applications
domain/model/CardProtocol.kt                            control validation
domain/model/SelectionError.kt                          control validation
ui/activities/deviceselection/PermissionDeniedDialog.kt control validation
ui/activities/deviceselection/PermissionHelper.kt       control validation
EOF

if [[ $violations -gt 0 ]]; then
  echo "$violations twin file difference(s) found"
  exit 1
fi
echo "Twin files checked: $checked groups, no difference"
