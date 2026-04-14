# Shizuku / AIDL
-keepclassmembers class * implements android.os.IInterface {
    public *;
}
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.** { *; }
-keep class com.hyperdeck.shizuku.ShellService { *; }
-keep class com.hyperdeck.shizuku.IShellService { *; }
-keep class com.hyperdeck.shizuku.IShellService$* { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hyperdeck.data.model.**$$serializer { *; }
-keepclassmembers class com.hyperdeck.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.hyperdeck.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-dontwarn androidx.compose.**
