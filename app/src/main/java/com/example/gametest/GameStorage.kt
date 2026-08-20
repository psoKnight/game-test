package com.example.gametest

import android.content.Context

private const val PREFS_NAME = "air_battle"
private const val KEY_HIGH_SCORE = "high_score"
private const val KEY_UNLOCKED_CHAPTER = "unlocked_chapter"
private const val KEY_CLEARED_CHAPTER = "cleared_chapter"
private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
private const val KEY_SOUND_ENABLED = "sound_enabled"
private const val KEY_MUSIC_ENABLED = "music_enabled"
private const val KEY_SOUND_VOLUME = "sound_volume"
private const val KEY_MUSIC_VOLUME = "music_volume"
private const val KEY_QUALITY_LEVEL = "quality_level"

fun readHighScore(context: Context): Int {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_HIGH_SCORE, 0)
}

fun saveHighScore(context: Context, score: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_HIGH_SCORE, score)
        .apply()
}

fun readUnlockedChapter(context: Context): Int {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_UNLOCKED_CHAPTER, 1)
        .coerceAtLeast(1)
}

fun saveUnlockedChapter(context: Context, chapter: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_UNLOCKED_CHAPTER, chapter.coerceAtLeast(1))
        .apply()
}

fun readClearedChapter(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val migrated = (prefs.getInt(KEY_UNLOCKED_CHAPTER, 1) - 1).coerceAtLeast(0)
    return prefs.getInt(KEY_CLEARED_CHAPTER, migrated)
        .coerceIn(0, ChapterCatalog.FINAL_CHAPTER)
}

fun saveClearedChapter(context: Context, chapter: Int) {
    val cleared = chapter.coerceIn(0, ChapterCatalog.FINAL_CHAPTER)
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_CLEARED_CHAPTER, cleared)
        .apply()
}

fun readPlayerProgress(context: Context): PlayerProgress {
    return ChapterCatalog.progressForClearedChapter(readClearedChapter(context))
}

fun readVibrationEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_VIBRATION_ENABLED, true)
}

fun saveVibrationEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_VIBRATION_ENABLED, enabled)
        .apply()
}

fun readSoundEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SOUND_ENABLED, true)
}

fun saveSoundEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_SOUND_ENABLED, enabled)
        .apply()
}

fun readMusicEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_MUSIC_ENABLED, true)
}

fun saveMusicEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_MUSIC_ENABLED, enabled)
        .apply()
}

fun readSoundVolume(context: Context): Float {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_SOUND_VOLUME, 0.85f)
        .coerceIn(0f, 1f)
}

fun saveSoundVolume(context: Context, volume: Float) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KEY_SOUND_VOLUME, volume.coerceIn(0f, 1f))
        .apply()
}

fun readMusicVolume(context: Context): Float {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_MUSIC_VOLUME, 0.72f)
        .coerceIn(0f, 1f)
}

fun saveMusicVolume(context: Context, volume: Float) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KEY_MUSIC_VOLUME, volume.coerceIn(0f, 1f))
        .apply()
}

fun readQualityLevel(context: Context): QualityLevel {
    val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_QUALITY_LEVEL, QualityLevel.Standard.name)
    return QualityLevel.entries.firstOrNull { it.name == value } ?: QualityLevel.Standard
}

fun saveQualityLevel(context: Context, qualityLevel: QualityLevel) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_QUALITY_LEVEL, qualityLevel.name)
        .apply()
}
