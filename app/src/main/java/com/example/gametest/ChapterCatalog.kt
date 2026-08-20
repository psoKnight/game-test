package com.example.gametest

import androidx.compose.ui.graphics.Color

object ChapterCatalog {
    const val FINAL_CHAPTER = 20

    private val chapters = listOf(
        chapter(1, "近地防线", "巡航母舰", 28, BackgroundStyle.LowOrbit,
            listOf(EnemyKind.Scout, EnemyKind.Scout, EnemyKind.Hunter, EnemyKind.Waver),
            listOf(Color(0xFF04101F), Color(0xFF0E2A44), Color(0xFF06111D)), 0.86f, 1.00f, 0.92f, 1.08f),
        chapter(2, "云层突袭", "雷暴堡垒", 34, BackgroundStyle.StormCloud,
            listOf(EnemyKind.Scout, EnemyKind.Scout, EnemyKind.Hunter, EnemyKind.Waver, EnemyKind.Gunner),
            listOf(Color(0xFF102A43), Color(0xFF4D96C8), Color(0xFF13293D)), 0.96f, 1.04f, 0.96f, 1.02f),
        chapter(3, "沙海空战", "荒漠要塞", 40, BackgroundStyle.DustWar,
            listOf(EnemyKind.Scout, EnemyKind.Hunter, EnemyKind.Gunner, EnemyKind.Waver, EnemyKind.Tank),
            listOf(Color(0xFF24130A), Color(0xFF9A5C20), Color(0xFF140B08)), 1.08f, 1.10f, 1.00f, 0.98f),
        chapter(4, "极光裂隙", "极光棱镜舰", 48, BackgroundStyle.Aurora,
            listOf(EnemyKind.Hunter, EnemyKind.Waver, EnemyKind.Waver, EnemyKind.Gunner, EnemyKind.Tank),
            listOf(Color(0xFF031A23), Color(0xFF0B8F8F), Color(0xFF08111F)), 1.20f, 1.18f, 1.05f, 0.94f),
        chapter(5, "轨道截击", "轨道歼星舰", 56, BackgroundStyle.Orbital,
            listOf(EnemyKind.Hunter, EnemyKind.Waver, EnemyKind.Gunner, EnemyKind.Gunner, EnemyKind.Tank),
            listOf(Color(0xFF07020D), Color(0xFF2D1B69), Color(0xFF050816)), 1.34f, 1.28f, 1.10f, 0.89f),
        chapter(6, "残骸星环", "碎星收割者", 64, BackgroundStyle.DebrisRing,
            listOf(EnemyKind.Hunter, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Waver, EnemyKind.Tank),
            listOf(Color(0xFF111827), Color(0xFF64748B), Color(0xFF090E16)), 1.52f, 1.40f, 1.16f, 0.84f),
        chapter(7, "深空虫洞", "虫洞守门者", 72, BackgroundStyle.Wormhole,
            listOf(EnemyKind.Waver, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Hunter, EnemyKind.Tank),
            listOf(Color(0xFF12002E), Color(0xFF7B2CBF), Color(0xFF050014)), 1.72f, 1.54f, 1.22f, 0.80f),
        chapter(8, "熔核星域", "熔核巨像", 80, BackgroundStyle.MoltenCore,
            listOf(EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Hunter, EnemyKind.Waver),
            listOf(Color(0xFF1A0500), Color(0xFFD9480F), Color(0xFF100200)), 1.96f, 1.72f, 1.28f, 0.76f),
        chapter(9, "暗物质边境", "暗物质执政官", 88, BackgroundStyle.DarkMatter,
            listOf(EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Hunter, EnemyKind.Tank, EnemyKind.Waver),
            listOf(Color(0xFF020617), Color(0xFF312E81), Color(0xFF000009)), 2.24f, 1.92f, 1.34f, 0.72f),
        chapter(10, "失落机库", "彩蛋Boss: 旧日王牌", 96, BackgroundStyle.FinalCore,
            listOf(EnemyKind.Hunter, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Waver),
            listOf(Color(0xFF16071F), Color(0xFFB089FF), Color(0xFF050005)), 2.48f, 2.04f, 1.38f, 0.70f, easterBoss = true),
        chapter(11, "月背船坞", "月背锻造舰", 78, BackgroundStyle.LowOrbit,
            listOf(EnemyKind.Hunter, EnemyKind.Gunner, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Waver),
            listOf(Color(0xFF030712), Color(0xFF475569), Color(0xFF020617)), 2.62f, 2.14f, 1.42f, 0.68f),
        chapter(12, "离子风暴", "离子审判者", 86, BackgroundStyle.StormCloud,
            listOf(EnemyKind.Waver, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Hunter, EnemyKind.Gunner),
            listOf(Color(0xFF061A2B), Color(0xFF00B4D8), Color(0xFF030712)), 2.78f, 2.24f, 1.46f, 0.66f),
        chapter(13, "赤砂裂谷", "赤砂战争机", 94, BackgroundStyle.DustWar,
            listOf(EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Hunter, EnemyKind.Waver),
            listOf(Color(0xFF210B05), Color(0xFFBC6C25), Color(0xFF090302)), 2.96f, 2.36f, 1.50f, 0.64f),
        chapter(14, "镜面极区", "镜面折跃舰", 102, BackgroundStyle.Aurora,
            listOf(EnemyKind.Waver, EnemyKind.Waver, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Hunter),
            listOf(Color(0xFF021C1E), Color(0xFF22D3EE), Color(0xFF020617)), 3.14f, 2.48f, 1.54f, 0.62f),
        chapter(15, "天基炮阵", "天基炮阵核心", 110, BackgroundStyle.Orbital,
            listOf(EnemyKind.Gunner, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Hunter),
            listOf(Color(0xFF030712), Color(0xFF1D4ED8), Color(0xFF020617)), 3.34f, 2.62f, 1.58f, 0.60f),
        chapter(16, "灰烬星环", "灰烬吞噬者", 118, BackgroundStyle.DebrisRing,
            listOf(EnemyKind.Tank, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Waver, EnemyKind.Hunter),
            listOf(Color(0xFF0B0F16), Color(0xFF78716C), Color(0xFF020617)), 3.56f, 2.78f, 1.62f, 0.58f),
        chapter(17, "量子回廊", "量子守望者", 126, BackgroundStyle.Wormhole,
            listOf(EnemyKind.Waver, EnemyKind.Tank, EnemyKind.Gunner, EnemyKind.Hunter, EnemyKind.Tank),
            listOf(Color(0xFF0B0220), Color(0xFF9333EA), Color(0xFF020012)), 3.80f, 2.94f, 1.66f, 0.56f),
        chapter(18, "太阳熔炉", "太阳熔炉巨兵", 134, BackgroundStyle.MoltenCore,
            listOf(EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Waver),
            listOf(Color(0xFF1C0500), Color(0xFFEA580C), Color(0xFF070100)), 4.06f, 3.12f, 1.70f, 0.54f),
        chapter(19, "虚数边界", "虚数裁决者", 142, BackgroundStyle.DarkMatter,
            listOf(EnemyKind.Tank, EnemyKind.Gunner, EnemyKind.Hunter, EnemyKind.Tank, EnemyKind.Waver),
            listOf(Color(0xFF010314), Color(0xFF4C1D95), Color(0xFF000005)), 4.34f, 3.30f, 1.74f, 0.52f),
        chapter(20, "核心终局", "终极Boss: 虚空核心", 156, BackgroundStyle.FinalCore,
            listOf(EnemyKind.Hunter, EnemyKind.Gunner, EnemyKind.Tank, EnemyKind.Tank, EnemyKind.Waver),
            listOf(Color(0xFF18020C), Color(0xFFB00020), Color(0xFF050005)), 4.72f, 3.55f, 1.82f, 0.50f, finalChapter = true),
    )

