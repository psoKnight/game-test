package com.example.gametest

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class GameScreen { Menu, ChapterSelect, Settings, Playing, Paused, GameOver, ChapterClear }
enum class GameMode { Infinite, Chapter, BossRush }
enum class EnemyKind { Scout, Hunter, Waver, Gunner, Tank, Boss }
enum class BulletOwner { Player, Enemy }
enum class BulletKind { Normal, Laser, Missile, Plasma }
enum class PowerUpKind { DoubleShot, Laser, Missile, Shield, Invincible, Heal }
enum class ChapterPhase { Wave, BossWarning, Boss, Clear }
enum class InfiniteModifier { None, Electromagnetic, DebrisField, FireSuppression, SupplyGap, CoreSurge }
enum class QualityLevel { Smooth, Standard, High }

data class Player(
    val x: Float = 0f,
    val y: Float = 0f,
    val radius: Float = 40f,
    val maxHp: Int = 8,
    val hp: Int = 8,
    val shield: Float = 0f,
    val invincible: Float = 3.0f,
    val doubleShot: Float = 0f,
    val laser: Float = 0f,
    val missile: Float = 0f,
    val ultimateEnergy: Float = 0f,
    val ultimateCooldown: Float = 0f,
    val ultimateActive: Float = 0f,
)

data class PlayerProgress(
    val clearedChapter: Int = 0,
    val maxHpBonus: Int = 0,
    val weaponLevel: Int = 1,
    val laserLevel: Int = 0,
    val missileLevel: Int = 0,
    val shieldLevel: Int = 0,
    val engineLevel: Int = 0,
    val title: String = "原型机",
)

data class ChapterReward(
    val chapter: Int,
    val title: String,
    val description: String,
    val maxHpBonus: Int = 0,
    val weaponLevelBonus: Int = 0,
    val laserLevelBonus: Int = 0,
    val missileLevelBonus: Int = 0,
    val shieldLevelBonus: Int = 0,
    val engineLevelBonus: Int = 0,
    val startDoubleShot: Float = 0f,
    val startShield: Float = 0f,
    val startInvincible: Float = 0f,
)

data class Bullet(
    val id: Long,
    val owner: BulletOwner,
    val kind: BulletKind,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val damage: Int,
    val ttl: Float = 4f,
    val trail: List<Offset> = emptyList(),
)

data class Enemy(
    val id: Long,
    val kind: EnemyKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    val hp: Int,
    val maxHp: Int,
    val speed: Float,
    val score: Int,
    val chapterId: Int = 1,
    val bossName: String = "",
    val finalBoss: Boolean = false,
    val age: Float = 0f,
    val phase: Float = Random.nextFloat() * 6.28f,
    val shootCooldown: Float = Random.nextFloat() * 1.2f + 0.6f,
    val skillCooldown: Float = 3.2f,
    val ultimateMarks: Int = 0,
    val vulnerable: Float = 0f,
    val modifier: InfiniteModifier = InfiniteModifier.None,
)

data class PowerUp(
    val id: Long,
    val kind: PowerUpKind,
    val x: Float,
    val y: Float,
    val vy: Float = 210f,
    val age: Float = 0f,
)

data class Particle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    val age: Float = 0f,
    val maxAge: Float = 0.75f,
)

data class Explosion(
    val id: Long,
    val x: Float,
    val y: Float,
    val age: Float = 0f,
    val maxAge: Float = 0.55f,
    val large: Boolean = false,
)

data class Star(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float,
    val layer: Int,
)

data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val age: Float = 0f,
    val color: Color = Color.White,
)

data class UltimateBlast(
    val id: Long,
    val x: Float,
    val y: Float,
    val radius: Float,
    val age: Float = 0f,
    val maxAge: Float = 0.65f,
    val playerOwned: Boolean = true,
)

data class GameState(
    val screen: GameScreen = GameScreen.Menu,
    val mode: GameMode = GameMode.Infinite,
    val phase: ChapterPhase = ChapterPhase.Wave,
    val chapter: Int = 1,
    val player: Player = Player(),
    val bullets: List<Bullet> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val powerUps: List<PowerUp> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val explosions: List<Explosion> = emptyList(),
    val stars: List<Star> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val ultimateBlasts: List<UltimateBlast> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val unlockedChapter: Int = 1,
    val playerProgress: PlayerProgress = PlayerProgress(),
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val soundVolume: Float = 0.85f,
    val musicVolume: Float = 0.72f,
    val qualityLevel: QualityLevel = QualityLevel.Standard,
    val kills: Int = 0,
    val bossKills: Int = 0,
    val infiniteRound: Int = 1,
    val roundKills: Int = 0,
    val elapsed: Float = 0f,
    val shootCooldown: Float = 0f,
    val spawnCooldown: Float = 0.75f,
    val bossWarning: Float = 0f,
    val shake: Float = 0f,
    val killsSincePowerDrop: Int = 0,
    val lowHpTimer: Float = 0f,
    val reviveUsed: Boolean = false,
    val ultimateRequested: Boolean = false,
    val bossUltimateWarning: Float = 0f,
    val bossUltimateText: String = "",
    val idSeed: Long = 0L,
)
