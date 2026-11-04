# Release rules on top of proguard-android-optimize.txt.

# Gson builds the backend DTOs by reflection: keep their names, fields and constructors.
-keep class com.resolum.alivia.features.**.data.datasources.remote.models.** { *; }
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
