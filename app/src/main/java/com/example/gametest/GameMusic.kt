package com.example.gametest

import android.content.Context
import android.media.MediaPlayer

enum class MusicTrack {
    Menu,
    Chapter01,
    Chapter02,
    Chapter03,
    Chapter04,
    Chapter05,
    Chapter06,
    Chapter07,
    Chapter08,
    Chapter09,
    Chapter10,
    InfiniteDrive,
    InfiniteHazard,
    InfiniteDebris,
    InfiniteSupplyGap,
    CoreSurge,
    BossRush,
    Boss,
    FinalBoss,
}

fun chapterMusicTrack(chapter: Int): MusicTrack {
    return when (chapter.coerceIn(1, ChapterCatalog.FINAL_CHAPTER)) {
        1 -> MusicTrack.Chapter01
        2 -> MusicTrack.Chapter02
        3 -> MusicTrack.Chapter03
        4 -> MusicTrack.Chapter04
        5 -> MusicTrack.Chapter05
        6 -> MusicTrack.Chapter06
        7 -> MusicTrack.Chapter07
        8 -> MusicTrack.Chapter08
        9 -> MusicTrack.Chapter09
        10 -> MusicTrack.Chapter10
        11 -> MusicTrack.Chapter06
        12 -> MusicTrack.Chapter07
        13 -> MusicTrack.Chapter08
        14 -> MusicTrack.Chapter09
        15 -> MusicTrack.Chapter10
        16 -> MusicTrack.Chapter07
        17 -> MusicTrack.Chapter08
        18 -> MusicTrack.Chapter09
        19 -> MusicTrack.Chapter10
        else -> MusicTrack.Chapter10
    }
}

class GameMusic(private val context: Context) {
    private var enabled = true
    private var volumeScale = 0.72f
    private var currentTrack: MusicTrack? = null
    private var player: MediaPlayer? = null

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            currentTrack?.let { play(it) }
        } else {
            player?.pause()
        }
    }

    fun play(track: MusicTrack) {
        if (currentTrack == track && player != null) {
            if (enabled && player?.isPlaying == false) player?.start()
            return
        }

        player?.release()
        currentTrack = track
        player = MediaPlayer.create(context, track.rawRes).apply {
            isLooping = true
            setVolume(track.volume * volumeScale, track.volume * volumeScale)
            if (enabled) start()
        }
    }

    fun setVolume(volume: Float) {
        volumeScale = volume.coerceIn(0f, 1f)
        currentTrack?.let { track ->
            player?.setVolume(track.volume * volumeScale, track.volume * volumeScale)
        }
    }

    fun pauseForLifecycle() {
        player?.pause()
    }

    fun resumeFromLifecycle() {
        if (enabled && player?.isPlaying == false) {
            player?.start()
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}

private val MusicTrack.rawRes: Int
    get() = when (this) {
        MusicTrack.Menu -> R.raw.bgm_menu
        MusicTrack.Chapter01 -> R.raw.bgm_chapter_01
        MusicTrack.Chapter02 -> R.raw.bgm_chapter_02
        MusicTrack.Chapter03 -> R.raw.bgm_chapter_03
        MusicTrack.Chapter04 -> R.raw.bgm_chapter_04
        MusicTrack.Chapter05 -> R.raw.bgm_chapter_05
        MusicTrack.Chapter06 -> R.raw.bgm_chapter_06
        MusicTrack.Chapter07 -> R.raw.bgm_chapter_07
        MusicTrack.Chapter08 -> R.raw.bgm_chapter_08
        MusicTrack.Chapter09 -> R.raw.bgm_chapter_09
        MusicTrack.Chapter10 -> R.raw.bgm_chapter_10
        MusicTrack.InfiniteDrive -> R.raw.bgm_infinite_drive
        MusicTrack.InfiniteHazard -> R.raw.bgm_infinite_hazard
        MusicTrack.InfiniteDebris -> R.raw.bgm_infinite_debris
        MusicTrack.InfiniteSupplyGap -> R.raw.bgm_supply_gap
        MusicTrack.CoreSurge -> R.raw.bgm_core_surge
        MusicTrack.BossRush -> R.raw.bgm_bossrush
        MusicTrack.Boss -> R.raw.bgm_boss
        MusicTrack.FinalBoss -> R.raw.bgm_final_boss
    }

private val MusicTrack.volume: Float
    get() = when (this) {
        MusicTrack.Menu -> 0.24f
        MusicTrack.Chapter01,
        MusicTrack.Chapter02,
        MusicTrack.Chapter03,
        MusicTrack.Chapter04,
        MusicTrack.Chapter05,
        MusicTrack.Chapter06,
        MusicTrack.Chapter07,
        MusicTrack.Chapter08,
        MusicTrack.Chapter09,
        MusicTrack.Chapter10 -> 0.34f
        MusicTrack.InfiniteDrive,
        MusicTrack.InfiniteDebris,
        MusicTrack.InfiniteSupplyGap -> 0.36f
        MusicTrack.InfiniteHazard,
        MusicTrack.CoreSurge,
        MusicTrack.BossRush -> 0.40f
        MusicTrack.Boss -> 0.40f
        MusicTrack.FinalBoss -> 0.44f
    }
