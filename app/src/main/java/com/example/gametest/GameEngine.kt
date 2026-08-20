package com.example.gametest

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val PLAYER_ULTIMATE_MAX = 100f

fun createInitialState(
    width: Float,
    height: Float,
    mode: GameMode,
    chapter: Int,
    highScore: Int,
    unlockedChapter: Int,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    soundVolume: Float,
    musicVolume: Float,
    qualityLevel: QualityLevel,
    playerProgress: PlayerProgress,
): GameState {
    val phase = when (mode) {
        GameMode.BossRush -> ChapterPhase.BossWarning
        else -> ChapterPhase.Wave
    }
    val startBonuses = ChapterCatalog.rewardsUpTo(playerProgress.clearedChapter)
    val maxHp = GameConfig.PLAYER_MAX_HP + playerProgress.maxHpBonus
    val chapterStartShield = when {
        chapter >= 10 -> 8f
        chapter >= 7 -> 6f
        chapter >= 4 -> 3f
        else -> 0f
    }
    val chapterStartInvincible = when {
        chapter >= 10 -> 4f
        chapter >= 4 -> 3f
        else -> GameConfig.PLAYER_INVINCIBLE_SECONDS
    }
    return GameState(
        screen = GameScreen.Playing,
        mode = mode,
        phase = phase,
        chapter = chapter,
        highScore = highScore,
        unlockedChapter = unlockedChapter,
        vibrationEnabled = vibrationEnabled,
        soundEnabled = soundEnabled,
        musicEnabled = musicEnabled,
        soundVolume = soundVolume,
        musicVolume = musicVolume,
        qualityLevel = qualityLevel,
        playerProgress = playerProgress,
        player = Player(
            x = width / 2f,
            y = height - 96f,
            maxHp = maxHp,
            hp = maxHp,
            shield = max(chapterStartShield, startBonuses.maxOfOrNull { it.startShield } ?: 0f),
            invincible = max(chapterStartInvincible, startBonuses.maxOfOrNull { it.startInvincible } ?: 0f),
            doubleShot = startBonuses.maxOfOrNull { it.startDoubleShot } ?: 0f,
        ),
        stars = createStars(width, height),
        bossWarning = if (mode == GameMode.BossRush) 2.4f else 0f,
        spawnCooldown = if (mode == GameMode.BossRush) 999f else 0.35f,
    )
}

fun playStateAudio(audio: GameAudio, before: GameState, next: GameState) {
    val beforePlayerShots = before.bullets.count { it.owner == BulletOwner.Player }
    val nextPlayerShots = next.bullets.count { it.owner == BulletOwner.Player }
    if (next.screen == GameScreen.Playing && nextPlayerShots > beforePlayerShots) {
        audio.playShoot()
    }

    val beforeExplosionIds = before.explosions.map { it.id }.toSet()
    val newExplosions = next.explosions.filter { it.id !in beforeExplosionIds }
    if (newExplosions.isNotEmpty()) {
        audio.playExplosion(large = newExplosions.any { it.large })
    }

    if (next.player.hp < before.player.hp) {
        audio.playHit()
    }

    val collectedPowerUp = next.floatingTexts.any { text ->
        text.age <= 0.04f && PowerUpKind.entries.any { GameConfig.powerUpLabel(it) == text.text }
    }
    if (collectedPowerUp) {
        audio.playPowerUp()
    }

    if (next.bossWarning > 0f && before.bossWarning <= 0f) {
        audio.playBossWarning()
    }

    if (next.bossUltimateWarning > 0f && before.bossUltimateWarning <= 0f) {
        audio.playBossWarning()
    }

    val beforeBlastIds = before.ultimateBlasts.map { it.id }.toSet()
    val newBlasts = next.ultimateBlasts.filter { it.id !in beforeBlastIds }
    if (newBlasts.isNotEmpty()) {
        audio.playExplosion(large = true)
    }
}

fun GameState.musicTrack(): MusicTrack {
    if (screen == GameScreen.Menu || screen == GameScreen.ChapterSelect || screen == GameScreen.Settings) return MusicTrack.Menu
    val boss = enemies.firstOrNull { it.kind == EnemyKind.Boss }
    if (boss?.finalBoss == true) return MusicTrack.FinalBoss
    if (mode == GameMode.BossRush && (phase == ChapterPhase.Boss || phase == ChapterPhase.BossWarning || boss != null)) return MusicTrack.BossRush
    if (mode == GameMode.Infinite) {
        val modifier = ChapterCatalog.infiniteModifierForRound(infiniteRound)
        if (modifier == InfiniteModifier.CoreSurge || phase == ChapterPhase.Boss || phase == ChapterPhase.BossWarning || boss != null) {
            return when (modifier) {
                InfiniteModifier.CoreSurge -> MusicTrack.CoreSurge
                InfiniteModifier.Electromagnetic,
                InfiniteModifier.FireSuppression -> MusicTrack.InfiniteHazard
                InfiniteModifier.DebrisField -> MusicTrack.InfiniteDebris
                InfiniteModifier.SupplyGap -> MusicTrack.InfiniteSupplyGap
                InfiniteModifier.None -> MusicTrack.Boss
            }
        }
        return when (modifier) {
            InfiniteModifier.Electromagnetic,
            InfiniteModifier.FireSuppression -> MusicTrack.InfiniteHazard
            InfiniteModifier.DebrisField -> MusicTrack.InfiniteDebris
            InfiniteModifier.SupplyGap -> MusicTrack.InfiniteSupplyGap
            InfiniteModifier.CoreSurge -> MusicTrack.CoreSurge
            InfiniteModifier.None -> MusicTrack.InfiniteDrive
        }
    }
    if (phase == ChapterPhase.Boss || phase == ChapterPhase.BossWarning || boss != null) return MusicTrack.Boss
    return when (screen) {
        GameScreen.Playing, GameScreen.Paused -> chapterMusicTrack(ChapterCatalog.activeSpec(this).id)
        GameScreen.GameOver, GameScreen.ChapterClear -> MusicTrack.Menu
        GameScreen.Menu, GameScreen.ChapterSelect, GameScreen.Settings -> MusicTrack.Menu
    }
}

fun createStars(width: Float, height: Float): List<Star> {
    return List(110) {
        val layer = Random.nextInt(3)
        Star(
            x = Random.nextFloat() * max(width, 1f),
            y = Random.nextFloat() * max(height, 1f),
            speed = 70f + layer * 95f + Random.nextFloat() * 80f,
            size = 1.2f + layer * 1.1f + Random.nextFloat() * 1.6f,
            alpha = 0.2f + layer * 0.15f + Random.nextFloat() * 0.25f,
            layer = layer,
        )
    }
}

fun GameState.movePlayer(offset: Offset, size: IntSize): GameState {
    if (size.width <= 0 || size.height <= 0) return this
    val x = offset.x.coerceIn(player.radius + 8f, size.width - player.radius - 8f)
    val y = offset.y.coerceIn(size.height * 0.22f, size.height - player.radius - 18f)
    return copy(player = player.copy(x = x, y = y))
}

fun GameState.requestUltimate(): GameState {
    if (screen != GameScreen.Playing) return this
    if (player.ultimateEnergy < PLAYER_ULTIMATE_MAX || player.ultimateCooldown > 0f) return this
    return copy(ultimateRequested = true)
}

