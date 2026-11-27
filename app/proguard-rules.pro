# DakhloKharj keeps Room + kotlinx.serialization models via generated code.
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