    private val rewards = listOf(
        ChapterReward(1, "装甲核心 I", "机体耐久提升", maxHpBonus = 1),
        ChapterReward(2, "双联炮校准", "主炮节奏提升", weaponLevelBonus = 1),
        ChapterReward(3, "聚束激光", "激光穿透增强", laserLevelBonus = 1, startDoubleShot = 4f),
        ChapterReward(4, "相位护盾", "护盾启动更快", shieldLevelBonus = 1, startInvincible = 3f),
        ChapterReward(5, "蜂群导弹", "导弹锁定强化", missileLevelBonus = 1, startDoubleShot = 5f),
        ChapterReward(6, "装甲核心 II", "机体与引擎升级", maxHpBonus = 1, engineLevelBonus = 1, startShield = 4f),
        ChapterReward(7, "棱镜激光", "光束输出升级", laserLevelBonus = 1, startShield = 5f),
        ChapterReward(8, "重型弹舱", "开场火力增强", missileLevelBonus = 1, startDoubleShot = 6f),
        ChapterReward(9, "虚空护盾", "护盾与机动升级", shieldLevelBonus = 1, engineLevelBonus = 1, startShield = 7f),
        ChapterReward(10, "旧日王牌徽记", "彩蛋火力授权", weaponLevelBonus = 1, startDoubleShot = 8f, startShield = 6f, startInvincible = 3f),
        ChapterReward(11, "月背合金", "机体耐久提升", maxHpBonus = 1),
        ChapterReward(12, "离子增幅器", "激光输出升级", laserLevelBonus = 1, startDoubleShot = 5f),
        ChapterReward(13, "赤砂装甲", "护盾稳定提升", shieldLevelBonus = 1, startShield = 5f),
        ChapterReward(14, "折跃舵机", "机动节奏提升", engineLevelBonus = 1, startInvincible = 2f),
        ChapterReward(15, "天基火控", "主炮节奏提升", weaponLevelBonus = 1, startDoubleShot = 6f),
        ChapterReward(16, "灰烬弹仓", "导弹弹头升级", missileLevelBonus = 1, startShield = 6f),
        ChapterReward(17, "量子棱镜", "激光与护盾升级", laserLevelBonus = 1, shieldLevelBonus = 1, startDoubleShot = 6f),
        ChapterReward(18, "太阳炉心", "机体与火力升级", maxHpBonus = 1, weaponLevelBonus = 1, startShield = 7f),
        ChapterReward(19, "虚数锁定器", "导弹与引擎升级", missileLevelBonus = 1, engineLevelBonus = 1, startDoubleShot = 7f),
        ChapterReward(20, "终极机甲核心", "全武装解放", weaponLevelBonus = 1, laserLevelBonus = 1, missileLevelBonus = 1, shieldLevelBonus = 1, engineLevelBonus = 1, startDoubleShot = 10f, startShield = 10f, startInvincible = 5f),
    )

