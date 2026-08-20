package com.example.gametest

import android.os.Bundle
import android.graphics.Paint
import android.graphics.Typeface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.gametest.ui.theme.GameTestTheme
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val TARGET_FRAME_NANOS = 16_666_667L

private val starshipTitleTypeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
private val starshipTitleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = starshipTitleTypeface
    textAlign = Paint.Align.CENTER
    textScaleX = 0.96f
    isFakeBoldText = true
}
private val starshipTitleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = starshipTitleTypeface
    textAlign = Paint.Align.CENTER
    textScaleX = 0.96f
    style = Paint.Style.STROKE
    strokeWidth = 7.2f
    isFakeBoldText = true
}
private val starshipTitleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = starshipTitleTypeface
    textAlign = Paint.Align.CENTER
    textScaleX = 0.96f
    style = Paint.Style.FILL
    isFakeBoldText = true
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            GameTestTheme(dynamicColor = false, darkTheme = true) {
                AirBattleGame()
            }
        }
    }
}

@Composable
private fun AirBattleGame() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val audio = remember { GameAudio(context.applicationContext) }
    val music = remember { GameMusic(context.applicationContext) }
    val density = LocalDensity.current
    val topSafe = with(density) { 38.dp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastFrameNanos by remember { mutableLongStateOf(0L) }
    var game by remember {
        mutableStateOf(
            GameState(
                highScore = readHighScore(context),
                unlockedChapter = readUnlockedChapter(context),
                playerProgress = readPlayerProgress(context),
                vibrationEnabled = readVibrationEnabled(context),
                soundEnabled = readSoundEnabled(context),
                musicEnabled = readMusicEnabled(context),
                soundVolume = readSoundVolume(context),
                musicVolume = readMusicVolume(context),
                qualityLevel = readQualityLevel(context),
            ),
        )
    }

    DisposableEffect(audio, music) {
        onDispose {
            audio.release()
            music.release()
        }
    }

    DisposableEffect(context, music) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        if (lifecycle == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> music.pauseForLifecycle()
                    Lifecycle.Event.ON_START -> music.resumeFromLifecycle()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(game.musicEnabled) {
        music.setEnabled(game.musicEnabled)
    }

    LaunchedEffect(game.soundVolume) {
        audio.setVolume(game.soundVolume)
    }

    LaunchedEffect(game.musicVolume) {
        music.setVolume(game.musicVolume)
    }

    LaunchedEffect(game.screen, game.soundEnabled) {
        if (game.screen == GameScreen.Menu && game.soundEnabled) {
            audio.playMenuBoot()
        }
    }

    LaunchedEffect(game.screen, game.phase, game.enemies, game.musicEnabled, game.infiniteRound, game.mode) {
        music.play(game.musicTrack())
    }

    fun startGame(mode: GameMode, chapter: Int = 1) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        game = createInitialState(
            width = canvasSize.width.toFloat(),
            height = canvasSize.height.toFloat(),
            mode = mode,
            chapter = chapter,
            highScore = readHighScore(context),
            unlockedChapter = readUnlockedChapter(context),
            vibrationEnabled = readVibrationEnabled(context),
            soundEnabled = readSoundEnabled(context),
            musicEnabled = readMusicEnabled(context),
            soundVolume = readSoundVolume(context),
            musicVolume = readMusicVolume(context),
            qualityLevel = readQualityLevel(context),
            playerProgress = readPlayerProgress(context),
        )
        lastFrameNanos = 0L
    }

    fun requestUltimateByDoubleTap(offset: Offset) {
        if (game.screen != GameScreen.Playing) return
        val distance = hypot((offset.x - game.player.x).toDouble(), (offset.y - game.player.y).toDouble()).toFloat()
        val doubleTapHotZone = max(game.player.radius * 3f, with(density) { 72.dp.toPx() })
        if (distance <= doubleTapHotZone) {
            game = game.requestUltimate()
        }
    }

    LaunchedEffect(canvasSize, game.screen, game.qualityLevel) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || game.screen != GameScreen.Playing) {
            lastFrameNanos = 0L
            return@LaunchedEffect
        }
        while (isActive) {
            withFrameNanos { nanos ->
                val last = lastFrameNanos
                if (last != 0L && nanos - last < TARGET_FRAME_NANOS) return@withFrameNanos
                lastFrameNanos = nanos
                if (last == 0L) return@withFrameNanos
                val dt = ((nanos - last) / 1_000_000_000f).coerceIn(0f, 0.033f)
                val before = game
                val next = before.step(dt, canvasSize.width.toFloat(), canvasSize.height.toFloat(), topSafe)
                if (next.score > next.highScore) saveHighScore(context, next.score)
                if (next.mode == GameMode.Chapter && next.screen == GameScreen.ChapterClear && before.screen != GameScreen.ChapterClear) {
                    saveClearedChapter(context, max(readClearedChapter(context), next.chapter))
                    val unlocked = if (ChapterCatalog.spec(next.chapter).finalChapter) {
                        next.chapter
                    } else {
                        (next.chapter + 1).coerceAtMost(ChapterCatalog.FINAL_CHAPTER)
                    }
                    saveUnlockedChapter(context, max(next.unlockedChapter, unlocked))
                }
                if (next.vibrationEnabled && (next.shake > before.shake + 2f || next.player.hp < before.player.hp)) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                if (next.soundEnabled) {
                    playStateAudio(audio, before, next)
                }
                game = next.copy(
                    highScore = max(next.highScore, next.score),
                    unlockedChapter = max(next.unlockedChapter, readUnlockedChapter(context)),
                    playerProgress = readPlayerProgress(context),
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06111D))
            .onSizeChanged {
                val firstSize = canvasSize == IntSize.Zero
                canvasSize = it
                if (firstSize && it.width > 0 && it.height > 0) {
                    game = game.copy(stars = createStars(it.width.toFloat(), it.height.toFloat()))
                }
            }
            .pointerInput(game.screen, game.player.x, game.player.y, game.player.ultimateEnergy, game.player.ultimateCooldown) {
                detectTapGestures(
                    onDoubleTap = { offset -> requestUltimateByDoubleTap(offset) },
                )
            }
            .pointerInput(game.screen) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (game.screen == GameScreen.Playing) game = game.movePlayer(offset, canvasSize)
                    },
                    onDrag = { change, _ ->
                        if (game.screen == GameScreen.Playing) game = game.movePlayer(change.position, canvasSize)
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGame(game)
        }

        when (game.screen) {
            GameScreen.Menu -> MenuOverlay(
                highScore = game.highScore,
                unlockedChapter = game.unlockedChapter,
                playerProgress = game.playerProgress,
                onInfinite = { startGame(GameMode.Infinite) },
                onChapter = { startGame(GameMode.Chapter, game.unlockedChapter) },
                onChapterSelect = { game = game.copy(screen = GameScreen.ChapterSelect) },
                onBossRush = { startGame(GameMode.BossRush) },
                onSettings = { game = game.copy(screen = GameScreen.Settings) },
            )

            GameScreen.ChapterSelect -> ChapterSelectOverlay(
                unlockedChapter = game.unlockedChapter,
                playerProgress = game.playerProgress,
                onSelect = { startGame(GameMode.Chapter, it) },
                onBack = { game = game.copy(screen = GameScreen.Menu) },
            )

            GameScreen.Settings -> SettingsOverlay(
                vibrationEnabled = game.vibrationEnabled,
                soundEnabled = game.soundEnabled,
                musicEnabled = game.musicEnabled,
                soundVolume = game.soundVolume,
                musicVolume = game.musicVolume,
                qualityLevel = game.qualityLevel,
                onVibrationChange = {
                    saveVibrationEnabled(context, it)
                    game = game.copy(vibrationEnabled = it)
                },
                onSoundChange = {
                    saveSoundEnabled(context, it)
                    game = game.copy(soundEnabled = it)
                },
                onMusicChange = {
                    saveMusicEnabled(context, it)
                    game = game.copy(musicEnabled = it)
                },
                onSoundVolumeChange = {
                    saveSoundVolume(context, it)
                    game = game.copy(soundVolume = it)
                },
                onMusicVolumeChange = {
                    saveMusicVolume(context, it)
                    game = game.copy(musicVolume = it)
                },
                onQualityChange = {
                    saveQualityLevel(context, it)
                    game = game.copy(qualityLevel = it)
                },
                onBack = { game = game.copy(screen = GameScreen.Menu) },
            )

            GameScreen.Playing -> {
                PlayingHud(
                    game = game,
                    onPause = { game = game.copy(screen = GameScreen.Paused) },
                )
                UltimateButton(
                    player = game.player,
                    onClick = { game = game.requestUltimate() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 24.dp),
                )
            }

            GameScreen.Paused -> {
                PlayingHud(game = game, onPause = { game = game.copy(screen = GameScreen.Playing) })
                PauseOverlay(
                    score = game.score,
                    onResume = { game = game.copy(screen = GameScreen.Playing) },
                    onRestart = { startGame(game.mode, game.chapter) },
                    onMenu = { game = game.copy(screen = GameScreen.Menu) },
                )
            }

            GameScreen.GameOver -> EndOverlay(
                title = "任务失败",
                score = game.score,
                highScore = game.highScore,
                onRestart = { startGame(game.mode, game.chapter) },
                onMenu = { game = game.copy(screen = GameScreen.Menu) },
            )

            GameScreen.ChapterClear -> EndOverlay(
                title = when {
                    game.mode == GameMode.BossRush -> "连战完成"
                    ChapterCatalog.spec(game.chapter).finalChapter -> "最终胜利"
                    else -> "章节完成"
                },
                score = game.score,
                highScore = game.highScore,
                subTitleExtra = if (game.mode == GameMode.BossRush) {
                    "${ChapterCatalog.FINAL_CHAPTER}舰击破 · 虚空核心已沉默"
                } else {
                    ChapterCatalog.reward(game.chapter).let { "战利品：${it.title} · ${it.description}" }
                },
                onRestart = {
                    if (game.mode == GameMode.BossRush) {
                        startGame(GameMode.BossRush)
                    } else {
                        val nextChapter = if (ChapterCatalog.spec(game.chapter).finalChapter) {
                            game.chapter
                        } else {
                            (game.chapter + 1).coerceAtMost(ChapterCatalog.FINAL_CHAPTER)
                        }
                        startGame(GameMode.Chapter, nextChapter)
                    }
                },
                onMenu = { game = game.copy(screen = GameScreen.Menu) },
                restartText = when {
                    game.mode == GameMode.BossRush -> "再来一轮"
                    ChapterCatalog.spec(game.chapter).finalChapter -> "重战终章"
                    else -> "下一章"
                },
            )
        }
    }
}