fun GameState.step(dt: Float, width: Float, height: Float, topSafe: Float): GameState {
    var ids = idSeed
    var nextScore = score
    var nextKills = kills
    var nextBossKills = bossKills
    var nextInfiniteRound = infiniteRound
    var nextRoundKills = roundKills
    var nextPhase = phase
    var nextScreen = screen
    var nextShake = max(0f, shake - dt * 18f)
    var nextKillsSincePowerDrop = killsSincePowerDrop
    var nextLowHpTimer = if (player.hp <= 2) lowHpTimer + dt else 0f
    var nextReviveUsed = reviveUsed
    val chapterSpec = ChapterCatalog.activeSpec(this)
    val infiniteModifier = if (mode == GameMode.Infinite) ChapterCatalog.infiniteModifierForRound(nextInfiniteRound) else InfiniteModifier.None
    val waveKills = if (mode == GameMode.Infinite) nextRoundKills else nextKills
    val waveDifficulty = chapterSpec.waveDifficulty(waveKills)
    val difficulty = difficulty() * chapterSpec.enemySpeedScale * waveDifficulty * modifierDifficultyScale(infiniteModifier)
    val nextElapsed = elapsed + dt
    val trailLimit = qualityLevel.trailLimit()
    val nextStars = stars.map { star ->
        val y = star.y + star.speed * dt
        if (y > height + 12f) star.copy(x = Random.nextFloat() * width, y = -12f) else star.copy(y = y)
    }

    var nextPlayer = player.tick(dt)
    var nextBossWarning = max(0f, bossWarning - dt)
    var nextBossUltimateWarning = max(0f, bossUltimateWarning - dt)
    var nextBossUltimateText = if (nextBossUltimateWarning > 0f) bossUltimateText else ""
    var nextSpawnCooldown = spawnCooldown - dt

    val spawnedEnemies = mutableListOf<Enemy>()
    val spawnedPowerUps = mutableListOf<PowerUp>()
    if (mode == GameMode.Chapter && nextPhase == ChapterPhase.Wave && nextKills >= chapterSpec.requiredKills) {
        nextPhase = ChapterPhase.BossWarning
        nextBossWarning = 2.8f
        nextSpawnCooldown = 999f
    }
    if (mode == GameMode.BossRush && nextPhase == ChapterPhase.BossWarning && nextBossWarning <= 0f && enemies.none { it.kind == EnemyKind.Boss }) {
        val bossRushSpec = ChapterCatalog.spec((nextBossKills + 1).coerceIn(1, ChapterCatalog.FINAL_CHAPTER))
        spawnedEnemies += createBoss(++ids, width, topSafe, difficulty + 0.35f, bossRushSpec)
        maybeBossSupply(nextPlayer)?.let { spawnedPowerUps += PowerUp(++ids, it, width * 0.5f, topSafe + 130f, vy = 150f) }
        nextPhase = ChapterPhase.Boss
        nextSpawnCooldown = bossRushEscortDelay(nextBossKills)
        nextShake = max(nextShake, 8f)
    }
    if (mode == GameMode.Chapter && nextPhase == ChapterPhase.BossWarning && nextBossWarning <= 0f && enemies.none { it.kind == EnemyKind.Boss }) {
        spawnedEnemies += createBoss(++ids, width, topSafe, difficulty, chapterSpec)
        maybeBossSupply(nextPlayer)?.let { spawnedPowerUps += PowerUp(++ids, it, width * 0.5f, topSafe + 130f, vy = 150f) }
        nextPhase = ChapterPhase.Boss
        nextShake = max(nextShake, 8f)
    }
    if (mode == GameMode.Infinite && nextPhase == ChapterPhase.Wave && nextRoundKills >= ChapterCatalog.infiniteRoundTarget(nextInfiniteRound) && enemies.none { it.kind == EnemyKind.Boss } && bossWarning <= 0f) {
        nextPhase = ChapterPhase.BossWarning
        nextBossWarning = 2.5f
        nextSpawnCooldown = 999f
    }
    if (mode == GameMode.Infinite && nextPhase == ChapterPhase.BossWarning && nextBossWarning <= 0f) {
        spawnedEnemies += createBoss(++ids, width, topSafe, difficulty, chapterSpec, ChapterCatalog.infiniteLoopTier(nextInfiniteRound), infiniteModifier)
        maybeBossSupply(nextPlayer)?.let { spawnedPowerUps += PowerUp(++ids, it, width * 0.5f, topSafe + 130f, vy = 150f) }
        nextPhase = ChapterPhase.Boss
    }
    if ((mode == GameMode.Infinite || mode == GameMode.Chapter) && nextPhase == ChapterPhase.Wave && nextSpawnCooldown <= 0f) {
        spawnedEnemies += createEnemy(++ids, width, topSafe, difficulty, chapterSpec, infiniteModifier)
        val supportChance = min(0.58f, 0.10f + elapsed / 110f + (chapterSpec.waveForKills(waveKills) - 1) * 0.12f + modifierSupportBonus(infiniteModifier))
        if (Random.nextFloat() < supportChance) {
            spawnedEnemies += createEnemy(++ids, width, topSafe - 72f, difficulty, chapterSpec, infiniteModifier)
        }
        nextSpawnCooldown = ((0.72f / difficulty) * chapterSpec.spawnScale * modifierSpawnScale(infiniteModifier) / waveDifficulty).coerceIn(0.13f, 0.78f)
    }
    if (mode == GameMode.BossRush && nextPhase == ChapterPhase.Boss && nextSpawnCooldown <= 0f && enemies.any { it.kind == EnemyKind.Boss }) {
        spawnedEnemies += createBossRushEscort(++ids, width, topSafe, difficulty, chapterSpec)
        val extraChance = (0.10f + nextBossKills * 0.025f).coerceAtMost(0.45f)
        if (Random.nextFloat() < extraChance) {
            spawnedEnemies += createBossRushEscort(++ids, width, topSafe - 72f, difficulty, chapterSpec)
        }
        nextSpawnCooldown = bossRushEscortDelay(nextBossKills)
    }

    val playerBullets = mutableListOf<Bullet>()
    val shootReady = shootCooldown <= 0f
    if (shootReady) {
        playerBullets += createPlayerBullets(nextPlayer, playerProgress, ++ids)
        ids += playerBullets.size - 1
    }
    if (nextPlayer.laser > 0f && shootReady) {
        val laserDamage = 3 + playerProgress.laserLevel
        val laserTtl = 0.24f + playerProgress.laserLevel * 0.04f
        playerBullets += Bullet(++ids, BulletOwner.Player, BulletKind.Laser, nextPlayer.x, nextPlayer.y - nextPlayer.radius - 100f, 0f, -1250f, 12f, laserDamage, ttl = laserTtl)
    }
    if (nextPlayer.missile > 0f && shootReady && enemies.isNotEmpty()) {
        val missiles = createPlayerMissiles(nextPlayer, playerProgress, ++ids)
        playerBullets += missiles
        ids += missiles.size - 1
    }

    val nextBullets = mutableListOf<Bullet>()
    (bullets + playerBullets).forEach { bullet ->
        val homed = if (bullet.kind == BulletKind.Missile && bullet.owner == BulletOwner.Player) {
            bullet.homeToward(enemies)
        } else bullet
        val trail = (listOf(Offset(homed.x, homed.y)) + homed.trail).take(trailLimit)
        val moved = homed.copy(
            x = homed.x + homed.vx * dt,
            y = homed.y + homed.vy * dt,
            ttl = homed.ttl - dt,
            trail = trail,
        )
        if (moved.ttl > 0f && moved.x > -80f && moved.x < width + 80f && moved.y > -160f && moved.y < height + 120f) {
            nextBullets += moved
        }
    }

    val enemyBullets = mutableListOf<Bullet>()
    val movedEnemies = mutableListOf<Enemy>()
    val earlyTexts = mutableListOf<FloatingText>()
    (enemies + spawnedEnemies).forEach { enemy ->
        val moved = enemy.move(dt, nextPlayer, width, height, difficulty)
        if (moved.kind != EnemyKind.Boss && moved.y - moved.radius > height + 20f) {
            if (nextPlayer.shield <= 0f && nextPlayer.invincible <= 0f) nextPlayer = nextPlayer.damage(1)
            nextShake = max(nextShake, 5f)
        } else {
            val shooting = moved.shootCooldown <= 0f
            val skilled = moved.kind == EnemyKind.Boss && moved.skillCooldown <= 0f
            if (shooting) {
                enemyBullets += moved.createEnemyShots(++ids, nextPlayer)
                ids += enemyBullets.size.coerceAtLeast(1) - 1
            }
            if (skilled) {
                val skillShots = moved.createBossSkill(++ids, nextPlayer)
                ids += skillShots.size.coerceAtLeast(1) - 1
                enemyBullets += skillShots
                earlyTexts += FloatingText(++ids, "破绽", moved.x, moved.y + moved.radius + 18f, color = Color(0xFFFFE66D))
                nextShake = max(nextShake, 4f)
            }
            movedEnemies += moved.copy(
                shootCooldown = if (shooting) moved.nextShotDelay(difficulty) else moved.shootCooldown - dt,
                skillCooldown = if (skilled) moved.nextBossSkillDelay() else moved.skillCooldown - dt,
                vulnerable = if (skilled) moved.bossWeakWindow() else moved.tickBossVulnerability(dt),
            )
        }
    }
    nextBullets += enemyBullets

    val remainingBullets = nextBullets.capBullets(qualityLevel).toMutableList()
    val remainingEnemies = mutableListOf<Enemy>()
    val newPowerUps = mutableListOf<PowerUp>()
    val newParticles = mutableListOf<Particle>()
    val newExplosions = mutableListOf<Explosion>()
    val newTexts = earlyTexts.toMutableList()
    val newUltimateBlasts = mutableListOf<UltimateBlast>()

    if (ultimateRequested && nextPlayer.ultimateEnergy >= PLAYER_ULTIMATE_MAX && nextPlayer.ultimateCooldown <= 0f) {
        nextPlayer = nextPlayer.copy(
            ultimateEnergy = 0f,
            ultimateCooldown = 8f,
            ultimateActive = 0.8f,
            invincible = max(nextPlayer.invincible, 0.8f),
        )
        remainingBullets.removeAll { it.owner == BulletOwner.Enemy }
        for (index in movedEnemies.indices) {
            val enemy = movedEnemies[index]
            val damage = if (enemy.kind == EnemyKind.Boss) {
                min((enemy.maxHp * 0.07f).toInt().coerceAtLeast(60), (enemy.maxHp * 0.04f).toInt() + 55)
            } else {
                999
            }
            movedEnemies[index] = enemy.copy(hp = enemy.hp - damage)
        }
        newUltimateBlasts += UltimateBlast(++ids, nextPlayer.x, nextPlayer.y - 160f, radius = min(width, height) * 0.72f, playerOwned = true)
        newTexts += FloatingText(++ids, "歼星轰击", nextPlayer.x, nextPlayer.y - 118f, color = Color(0xFF80FFDB))
        nextShake = max(nextShake, 14f)
    }

    movedEnemies.forEach { enemy ->
        var damaged = enemy
        var bulletIndex = 0
        while (bulletIndex < remainingBullets.size) {
            val bullet = remainingBullets[bulletIndex]
            if (bullet.owner == BulletOwner.Player && hit(bullet.x, bullet.y, bullet.radius, damaged.x, damaged.y, damaged.radius)) {
                if (bullet.kind != BulletKind.Laser) {
                    remainingBullets.removeAt(bulletIndex)
                } else {
                    bulletIndex += 1
                }
                val bossGuard = damaged.kind == EnemyKind.Boss && damaged.vulnerable < 0f
                val baseDamage = if (bossGuard) max(1, (bullet.damage * 0.45f).toInt()) else bullet.damage
                val bonusDamage = if (damaged.kind == EnemyKind.Boss && damaged.vulnerable > 0f) max(1, bullet.damage / 2) else 0
                damaged = damaged.copy(hp = damaged.hp - baseDamage - bonusDamage)
                if (damaged.kind == EnemyKind.Boss) {
                    nextPlayer = nextPlayer.addUltimateEnergy(0.8f + bonusDamage * 0.15f)
                }
                val particles = impactParticles(ids + 1, bullet.x, bullet.y, Color(0xFFFFE66D), qualityLevel)
                newParticles += particles
                ids += particles.size
            } else {
                bulletIndex += 1
            }
        }
        if (hit(nextPlayer.x, nextPlayer.y, nextPlayer.radius * 0.60f, damaged.x, damaged.y, damaged.radius * 0.82f)) {
            if (nextPlayer.shield > 0f) {
                nextPlayer = nextPlayer.copy(shield = 0f, invincible = max(nextPlayer.invincible, 0.8f))
            } else if (nextPlayer.invincible <= 0f) {
                nextPlayer = nextPlayer.damage(if (damaged.kind == EnemyKind.Boss) 2 else 1)
            }
            damaged = damaged.copy(hp = 0)
            nextShake = max(nextShake, 9f)
        }
        if (damaged.hp <= 0) {
            nextScore += damaged.score
            nextKills += if (damaged.kind == EnemyKind.Boss) 0 else 1
            if (mode == GameMode.Infinite && damaged.kind != EnemyKind.Boss) nextRoundKills += 1
            if (damaged.kind == EnemyKind.Boss) nextBossKills += 1
            nextPlayer = if (damaged.kind == EnemyKind.Boss) {
                nextPlayer.addUltimateEnergy(if (mode == GameMode.BossRush) 25f else 18f)
            } else {
                nextPlayer.addUltimateEnergy(6f)
            }
            newExplosions += Explosion(++ids, damaged.x, damaged.y, large = damaged.kind == EnemyKind.Boss)
            val particles = explosionParticles(ids + 1, damaged.x, damaged.y, damaged.kind == EnemyKind.Boss, qualityLevel)
            newParticles += particles
            ids += particles.size
            newTexts += FloatingText(++ids, "+${damaged.score}", damaged.x, damaged.y, color = Color(0xFFFFE66D))
            if (damaged.kind != EnemyKind.Boss) nextKillsSincePowerDrop += 1
            val lowHpBonus = if (nextPlayer.hp <= 2) 1.25f else 1f
            val pityLimit = if (nextPlayer.hp <= 2) 8 else 12
            val forcedDrop = damaged.kind == EnemyKind.Boss || nextKillsSincePowerDrop >= pityLimit || nextLowHpTimer >= 14f
            if (forcedDrop || Random.nextFloat() < GameConfig.dropChance(damaged.kind) * lowHpBonus * modifierDropScale(damaged.modifier)) {
                val powerKind = choosePowerUp(nextPlayer, playerProgress, nextPhase, damaged.kind == EnemyKind.Boss, nextLowHpTimer)
                newPowerUps += PowerUp(++ids, powerKind, damaged.x, damaged.y)
                nextKillsSincePowerDrop = 0
            }
            nextShake = max(nextShake, if (damaged.kind == EnemyKind.Boss) 12f else 4f)
            if (damaged.kind == EnemyKind.Boss) {
                nextPhase = when (mode) {
                    GameMode.Infinite -> ChapterPhase.Wave
                    GameMode.Chapter -> ChapterPhase.Clear
                    GameMode.BossRush -> if (nextBossKills >= ChapterCatalog.FINAL_CHAPTER) ChapterPhase.Clear else ChapterPhase.BossWarning
                }
                if (mode == GameMode.Infinite) {
                    nextInfiniteRound += 1
                    nextRoundKills = 0
                    if (nextInfiniteRound % 5 == 0) {
                        val event = when {
                            nextInfiniteRound % ChapterCatalog.FINAL_CHAPTER == 0 -> "终章潮汐"
                            nextInfiniteRound % 10 == 0 -> "彩蛋补给"
                            else -> "补给舰"
                        }
                        newTexts += FloatingText(++ids, event, nextPlayer.x, nextPlayer.y - 96f, color = Color(0xFFFFE66D))
                        val specialNode = nextInfiniteRound % 10 == 0
                        val finalNode = nextInfiniteRound % ChapterCatalog.FINAL_CHAPTER == 0
                        newPowerUps += PowerUp(++ids, if (specialNode) PowerUpKind.Invincible else PowerUpKind.Shield, nextPlayer.x - 46f, nextPlayer.y - 170f, vy = 130f)
                        newPowerUps += PowerUp(++ids, if (Random.nextBoolean()) PowerUpKind.Laser else PowerUpKind.Missile, nextPlayer.x + 46f, nextPlayer.y - 170f, vy = 130f)
                        if (finalNode) {
                            newPowerUps += PowerUp(++ids, PowerUpKind.Heal, nextPlayer.x, nextPlayer.y - 210f, vy = 125f)
                        }
                        if (specialNode) nextShake = max(nextShake, if (finalNode) 12f else 9f)
                    }
                }
                if (mode == GameMode.Chapter) nextScreen = GameScreen.ChapterClear
                if (mode == GameMode.BossRush && nextBossKills >= ChapterCatalog.FINAL_CHAPTER) nextScreen = GameScreen.ChapterClear
                if (mode == GameMode.BossRush && nextPhase == ChapterPhase.BossWarning) nextBossWarning = 2.6f
                nextSpawnCooldown = 0.7f
            }
        } else {
            if (damaged.kind == EnemyKind.Boss) {
                val result = damaged.maybeTriggerBossUltimate(++ids, nextPlayer, width, height)
                ids = result.nextId
                damaged = result.boss
                if (result.bullets.isNotEmpty()) {
                    remainingBullets += result.bullets
                    newUltimateBlasts += UltimateBlast(++ids, damaged.x, damaged.y + damaged.radius * 0.35f, radius = damaged.radius * 1.45f, playerOwned = false)
                    newTexts += FloatingText(++ids, result.warningText, damaged.x, damaged.y + damaged.radius + 34f, color = Color(0xFFFF3B30))
                    nextBossUltimateWarning = 1.25f
                    nextBossUltimateText = result.warningText
                    nextShake = max(nextShake, 10f)
                }
            }
            remainingEnemies += damaged
        }
    }

    var enemyBulletIndex = 0
    while (enemyBulletIndex < remainingBullets.size) {
        val bullet = remainingBullets[enemyBulletIndex]
        if (bullet.owner == BulletOwner.Enemy && hit(bullet.x, bullet.y, bullet.radius, nextPlayer.x, nextPlayer.y, nextPlayer.radius * 0.60f)) {
            remainingBullets.removeAt(enemyBulletIndex)
            if (nextPlayer.shield > 0f) {
                nextPlayer = nextPlayer.copy(shield = max(0f, nextPlayer.shield - 4.0f))
            } else if (nextPlayer.invincible <= 0f) {
                nextPlayer = nextPlayer.damage(1)
                nextPlayer = nextPlayer.addUltimateEnergy(12f)
                remainingBullets.removeAll { other ->
                    other.owner == BulletOwner.Enemy && hypot((other.x - nextPlayer.x).toDouble(), (other.y - nextPlayer.y).toDouble()) < 130.0
                }
                nextShake = max(nextShake, 6f)
            }
            val particles = impactParticles(ids + 1, bullet.x, bullet.y, Color(0xFFFF6B6B), qualityLevel)
            newParticles += particles
            ids += particles.size
        } else {
            enemyBulletIndex += 1
        }
    }

    val movedPowerUps = (powerUps + spawnedPowerUps + newPowerUps).map { it.moveTowardPlayer(dt, nextPlayer) }
    val remainingPowerUps = mutableListOf<PowerUp>()
    movedPowerUps.forEach { power ->
        if (hit(power.x, power.y, 28f, nextPlayer.x, nextPlayer.y, nextPlayer.radius)) {
            nextPlayer = nextPlayer.applyPower(power.kind)
            nextPlayer = nextPlayer.addUltimateEnergy(10f)
            newTexts += FloatingText(
                ++ids,
                GameConfig.powerUpLabel(power.kind),
                nextPlayer.x,
                nextPlayer.y - 70f,
                color = GameConfig.powerUpColor(power.kind),
            )
            nextShake = max(nextShake, 2f)
        } else if (power.y < height + 60f) {
            remainingPowerUps += power
        }
    }

    if (nextPlayer.hp <= 0 && nextScreen == GameScreen.Playing) {
        nextScreen = GameScreen.GameOver
        newExplosions += Explosion(++ids, nextPlayer.x, nextPlayer.y, large = true)
        nextShake = 14f
    }

    val nextParticles = (particles + newParticles).map {
        it.copy(x = it.x + it.vx * dt, y = it.y + it.vy * dt, vy = it.vy + 520f * dt, age = it.age + dt)
    }.filter { it.age < it.maxAge }.takeLast(qualityLevel.maxParticles())
    val nextExplosions = (explosions + newExplosions).map { it.copy(age = it.age + dt) }
        .filter { it.age < it.maxAge }
        .takeLast(qualityLevel.maxExplosions())
    val nextTexts = (floatingTexts + newTexts).map { it.copy(y = it.y - 42f * dt, age = it.age + dt) }
        .filter { it.age < 0.9f }
        .takeLast(qualityLevel.maxFloatingTexts())
    val nextUltimateBlasts = (ultimateBlasts + newUltimateBlasts).map { it.copy(age = it.age + dt) }
        .filter { it.age < it.maxAge }
        .takeLast(qualityLevel.maxUltimateBlasts())

    return copy(
        screen = nextScreen,
        phase = nextPhase,
        player = nextPlayer,
        bullets = remainingBullets.capBullets(qualityLevel),
        enemies = remainingEnemies,
        powerUps = remainingPowerUps,
        particles = nextParticles,
        explosions = nextExplosions,
        stars = nextStars,
        floatingTexts = nextTexts,
        ultimateBlasts = nextUltimateBlasts,
        score = nextScore,
        highScore = max(highScore, nextScore),
        kills = nextKills,
        bossKills = nextBossKills,
        infiniteRound = nextInfiniteRound,
        roundKills = nextRoundKills,
        elapsed = nextElapsed,
        shootCooldown = if (shootReady) playerShootCooldown(playerProgress) else shootCooldown - dt,
        spawnCooldown = nextSpawnCooldown,
        bossWarning = nextBossWarning,
        shake = nextShake,
        killsSincePowerDrop = nextKillsSincePowerDrop,
        lowHpTimer = nextLowHpTimer,
        reviveUsed = nextReviveUsed,
        ultimateRequested = false,
        bossUltimateWarning = nextBossUltimateWarning,
        bossUltimateText = nextBossUltimateText,
        idSeed = ids,
    )
}

