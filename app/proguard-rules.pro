# ProGuard/R8 rules for the release build.
# Minification is currently disabled (see app/build.gradle), so this file is a
# placeholder. If it is ever enabled, Firestore needs its model classes kept so
# that automatic POJO (de)serialisation keeps working:
#
# -keepclassmembers class com.skaflik.model.** { *; }