@Composable
private fun UltimateButton(player: Player, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ready = player.ultimateEnergy >= 100f && player.ultimateCooldown <= 0f
    val cooldown = player.ultimateCooldown > 0f
    val energyRate = (player.ultimateEnergy / 100f).coerceIn(0f, 1f)
    val color = when {
        ready -> Color(0xFF80FFDB)
        cooldown -> Color(0xFF6B7280)
        else -> Color(0xFF3A86FF)
    }
    Button(
        onClick = onClick,
        enabled = ready,
        modifier = modifier.size(76.dp),
        shape = RoundedCornerShape(38.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.5.dp, color.copy(alpha = if (ready) 0.95f else 0.55f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xDD07131F),
            disabledContainerColor = Color(0xCC111827),
            disabledContentColor = Color(0xFFC7D9EE),
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color.copy(alpha = 0.10f + energyRate * 0.16f), radius = size.minDimension * 0.46f, center = center)
                drawArc(
                    color = color.copy(alpha = 0.85f),
                    startAngle = -90f,
                    sweepAngle = 360f * energyRate,
                    useCenter = false,
                    topLeft = Offset(6f, 6f),
                    size = Size(size.width - 12f, size.height - 12f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
                )
                if (ready) {
                    drawCircle(Color(0x3380FFDB), radius = size.minDimension * 0.55f, center = center, style = Stroke(width = 2f))
                }
            }
            Text(
                text = when {
                    cooldown -> "${player.ultimateCooldown.toInt() + 1}s"
                    ready -> "轰击"
                    else -> "${(energyRate * 100).toInt()}%"
                },
                color = if (ready) Color.White else Color(0xFFC7D9EE),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ElectricTitle() {
    val transition = rememberInfiniteTransition(label = "electricTitle")
    val spark by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(820), RepeatMode.Restart),
        label = "spark",
    )
    val flicker by transition.animateFloat(
        initialValue = 0.74f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(140), RepeatMode.Reverse),
        label = "flicker",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val left = size.width * 0.18f
            val right = size.width * 0.82f
            repeat(3) { ring ->
                drawCircle(
                    Color(0xFF80FFDB).copy(alpha = (0.12f - ring * 0.025f) * flicker),
                    radius = 96f + ring * 32f + spark * 22f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2f),
                )
            }
            repeat(2) { lane ->
                val yBase = cy + if (lane == 0) -50f else 50f
                var last = Offset(left, yBase)
                repeat(12) { index ->
                    val t = index / 11f
                    val x = left + (right - left) * t
                    val jitter = sin((index * 1.7f + spark * 9f + lane) * PI.toFloat()) * (5f + lane * 2f)
                    val next = Offset(x, yBase + jitter)
                    drawLine(
                        Color(0xFF80FFDB).copy(alpha = 0.36f + 0.32f * flicker),
                        last,
                        next,
                        strokeWidth = if (index % 3 == 0) 2.5f else 1.3f,
                        cap = StrokeCap.Round,
                    )
                    if ((index + lane) % 5 == 0) {
                        drawLine(
                            Color(0xFFFFE66D).copy(alpha = 0.42f),
                            next,
                            Offset(next.x + 9f, next.y + if (lane == 0) 13f else -13f),
                            strokeWidth = 1.4f,
                        )
                    }
                    last = next
                }
            }
            val scanY = cy - 58f + spark * 116f
            drawLine(Color(0x553A86FF), Offset(left, scanY), Offset(right, scanY), strokeWidth = 1.2f)
            drawLine(Color(0xAA80FFDB), Offset(left - 10f, cy - 56f), Offset(left + 68f, cy - 56f), strokeWidth = 3.4f)
            drawLine(Color(0xAA80FFDB), Offset(right - 68f, cy + 56f), Offset(right + 10f, cy + 56f), strokeWidth = 3.4f)
            drawLine(Color(0x88FFE66D), Offset(cx - 170f, cy + 51f), Offset(cx + 170f, cy + 51f), strokeWidth = 1.8f)

            val title = "星舰终局：虚空核心"
            val baseline = cy + 20f
            val canvas = drawContext.canvas.nativeCanvas
            fun fittedTitleSize(text: String, preferred: Float, minSize: Float): Float {
                starshipTitleFillPaint.textSize = preferred
                val maxWidth = size.width * 0.96f
                val measured = starshipTitleFillPaint.measureText(text).coerceAtLeast(1f)
                return min(preferred, preferred * maxWidth / measured).coerceAtLeast(minSize)
            }
            fun drawStarshipTitleLine(text: String, baseline: Float, textSize: Float) {
                starshipTitleGlowPaint.textSize = textSize
                starshipTitleGlowPaint.color = android.graphics.Color.argb((58 * flicker).toInt().coerceIn(0, 255), 58, 134, 255)
                canvas.drawText(text, cx + 4f, baseline + 4f, starshipTitleGlowPaint)
                starshipTitleGlowPaint.color = android.graphics.Color.argb((44 + 40 * flicker).toInt().coerceIn(0, 255), 128, 255, 219)
                canvas.drawText(text, cx - 3f, baseline - 2f, starshipTitleGlowPaint)

                starshipTitleStrokePaint.textSize = textSize
                starshipTitleStrokePaint.color = android.graphics.Color.argb(230, 9, 22, 36)
                canvas.drawText(text, cx, baseline, starshipTitleStrokePaint)
                starshipTitleStrokePaint.strokeWidth = 2.2f
                starshipTitleStrokePaint.color = android.graphics.Color.argb(210, 128, 255, 219)
                canvas.drawText(text, cx, baseline, starshipTitleStrokePaint)
                starshipTitleStrokePaint.strokeWidth = 7.2f

                starshipTitleFillPaint.textSize = textSize
                starshipTitleFillPaint.color = android.graphics.Color.argb((232 + 23 * flicker).toInt().coerceIn(0, 255), 238, 248, 255)
                canvas.drawText(text, cx, baseline, starshipTitleFillPaint)
            }
            val titleSize = fittedTitleSize(title, 58f, 38f)
            drawStarshipTitleLine(title, baseline, titleSize)

            drawLine(Color(0xDD06111D), Offset(cx - 214f, baseline - 28f), Offset(cx + 210f, baseline - 36f), strokeWidth = 3.2f)
            drawLine(Color(0xAA3A86FF), Offset(cx - 222f + spark * 50f, baseline - 3f), Offset(cx - 72f + spark * 50f, baseline - 9f), strokeWidth = 2.2f)
            drawLine(Color(0xAA80FFDB), Offset(cx + 68f - spark * 46f, baseline + 18f), Offset(cx + 220f - spark * 46f, baseline + 10f), strokeWidth = 2.2f)
        }
    }
}