private fun GameState.difficulty(): Float {
    return when (mode) {
        GameMode.Infinite -> {
            val chapter = ChapterCatalog.infiniteChapterForRound(infiniteRound)
            val tier = ChapterCatalog.infiniteLoopTier(infiniteRound)
            1f + chapter * 0.055f + tier * 0.34f + elapsed / 150f
        }
        GameMode.Chapter -> 1f + chapter * 0.20f + kills / 62f
        GameMode.BossRush -> 1.25f + bossKills * 0.16f
    }.coerceAtMost(4.8f)
}

private fun Player.tick(dt: Float): Player {
    return copy(
        shield = max(0f, shield - dt),
        invincible = max(0f, invincible - dt),
        doubleShot = max(0f, doubleShot - dt),
        laser = max(0f, laser - dt),
        missile = max(0f, missile - dt),
        ultimateCooldown = max(0f, ultimateCooldown - dt),
        ultimateActive = max(0f, ultimateActive - dt),
    )
}

private fun Player.damage(amount: Int): Player = copy(hp = max(0, hp - amount), invincible = 1.2f)

private fun Player.addUltimateEnergy(value: Float): Player {
    if (value <= 0f || ultimateEnergy >= PLAYER_ULTIMATE_MAX) return this
    return copy(ultimateEnergy = min(PLAYER_ULTIMATE_MAX, ultimateEnergy + value))
}

