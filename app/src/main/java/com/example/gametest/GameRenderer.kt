package com.example.gametest

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val powerUpTextPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textSize = 24f
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
}

private val floatingTextPaint = android.graphics.Paint().apply {
    textSize = 30f
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
}

private val bossWarningPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
}

fun DrawScope.drawGame(game: GameState) {
    val chapter = ChapterCatalog.activeSpec(game)
    drawRect(
        brush = Brush.verticalGradient(
            chapter.background,
        ),
    )
    if (game.qualityLevel != QualityLevel.Smooth) {
        drawChapterAtmosphere(chapter, game.elapsed)
    }
    drawInfiniteBackground(game.stars, game.qualityLevel)

    val shakeX = if (game.shake > 0f) (Random.nextFloat() - 0.5f) * game.shake else 0f
    val shakeY = if (game.shake > 0f) (Random.nextFloat() - 0.5f) * game.shake else 0f
    translate(shakeX, shakeY) {
        game.powerUps.forEach { drawPowerUp(it) }
        game.bullets.forEach { drawBullet(it) }
        game.enemies.forEach { drawEnemy(it) }
        if (game.screen != GameScreen.Menu) drawPlayer(game.player)
        game.explosions.forEach { drawExplosion(it) }
        game.ultimateBlasts.forEach { drawUltimateBlast(it) }
        game.particles.forEach { drawParticle(it) }
        game.floatingTexts.forEach { drawFloatingText(it) }
    }

    if (game.bossWarning > 0f) drawBossWarning(game.bossWarning, chapter)
    if (game.bossUltimateWarning > 0f) drawBossUltimateWarning(game.bossUltimateWarning, game.bossUltimateText)
}