@Composable
private fun MenuOverlay(
    highScore: Int,
    unlockedChapter: Int,
    playerProgress: PlayerProgress,
    onInfinite: () -> Unit,
    onChapter: () -> Unit,
    onChapterSelect: () -> Unit,
    onBossRush: () -> Unit,
    onSettings: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "menu")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Restart),
        label = "menuScan",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 38.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * scan
            drawLine(Color(0x3380FFDB), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
            drawLine(Color(0x223A86FF), Offset(size.width * 0.08f, 0f), Offset(size.width * 0.92f, size.height), strokeWidth = 1f)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))
            ElectricTitle()
            Text(
                "STARSHIP COMMAND // DEEP SPACE STRIKE",
                color = Color(0xFF80FFDB),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(18.dp))
            MenuStatusPanel(highScore, unlockedChapter, playerProgress)
            Spacer(Modifier.height(18.dp))
            ModeCard("无限模式", "20舰循环 · 彩蛋节点 · 无限 Boss", Color(0xFF2A9D8F), onInfinite)
            ModeCard("继续章节", "第 $unlockedChapter 章 · ${ChapterCatalog.spec(unlockedChapter).name}", Color(0xFF3A86FF), onChapter)
            ModeCard("Boss连战", "20舰连破 · 彩蛋王牌 · 终章核心", Color(0xFFE85D04), onBossRush)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    MenuButton("章节选择", onChapterSelect, Color(0xFF4361EE))
                }
                Box(Modifier.weight(1f)) {
                    MenuButton("设置", onSettings, Color(0xFF3A506B))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "越往深空，火力越密。终章目标：虚空核心。",
                color = Color(0xFFC7D9EE),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun MenuStatusPanel(highScore: Int, unlockedChapter: Int, playerProgress: PlayerProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x9908121F), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BEST $highScore", color = Color(0xFFFFE66D), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("CH-$unlockedChapter ${ChapterCatalog.spec(unlockedChapter).name}", color = Color(0xFF80FFDB), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(playerProgress.title, Color(0xFFB8D8FF))
            StatChip("主炮 ${playerProgress.weaponLevel}", Color(0xFF3A86FF))
            StatChip("激光 ${playerProgress.laserLevel}", Color(0xFF80FFDB))
            StatChip("导弹 ${playerProgress.missileLevel}", Color(0xFFFF9F1C))
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0x7710192A), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun ModeCard(title: String, subTitle: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.72f)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD07131F)),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(color.copy(alpha = 0.10f), size = size)
                drawRect(
                    Brush.horizontalGradient(listOf(color.copy(alpha = 0.24f), Color.Transparent)),
                    size = Size(size.width * 0.42f, size.height),
                )
                drawLine(color.copy(alpha = 0.75f), Offset(0f, 0f), Offset(size.width * 0.22f, 0f), strokeWidth = 3f)
                drawLine(color.copy(alpha = 0.45f), Offset(12f, size.height - 12f), Offset(size.width - 12f, size.height - 12f), strokeWidth = 1.2f)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 18.dp),
            ) {
                Text(title, color = Color.White, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                Text(subTitle, color = Color(0xFFC7D9EE), fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 15.sp)
            }
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(22.dp),
            ) {
                val path = Path().apply {
                    moveTo(2f, 2f)
                    lineTo(size.width - 2f, size.height / 2f)
                    lineTo(2f, size.height - 2f)
                }
                drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun ChapterSelectOverlay(
    unlockedChapter: Int,
    playerProgress: PlayerProgress,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    CenterPanel(title = "章节选择", subTitle = "${playerProgress.title} · 带着战利品继续推进") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            (1..ChapterCatalog.FINAL_CHAPTER).forEach { chapter ->
                val spec = ChapterCatalog.spec(chapter)
                val reward = ChapterCatalog.reward(chapter)
                val unlocked = chapter <= unlockedChapter
                val status = when {
                    spec.finalChapter && unlocked -> "终章 · ${spec.bossName}"
                    spec.finalChapter -> "终章 · 未解锁"
                    spec.easterBoss && unlocked -> "彩蛋 · ${spec.bossName}"
                    spec.easterBoss -> "彩蛋 · 未解锁"
                    chapter < unlockedChapter -> "已通关 · ${spec.bossName}"
                    chapter == unlockedChapter -> "当前进度 · ${spec.bossName}"
                    else -> "未解锁"
                }
                MenuButton(
                    text = "第 $chapter 章  ${spec.name}\n$status · ${reward.title}",
                    onClick = { onSelect(chapter) },
                    color = when {
                        spec.finalChapter -> Color(0xFF9D0208)
                        spec.easterBoss -> Color(0xFF8A5CF6)
                        else -> Color(0xFF3A86FF)
                    },
                    enabled = unlocked,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        MenuButton("返回", onBack, Color(0xFF3A506B))
    }
}

@Composable
private fun SettingsOverlay(
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    soundVolume: Float,
    musicVolume: Float,
    qualityLevel: QualityLevel,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onMusicChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onQualityChange: (QualityLevel) -> Unit,
    onBack: () -> Unit,
) {
    CenterPanel(title = "设置", subTitle = "按你的手感来") {
        QualitySelector(qualityLevel, onQualityChange)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("背景音乐", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("战斗音乐", color = Color(0xFFB8D8FF), fontSize = 12.sp)
            }
            Switch(
                checked = musicEnabled,
                onCheckedChange = onMusicChange,
            )
        }
        VolumeSlider("音乐音量", musicVolume, onMusicVolumeChange)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("音效", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("射击、爆炸、拾取、警报", color = Color(0xFFB8D8FF), fontSize = 12.sp)
            }
            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundChange,
            )
        }
        VolumeSlider("音效音量", soundVolume, onSoundVolumeChange)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("屏幕震动", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("受击和爆炸反馈", color = Color(0xFFB8D8FF), fontSize = 12.sp)
            }
            Switch(
                checked = vibrationEnabled,
                onCheckedChange = onVibrationChange,
            )
        }
        Spacer(Modifier.height(12.dp))
        MenuButton("返回", onBack, Color(0xFF3A506B))
    }
}