private fun Player.applyPower(kind: PowerUpKind): Player {
    return when (kind) {
        PowerUpKind.DoubleShot -> copy(doubleShot = max(doubleShot, 14f))
        PowerUpKind.Laser -> copy(laser = max(laser, 10f))
        PowerUpKind.Missile -> copy(missile = max(missile, 11f))
        PowerUpKind.Shield -> copy(shield = max(shield, 8f))
        PowerUpKind.Invincible -> copy(invincible = 3f)
        PowerUpKind.Heal -> copy(hp = min(maxHp, hp + 2))
    }
}

private fun PowerUp.moveTowardPlayer(dt: Float, player: Player): PowerUp {
    val baseY = y + vy * dt
    val dx = player.x - x
    val dy = player.y - baseY
    val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    val magnetRange = when (kind) {
        PowerUpKind.Heal, PowerUpKind.Shield -> 320f
        else -> 270f
    }
    if (distance <= 0.01f || distance > magnetRange) {
        return copy(y = baseY, age = age + dt)
    }

    val strength = (1f - distance / magnetRange).coerceIn(0.18f, 1f)
    val speed = 360f + strength * 820f
    val step = min(distance, speed * dt)
    return copy(
        x = x + dx / distance * step,
        y = baseY + dy / distance * step,
        age = age + dt,
    )
}

private fun playerShootCooldown(progress: PlayerProgress): Float {
    return (0.13f - (progress.weaponLevel - 1) * 0.011f - progress.engineLevel * 0.005f).coerceAtLeast(0.098f)
}

private fun QualityLevel.trailLimit(): Int {
    return when (this) {
        QualityLevel.Smooth -> 3
        QualityLevel.Standard -> 5
        QualityLevel.High -> 7
    }
}

private fun QualityLevel.maxBullets(): Int {
    return when (this) {
        QualityLevel.Smooth -> 150
        QualityLevel.Standard -> 220
        QualityLevel.High -> 320
    }
}

