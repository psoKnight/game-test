package com.example.gametest

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class GameAudio(context: Context) {
    private var masterVolume = 0.85f

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val shoot = soundPool.load(context, R.raw.sfx_shoot, 1)
    private val explosion = soundPool.load(context, R.raw.sfx_explosion, 1)
    private val powerUp = soundPool.load(context, R.raw.sfx_powerup, 1)
    private val hit = soundPool.load(context, R.raw.sfx_hit, 1)
    private val bossWarning = soundPool.load(context, R.raw.sfx_boss_warning, 1)
    private val menuBoot = soundPool.load(context, R.raw.sfx_menu_boot, 1)

    fun playShoot() = play(shoot, volume = 0.32f)
    fun playExplosion(large: Boolean) = play(explosion, volume = if (large) 0.82f else 0.58f, rate = if (large) 0.85f else 1.08f)
    fun playPowerUp() = play(powerUp, volume = 0.62f)
    fun playHit() = play(hit, volume = 0.68f)
    fun playBossWarning() = play(bossWarning, volume = 0.75f)
    fun playMenuBoot() = play(menuBoot, volume = 0.46f)

    fun setVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        soundPool.release()
    }

    private fun play(soundId: Int, volume: Float, rate: Float = 1f) {
        if (soundId == 0) return
        val finalVolume = (volume * masterVolume).coerceIn(0f, 1f)
        soundPool.play(soundId, finalVolume, finalVolume, 1, 0, rate)
    }
}
