# 星舰终局：虚空核心

`星舰终局：虚空核心` 是一款 Android 单机竖版机甲空战射击游戏。项目基于 Kotlin 和 Jetpack Compose Canvas 构建，包含章节推进、无限模式、Boss 连战、玩家成长、道具掉落、音效和背景音乐等完整玩法闭环。

> 当前仓库仍保留 Android Studio 初始包名 `com.example.gametest`。正式发布前建议统一调整为商业化包名，例如 `com.starshipendgame.voidcore`。

## 游戏截图 / GIF

截图和录屏资源放在 `docs/screenshots/` 目录。

| 首页 | 章节战斗 | Boss 战 |
| --- | --- | --- |
| `docs/screenshots/menu.png` | `docs/screenshots/chapter-battle.png` | `docs/screenshots/boss-fight.gif` |

当前仓库已预留截图目录。真机或模拟器录制后，按上面的文件名放入即可在 README 中展示。

## 功能清单

### 基础玩法

- 无限滚动星域背景
- 玩家机甲战机
- 多类型敌机：侦察、追踪、曲线、火力、重甲、Boss
- 玩家子弹、敌机子弹、激光、导弹、等离子弹
- 碰撞检测、伤害计算、血量系统
- 爆炸动画、火焰粒子、子弹拖尾
- 道具掉落、自动吸附、拾取提示
- 得分系统、最高分存档
- 暂停、游戏结束、重新开始、章节通关

### 游戏体验

- 屏幕震动反馈
- 粒子特效和爆炸火焰
- UI 按钮动效
- 首页标题电流/无线电风格效果
- Boss 警告动画
- Boss 大招警告
- 三档画质：流畅、标准、高

### 成长系统

- 双倍子弹
- 激光强化
- 导弹强化
- 护盾强化
- 开局无敌
- 加血补给
- 章节通关永久奖励
- 玩家称号随章节进度升级

### 敌人 AI

- 跟踪玩家
- 曲线飞行
- 随机射击
- Boss 技能弹幕
- Boss 护盾窗口
- Boss 虚弱窗口
- Boss 阶段大招

### 关卡模式

- 章节模式：20 个章节，逐章解锁
- 无限模式：20 个 Boss 循环，后续轮次继续强化
- Boss 连战：按章节顺序连续挑战 20 个 Boss
- 第 10 章：彩蛋 Boss
- 第 20 章：最终 Boss

## 章节设计

当前共有 20 个章节。每章配置包含：

- 章节名称
- Boss 名称
- 击杀目标
- 背景风格
- 敌机池
- Boss 血量倍率
- 敌机血量倍率
- 敌机速度倍率
- 出怪节奏倍率
- 是否彩蛋 Boss / 最终 Boss

章节配置集中在：

```text
app/src/main/java/com/example/gametest/ChapterCatalog.kt
```

核心章节节点：

| 章节 | 名称 | Boss |
| --- | --- | --- |
| 1 | 近地防线 | 巡航母舰 |
| 5 | 轨道截击 | 轨道歼星舰 |
| 10 | 失落机库 | 彩蛋Boss: 旧日王牌 |
| 15 | 天基炮阵 | 天基炮阵核心 |
| 20 | 核心终局 | 终极Boss: 虚空核心 |

## APK 构建说明

### 环境要求

- Android Studio
- Android Gradle Plugin 项目内版本
- JDK 21
- Android SDK

当前本地指定 JDK：

```text
C:\Users\sunguangzong\.jdks\ms-21.0.12
```

### 命令行构建

在项目根目录执行：

```bash
./gradlew assembleDebug
```

Windows 下可执行：

```bat
gradlew.bat assembleDebug
```

如果需要显式指定 JDK：

```bat
C:\Users\sunguangzong\.jdks\ms-21.0.12\bin\java.exe -Dorg.gradle.java.home=C:\Users\sunguangzong\.jdks\ms-21.0.12 -Dorg.gradle.appname=gradlew -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug --no-daemon --console=plain
```

构建产物位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 安装到设备

连接 Android 手机并开启 USB 调试后执行：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

如果手机提示禁止 USB 安装，需要在系统开发者选项或安全设置中允许 USB 安装。

## 技术架构

项目采用单 Activity + Compose Canvas 的轻量架构。游戏逻辑、渲染、配置、音频和存档分别拆分，方便后续扩展。

```text
app/src/main/java/com/example/gametest/
├── MainActivity.kt        # Compose UI、输入、生命周期、HUD
├── GameEngine.kt         # 游戏主循环、移动、生成、碰撞、结算
├── GameRenderer.kt       # Canvas 绘制、背景、飞机、子弹、爆炸、Boss 警告
├── GameModels.kt         # 状态模型、枚举、实体数据
├── GameConfig.kt         # 基础数值、掉落、显示文本
├── ChapterCatalog.kt     # 20 章配置、奖励、无限模式映射
├── GameAudio.kt          # SoundPool 音效
├── GameMusic.kt          # MediaPlayer 背景音乐
└── GameStorage.kt        # SharedPreferences 存档
```

### 状态模型

核心状态集中在 `GameState`：

- 当前界面
- 游戏模式
- 玩家
- 敌机列表
- 子弹列表
- 道具列表
- 粒子和爆炸
- 分数和章节进度
- 音量、画质、震动设置
- Boss 警告和大招状态

### 游戏循环

`MainActivity` 使用 Compose 帧回调驱动游戏：

1. 读取当前帧时间差
2. 调用 `GameState.update(...)`
3. 更新玩家、敌机、子弹、道具和粒子
4. 执行碰撞检测和伤害结算
5. 触发音效、震动、存档
6. 通过 `GameRenderer` 绘制当前画面

### 扩展方式

新增章节：

1. 在 `ChapterCatalog.kt` 增加 `chapter(...)` 配置
2. 配置敌机池、背景、Boss 名称和倍率
3. 在奖励表中增加 `ChapterReward`
4. 如需特殊外观，在 `GameRenderer.kt` 增加 Boss 绘制分支

新增敌机：

1. 在 `EnemyKind` 增加类型
2. 在 `GameConfig.enemySpec(...)` 配置基础属性
3. 在 `Enemy.move(...)` 或生成逻辑中添加行为
4. 在 `GameRenderer.drawEnemy(...)` 添加外观

新增道具：

1. 在 `PowerUpKind` 增加类型
2. 在 `GameConfig` 增加文本和颜色
3. 在拾取逻辑中增加效果
4. 在渲染中增加图标表现

## 仓库说明

`.gitignore` 已忽略构建产物、本地配置和安装包：

- `.gradle/`
- `.kotlin/`
- `build/`
- `app/build/`
- `local.properties`
- `*.apk`
- `*.aab`

源码、Gradle Wrapper、资源文件、音效和 BGM 会正常纳入版本管理。
