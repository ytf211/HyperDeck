# Shizuku / AIDL
-keepclassmembers class * implements android.os.IInterface {
    public *;
}
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.** { *; }
-keep class com.hyperdeck.shizuku.ShellService { *; }
-keep class com.hyperdeck.shizuku.IShellService { *; }
-keep class com.hyperdeck.shizuku.IShellService$* { *; }

# Application
-keep class com.hyperdeck.HyperDeckApp { *; }

# Navigation routes (@Serializable)
-keep class com.hyperdeck.navigation.* { *; }

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

# Keep all @Serializable classes
-if @kotlinx.serialization.Serializable class **
-keep class <1> { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# DataStore
-keep class androidx.datastore.** { *; }

# Compose
-dontwarn androidx.compose.**
