# Add project specific ProGuard rules here.
# minifyEnabled is off by default for the release build (see app/build.gradle),
# so these rules are inactive unless you turn minification back on.

-keep class com.mfsrelay.app.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