private fun QualityLevel.maxParticles(): Int {
    return when (this) {
        QualityLevel.Smooth -> 90
        QualityLevel.Standard -> 150
        QualityLevel.High -> 240
    }
}

private fun QualityLevel.maxExplosions(): Int {
    return when (this) {
        QualityLevel.Smooth -> 10
        QualityLevel.Standard -> 18
        QualityLevel.High -> 28
    }
}

private fun QualityLevel.maxFloatingTexts(): Int {
    return when (this) {
        QualityLevel.Smooth -> 10
        QualityLevel.Standard -> 16
        QualityLevel.High -> 24
    }
}

private fun QualityLevel.maxUltimateBlasts(): Int {
    return when (this) {
        QualityLevel.Smooth -> 3
        QualityLevel.Standard -> 5
        QualityLevel.High -> 8
    }
}

private fun List<Bullet>.capBullets(qualityLevel: QualityLevel): List<Bullet> {
    val maxBullets = qualityLevel.maxBullets()
    if (size <= maxBullets) return this
    val playerLimit = maxBullets / 2
    val playerBullets = filter { it.owner == BulletOwner.Player }.takeLast(playerLimit)
    val enemyBullets = filter { it.owner == BulletOwner.Enemy }.takeLast(maxBullets - playerBullets.size)
    return (enemyBullets + playerBullets).sortedBy { it.id }
}

private fun createPlayerBullets(player: Player, progress: PlayerProgress, firstId: Long): List<Bullet> {
    val bullets = mutableListOf<Bullet>()
    var id = firstId
    val normalDamage = if (progress.weaponLevel >= 4) 2 else 1
    bullets += Bullet(id++, BulletOwner.Player, BulletKind.Normal, player.x - 10f, player.y - player.radius, -20f, -980f, 8f, normalDamage)
    bullets += Bullet(id++, BulletOwner.Player, BulletKind.Normal, player.x + 10f, player.y - player.radius, 20f, -980f, 8f, normalDamage)
    if (progress.weaponLevel >= 3) {
        bullets += Bullet(id++, BulletOwner.Player, BulletKind.Plasma, player.x, player.y - player.radius - 8f, 0f, -1040f, 8f, 1)
    }
    if (player.doubleShot > 0f) {
        bullets += Bullet(id++, BulletOwner.Player, BulletKind.Plasma, player.x - 30f, player.y - player.radius + 8f, -95f, -930f, 9f, 1)
        bullets += Bullet(id, BulletOwner.Player, BulletKind.Plasma, player.x + 30f, player.y - player.radius + 8f, 95f, -930f, 9f, 1)
    }
    return bullets
}

private fun createPlayerMissiles(player: Player, progress: PlayerProgress, firstId: Long): List<Bullet> {
    val bullets = mutableListOf<Bullet>()
    var id = firstId
    val damage = 4 + progress.missileLevel
    bullets += Bullet(id++, BulletOwner.Player, BulletKind.Missile, player.x, player.y - player.radius, -115f, -760f, 12f, damage, ttl = 5f)
    bullets += Bullet(id++, BulletOwner.Player, BulletKind.Missile, player.x, player.y - player.radius, 115f, -760f, 12f, damage, ttl = 5f)
    if (progress.missileLevel >= 2) {
        bullets += Bullet(id, BulletOwner.Player, BulletKind.Missile, player.x, player.y - player.radius - 10f, 0f, -820f, 13f, damage + 1, ttl = 5f)
    }
    return bullets
}

private fun createEnemy(id: Long, width: Float, topSafe: Float, difficulty: Float, chapter: ChapterSpec, modifier: InfiniteModifier = InfiniteModifier.None): Enemy {
    val kind = randomEnemyKindForModifier(chapter, modifier)
    val spec = GameConfig.enemySpec(kind)
    val hp = (spec.hp * chapter.enemyHpScale * modifierEnemyHpScale(modifier)).toInt().coerceAtLeast(spec.hp)
    return Enemy(
        id = id,
        kind = kind,
        x = Random.nextFloat() * (width - spec.radius * 2f) + spec.radius,
        y = topSafe - spec.radius - Random.nextFloat() * 120f,
        radius = spec.radius,
        hp = hp,
        maxHp = hp,
        speed = (145f + Random.nextFloat() * 90f) * difficulty * spec.speedScale,
        score = (spec.score * chapter.enemyHpScale).toInt(),
        chapterId = chapter.id,
        modifier = modifier,
    )
}

private fun createBossRushEscort(id: Long, width: Float, topSafe: Float, difficulty: Float, chapter: ChapterSpec): Enemy {
    val enemy = createEnemy(id, width, topSafe, (difficulty * 0.76f).coerceAtLeast(1f), chapter)
    val hp = (enemy.maxHp * 0.62f).toInt().coerceAtLeast(1)
    return enemy.copy(
        hp = hp,
        maxHp = hp,
        score = (enemy.score * 0.70f).toInt().coerceAtLeast(8),
        speed = enemy.speed * 0.92f,
    )
}

private fun createBoss(id: Long, width: Float, topSafe: Float, difficulty: Float, chapter: ChapterSpec, loopTier: Int = 0, modifier: InfiniteModifier = InfiniteModifier.None): Enemy {
    val radius = when {
        chapter.finalChapter -> 132f
        chapter.easterBoss -> 118f
        else -> 106f
    }
    val loopScale = 1f + loopTier.coerceAtLeast(0) * 0.28f + if (modifier == InfiniteModifier.CoreSurge) 0.18f else 0f
    val specialScale = when {
        chapter.finalChapter -> 1.32f
        chapter.easterBoss -> 1.12f
        else -> 1f
    }
    val hp = ((140 + difficulty * 46f) * chapter.bossHpScale * specialScale * loopScale).toInt()
    return Enemy(
        id = id,
        kind = EnemyKind.Boss,
        x = width / 2f,
        y = topSafe + 80f,
        radius = radius,
        hp = hp,
        maxHp = hp,
        speed = 62f + difficulty * 9f + (if (chapter.finalChapter) 12f else if (chapter.easterBoss) 6f else 0f) + loopTier.coerceAtLeast(0) * 2f,
        score = ((900 + (difficulty * 160f).toInt()) * when {
            chapter.finalChapter -> 2.2f
            chapter.easterBoss -> 1.45f
            else -> 1f
        }).toInt() + loopTier.coerceAtLeast(0) * 250,
        chapterId = chapter.id,
        bossName = chapter.bossName,
        finalBoss = chapter.finalChapter,
        shootCooldown = bossShotDelay(chapter),
        skillCooldown = bossSkillDelay(chapter),
        modifier = modifier,
    )
}

private fun bossRushEscortDelay(bossKills: Int): Float {
    return (2.4f - bossKills * 0.045f).coerceIn(1.35f, 2.4f)
}

private fun randomEnemyKindForModifier(chapter: ChapterSpec, modifier: InfiniteModifier): EnemyKind {
    if (modifier == InfiniteModifier.FireSuppression && Random.nextFloat() < 0.58f) {
        return if (Random.nextBoolean()) EnemyKind.Gunner else EnemyKind.Tank
    }
    if (modifier == InfiniteModifier.DebrisField && Random.nextFloat() < 0.45f) {
        return if (Random.nextBoolean()) EnemyKind.Scout else EnemyKind.Waver
    }
    return GameConfig.randomEnemyKind(chapter)
}

private fun modifierDifficultyScale(modifier: InfiniteModifier): Float {
    return when (modifier) {
        InfiniteModifier.Electromagnetic -> 1.04f
        InfiniteModifier.DebrisField -> 0.96f
        InfiniteModifier.FireSuppression -> 1.06f
        InfiniteModifier.SupplyGap -> 1.02f
        InfiniteModifier.CoreSurge -> 1.10f
        InfiniteModifier.None -> 1f
    }
}

private fun modifierSupportBonus(modifier: InfiniteModifier): Float {
    return when (modifier) {
        InfiniteModifier.DebrisField -> 0.16f
        InfiniteModifier.FireSuppression -> 0.07f
        else -> 0f
    }
}

private fun modifierSpawnScale(modifier: InfiniteModifier): Float {
    return when (modifier) {
        InfiniteModifier.DebrisField -> 0.88f
        InfiniteModifier.FireSuppression -> 0.94f
        InfiniteModifier.CoreSurge -> 0.92f
        else -> 1f
    }
}

