# OneUI Clock ProGuard Rules

# Room Database
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.sec.oneui.clock.alarm.data.** { *; }
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier$Element {
    <init>(...);
}
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Serialization / Data classes reflection safety
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Material Icons Extended
-dontwarn androidx.compose.material.icons.**