@Composable
private fun QualitySelector(value: QualityLevel, onChange: (QualityLevel) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text("性能", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("帧率稳定优先", color = Color(0xFFB8D8FF), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QualityLevel.entries.forEach { level ->
                val selected = level == value
                Button(
                    onClick = { onChange(level) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) Color(0xFF80FFDB) else Color(0xFF31506C),
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF123244) else Color(0xDD07131F),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(level.label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold)
                }
            }
        }
    }
}

private val QualityLevel.label: String
    get() = when (this) {
        QualityLevel.Smooth -> "流畅"
        QualityLevel.Standard -> "标准"
        QualityLevel.High -> "高"
    }

@Composable
private fun VolumeSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", color = Color(0xFFB8D8FF), fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
        )
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit, color: Color, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(90),
        label = "buttonScale",
    )
    val transition = rememberInfiniteTransition(label = "buttonPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "buttonGlow",
    )
    val buttonHeight = if (text.contains('\n')) 62.dp else 50.dp
    val accent = if (enabled) color else Color(0xFF5B6675)
    val textColor = if (enabled) Color.White else Color(0xFF9AA6B8)

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .padding(vertical = 4.dp)
            .scale(scale),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (pressed) 0.95f else 0.58f)),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xDD07131F),
            disabledContainerColor = Color(0xAA101824),
            disabledContentColor = Color(0xFF8EA0B8),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val glow = if (pressed) 1f else pulse
                val line = accent.copy(alpha = if (enabled) 0.38f + glow * 0.28f else 0.22f)
                val fill = accent.copy(alpha = if (pressed) 0.22f else 0.08f + glow * 0.035f)
                drawRect(fill, size = size)
                drawLine(line, Offset(0f, 0f), Offset(size.width * 0.18f, 0f), strokeWidth = 3f)
                drawLine(line, Offset(size.width * 0.82f, size.height), Offset(size.width, size.height), strokeWidth = 3f)
                drawLine(line, Offset(0f, size.height), Offset(18f, size.height - 18f), strokeWidth = 3f)
                drawLine(line, Offset(size.width, 0f), Offset(size.width - 18f, 18f), strokeWidth = 3f)
                drawLine(
                    accent.copy(alpha = if (pressed) 0.55f else 0.24f),
                    Offset(size.width * (0.10f + glow * 0.18f), size.height * 0.18f),
                    Offset(size.width * (0.42f + glow * 0.18f), size.height * 0.18f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    accent.copy(alpha = 0.18f),
                    Offset(size.width * 0.08f, size.height * 0.74f),
                    Offset(size.width * 0.92f, size.height * 0.74f),
                    strokeWidth = 1f,
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = if (text.contains('\n')) 14.sp else 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = if (text.contains('\n')) 18.sp else 20.sp,
            )
        }
    }
}

