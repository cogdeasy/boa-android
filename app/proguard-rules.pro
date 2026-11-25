# Retrofit / Gson models are reflected over at runtime.
-keep class com.bofa.mobile.model.** { *; }
-keepattributes Signature, *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