private fun DrawScope.drawChapterAtmosphere(chapter: ChapterSpec, elapsed: Float) {
    val t = elapsed
    when (chapter.backgroundStyle) {
        BackgroundStyle.LowOrbit -> {
            drawCircle(Color(0x224D96C8), size.width * 0.62f, Offset(size.width * 0.5f, size.height * 1.05f))
            repeat(5) { i ->
                val y = (size.height * (0.18f + i * 0.16f) + t * (12f + i * 4f)) % size.height
                drawLine(Color(0x223A86FF), Offset(0f, y), Offset(size.width, y + 32f), strokeWidth = 2f)
            }
        }
        BackgroundStyle.StormCloud -> {
            repeat(8) { i ->
                val x = ((i * 173f + t * (18f + i)) % (size.width + 220f)) - 110f
                val y = size.height * (0.16f + (i % 5) * 0.12f)
                drawOval(Color.White.copy(alpha = 0.05f + (i % 3) * 0.025f), Offset(x, y), Size(180f + i * 16f, 42f + i * 4f))
            }
            if ((t * 2).toInt() % 7 == 0) drawLine(Color(0x55BDE0FE), Offset(size.width * 0.72f, 0f), Offset(size.width * 0.56f, size.height * 0.36f), strokeWidth = 4f)
        }
        BackgroundStyle.DustWar -> {
            repeat(12) { i ->
                val y = (i * 83f + t * (42f + i * 5f)) % size.height
                drawLine(Color(0x33F4A261), Offset(-40f, y), Offset(size.width + 40f, y + 48f), strokeWidth = 3f + i % 3)
            }
        }
        BackgroundStyle.Aurora -> {
            repeat(5) { i ->
                val x = size.width * (0.12f + i * 0.2f)
                val wave = sin(t * 1.2f + i) * 34f
                drawLine(Color(0x552AFFC3), Offset(x, 0f), Offset(x + wave, size.height), strokeWidth = 18f, cap = StrokeCap.Round)
                drawLine(Color(0x334D96FF), Offset(x + 24f, 0f), Offset(x + wave + 58f, size.height), strokeWidth = 10f, cap = StrokeCap.Round)
            }
        }
        BackgroundStyle.Orbital -> {
            repeat(7) { i ->
                val y = (size.height * 0.12f + i * 92f + t * 28f) % size.height
                drawLine(Color(0x4494A3B8), Offset(0f, y), Offset(size.width, y - 52f), strokeWidth = 2f)
            }
            drawCircle(Color(0x334338CA), size.width * 0.42f, Offset(size.width * 0.1f, size.height * 0.18f), style = Stroke(width = 7f))
        }
        BackgroundStyle.DebrisRing -> {
            repeat(26) { i ->
                val x = (i * 97f + t * (38f + i % 5)) % size.width
                val y = (i * 59f + t * (22f + i % 7)) % size.height
                rotate(degrees = (i * 31f + t * 40f) % 360f, pivot = Offset(x, y)) {
                    drawRoundRect(Color(0x5564748B), Offset(x - 8f, y - 3f), Size(18f + i % 5 * 4f, 6f), CornerRadius(2f, 2f))
                }
            }
        }
        BackgroundStyle.Wormhole -> {
            val center = Offset(size.width * 0.5f, size.height * 0.28f)
            repeat(9) { i ->
                val r = 46f + i * 34f + sin(t * 2f + i) * 8f
                drawCircle(Color(0x338A2BE2), r, center, style = Stroke(width = 5f))
            }
            drawCircle(Color(0x55111127), 42f, center)
        }
        BackgroundStyle.MoltenCore -> {
            repeat(10) { i ->
                val y = (i * 101f + t * (46f + i * 2f)) % size.height
                drawLine(Color(0x44FF6B00), Offset(size.width * (i % 4) / 4f, y), Offset(size.width * ((i % 4) + 1) / 4f, y + 70f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
            drawCircle(Color(0x33FF9F1C), size.width * 0.34f, Offset(size.width * 0.78f, size.height * 0.18f))
        }
        BackgroundStyle.DarkMatter -> {
            repeat(6) { i ->
                val r = size.width * (0.18f + i * 0.04f)
                drawCircle(Color(0x222D1B69), r, Offset(size.width * (0.2f + i * 0.13f), size.height * (0.22f + sin(t + i) * 0.08f)), style = Stroke(width = 10f))
            }
        }
        BackgroundStyle.FinalCore -> {
            val center = Offset(size.width * 0.5f, size.height * 0.22f)
            repeat(10) { i ->
                rotate(degrees = t * (18f + i * 3f) + i * 36f, pivot = center) {
                    drawLine(Color(0x44FF3B30), center, Offset(center.x, center.y - 72f - i * 22f), strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
            drawCircle(Color(0x55FFD166), 58f + sin(t * 3f) * 8f, center, style = Stroke(width = 7f))
            drawCircle(Color(0x66B00020), 34f, center)
        }
    }
}

private fun DrawScope.drawInfiniteBackground(stars: List<Star>, qualityLevel: QualityLevel) {
    val maxStars = when (qualityLevel) {
        QualityLevel.Smooth -> 56
        QualityLevel.Standard -> 88
        QualityLevel.High -> stars.size
    }.coerceAtMost(stars.size)
    for (index in 0 until maxStars) {
        val star = stars[index]
        drawCircle(
            color = Color.White.copy(alpha = star.alpha),
            radius = star.size,
            center = Offset(star.x, star.y),
        )
        if (qualityLevel != QualityLevel.Smooth && star.layer == 2) {
            drawLine(
                color = Color(0x66B8D8FF),
                start = Offset(star.x, star.y - star.size * 5f),
                end = Offset(star.x, star.y + star.size * 5f),
                strokeWidth = star.size,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawPlayer(player: Player) {
    if (player.invincible > 0f && ((player.invincible * 10).toInt() % 2 == 0)) return
    val x = player.x
    val y = player.y
    val r = player.radius
    val glow = Color(0xFF7DF9FF)
    val body = Path().apply {
        moveTo(x, y - r * 1.18f)
        lineTo(x - r * 0.34f, y - r * 0.34f)
        lineTo(x - r * 0.2f, y + r * 0.88f)
        lineTo(x, y + r * 0.58f)
        lineTo(x + r * 0.2f, y + r * 0.88f)
        lineTo(x + r * 0.34f, y - r * 0.34f)
        close()
    }
    val leftWing = Path().apply {
        moveTo(x - r * 0.26f, y - r * 0.18f)
        lineTo(x - r * 1.14f, y + r * 0.32f)
        lineTo(x - r * 0.88f, y + r * 0.72f)
        lineTo(x - r * 0.2f, y + r * 0.42f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + r * 0.26f, y - r * 0.18f)
        lineTo(x + r * 1.14f, y + r * 0.32f)
        lineTo(x + r * 0.88f, y + r * 0.72f)
        lineTo(x + r * 0.2f, y + r * 0.42f)
        close()
    }
    drawPath(leftWing, Brush.verticalGradient(listOf(Color(0xFF52677A), Color(0xFF1A2633))))
    drawPath(rightWing, Brush.verticalGradient(listOf(Color(0xFF52677A), Color(0xFF1A2633))))
    drawPath(body, Brush.verticalGradient(listOf(Color(0xFFB9F6FF), Color(0xFF1D4ED8), Color(0xFF0F172A))))
    drawPath(body, Color(0xAAE0F7FF), style = Stroke(width = 2.2f))
    drawRoundRect(Color(0xFF101827), Offset(x - r * 0.18f, y - r * 0.22f), Size(r * 0.36f, r * 0.72f), CornerRadius(6f, 6f))
    drawCircle(glow.copy(alpha = 0.34f), r * 0.34f, Offset(x, y - r * 0.2f))
    drawCircle(Color.White, r * 0.14f, Offset(x, y - r * 0.2f))
    drawLine(Color(0xFF111827), Offset(x - r * 0.82f, y + r * 0.34f), Offset(x - r * 1.03f, y + r * 0.73f), strokeWidth = 7f, cap = StrokeCap.Square)
    drawLine(Color(0xFF111827), Offset(x + r * 0.82f, y + r * 0.34f), Offset(x + r * 1.03f, y + r * 0.73f), strokeWidth = 7f, cap = StrokeCap.Square)
    drawLine(Color(0xFFFFD166), Offset(x - r * 0.48f, y - r * 0.1f), Offset(x - r * 0.48f, y - r * 0.5f), strokeWidth = 4.5f, cap = StrokeCap.Square)
    drawLine(Color(0xFFFFD166), Offset(x + r * 0.48f, y - r * 0.1f), Offset(x + r * 0.48f, y - r * 0.5f), strokeWidth = 4.5f, cap = StrokeCap.Square)
    drawLine(
        color = Color(0xFF80FFDB).copy(alpha = 0.82f),
        start = Offset(x - 8f, y + r * 0.68f),
        end = Offset(x - 8f, y + r * 1.45f),
        strokeWidth = 8f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFFFE66D).copy(alpha = 0.78f),
        start = Offset(x + 8f, y + r * 0.68f),
        end = Offset(x + 8f, y + r * 1.32f),
        strokeWidth = 8f,
        cap = StrokeCap.Round,
    )
    if (player.shield > 0f) {
        drawCircle(
            color = Color(0x663A86FF),
            radius = player.radius * 1.34f,
            center = Offset(player.x, player.y),
            style = Stroke(width = 5f),
        )
    }
}

private fun DrawScope.drawBullet(bullet: Bullet) {
    bullet.trail.zipWithNext().forEachIndexed { index, pair ->
        val alpha = (0.42f - index * 0.05f).coerceAtLeast(0.05f)
        drawLine(
            color = bulletColor(bullet).copy(alpha = alpha),
            start = pair.first,
            end = pair.second,
            strokeWidth = bullet.radius * 0.72f,
            cap = StrokeCap.Round,
        )
    }
    when (bullet.kind) {
        BulletKind.Laser -> {
            drawLine(
                color = bulletColor(bullet).copy(alpha = 0.88f),
                start = Offset(bullet.x, bullet.y + 70f),
                end = Offset(bullet.x, bullet.y - if (bullet.owner == BulletOwner.Player) 160f else -160f),
                strokeWidth = bullet.radius,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(bullet.x, bullet.y + 40f),
                end = Offset(bullet.x, bullet.y - if (bullet.owner == BulletOwner.Player) 120f else -120f),
                strokeWidth = bullet.radius * 0.35f,
                cap = StrokeCap.Round,
            )
        }
        BulletKind.Missile -> {
            drawCircle(Color(0xFFFF9F1C), bullet.radius, Offset(bullet.x, bullet.y))
            drawCircle(Color.White, bullet.radius * 0.45f, Offset(bullet.x, bullet.y))
        }
        else -> {
            drawCircle(bulletColor(bullet), bullet.radius, Offset(bullet.x, bullet.y))
            drawCircle(Color.White.copy(alpha = 0.75f), bullet.radius * 0.45f, Offset(bullet.x, bullet.y - bullet.radius * 0.25f))
        }
    }
}

private fun bulletColor(bullet: Bullet): Color {
    return when {
        bullet.owner == BulletOwner.Enemy && bullet.kind == BulletKind.Laser -> Color(0xFFFF3B30)
        bullet.owner == BulletOwner.Enemy -> Color(0xFFFF6B6B)
        bullet.kind == BulletKind.Laser -> Color(0xFF80FFDB)
        bullet.kind == BulletKind.Missile -> Color(0xFFFF9F1C)
        bullet.kind == BulletKind.Plasma -> Color(0xFFE0AAFF)
        else -> Color(0xFFFFF3B0)
    }
}

private fun DrawScope.drawEnemy(enemy: Enemy) {
    if (enemy.kind == EnemyKind.Boss) {
        drawBoss(enemy)
        return
    }
    val colors = when (enemy.kind) {
        EnemyKind.Scout -> listOf(Color(0xFFFFC857), Color(0xFFE85D04))
        EnemyKind.Hunter -> listOf(Color(0xFFFF7B9C), Color(0xFFC9184A))
        EnemyKind.Waver -> listOf(Color(0xFFB8F2E6), Color(0xFF2A9D8F))
        EnemyKind.Gunner -> listOf(Color(0xFFFFB703), Color(0xFF9D0208))
        EnemyKind.Tank -> listOf(Color(0xFFCDB4DB), Color(0xFF5A189A))
        EnemyKind.Boss -> listOf(Color.Red, Color.DarkGray)
    }
    rotate(degrees = 180f, pivot = Offset(enemy.x, enemy.y)) {
        val r = enemy.radius
        val x = enemy.x
        val y = enemy.y
        val wing = r * when (enemy.kind) {
            EnemyKind.Tank -> 1.18f
            EnemyKind.Gunner -> 1.05f
            else -> 0.92f
        }
        val leftArmor = Path().apply {
            moveTo(x - r * 0.28f, y - r * 0.24f)
            lineTo(x - wing, y + r * 0.34f)
            lineTo(x - wing * 0.74f, y + r * 0.66f)
            lineTo(x - r * 0.18f, y + r * 0.32f)
            close()
        }
        val rightArmor = Path().apply {
            moveTo(x + r * 0.28f, y - r * 0.24f)
            lineTo(x + wing, y + r * 0.34f)
            lineTo(x + wing * 0.74f, y + r * 0.66f)
            lineTo(x + r * 0.18f, y + r * 0.32f)
            close()
        }
        val body = Path().apply {
            moveTo(x, y - r * 1.05f)
            lineTo(x - r * 0.42f, y - r * 0.22f)
            lineTo(x - r * 0.28f, y + r * 0.62f)
            lineTo(x, y + r * 0.94f)
            lineTo(x + r * 0.28f, y + r * 0.62f)
            lineTo(x + r * 0.42f, y - r * 0.22f)
            close()
        }
        drawPath(leftArmor, Brush.verticalGradient(listOf(colors.first().copy(alpha = 0.92f), Color(0xFF161A22))))
        drawPath(rightArmor, Brush.verticalGradient(listOf(colors.first().copy(alpha = 0.92f), Color(0xFF161A22))))
        drawPath(body, Brush.verticalGradient(colors))
        drawPath(body, Color(0xDD1A1A1A), style = Stroke(width = 2f))
        drawRoundRect(Color(0xAA0B0F16), Offset(x - r * 0.22f, y - r * 0.18f), Size(r * 0.44f, r * 0.52f), CornerRadius(5f, 5f))
        drawCircle(Color(0xFFFFF1D0), r * 0.15f, Offset(x, y - r * 0.12f))
        val muzzleColor = if (enemy.kind == EnemyKind.Gunner || enemy.kind == EnemyKind.Tank) Color(0xFFFF3B30) else Color(0xFFFFD166)
        drawLine(muzzleColor, Offset(x - r * 0.46f, y + r * 0.28f), Offset(x - r * 0.46f, y + r * 0.72f), strokeWidth = 4f, cap = StrokeCap.Square)
        drawLine(muzzleColor, Offset(x + r * 0.46f, y + r * 0.28f), Offset(x + r * 0.46f, y + r * 0.72f), strokeWidth = 4f, cap = StrokeCap.Square)
        when (enemy.chapterId) {
            1 -> {
                drawLine(Color(0xFF7DD3FC), Offset(x - r * 0.72f, y + r * 0.54f), Offset(x - r * 1.02f, y + r * 0.9f), strokeWidth = 3f, cap = StrokeCap.Square)
                drawLine(Color(0xFF7DD3FC), Offset(x + r * 0.72f, y + r * 0.54f), Offset(x + r * 1.02f, y + r * 0.9f), strokeWidth = 3f, cap = StrokeCap.Square)
            }
            2 -> {
                drawCircle(Color(0xFFBAE6FD).copy(alpha = 0.45f), r * 0.22f, Offset(x - r * 0.62f, y + r * 0.1f), style = Stroke(width = 3f))
                drawCircle(Color(0xFFBAE6FD).copy(alpha = 0.45f), r * 0.22f, Offset(x + r * 0.62f, y + r * 0.1f), style = Stroke(width = 3f))
                drawLine(Color(0xFFE0F2FE), Offset(x, y - r * 0.72f), Offset(x, y - r * 1.08f), strokeWidth = 3f, cap = StrokeCap.Square)
            }
            3 -> {
                drawCircle(Color(0xFFA78BFA).copy(alpha = 0.5f), r * 0.86f, Offset(x, y + r * 0.08f), style = Stroke(width = 3f))
                drawRoundRect(Color(0xFF312E81), Offset(x - r * 0.12f, y + r * 0.58f), Size(r * 0.24f, r * 0.28f), CornerRadius(3f, 3f))
            }
            4 -> {
                drawLine(Color(0xFF5EEAD4), Offset(x - r * 0.86f, y + r * 0.22f), Offset(x - r * 0.54f, y - r * 0.42f), strokeWidth = 4f, cap = StrokeCap.Square)
                drawLine(Color(0xFF5EEAD4), Offset(x + r * 0.86f, y + r * 0.22f), Offset(x + r * 0.54f, y - r * 0.42f), strokeWidth = 4f, cap = StrokeCap.Square)
            }
            5 -> {
                drawCircle(Color(0xFF818CF8).copy(alpha = 0.55f), r * 0.72f, Offset(x, y + r * 0.04f), style = Stroke(width = 4f))
                drawCircle(Color(0xFF818CF8), r * 0.08f, Offset(x - r * 0.72f, y + r * 0.04f))
                drawCircle(Color(0xFF818CF8), r * 0.08f, Offset(x + r * 0.72f, y + r * 0.04f))
            }
            6 -> {
                drawRoundRect(Color(0xFF94A3B8), Offset(x - r * 0.98f, y + r * 0.44f), Size(r * 0.42f, r * 0.18f), CornerRadius(3f, 3f))
                drawRoundRect(Color(0xFF94A3B8), Offset(x + r * 0.56f, y + r * 0.44f), Size(r * 0.42f, r * 0.18f), CornerRadius(3f, 3f))
                drawLine(Color(0xFFE2E8F0), Offset(x - r * 0.2f, y - r * 0.86f), Offset(x + r * 0.2f, y - r * 0.86f), strokeWidth = 3f, cap = StrokeCap.Square)
            }
            7 -> {
                drawCircle(Color(0xFFC084FC).copy(alpha = 0.42f), r * 0.48f, Offset(x - r * 0.5f, y + r * 0.2f), style = Stroke(width = 4f))
                drawCircle(Color(0xFFC084FC).copy(alpha = 0.42f), r * 0.48f, Offset(x + r * 0.5f, y + r * 0.2f), style = Stroke(width = 4f))
            }
            8 -> {
                drawLine(Color(0xFFFF6B00), Offset(x - r * 0.62f, y - r * 0.22f), Offset(x - r * 1.08f, y + r * 0.12f), strokeWidth = 6f, cap = StrokeCap.Square)
                drawLine(Color(0xFFFF6B00), Offset(x + r * 0.62f, y - r * 0.22f), Offset(x + r * 1.08f, y + r * 0.12f), strokeWidth = 6f, cap = StrokeCap.Square)
                drawCircle(Color(0x66FFB703), r * 0.34f, Offset(x, y - r * 0.12f), style = Stroke(width = 4f))
            }
            9 -> {
                drawCircle(Color(0xFF111827), r * 0.38f, Offset(x, y - r * 0.1f), style = Stroke(width = 5f))
                drawLine(Color(0xFF6366F1), Offset(x - r * 0.9f, y + r * 0.56f), Offset(x + r * 0.9f, y + r * 0.56f), strokeWidth = 3f, cap = StrokeCap.Square)
            }
            else -> {
                val spikeColor = Color(0xFFFF3B30)
                drawLine(spikeColor, Offset(x - r * 0.88f, y + r * 0.18f), Offset(x - r * 1.22f, y - r * 0.08f), strokeWidth = 5f, cap = StrokeCap.Square)
                drawLine(spikeColor, Offset(x + r * 0.88f, y + r * 0.18f), Offset(x + r * 1.22f, y - r * 0.08f), strokeWidth = 5f, cap = StrokeCap.Square)
                drawCircle(Color(0x55FF3B30), r * 0.28f, Offset(x, y - r * 0.12f), style = Stroke(width = 4f))
            }
        }
    }
    drawEnemyHp(enemy)
}

private fun DrawScope.drawBoss(enemy: Enemy) {
    val x = enemy.x
    val y = enemy.y
    val r = enemy.radius
    drawCircle(Color(0x44000000), r * 1.18f, Offset(x, y + 10f))
    val hull = Path().apply {
        when (enemy.chapterId) {
            1 -> {
                moveTo(x, y - r * 0.9f)
                lineTo(x - r * 1.25f, y - r * 0.08f)
                lineTo(x - r * 1.05f, y + r * 0.42f)
                lineTo(x - r * 0.28f, y + r * 0.82f)
                lineTo(x, y + r * 0.48f)
                lineTo(x + r * 0.28f, y + r * 0.82f)
                lineTo(x + r * 1.05f, y + r * 0.42f)
                lineTo(x + r * 1.25f, y - r * 0.08f)
            }
            2 -> {
                moveTo(x - r * 0.55f, y - r * 0.82f)
                lineTo(x + r * 0.55f, y - r * 0.82f)
                lineTo(x + r * 1.18f, y - r * 0.18f)
                lineTo(x + r * 0.95f, y + r * 0.68f)
                lineTo(x + r * 0.32f, y + r * 0.9f)
                lineTo(x, y + r * 0.58f)
                lineTo(x - r * 0.32f, y + r * 0.9f)
                lineTo(x - r * 0.95f, y + r * 0.68f)
                lineTo(x - r * 1.18f, y - r * 0.18f)
            }
            3 -> {
                moveTo(x, y - r * 1.08f)
                lineTo(x - r * 0.62f, y - r * 0.52f)
                lineTo(x - r * 0.84f, y + r * 0.28f)
                lineTo(x - r * 0.22f, y + r * 0.96f)
                lineTo(x, y + r * 0.58f)
                lineTo(x + r * 0.22f, y + r * 0.96f)
                lineTo(x + r * 0.84f, y + r * 0.28f)
                lineTo(x + r * 0.62f, y - r * 0.52f)
            }
            4 -> {
                moveTo(x, y - r * 1.0f)
                lineTo(x - r * 0.8f, y - r * 0.62f)
                lineTo(x - r * 1.0f, y + r * 0.24f)
                lineTo(x - r * 0.42f, y + r * 0.86f)
                lineTo(x, y + r * 0.46f)
                lineTo(x + r * 0.42f, y + r * 0.86f)
                lineTo(x + r * 1.0f, y + r * 0.24f)
                lineTo(x + r * 0.8f, y - r * 0.62f)
            }
            5 -> {
                moveTo(x, y - r * 0.96f)
                lineTo(x - r * 0.38f, y - r * 0.7f)
                lineTo(x - r * 1.28f, y - r * 0.06f)
                lineTo(x - r * 0.7f, y + r * 0.22f)
                lineTo(x - r * 0.44f, y + r * 0.88f)
                lineTo(x, y + r * 0.54f)
                lineTo(x + r * 0.44f, y + r * 0.88f)
                lineTo(x + r * 0.7f, y + r * 0.22f)
                lineTo(x + r * 1.28f, y - r * 0.06f)
                lineTo(x + r * 0.38f, y - r * 0.7f)
            }
            6 -> {
                moveTo(x - r * 0.18f, y - r * 0.9f)
                lineTo(x - r * 1.08f, y - r * 0.46f)
                lineTo(x - r * 1.16f, y + r * 0.24f)
                lineTo(x - r * 0.58f, y + r * 0.7f)
                lineTo(x - r * 0.08f, y + r * 0.98f)
                lineTo(x + r * 0.54f, y + r * 0.7f)
                lineTo(x + r * 1.08f, y + r * 0.28f)
                lineTo(x + r * 0.86f, y - r * 0.44f)
                lineTo(x + r * 0.24f, y - r * 0.72f)
            }
            7 -> {
                moveTo(x, y - r * 1.12f)
                lineTo(x - r * 0.58f, y - r * 0.54f)
                lineTo(x - r * 1.12f, y - r * 0.64f)
                lineTo(x - r * 0.78f, y + r * 0.08f)
                lineTo(x - r * 1.04f, y + r * 0.74f)
                lineTo(x - r * 0.28f, y + r * 0.58f)
                lineTo(x, y + r * 1.0f)
                lineTo(x + r * 0.28f, y + r * 0.58f)
                lineTo(x + r * 1.04f, y + r * 0.74f)
                lineTo(x + r * 0.78f, y + r * 0.08f)
                lineTo(x + r * 1.12f, y - r * 0.64f)
                lineTo(x + r * 0.58f, y - r * 0.54f)
            }
            8 -> {
                moveTo(x, y - r * 0.88f)
                lineTo(x - r * 0.52f, y - r * 0.84f)
                lineTo(x - r * 1.22f, y - r * 0.16f)
                lineTo(x - r * 0.92f, y + r * 0.48f)
                lineTo(x - r * 0.18f, y + r * 0.98f)
                lineTo(x, y + r * 0.62f)
                lineTo(x + r * 0.18f, y + r * 0.98f)
                lineTo(x + r * 0.92f, y + r * 0.48f)
                lineTo(x + r * 1.22f, y - r * 0.16f)
                lineTo(x + r * 0.52f, y - r * 0.84f)
            }
            9 -> {
                moveTo(x, y - r * 1.18f)
                lineTo(x - r * 0.3f, y - r * 0.48f)
                lineTo(x - r * 1.24f, y - r * 0.34f)
                lineTo(x - r * 0.66f, y + r * 0.2f)
                lineTo(x - r * 0.94f, y + r * 0.86f)
                lineTo(x, y + r * 0.46f)
                lineTo(x + r * 0.94f, y + r * 0.86f)
                lineTo(x + r * 0.66f, y + r * 0.2f)
                lineTo(x + r * 1.24f, y - r * 0.34f)
                lineTo(x + r * 0.3f, y - r * 0.48f)
            }
            else -> {
                moveTo(x, y - r * 1.02f)
                lineTo(x - r * 0.34f, y - r * 0.62f)
                lineTo(x - r * 1.1f, y - r * 0.42f)
                lineTo(x - r * 0.72f, y + r * 0.02f)
                lineTo(x - r * 1.14f, y + r * 0.64f)
                lineTo(x - r * 0.36f, y + r * 0.78f)
                lineTo(x, y + r * 1.02f)
                lineTo(x + r * 0.36f, y + r * 0.78f)
                lineTo(x + r * 1.14f, y + r * 0.64f)
                lineTo(x + r * 0.72f, y + r * 0.02f)
                lineTo(x + r * 1.1f, y - r * 0.42f)
                lineTo(x + r * 0.34f, y - r * 0.62f)
            }
        }
        close()
    }
    val hullColors = if (enemy.finalBoss) {
        listOf(Color(0xFFFFD166), Color(0xFFD00000), Color(0xFF22000A))
    } else {
        listOf(Color(0xFFFF5A5F), Color(0xFF5A189A), Color(0xFF111827))
    }
    drawPath(hull, Brush.verticalGradient(hullColors))
    drawPath(hull, Color(0xEE111827), style = Stroke(width = 4f))
    if (enemy.vulnerable < 0f) {
        val pulse = 0.55f + sin(enemy.age * 9f) * 0.18f
        drawCircle(Color(0xFF8A5CF6).copy(alpha = pulse), r * 1.08f, Offset(x, y), style = Stroke(width = 7f))
        drawCircle(Color(0x553A86FF), r * 1.24f, Offset(x, y), style = Stroke(width = 3f))
    } else if (enemy.vulnerable > 0f) {
        val pulse = 0.48f + sin(enemy.age * 12f) * 0.20f
        drawCircle(Color(0xFFFFE66D).copy(alpha = pulse), r * 1.10f, Offset(x, y), style = Stroke(width = 6f))
    }
    drawRoundRect(Color(0xCC111827), Offset(x - r * 0.32f, y - r * 0.5f), Size(r * 0.64f, r * 0.98f), CornerRadius(10f, 10f))
    drawLine(Color(0xFF94A3B8), Offset(x - r * 0.28f, y - r * 0.25f), Offset(x + r * 0.28f, y - r * 0.25f), strokeWidth = 3f, cap = StrokeCap.Square)
    drawLine(Color(0xFF94A3B8), Offset(x - r * 0.24f, y + r * 0.08f), Offset(x + r * 0.24f, y + r * 0.08f), strokeWidth = 3f, cap = StrokeCap.Square)
    drawCircle(if (enemy.finalBoss) Color(0xFFFFFFFF) else Color(0xFFFFE66D), r * 0.22f, Offset(x, y - r * 0.18f))
    drawCircle((if (enemy.finalBoss) Color(0xFFFFD166) else Color(0xFF80FFDB)).copy(alpha = 0.36f), r * 0.36f, Offset(x, y - r * 0.18f), style = Stroke(width = 5f))
    listOf(-0.72f, -0.42f, 0.42f, 0.72f).forEach { lane ->
        val px = x + r * lane
        drawRoundRect(Color(0xDD0F172A), Offset(px - r * 0.08f, y + r * 0.05f), Size(r * 0.16f, r * 0.42f), CornerRadius(5f, 5f))
        drawCircle(Color(0xAAFF3B30), r * 0.09f, Offset(px, y + r * 0.16f))
    }
    if (enemy.finalBoss) {
        drawCircle(Color(0x77FFD166), r * 0.98f, Offset(x, y), style = Stroke(width = 6f))
        drawCircle(Color(0x55FF3B30), r * 1.14f, Offset(x, y), style = Stroke(width = 3f))
    } else if (enemy.chapterId == 9) {
        drawCircle(Color(0x66312E81), r * 1.08f, Offset(x, y), style = Stroke(width = 5f))
        drawCircle(Color(0xFF6366F1), r * 0.08f, Offset(x - r * 1.03f, y + r * 0.1f))
        drawCircle(Color(0xFF6366F1), r * 0.08f, Offset(x + r * 1.03f, y + r * 0.1f))
    } else if (enemy.chapterId == 8) {
        drawLine(Color(0xFFFF6B00), Offset(x - r * 0.82f, y + r * 0.55f), Offset(x - r * 1.22f, y + r * 0.95f), strokeWidth = 7f, cap = StrokeCap.Square)
        drawLine(Color(0xFFFF6B00), Offset(x + r * 0.82f, y + r * 0.55f), Offset(x + r * 1.22f, y + r * 0.95f), strokeWidth = 7f, cap = StrokeCap.Square)
    } else if (enemy.chapterId == 7) {
        drawCircle(Color(0x66C084FC), r * 0.54f, Offset(x - r * 0.72f, y + r * 0.08f), style = Stroke(width = 5f))
        drawCircle(Color(0x66C084FC), r * 0.54f, Offset(x + r * 0.72f, y + r * 0.08f), style = Stroke(width = 5f))
    } else if (enemy.chapterId == 6) {
        repeat(4) { index ->
            val px = x + (index - 1.5f) * r * 0.38f
            drawRoundRect(Color(0xFF475569), Offset(px - r * 0.08f, y + r * 0.78f), Size(r * 0.16f, r * 0.28f), CornerRadius(4f, 4f))
        }
    } else if (enemy.chapterId == 5) {
        drawCircle(Color(0x66818CF8), r * 0.95f, Offset(x, y), style = Stroke(width = 5f))
    } else if (enemy.chapterId == 4) {
        drawLine(Color(0xFF5EEAD4), Offset(x - r * 0.9f, y - r * 0.16f), Offset(x + r * 0.9f, y - r * 0.16f), strokeWidth = 4f, cap = StrokeCap.Square)
    } else if (enemy.chapterId == 3) {
        drawCircle(Color(0x66A78BFA), r * 0.9f, Offset(x, y), style = Stroke(width = 4f))
    } else if (enemy.chapterId == 2) {
        drawRoundRect(Color(0x6687CEEB), Offset(x - r * 0.92f, y - r * 0.05f), Size(r * 1.84f, r * 0.18f), CornerRadius(8f, 8f))
    }
}

private fun DrawScope.drawEnemyHp(enemy: Enemy) {
    if (enemy.hp == enemy.maxHp) return
    val w = enemy.radius * 1.6f
    val top = enemy.y - enemy.radius - 13f
    drawRoundRect(Color(0xAA000000), Offset(enemy.x - w / 2f, top), Size(w, 6f), CornerRadius(4f, 4f))
    drawRoundRect(Color(0xFFFFE66D), Offset(enemy.x - w / 2f, top), Size(w * enemy.hp / enemy.maxHp, 6f), CornerRadius(4f, 4f))
}

private fun DrawScope.drawPowerUp(power: PowerUp) {
    val pulse = 1f + sin(power.age * 6f) * 0.12f
    drawCircle(GameConfig.powerUpColor(power.kind).copy(alpha = 0.28f), 27f * pulse, Offset(power.x, power.y))
    drawCircle(GameConfig.powerUpColor(power.kind), 18f * pulse, Offset(power.x, power.y))
    val text = when (power.kind) {
        PowerUpKind.DoubleShot -> "2"
        PowerUpKind.Laser -> "L"
        PowerUpKind.Missile -> "M"
        PowerUpKind.Shield -> "S"
        PowerUpKind.Invincible -> "I"
        PowerUpKind.Heal -> "+"
    }
    drawContext.canvas.nativeCanvas.drawText(
        text,
        power.x,
        power.y + 7f,
        powerUpTextPaint,
    )
}

private fun DrawScope.drawParticle(particle: Particle) {
    val alpha = (1f - particle.age / particle.maxAge).coerceIn(0f, 1f)
    drawCircle(particle.color.copy(alpha = alpha), particle.radius * alpha.coerceAtLeast(0.25f), Offset(particle.x, particle.y))
}

private fun DrawScope.drawExplosion(explosion: Explosion) {
    val progress = explosion.age / explosion.maxAge
    val radius = (if (explosion.large) 82f else 34f) * (0.35f + progress)
    val alpha = 1f - progress
    drawCircle(Color(0xFFFFD166).copy(alpha = alpha * 0.45f), radius * 0.72f, Offset(explosion.x, explosion.y))
    drawCircle(Color(0xFFFF3B30).copy(alpha = alpha * 0.42f), radius, Offset(explosion.x, explosion.y), style = Stroke(width = if (explosion.large) 9f else 5f))
    repeat(if (explosion.large) 14 else 8) { i ->
        val a = (PI * 2.0 * i / if (explosion.large) 14 else 8).toFloat()
        drawLine(
            color = Color(0xFFFF9F1C).copy(alpha = alpha),
            start = Offset(explosion.x + cos(a) * radius * 0.25f, explosion.y + sin(a) * radius * 0.25f),
            end = Offset(explosion.x + cos(a) * radius * 1.1f, explosion.y + sin(a) * radius * 1.1f),
            strokeWidth = if (explosion.large) 7f else 4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawUltimateBlast(blast: UltimateBlast) {
    val progress = (blast.age / blast.maxAge).coerceIn(0f, 1f)
    val alpha = 1f - progress
    val color = if (blast.playerOwned) Color(0xFF80FFDB) else Color(0xFFFF3B30)
    val core = if (blast.playerOwned) Color.White else Color(0xFFFFD166)
    drawCircle(
        color.copy(alpha = alpha * 0.24f),
        radius = blast.radius * (0.25f + progress * 0.85f),
        center = Offset(blast.x, blast.y),
    )
    drawCircle(
        color.copy(alpha = alpha * 0.70f),
        radius = blast.radius * (0.18f + progress * 0.78f),
        center = Offset(blast.x, blast.y),
        style = Stroke(width = if (blast.playerOwned) 9f else 6f),
    )
    repeat(if (blast.playerOwned) 18 else 12) { i ->
        val a = (PI * 2.0 * i / if (blast.playerOwned) 18 else 12).toFloat()
        drawLine(
            color = core.copy(alpha = alpha * 0.72f),
            start = Offset(blast.x + cos(a) * blast.radius * 0.08f, blast.y + sin(a) * blast.radius * 0.08f),
            end = Offset(blast.x + cos(a) * blast.radius * (0.42f + progress * 0.48f), blast.y + sin(a) * blast.radius * (0.42f + progress * 0.48f)),
            strokeWidth = if (blast.playerOwned) 5f else 4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawFloatingText(text: FloatingText) {
    val alpha = (1f - text.age / 0.9f).coerceIn(0f, 1f)
    floatingTextPaint.color = text.color.copy(alpha = alpha).toArgbCompat()
    drawContext.canvas.nativeCanvas.drawText(
        text.text,
        text.x,
        text.y,
        floatingTextPaint,
    )
}

private fun DrawScope.drawBossUltimateWarning(time: Float, label: String) {
    val pulse = if ((time * 10f).toInt() % 2 == 0) 0.75f else 0.34f
    drawRect(Color(0xAA8B0000).copy(alpha = pulse * 0.28f))
    drawLine(Color(0xFFFF3B30).copy(alpha = pulse), Offset(0f, size.height * 0.24f), Offset(size.width, size.height * 0.24f), strokeWidth = 3f)
    drawLine(Color(0xFFFFD166).copy(alpha = pulse * 0.65f), Offset(0f, size.height * 0.29f), Offset(size.width, size.height * 0.29f), strokeWidth = 1.4f)
    drawContext.canvas.nativeCanvas.drawText(
        label.ifBlank { "高能反应" },
        size.width / 2f,
        size.height * 0.27f,
        bossWarningPaint.apply {
            color = android.graphics.Color.WHITE
            textSize = min(size.width * 0.062f, 34f)
        },
    )
}

private fun DrawScope.drawBossWarning(time: Float, chapter: ChapterSpec) {
    val alpha = if ((time * 6f).toInt() % 2 == 0) 0.8f else 0.28f
    drawRect(Color(0xAA8B0000).copy(alpha = alpha * 0.45f))
    drawContext.canvas.nativeCanvas.drawText(
        when {
            chapter.finalChapter -> "FINAL BOSS"
            chapter.easterBoss -> "SECRET BOSS"
            else -> "BOSS WARNING"
        },
        size.width / 2f,
        size.height * 0.45f,
        bossWarningPaint.apply {
            textSize = min(size.width * 0.095f, 48f)
        },
    )
    drawContext.canvas.nativeCanvas.drawText(
        chapter.bossName,
        size.width / 2f,
        size.height * 0.45f + 44f,
        bossWarningPaint.apply {
            textSize = min(size.width * 0.055f, 30f)
        },
    )
}

private fun Color.toArgbCompat(): Int {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(a, r, g, b)
}
