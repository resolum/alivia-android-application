# Release rules on top of proguard-android-optimize.txt.

# Gson builds the backend DTOs by reflection: keep their names, fields and constructors.
-keep class com.resolum.alivia.features.**.data.datasources.remote.models.** { *; }
# DTOs declared next to their Retrofit service (for example the push device registration): Gson reads and writes
# their field names, so they must not be renamed either.
-keep class com.resolum.alivia.features.**Dto { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit reads the generic signatures and annotations of the service interfaces.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation interface com.resolum.alivia.features.**.data.datasources.remote.services.** { *; }

# Gson TypeToken generic types (R8 full mode).
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Planned agenda reminders are stored as JSON with Gson so they can be rescheduled after a reboot.
-keep class com.resolum.alivia.features.nursing.reminders.Reminder { *; }
-keep class com.resolum.alivia.features.nursing.reminders.ReminderKind { *; }