private fun modifierDropScale(modifier: InfiniteModifier): Float {
    return when (modifier) {
        InfiniteModifier.Electromagnetic -> 1.18f
        InfiniteModifier.SupplyGap -> 0.62f
        InfiniteModifier.CoreSurge -> 0.85f
        else -> 1f
    }
}

private fun modifierEnemyHpScale(modifier: InfiniteModifier): Float {
    return when (modifier) {
        InfiniteModifier.DebrisField -> 0.84f
        InfiniteModifier.FireSuppression -> 1.12f
        InfiniteModifier.CoreSurge -> 1.10f
        else -> 1f
    }
}

private fun bossShotDelay(chapter: ChapterSpec): Float {
    return when {
        chapter.finalChapter -> 0.55f
        chapter.id >= 8 -> 0.68f
        chapter.id >= 5 -> 0.80f
        else -> 0.95f
    }
}

private fun bossSkillDelay(chapter: ChapterSpec): Float {
    return when {
        chapter.finalChapter -> 2.7f
        chapter.id >= 9 -> 3.0f
        chapter.id >= 6 -> 3.5f
        chapter.id >= 3 -> 4.2f
        else -> 4.8f
    }
}

private fun Enemy.nextBossSkillDelay(): Float {
    if (kind != EnemyKind.Boss) return 999f
    val chapter = ChapterCatalog.spec(chapterId)
    val hpRate = hp.toFloat() / maxHp.coerceAtLeast(1)
    val base = if (hpRate > 0.35f) bossSkillDelay(chapter) else when {
        chapter.finalChapter -> 2.2f
        chapter.id >= 9 -> 2.5f
        chapter.id >= 6 -> 2.9f
        chapter.id >= 3 -> 3.4f
        else -> 4.0f
    }
    return when (modifier) {
        InfiniteModifier.CoreSurge -> base * 0.82f
        InfiniteModifier.SupplyGap -> base * 1.08f
        else -> base
    }
}

private fun Enemy.bossWeakWindow(): Float {
    val chapter = ChapterCatalog.spec(chapterId)
    val base = when {
        chapter.finalChapter -> 1.45f
        chapter.id >= 8 -> 1.65f
        else -> 1.9f
    }
    return when (modifier) {
        InfiniteModifier.SupplyGap -> base + 0.45f
        InfiniteModifier.CoreSurge -> max(1.0f, base - 0.25f)
        else -> base
    }
}

private fun Enemy.tickBossVulnerability(dt: Float): Float {
    if (kind != EnemyKind.Boss) return vulnerable
    if (vulnerable < 0f) {
        val next = vulnerable + dt
        return if (next >= 0f) bossWeakWindow() * 0.78f else next
    }
    return max(0f, vulnerable - dt)
}

private fun maybeBossSupply(player: Player): PowerUpKind? {
    return when {
        player.hp <= 1 -> PowerUpKind.Heal
        player.hp <= 2 && player.shield <= 0f -> PowerUpKind.Shield
        player.hp <= 3 && Random.nextFloat() < 0.45f -> if (Random.nextBoolean()) PowerUpKind.Heal else PowerUpKind.Shield
        Random.nextFloat() < 0.18f -> if (Random.nextBoolean()) PowerUpKind.Laser else PowerUpKind.Missile
        else -> null
    }
}

private fun choosePowerUp(
    player: Player,
    progress: PlayerProgress,
    phase: ChapterPhase,
    fromBoss: Boolean,
    lowHpTimer: Float,
): PowerUpKind {
    if (player.hp <= 2 && lowHpTimer >= 14f) {
        return if (player.hp <= 1) PowerUpKind.Heal else PowerUpKind.Shield
    }

    val weights = mutableListOf(
        PowerUpKind.DoubleShot to 18,
        PowerUpKind.Laser to if (progress.laserLevel > 0) 18 else 10,
        PowerUpKind.Missile to if (progress.missileLevel > 0) 18 else 10,
        PowerUpKind.Shield to 14,
        PowerUpKind.Invincible to 8,
        PowerUpKind.Heal to 12,
    )

    fun add(kind: PowerUpKind, value: Int) {
        val index = weights.indexOfFirst { it.first == kind }
        if (index >= 0) weights[index] = weights[index].first to weights[index].second + value
    }

    if (player.hp <= 2) {
        add(PowerUpKind.Heal, 34)
        add(PowerUpKind.Shield, 28)
        add(PowerUpKind.Invincible, 14)
    }
    if (phase == ChapterPhase.Boss || fromBoss) {
        add(PowerUpKind.Laser, 20)
        add(PowerUpKind.Missile, 20)
        add(PowerUpKind.Shield, 12)
    }
    if (player.doubleShot > 5f) add(PowerUpKind.DoubleShot, -10)
    if (player.laser > 5f) add(PowerUpKind.Laser, -10)
    if (player.missile > 5f) add(PowerUpKind.Missile, -10)
    if (player.shield > 5f) add(PowerUpKind.Shield, -10)

    return weightedPowerUp(weights.map { it.first to it.second.coerceAtLeast(1) })
}

private fun weightedPowerUp(weights: List<Pair<PowerUpKind, Int>>): PowerUpKind {
    val total = weights.sumOf { it.second }.coerceAtLeast(1)
    var roll = Random.nextInt(total)
    weights.forEach { (kind, weight) ->
        roll -= weight
        if (roll < 0) return kind
    }
    return weights.last().first
}

private fun Enemy.move(dt: Float, player: Player, width: Float, height: Float, difficulty: Float): Enemy {
    var nx = x
    var ny = y
    when (kind) {
        EnemyKind.Scout, EnemyKind.Gunner, EnemyKind.Tank -> {
            ny += speed * dt
        }
        EnemyKind.Hunter -> {
            val steer = (player.x - x).coerceIn(-160f, 160f)
            nx += steer * dt * (0.9f + difficulty * 0.08f)
            ny += speed * dt
        }
        EnemyKind.Waver -> {
            nx += sin(age * 3.2f + phase) * 180f * dt
            ny += speed * dt
        }
        EnemyKind.Boss -> {
            nx += sin(age * 1.25f) * speed * 1.8f * dt
            ny += ((height * 0.23f) - y).coerceIn(-52f, 52f) * dt
        }
    }
    return copy(
        x = nx.coerceIn(radius + 8f, width - radius - 8f),
        y = ny,
        age = age + dt,
        vulnerable = max(0f, vulnerable - dt),
    )
}

private fun Enemy.nextShotDelay(difficulty: Float): Float {
    val base = when (kind) {
        EnemyKind.Scout, EnemyKind.Waver -> 999f
        EnemyKind.Hunter -> 1.8f / difficulty
        EnemyKind.Gunner -> 1.05f / difficulty
        EnemyKind.Tank -> 1.35f / difficulty
        EnemyKind.Boss -> bossShotDelay(ChapterCatalog.spec(chapterId))
    }
    val modified = when (modifier) {
        InfiniteModifier.Electromagnetic -> base * 0.92f
        InfiniteModifier.FireSuppression -> base * 0.84f
        InfiniteModifier.CoreSurge -> base * 0.88f
        else -> base
    }
    return modified.coerceAtLeast(0.24f)
}

private fun chapterBulletSpeed(chapterId: Int, first: Float, last: Float): Float {
    val t = ((chapterId.coerceIn(1, ChapterCatalog.FINAL_CHAPTER) - 1) / (ChapterCatalog.FINAL_CHAPTER - 1).toFloat()).coerceIn(0f, 1f)
    return first + (last - first) * t
}

