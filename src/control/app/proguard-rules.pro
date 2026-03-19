# Ensures that low-level SLF4J debug and trace logging is disabled in release builds.
# Any call to logger.isDebugEnabled() or logger.isTraceEnabled() will return false,
# allowing R8/ProGuard to remove guarded debug/trace blocks and avoid runtime overhead.
# This also applies to SLF4J calls made directly by third-party libraries used in the project,
# such as Keyple.
-assumenosideeffects interface org.slf4j.Logger {
    public boolean isTraceEnabled() return false;
    public boolean isDebugEnabled() return false;
}

# Keep demo app classes

# Keep Keyple library classes
-keep class org.calypsonet.keyple.plugin.coppernic.ParagonReader { *; }

# Keep Coppernic SDK classes
-keep public class fr.coppernic.sdk.** { *; }

# Keep Bluebird NFC library classes
-keep class com.bluebird.extnfc.** { *; }
-keep interface com.bluebird.extnfc.** { *; }

# Keep Bluebird SAM library classes
-keep class com.bluebird.payment.sam.** { *; }
-keep interface com.bluebird.payment.sam.** { *; }

# Keep Keyple storage card internal classes
-keep class org.eclipse.keyple.core.plugin.storagecard.internal.** { *; }

# Keep Keyple plugin Bluebird classes
-keep class org.calypsonet.keyple.plugin.bluebird.** { *; }
-keep interface org.calypsonet.keyple.plugin.bluebird.** { *; }

# Keep Keyple storage card classes
-keep class org.calypsonet.keyple.card.storagecard.** { *; }
-keep interface org.calypsonet.keyple.card.storagecard.** { *; }
-keep class org.calypsonet.keyple.plugin.storagecard.** { *; }
-keep interface org.calypsonet.keyple.plugin.storagecard.** { *; }

# Suppress warnings for missing Bluebird SDK classes (when using mock libraries)
-dontwarn com.bluebird.extnfc.ExtNfcReader$ECP
-dontwarn com.bluebird.extnfc.ExtNfcReader$TransmitResult
-dontwarn com.bluebird.extnfc.ExtNfcReader
-dontwarn com.bluebird.payment.sam.SamInterface
-dontwarn org.eclipse.keyple.core.plugin.storagecard.internal.KeyStorageType

-dontwarn javax.annotation.Nullable