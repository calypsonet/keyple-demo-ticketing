# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.old.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Ensures that low-level SLF4J debug and trace logging is disabled in release builds.
# Any call to logger.isDebugEnabled() or logger.isTraceEnabled() will return false,
# allowing R8/ProGuard to remove guarded debug/trace blocks and avoid runtime overhead.
# This also applies to SLF4J calls made directly by third-party libraries used in the project,
# such as Keyple.
-assumenosideeffects interface org.slf4j.Logger {
    public boolean isTraceEnabled() return false;
    public boolean isDebugEnabled() return false;
}

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.-KotlinExtensions

# Keep Bluebird NFC library classes
-keep class com.bluebird.extnfc.** { *; }
-keep interface com.bluebird.extnfc.** { *; }

# Keep Bluebird SAM library classes
-keep class com.bluebird.payment.sam.** { *; }
-keep interface com.bluebird.payment.sam.** { *; }

# Keep Keyple core service internal DTOs used by Gson reflection in the distributed API.
# R8 optimization can make these abstract or remove constructors, breaking deserialization.
-keep class org.eclipse.keyple.core.service.InternalDto { *; }
-keep class org.eclipse.keyple.core.service.InternalDto$** { *; }

# Keep common demo DTOs and model classes serialized/deserialized by Gson over the remote service.
-keep class org.calypsonet.keyple.demo.common.dto.** { *; }
-keep class org.calypsonet.keyple.demo.common.model.** { *; }

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

# Suppress warnings for missing OMAPI classes (system library present only on compatible devices)
-dontwarn org.simalliance.openmobileapi.**