private fun Enemy.createEnemyShots(firstId: Long, player: Player): List<Bullet> {
    if (kind == EnemyKind.Scout || kind == EnemyKind.Waver) return emptyList()
    val bullets = mutableListOf<Bullet>()
    var id = firstId
    val angle = atan2(player.y - y, player.x - x)
    fun shot(offset: Float, speed: Float, radius: Float = 8f, damage: Int = 1) {
        val a = angle + offset
        val adjustedSpeed = speed * modifierBulletSpeedScale(modifier)
        val vx = cos(a) * adjustedSpeed
        val vy = sin(a) * adjustedSpeed
        bullets += Bullet(
            id++,
            BulletOwner.Enemy,
            BulletKind.Normal,
            x,
            y + radius,
            vx,
            vy,
            radius,
            damage,
            ttl = projectileTtlToPlayer(x, y + radius, vx, vy, player, if (kind == EnemyKind.Boss) 6.8f else 5.8f),
        )
    }
    when (kind) {
        EnemyKind.Hunter -> shot(0f, chapterBulletSpeed(chapterId, 320f, 380f))
        EnemyKind.Gunner -> {
            val speed = chapterBulletSpeed(chapterId, 310f, 390f)
            shot(-0.18f, speed)
            shot(0.18f, speed)
        }
        EnemyKind.Tank -> {
            val speed = chapterBulletSpeed(chapterId, 280f, 360f)
            shot(-0.28f, speed, 11f)
            shot(0f, speed, 11f)
            shot(0.28f, speed, 11f)
        }
        EnemyKind.Boss -> {
            val count = if (finalBoss) 7 else 5
            val center = count / 2
            val speed = if (finalBoss) 500f else chapterBulletSpeed(chapterId, 360f, 460f)
            repeat(count) { i -> shot((i - center) * 0.16f, speed, 10f) }
        }
        else -> Unit
    }
    return bullets
}

private fun Enemy.createBossSkill(firstId: Long, player: Player): List<Bullet> {
    if (kind != EnemyKind.Boss) return emptyList()
    val bullets = mutableListOf<Bullet>()
    var id = firstId
    val base = atan2(player.y - y, player.x - x)
    val fanCount = if (finalBoss) 28 else 18
    val half = fanCount / 2
    repeat(fanCount) { i ->
        val a = base + (i - half) * if (finalBoss) 0.14f else 0.18f
        val speed = (if (finalBoss) 390f else chapterBulletSpeed(chapterId, 300f, 370f)) * modifierBulletSpeedScale(modifier)
        val vx = cos(a) * speed
        val vy = sin(a) * speed
        bullets += Bullet(
            id++,
            BulletOwner.Enemy,
            BulletKind.Plasma,
            x,
            y,
            vx,
            vy,
            9f,
            1,
            ttl = projectileTtlToPlayer(x, y, vx, vy, player, 7.0f),
        )
    }
    val lanes = if (finalBoss) listOf(-78f, 0f, 78f) else listOf(-55f, 55f)
    lanes.forEach { lane ->
        val speed = (if (finalBoss) 660f else chapterBulletSpeed(chapterId, 520f, 620f)) * modifierBulletSpeedScale(modifier)
        bullets += Bullet(
            id++,
            BulletOwner.Enemy,
            BulletKind.Laser,
            x + lane,
            y + radius,
            0f,
            speed,
            18f,
            2,
            ttl = projectileTtlToPlayer(x + lane, y + radius, 0f, speed, player, 4.2f),
        )
    }
    id = appendChapterBossSkill(bullets, id, player)
    return bullets
}

private fun modifierBulletSpeedScale(modifier: InfiniteModifier): Float {
    return when (modifier) {
        InfiniteModifier.Electromagnetic -> 1.16f
        InfiniteModifier.CoreSurge -> 1.10f
        InfiniteModifier.SupplyGap -> 0.94f
        else -> 1f
    }
}

private data class BossUltimateResult(
    val boss: Enemy,
    val bullets: List<Bullet>,
    val warningText: String,
    val nextId: Long,
)

private fun Enemy.maybeTriggerBossUltimate(firstId: Long, player: Player, width: Float, height: Float): BossUltimateResult {
    if (kind != EnemyKind.Boss) return BossUltimateResult(this, emptyList(), "", firstId)
    val hpRate = hp.toFloat() / maxHp.coerceAtLeast(1)
    val stage = when {
        hpRate <= 0.15f && ultimateMarks and 0b100 == 0 -> 3
        hpRate <= 0.40f && ultimateMarks and 0b010 == 0 -> 2
        hpRate <= 0.70f && ultimateMarks and 0b001 == 0 -> 1
        else -> 0
    }
    if (stage == 0) return BossUltimateResult(this, emptyList(), "", firstId)

    val chapter = ChapterCatalog.spec(chapterId)
    val mark = when (stage) {
        1 -> 0b001
        2 -> 0b010
        else -> 0b100
    }
    val warning = when {
        chapter.finalChapter && stage == 3 -> "虚空核心过载"
        chapter.easterBoss && stage == 3 -> "王牌极限机动"
        stage == 1 -> "高能反应"
        stage == 2 -> "火力封锁"
        else -> "核心过载"
    }
    val bullets = mutableListOf<Bullet>()
    var id = firstId
    val speedScale = modifierBulletSpeedScale(modifier)

    fun add(kind: BulletKind, sx: Float, sy: Float, vx: Float, vy: Float, radius: Float, damage: Int = 1, ttl: Float = 7.2f) {
        val scaledVx = vx * speedScale
        val scaledVy = vy * speedScale
        bullets += Bullet(
            id++,
            BulletOwner.Enemy,
            kind,
            sx,
            sy,
            scaledVx,
            scaledVy,
            radius,
            damage,
            ttl = projectileTtlToPlayer(sx, sy, scaledVx, scaledVy, player, ttl),
        )
    }

    fun aimed(offset: Float, speed: Float, radius: Float = 10f, damage: Int = 1) {
        val a = atan2(player.y - y, player.x - x) + offset
        add(BulletKind.Plasma, x, y, cos(a) * speed, sin(a) * speed, radius, damage)
    }

    val finalBoost = if (chapter.finalChapter) 1.18f else if (chapter.easterBoss) 1.08f else 1f
    when (stage) {
        1 -> {
            val count = (14 + chapterId / 2).coerceAtMost(24)
            repeat(count) { i ->
                aimed((i - (count - 1) / 2f) * 0.105f, 360f * finalBoost, 8f)
            }
        }
        2 -> {
            val lanes = if (chapterId >= 14) listOf(-150f, -90f, -30f, 30f, 90f, 150f) else listOf(-110f, -55f, 0f, 55f, 110f)
            lanes.forEach { lane ->
                add(BulletKind.Laser, player.x + lane, y + radius, 0f, 720f * finalBoost, 15f, damage = 2, ttl = 2.8f)
            }
            repeat(10 + chapterId / 3) { i ->
                aimed((i - 6f) * 0.13f, 330f * finalBoost, 9f)
            }
        }
        else -> {
            val ring = if (chapter.finalChapter) 36 else if (chapter.easterBoss) 30 else 26
            repeat(ring) { i ->
                val a = i / ring.toFloat() * PI.toFloat() * 2f + age * 0.5f
                add(BulletKind.Plasma, x, y, cos(a) * 390f * finalBoost, sin(a) * 390f * finalBoost, 10f, damage = if (chapter.finalChapter) 2 else 1)
            }
            listOf(-118f, -39f, 39f, 118f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, 0f, 790f * finalBoost, 18f, damage = 2, ttl = 3.1f)
            }
        }
    }
    return BossUltimateResult(copy(ultimateMarks = ultimateMarks or mark, vulnerable = -1.6f), bullets, warning, id - 1)
}

private fun projectileTtlToPlayer(
    sx: Float,
    sy: Float,
    vx: Float,
    vy: Float,
    player: Player,
    baseTtl: Float,
): Float {
    if (vy <= 20f) return baseTtl
    val directDistance = hypot((player.x - sx).toDouble(), (player.y - sy).toDouble()).toFloat()
    val speed = hypot(vx.toDouble(), vy.toDouble()).toFloat().coerceAtLeast(1f)
    val travelTtl = directDistance / speed + 1.25f
    return max(baseTtl, travelTtl).coerceAtMost(9.0f)
}