    fun spec(chapter: Int): ChapterSpec {
        return chapters.firstOrNull { it.id == chapter } ?: chapters.last()
    }

    fun activeSpec(game: GameState): ChapterSpec {
        val virtualChapter = when (game.mode) {
            GameMode.Infinite -> infiniteChapterForRound(game.infiniteRound)
            GameMode.BossRush -> (game.bossKills + 1).coerceIn(1, FINAL_CHAPTER)
            GameMode.Chapter -> game.chapter.coerceIn(1, FINAL_CHAPTER)
        }
        return spec(virtualChapter)
    }

    fun infiniteChapterForRound(round: Int): Int {
        return ((round.coerceAtLeast(1) - 1) % FINAL_CHAPTER) + 1
    }

    fun infiniteLoopTier(round: Int): Int {
        return (round.coerceAtLeast(1) - 1) / FINAL_CHAPTER
    }

    fun infiniteRoundTarget(round: Int): Int {
        val safeRound = round.coerceAtLeast(1)
        val chapter = infiniteChapterForRound(safeRound)
        val tier = infiniteLoopTier(safeRound)
        return (24 + chapter * 3 + tier * 8).coerceAtMost(96)
    }

    fun infiniteModifierForRound(round: Int): InfiniteModifier {
        return when ((round.coerceAtLeast(1) - 1) % 5) {
            0 -> InfiniteModifier.Electromagnetic
            1 -> InfiniteModifier.DebrisField
            2 -> InfiniteModifier.FireSuppression
            3 -> InfiniteModifier.SupplyGap
            else -> InfiniteModifier.CoreSurge
        }
    }

