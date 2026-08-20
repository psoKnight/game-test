package com.example.gametest

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

object GameConfig {
    const val PLAYER_RADIUS = 40f
    const val PLAYER_MAX_HP = 8
    const val PLAYER_INVINCIBLE_SECONDS = 3.0f

    fun modeName(mode: GameMode): String {
        return when (mode) {
            GameMode.Infinite -> "无限模式"
            GameMode.Chapter -> "章节模式"
            GameMode.BossRush -> "Boss连战"
        }
    }

    fun phaseName(game: GameState): String {
        val chapter = ChapterCatalog.activeSpec(game)
        if (game.mode == GameMode.Infinite) {
            val target = ChapterCatalog.infiniteRoundTarget(game.infiniteRound)
            val tier = ChapterCatalog.infiniteLoopTier(game.infiniteRound)
            val modifier = ChapterCatalog.infiniteModifierForRound(game.infiniteRound)
            val bossIndex = ChapterCatalog.infiniteChapterForRound(game.infiniteRound)
            val prefix = if (tier > 0) {
                "无限模式 第 ${game.infiniteRound} 轮 · 目标 $bossIndex/${ChapterCatalog.FINAL_CHAPTER} · 强化$tier · ${infiniteModifierName(modifier)}"
            } else {
                "无限模式 第 ${game.infiniteRound} 轮 · 目标 $bossIndex/${ChapterCatalog.FINAL_CHAPTER} · ${infiniteModifierName(modifier)}"
            }
            return when (game.phase) {
                ChapterPhase.Wave -> "$prefix  ${game.roundKills}/$target"
                ChapterPhase.BossWarning -> if (chapter.easterBoss) "$prefix  彩蛋信号 ${chapter.bossName}" else "$prefix  ${chapter.bossName} 来袭"
                ChapterPhase.Boss -> "$prefix  ${chapter.bossName}"
                ChapterPhase.Clear -> "$prefix  完成"
            }
        }
        return when (game.phase) {
            ChapterPhase.Wave -> "${chapter.name} 第 ${chapter.waveForKills(game.kills)}/${chapter.waveCount} 波  ${game.kills}/${chapter.requiredKills}"
            ChapterPhase.BossWarning -> if (game.mode == GameMode.BossRush) {
                "第 ${game.bossKills + 1}/${ChapterCatalog.FINAL_CHAPTER} 战  ${chapter.bossName}"
            } else if (chapter.easterBoss) {
                "彩蛋信号  ${chapter.bossName}"
            } else {
                "${chapter.bossName} 来袭"
            }
            ChapterPhase.Boss -> if (game.mode == GameMode.BossRush) "第 ${game.bossKills + 1}/${ChapterCatalog.FINAL_CHAPTER} 战  ${chapter.bossName}" else chapter.bossName
            ChapterPhase.Clear -> "完成"
        }
    }

    fun infiniteModifierName(modifier: InfiniteModifier): String {
        return when (modifier) {
            InfiniteModifier.None -> "常规战区"
            InfiniteModifier.Electromagnetic -> "电磁干扰"
            InfiniteModifier.DebrisField -> "残骸密集"
            InfiniteModifier.FireSuppression -> "火力压制"
            InfiniteModifier.SupplyGap -> "补给断层"
            InfiniteModifier.CoreSurge -> "核心暴走"
        }
    }

    fun dropChance(kind: EnemyKind): Float {
        return when (kind) {
            EnemyKind.Scout -> 0.08f
            EnemyKind.Hunter -> 0.12f
            EnemyKind.Waver -> 0.14f
            EnemyKind.Gunner -> 0.17f
            EnemyKind.Tank -> 0.26f
            EnemyKind.Boss -> 1f
        }
    }

    fun randomPowerUp(): PowerUpKind {
        return PowerUpKind.entries.random()
    }

    fun powerUpLabel(kind: PowerUpKind): String {
        return when (kind) {
            PowerUpKind.DoubleShot -> "双倍子弹"
            PowerUpKind.Laser -> "激光"
            PowerUpKind.Missile -> "导弹"
            PowerUpKind.Shield -> "护盾"
            PowerUpKind.Invincible -> "无敌"
            PowerUpKind.Heal -> "加血"
        }
    }

    fun powerUpColor(kind: PowerUpKind): Color {
        return when (kind) {
            PowerUpKind.DoubleShot -> Color(0xFFFFE66D)
            PowerUpKind.Laser -> Color(0xFF80FFDB)
            PowerUpKind.Missile -> Color(0xFFFF9F1C)
            PowerUpKind.Shield -> Color(0xFF3A86FF)
            PowerUpKind.Invincible -> Color(0xFFE0AAFF)
            PowerUpKind.Heal -> Color(0xFFFF5A5F)
        }
    }

    fun enemySpec(kind: EnemyKind): EnemySpec {
        return when (kind) {
            EnemyKind.Scout -> EnemySpec(radius = 30f, hp = 1, score = 10, speedScale = 0.90f)
            EnemyKind.Hunter -> EnemySpec(radius = 34f, hp = 2, score = 18, speedScale = 0.92f)
            EnemyKind.Waver -> EnemySpec(radius = 33f, hp = 2, score = 20, speedScale = 0.88f)
            EnemyKind.Gunner -> EnemySpec(radius = 38f, hp = 3, score = 28, speedScale = 0.82f)
            EnemyKind.Tank -> EnemySpec(radius = 49f, hp = 4, score = 45, speedScale = 0.62f)
            EnemyKind.Boss -> EnemySpec(radius = 106f, hp = 80, score = 1000, speedScale = 1f)
        }
    }

    fun randomEnemyKind(chapter: ChapterSpec): EnemyKind {
        return chapter.enemyPool.random()
    }
}

data class EnemySpec(
    val radius: Float,
    val hp: Int,
    val score: Int,
    val speedScale: Float,
)

val ChapterSpec.waveCount: Int
    get() = 3

fun ChapterSpec.waveForKills(kills: Int): Int {
    val waveSize = (requiredKills / waveCount).coerceAtLeast(1)
    return (kills / waveSize + 1).coerceIn(1, waveCount)
}

fun ChapterSpec.waveDifficulty(kills: Int): Float {
    return 1f + (waveForKills(kills) - 1) * 0.08f
}