private fun Enemy.appendChapterBossSkill(bullets: MutableList<Bullet>, firstId: Long, player: Player): Long {
    var id = firstId
    val speedScale = modifierBulletSpeedScale(modifier)
    fun add(kind: BulletKind, sx: Float, sy: Float, vx: Float, vy: Float, radius: Float, damage: Int = 1, ttl: Float = 6.8f) {
        val scaledVx = vx * speedScale
        val scaledVy = vy * speedScale
        bullets += Bullet(
            id++,
            BulletOwner.Enemy,
            kind,
            sx,
            sy,
            scaledVx,
            scaledVy,
            radius,
            damage,
            ttl = projectileTtlToPlayer(sx, sy, scaledVx, scaledVy, player, ttl),
        )
    }
    fun aimed(offset: Float, speed: Float, radius: Float = 9f, kind: BulletKind = BulletKind.Plasma) {
        val a = atan2(player.y - y, player.x - x) + offset
        add(kind, x, y, cos(a) * speed, sin(a) * speed, radius)
    }

    when (chapterId) {
        1 -> {
            repeat(7) { i -> aimed((i - 3) * 0.25f, 260f, 8f) }
        }
        2 -> {
            listOf(-70f, 0f, 70f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, 0f, 610f, 15f, damage = 2, ttl = 1.7f)
            }
        }
        3 -> {
            repeat(6) { i ->
                val yy = y + 40f + i * 28f
                add(BulletKind.Plasma, -30f, yy, 360f, 120f, 8f)
                add(BulletKind.Plasma, x * 2f + 30f, yy + 14f, -360f, 120f, 8f)
            }
        }
        4 -> {
            listOf(-0.42f, -0.18f, 0.18f, 0.42f).forEach { offset ->
                aimed(offset, 390f, 7f, BulletKind.Plasma)
                aimed(offset * 0.55f, 300f, 6f, BulletKind.Normal)
            }
        }
        5 -> {
            listOf(-86f, -38f, 38f, 86f).forEach { lane ->
                add(BulletKind.Laser, player.x + lane, y + radius, 0f, 680f, 13f, damage = 2, ttl = 1.45f)
            }
        }
        6 -> {
            repeat(9) { i ->
                val lane = (i - 4) * 34f
                add(BulletKind.Plasma, x + lane, y + radius * 0.3f, lane * 0.9f, 250f + (i % 3) * 35f, 12f, ttl = 4.5f)
            }
        }
        7 -> {
            repeat(5) { i ->
                val yy = y + 40f + i * 46f
                add(BulletKind.Plasma, 22f, yy, 330f, 170f, 9f)
                add(BulletKind.Plasma, player.x * 2f - 22f, yy + 20f, -330f, 170f, 9f)
            }
        }
        8 -> {
            repeat(18) { i ->
                val a = i / 18f * PI.toFloat() * 2f
                add(BulletKind.Plasma, x, y, cos(a) * 330f, sin(a) * 330f, 10f)
            }
        }
        9 -> {
            repeat(8) { i -> aimed((i - 3.5f) * 0.12f, 430f + i * 12f, 8f, BulletKind.Plasma) }
        }
        10 -> {
            repeat(24) { i ->
                val a = i / 24f * PI.toFloat() * 2f + age * 0.4f
                add(BulletKind.Plasma, x, y, cos(a) * 360f, sin(a) * 360f, 9f)
            }
            listOf(-96f, 0f, 96f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, 0f, 720f, 17f, damage = 2, ttl = 1.9f)
            }
        }
        11 -> {
            listOf(-112f, -56f, 0f, 56f, 112f).forEach { lane ->
                add(BulletKind.Plasma, x + lane, y + radius * 0.2f, lane * 0.55f, 390f, 10f)
            }
            repeat(6) { i -> aimed((i - 2.5f) * 0.18f, 410f, 8f) }
        }
        12 -> {
            listOf(-120f, -40f, 40f, 120f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, 0f, 735f, 15f, damage = 2, ttl = 1.55f)
            }
            repeat(8) { i -> aimed((i - 3.5f) * 0.14f, 380f + i * 10f, 8f, BulletKind.Plasma) }
        }
        13 -> {
            repeat(7) { i ->
                val yy = y + 28f + i * 32f
                add(BulletKind.Plasma, -40f, yy, 410f, 155f, 10f)
                add(BulletKind.Plasma, player.x * 2f + 40f, yy + 16f, -410f, 155f, 10f)
            }
        }
        14 -> {
            repeat(12) { i ->
                val offset = (i - 5.5f) * 0.11f
                aimed(offset, 420f, 7f, if (i % 3 == 0) BulletKind.Normal else BulletKind.Plasma)
            }
            listOf(-78f, 78f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, 0f, 700f, 14f, damage = 2, ttl = 1.65f)
            }
        }
        15 -> {
            listOf(-130f, -78f, -26f, 26f, 78f, 130f).forEach { lane ->
                add(BulletKind.Laser, player.x + lane, y + radius, 0f, 760f, 12f, damage = 2, ttl = 1.25f)
            }
            repeat(10) { i -> aimed((i - 4.5f) * 0.12f, 430f, 8f) }
        }
        16 -> {
            repeat(11) { i ->
                val lane = (i - 5) * 31f
                add(BulletKind.Plasma, x + lane, y + radius * 0.35f, lane * 1.1f, 285f + (i % 4) * 30f, 12f, ttl = 4.4f)
            }
        }
        17 -> {
            repeat(6) { i ->
                val yy = y + 34f + i * 42f
                add(BulletKind.Plasma, 18f, yy, 380f, 190f, 9f)
                add(BulletKind.Plasma, player.x * 2f - 18f, yy + 22f, -380f, 190f, 9f)
            }
            repeat(8) { i -> aimed((i - 3.5f) * 0.16f, 395f, 8f) }
        }
        18 -> {
            repeat(24) { i ->
                val a = i / 24f * PI.toFloat() * 2f + age * 0.55f
                add(BulletKind.Plasma, x, y, cos(a) * 390f, sin(a) * 390f, 10f)
            }
        }
        19 -> {
            repeat(10) { i -> aimed((i - 4.5f) * 0.10f, 455f + i * 9f, 8f, BulletKind.Plasma) }
            listOf(-105f, 0f, 105f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, lane * 0.18f, 750f, 16f, damage = 2, ttl = 1.75f)
            }
        }
        20 -> {
            repeat(32) { i ->
                val a = i / 32f * PI.toFloat() * 2f + age * 0.7f
                add(BulletKind.Plasma, x, y, cos(a) * 430f, sin(a) * 430f, 10f)
            }
            listOf(-126f, -42f, 42f, 126f).forEach { lane ->
                add(BulletKind.Laser, x + lane, y + radius, 0f, 790f, 18f, damage = 2, ttl = 2.0f)
            }
        }
    }
    return id
}

private fun Bullet.homeToward(enemies: List<Enemy>): Bullet {
    val target = enemies.minByOrNull { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) } ?: return this
    val angle = atan2(target.y - y, target.x - x)
    val speed = hypot(vx.toDouble(), vy.toDouble()).toFloat().coerceAtLeast(680f)
    val nvx = vx * 0.88f + cos(angle) * speed * 0.12f
    val nvy = vy * 0.88f + sin(angle) * speed * 0.12f
    return copy(vx = nvx, vy = nvy)
}

private fun hit(x1: Float, y1: Float, r1: Float, x2: Float, y2: Float, r2: Float): Boolean {
    val dx = x1 - x2
    val dy = y1 - y2
    val radius = r1 + r2
    return dx * dx + dy * dy <= radius * radius
}

private fun impactParticles(firstId: Long, x: Float, y: Float, color: Color, qualityLevel: QualityLevel): List<Particle> {
    val count = when (qualityLevel) {
        QualityLevel.Smooth -> 3
        QualityLevel.Standard -> 5
        QualityLevel.High -> 6
    }
    return List(count) { index ->
        val angle = Random.nextFloat() * PI.toFloat() * 2f
        val speed = Random.nextFloat() * 220f + 80f
        Particle(firstId + index, x, y, cos(angle) * speed, sin(angle) * speed, Random.nextFloat() * 4f + 2f, color, maxAge = 0.38f)
    }
}

private fun explosionParticles(firstId: Long, x: Float, y: Float, large: Boolean, qualityLevel: QualityLevel): List<Particle> {
    val baseCount = if (large) 42 else 20
    val count = when (qualityLevel) {
        QualityLevel.Smooth -> (baseCount * 0.45f).toInt()
        QualityLevel.Standard -> (baseCount * 0.72f).toInt()
        QualityLevel.High -> baseCount
    }.coerceAtLeast(if (large) 12 else 6)
    val scale = if (large) 1.8f else 1f
    val palette = listOf(Color(0xFFFFD166), Color(0xFFFF7B00), Color(0xFFFF3B30), Color(0xFFFFFFFF))
    return List(count) { index ->
        val angle = Random.nextFloat() * PI.toFloat() * 2f
        val speed = (Random.nextFloat() * 360f + 100f) * scale
        Particle(
            id = firstId + index,
            x = x,
            y = y,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed - 80f,
            radius = (Random.nextFloat() * 5f + 2f) * scale,
            color = palette.random(),
            maxAge = Random.nextFloat() * 0.45f + 0.55f,
        )
    }
}