@Composable
private fun PlayingHud(game: GameState, onPause: () -> Unit) {
    val boss = game.enemies.firstOrNull { it.kind == EnemyKind.Boss }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            ) {
                Text("SCORE ${game.score}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("BEST ${game.highScore}", color = Color(0xFFB8D8FF), fontSize = 12.sp)
                Text(
                    text = "${GameConfig.modeName(game.mode)}  ${GameConfig.phaseName(game)}",
                    color = Color(0xFF80FFDB),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
            }
            Column(
                modifier = Modifier.width(136.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text("HP ${game.player.hp}/${game.player.maxHp}", color = Color(0xFFFFE66D), fontSize = 14.sp)
                LifePlaneBar(hp = game.player.hp, maxHp = game.player.maxHp)
                Button(
                    onClick = onPause,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC20324A)),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(72.dp)
                        .height(34.dp),
                ) {
                    Text("暂停", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        boss?.let {
            Spacer(Modifier.height(8.dp))
            StatBar(
                fraction = it.hp.toFloat() / it.maxHp,
                color = Color(0xFFFF3B30),
                label = if (it.bossName.isNotBlank()) it.bossName else "BOSS",
            )
        }
        PowerHud(game.player)
    }
}

@Composable
private fun LifePlaneBar(hp: Int, maxHp: Int) {
    val rows = if (maxHp > 10) 2 else 1
    Canvas(
        modifier = Modifier
            .width(132.dp)
            .height(if (rows == 2) 28.dp else 14.dp),
    ) {
        val perRow = if (rows == 2) ((maxHp + 1) / 2).coerceAtLeast(1) else maxHp.coerceAtLeast(1)
        val iconWidth = 13f
        val iconHeight = 12f
        val rowHeight = size.height / rows
        repeat(maxHp) { index ->
            val row = index / perRow
            val column = index % perRow
            val spacing = if (perRow <= 1) 0f else (size.width - iconWidth) / (perRow - 1)
            val left = column * spacing
            val top = row * rowHeight + (rowHeight - iconHeight) / 2f
            drawLifePlaneIcon(left, top, iconWidth, iconHeight, active = index < hp)
        }
    }
}

private fun DrawScope.drawLifePlaneIcon(left: Float, top: Float, iconWidth: Float, iconHeight: Float, active: Boolean) {
    val cx = left + iconWidth / 2f
    val cy = top + iconHeight / 2f
    val body = Path().apply {
        moveTo(cx, top + 1.0f)
        lineTo(cx - 2.2f, cy)
        lineTo(cx - 1.4f, top + iconHeight - 1.8f)
        lineTo(cx, top + iconHeight - 3.0f)
        lineTo(cx + 1.4f, top + iconHeight - 1.8f)
        lineTo(cx + 2.2f, cy)
        close()
    }
    val leftWing = Path().apply {
        moveTo(cx - 1.6f, cy)
        lineTo(left + 1.0f, top + iconHeight - 2.6f)
        lineTo(cx - 1.1f, top + iconHeight - 3.6f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(cx + 1.6f, cy)
        lineTo(left + iconWidth - 1.0f, top + iconHeight - 2.6f)
        lineTo(cx + 1.1f, top + iconHeight - 3.6f)
        close()
    }
    val fill = if (active) Color(0xFFFF5A5F) else Color(0x443A506B)
    val edge = if (active) Color(0xFFFFE66D) else Color(0x668EA0B8)
    drawPath(leftWing, fill)
    drawPath(rightWing, fill)
    drawPath(body, if (active) Color(0xFFFFD166) else Color(0x554C5565))
    drawPath(leftWing, edge, style = Stroke(width = 0.75f))
    drawPath(rightWing, edge, style = Stroke(width = 0.75f))
    drawPath(body, edge, style = Stroke(width = 0.75f))
}

@Composable
private fun LifePlaneIcon(active: Boolean) {
    Canvas(
        modifier = Modifier
            .width(15.dp)
            .height(13.dp),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val body = Path().apply {
            moveTo(cx, 1.2f)
            lineTo(cx - 2.4f, cy)
            lineTo(cx - 1.5f, size.height - 2f)
            lineTo(cx, size.height - 3.3f)
            lineTo(cx + 1.5f, size.height - 2f)
            lineTo(cx + 2.4f, cy)
            close()
        }
        val leftWing = Path().apply {
            moveTo(cx - 1.8f, cy)
            lineTo(1.2f, size.height - 3f)
            lineTo(cx - 1.2f, size.height - 4f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(cx + 1.8f, cy)
            lineTo(size.width - 1.2f, size.height - 3f)
            lineTo(cx + 1.2f, size.height - 4f)
            close()
        }
        val fill = if (active) Color(0xFFFF5A5F) else Color(0x443A506B)
        val edge = if (active) Color(0xFFFFE66D) else Color(0x668EA0B8)
        drawPath(leftWing, fill)
        drawPath(rightWing, fill)
        drawPath(body, if (active) Color(0xFFFFD166) else Color(0x554C5565))
        drawPath(leftWing, edge, style = Stroke(width = 0.8f))
        drawPath(rightWing, edge, style = Stroke(width = 0.8f))
        drawPath(body, edge, style = Stroke(width = 0.8f))
    }
}

@Composable
private fun StatBar(fraction: Float, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        ) {
            drawRoundRect(Color(0x66000000), size = size, cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(color, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = CornerRadius(8f, 8f))
        }
    }
}

@Composable
private fun PowerHud(player: Player) {
    val powers = listOf(
        "双弹" to player.doubleShot,
        "激光" to player.laser,
        "导弹" to player.missile,
        "护盾" to player.shield,
        "无敌" to player.invincible,
    ).filter { it.second > 0f }

    if (powers.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        powers.take(5).forEach { (label, value) ->
            Text(
                "$label ${value.toInt()}",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0xAA14213D), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    score: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
) {
    CenterPanel(title = "已暂停", subTitle = "得分 $score") {
        MenuButton("继续", onResume, Color(0xFF2A9D8F))
        MenuButton("重新开始", onRestart, Color(0xFFE85D04))
        MenuButton("返回菜单", onMenu, Color(0xFF3A506B))
    }
}

@Composable
private fun EndOverlay(
    title: String,
    score: Int,
    highScore: Int,
    subTitleExtra: String = "",
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    restartText: String = "重新开始",
) {
    val subTitle = if (subTitleExtra.isBlank()) {
        "得分 $score    最高分 $highScore"
    } else {
        "得分 $score    最高分 $highScore\n$subTitleExtra"
    }
    CenterPanel(title = title, subTitle = subTitle) {
        MenuButton(restartText, onRestart, Color(0xFFE85D04))
        MenuButton("返回菜单", onMenu, Color(0xFF3A506B))
    }
}

@Composable
private fun CenterPanel(title: String, subTitle: String, content: @Composable ColumnScope.() -> Unit) {
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(260), label = "panel")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .background(Color(0xEE0B1628), RoundedCornerShape(8.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(subTitle, color = Color(0xFFB8D8FF), fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            content()
        }
    }
}
