# 简账 (JianZhang) — Android 版

极简 · 本地 · 私密 的个人记账应用，由桌面版（Tauri 2 + Vue 3）移植而来。

## 特性
- 记账：收入/支出、金额、分类（内置 + 自定义）、日期、备注（≤40 字）
- 流水：按月分组列表、全部/支出/收入筛选、分类或备注搜索、点击编辑、二次确认删除
- 概览：月度收支统计（数字滚动动画）、收支比对、近 6 个月趋势折线图、每日支出柱状图、分类条形图与占比环形图
- 预算：设置每月总预算与分类预算，实时进度、结余 / 超支、剩余日均可用额度
- 数据管理：导出 JSON 完整备份（分类 + 账目 + 预算）、导出 CSV 流水、导入备份（合并 / 覆盖）
- 安全：启动密码锁屏、修改密码、立即锁定；密码加盐哈希存储，不联网
- 设置：字体大小（小/标准/大/特大）、字体格式（系统/等宽）、外观（跟随系统/浅色/深色）、默认账目类型、记住上次分类、分类管理
- AI 分析：配置 DeepSeek 或其他 OpenAI 兼容 Chat Completions 接口，用本机账本数据生成财务健康、消费结构、预算风险或自定义分析；API Key 仅存本机
- UI 双风格：保留《看门狗：军团》黑客终端切角 HUD，同时提供圆角玻璃风，可在设置中即时切换
- 关于：极简信息页与 100% 本地存储徽章
- UI：以《看门狗：军团》黑客 HUD 为蓝本全面重构（军团青 + 橙警示、切角面板、网格扫描线背景、等宽字体、故障辉光），少量借鉴《看门狗2》

## 隐私保证
- 仅声明 `INTERNET` 权限：只有用户主动配置并点击 AI 分析时才向所填接口发送请求
- 允许用户填写的本地 HTTP 模型端点（例如 Ollama / LM Studio）；默认和推荐使用 HTTPS
- 账本数据不会自动上传；AI 请求仅发送由当前账本聚合生成的分析上下文和用户补充问题
- 数据仅存于应用私有目录 SQLite（`jianzhang.db`），不申请存储权限
- 关闭 Android 系统备份（`allowBackup=false`），数据不会进入云端备份
- 备份文件格式与桌面版互通（`jianzhang-backup`），可在手机与电脑之间互相迁移

## 技术栈
- Kotlin 2.1.20 + Jetpack Compose（BOM 2025.05.01）+ Material 3
- Room 2.7.1（SQLite，数据模型与桌面版一致：金额以"分"存储）
- 图表全部使用 Compose Canvas 自绘（无第三方图表库，包体更小、动效可控）
- minSdk 26（Android 8.0），targetSdk 35

## 构建
### 方式一：Android Studio
1. 用 Android Studio（自带 JDK）打开本目录
2. 设置 SDK：`local.properties` 中 `sdk.dir` 指向 Android SDK 路径
3. 同步后运行 `assembleRelease` / `assembleDebug`

### 方式二：命令行（本项目开发机）
需要 JDK 17+ 和 Android SDK 35。项目使用 Gradle Wrapper：
```powershell
cd JianZhang-Android
.\gradlew.bat assembleRelease
```
产物：`app\build\outputs\apk\release\app-release.apk`。若未配置 `keystore.properties`，release 会回退为 debug 签名。

> 依赖仓库已配置阿里云镜像（见 `settings.gradle.kts`），国内网络可直接下载。
> 发布版 APK 约 1.5 MB（R8 压缩后），远低于 30 MB 目标。

## 测试
```powershell
.\gradlew.bat testDebugUnitTest
```

## 版本
- 0.5.0（新增 AI 分析，支持 DeepSeek 等 OpenAI 兼容 API；新增“黑客终端 / 圆角玻璃”双 UI 风格；声明 `INTERNET` 权限且仅用于用户主动发起的 AI 请求；`versionCode=6`）
- 0.4.0（UI 全面重构为《看门狗：军团》黑客 HUD 风格：配色令牌、切角面板/按钮/导航、动态网格 + 扫描线背景、图表配色联动、锁屏与关于页重制；功能与数据格式完全不变，无网络权限保持）
- 0.3.1（修复 v1→v2 升级时数据库 schema 校验失败导致的闪退；功能与桌面版 v0.3.0 对齐：新增预算分析、数据导入导出、密码锁屏；数据库升级 v1→v2 保留旧数据）