    fun reward(chapter: Int): ChapterReward {
        return rewards.firstOrNull { it.chapter == chapter } ?: rewards.last()
    }

    fun rewardsUpTo(clearedChapter: Int): List<ChapterReward> {
        return rewards.filter { it.chapter <= clearedChapter.coerceIn(0, FINAL_CHAPTER) }
    }

    fun progressForClearedChapter(clearedChapter: Int): PlayerProgress {
        val earned = rewardsUpTo(clearedChapter)
        return PlayerProgress(
            clearedChapter = clearedChapter.coerceIn(0, FINAL_CHAPTER),
            maxHpBonus = earned.sumOf { it.maxHpBonus },
            weaponLevel = (1 + earned.sumOf { it.weaponLevelBonus }).coerceIn(1, 6),
            laserLevel = earned.sumOf { it.laserLevelBonus }.coerceIn(0, 5),
            missileLevel = earned.sumOf { it.missileLevelBonus }.coerceIn(0, 5),
            shieldLevel = earned.sumOf { it.shieldLevelBonus }.coerceIn(0, 5),
            engineLevel = earned.sumOf { it.engineLevelBonus }.coerceIn(0, 5),
            title = when {
                clearedChapter >= 20 -> "终极机甲"
                clearedChapter >= 16 -> "虚空重装"
                clearedChapter >= 10 -> "彩蛋王牌"
                clearedChapter >= 8 -> "深空重装"
                clearedChapter >= 5 -> "轨道强袭"
                clearedChapter >= 2 -> "进阶战机"
                else -> "原型机"
            },
        )
    }

    private fun chapter(
        id: Int,
        name: String,
        bossName: String,
        requiredKills: Int,
        backgroundStyle: BackgroundStyle,
        enemyPool: List<EnemyKind>,
        background: List<Color>,
        bossHpScale: Float,
        enemyHpScale: Float,
        enemySpeedScale: Float,
        spawnScale: Float,
        finalChapter: Boolean = false,
        easterBoss: Boolean = false,
    ) = ChapterSpec(
        id = id,
        name = name,
        requiredKills = requiredKills,
        enemyPool = enemyPool,
        background = background,
        backgroundStyle = backgroundStyle,
        bossName = bossName,
        bossHpScale = bossHpScale,
        enemyHpScale = enemyHpScale,
        enemySpeedScale = enemySpeedScale,
        spawnScale = spawnScale,
        finalChapter = finalChapter,
        easterBoss = easterBoss,
    )
}

enum class BackgroundStyle {
    LowOrbit,
    StormCloud,
    DustWar,
    Aurora,
    Orbital,
    DebrisRing,
    Wormhole,
    MoltenCore,
    DarkMatter,
    FinalCore,
}

data class ChapterSpec(
    val id: Int,
    val name: String,
    val requiredKills: Int,
    val enemyPool: List<EnemyKind>,
    val background: List<Color>,
    val backgroundStyle: BackgroundStyle,
    val bossName: String,
    val bossHpScale: Float,
    val enemyHpScale: Float,
    val enemySpeedScale: Float,
    val spawnScale: Float,
    val finalChapter: Boolean = false,
    val easterBoss: Boolean = false,
)
