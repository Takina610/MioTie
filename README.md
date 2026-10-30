<p align="center">
  <img src="docs/images/logo.png?v=1.1.1" width="128" alt="MioTie Logo" />
</p>

<h1 align="center">MioTie</h1>

<p align="center">
  <strong>为兴趣而生</strong> —— 基于 TiebaLite 深度重构的非官方第三方贴吧客户端
</p>

<p align="center">
  <a href="https://github.com/Takina610/MioTie/releases"><img src="https://img.shields.io/github/v/release/Takina610/MioTie?color=1e88e5&label=Release" alt="Release" /></a>
  <a href="https://developer.android.com/studio/releases/platforms#6.0"><img src="https://img.shields.io/badge/Android-6.0%2B%20(API%2023%2B)-3ddc84?logo=android&logoColor=white" alt="Android" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-MaterialKolor-4285f4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://github.com/Takina610/MioTie/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-red?logo=gnu&logoColor=white" alt="License" /></a>
</p>

> [!WARNING]
> 本项目仅供学习交流，不用于商业用途；它不是百度或贴吧的官方客户端，相关商标与内容权利归其权利人所有。

## 项目简介

MioTie fork 自 [makise2060/FluxTie](https://github.com/makise2060/FluxTie)，上游项目源自 [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite) 并基于 [zzc10086/TiebaLite](https://github.com/zzc10086/TiebaLite) 的 4.0-dev 分支进行了深度重构。项目保留贴吧客户端的核心能力，同时以 Jetpack Compose、MaterialKolor 与现代 Android 工具链重新打磨浏览、主题、动效和更新体验。

- 无自建内容后端，帖子、图片等贴吧业务数据直连官方接口。
- 应用更新检查使用 GitHub Releases，不后台静默下载。
- 应用 ID 为 <code>com.takina.miotie</code>，可与官方贴吧、原版 TiebaLite 及 FluxTie 共存。
- 包名不同，因此不会接管原版或上游数据；从原版迁移需重新登录并按需调整设置。

## 核心特性

- **双阶段流体启动**：系统启动屏与 Compose 启动覆盖层衔接，双水滴、贴字与星石构成完整启动动效。
- **官方贴字新标**：参考贴吧 12.0 MEUX 字形重构“贴”字图标，并同步替换应用内主要图标资源。
- **MaterialKolor 种子配色**：14 个预置种子色、自定义取色、9 种配色风格、浅色/深色/AMOLED 纯黑与 Android 12+ 系统取色。
- **真悬浮底栏**：毛玻璃 Dock 悬浮于内容之上，支持按住拖拽、跟手预览、就近切换与触感反馈；低版本系统自动降级。
- **个人表情包**：收藏与 DIY 表情支持云端同步，可在编辑器、帖子和评论中直接渲染。
- **应用内更新**：检查、更新日志、下载进度、取消、系统安装与“忽略当前版本”一站式完成。
- **现代图片链路**：以 Sketch 为主要图片加载框架，大图预览使用 ZoomImage，并保留缩略图占位与加载状态反馈。

## 相对原版的升级

| 维度 | 原版 TiebaLite | MioTie |
| --- | --- | --- |
| 界面方向 | 以既有 View/XML 界面为主 | 新 UI 以 Jetpack Compose 为主，逐步统一页面与组件 |
| 主题系统 | 固定主题列表，部分调整不够即时 | MaterialKolor 种子色引擎，14 个预置色、9 种风格与实时预览 |
| 深色体验 | 深色模式能力较基础 | 浅色、深色、深邃灰、AMOLED 纯黑及 Android 12+ 系统取色 |
| 底部导航 | 贴底导航栏 | 可开关的悬浮毛玻璃 Dock，支持拖拽切换与动效反馈 |
| 启动体验 | 静态图标与传统淡入 | 系统启动屏加 Compose 覆盖层的双阶段流体动效 |
| 图片体验 | 传统图片加载与查看 | Sketch 加载、缩略图占位、crossfade 与 ZoomImage 大图预览 |
| 表情能力 | 基础表情与收藏 | 收藏/DIY 云端同步、编辑器渲染、长按预览与帖子评论展示 |
| 应用更新 | 以手动下载安装为主 | GitHub Releases 检查、下载、安装与版本忽略 |
| 工具链 | AGP 8.13、Gradle 8.14、compileSdk 36 | AGP 9.4、Gradle 9.7、Kotlin 2.3、compileSdk/targetSdk 37 |
| 应用共存 | <code>com.huanchengfly.tieba.post</code> | <code>com.takina.miotie</code>，可与原版、官方客户端及 FluxTie 共存 |

签到、多账号、发帖回帖、收藏、屏蔽、浏览记录、内置日志查看器等上游能力继续保留，并在新界面中逐步统一交互与视觉。

## 构建

要求 **JDK 17+**、Android SDK 与可执行 Gradle Wrapper 的环境。

~~~bash
git clone https://github.com/Takina610/MioTie.git
cd MioTie
./gradlew assembleDebug
~~~

Debug APK 输出位置：

~~~text
app/build/outputs/apk/debug/MioTie-v{version}.apk
~~~

本地 SDK 路径通过 <code>local.properties</code> 的 <code>sdk.dir</code> 配置；Release 构建会启用 R8 与资源收缩。

### 本地签名

仓库根目录的 <code>dev.keystore</code> 是固定签名的开发密钥，**仅供 debug 构建使用**。
固定签名后反复覆盖安装不会清除登录态与本地数据，方便真机调试。

不要删除该文件。AGP 自动生成的 <code>debug.keystore</code> 位置随 <code>ANDROID_USER_HOME</code>
或 <code>~/.android</code> 变化，签名一旦改变就只能卸载重装，本地数据会一起丢失。

release 构建只接受 <code>keystore.properties</code> 中配置的正式签名，缺少该文件时构建直接失败，
不会回退到开发签名或 debug 签名，避免测试签名被误当作发布包。CI 发布构建通过 secrets 注入该文件。

真机调试使用 <code>run_phone.bat</code>，它构建并安装 debug 包。

## 技术栈

- Kotlin 2.3
- Jetpack Compose（Material 2 组件基座 + MaterialKolor 动态配色）
- Hilt、KSP、手写 MVI
- Retrofit、OkHttp、Wire
- Room、DataStore、kotlinx.serialization
- Sketch、ZoomImage
- AGP 9.4、Gradle 9.7、compileSdk/targetSdk 37

## 开源边界与安全说明

- 本仓库不提供贴吧内容镜像，也不收集帖子数据；登录与业务请求由客户端直连相应服务。
- 应用更新、发布说明与安装包来自 GitHub Releases。
- 请勿将本项目用于冒充官方客户端、抓取滥用、骚扰他人或其他违反服务条款与法律法规的用途。
- 源码按 GPL-3.0 发布；使用、修改与分发时仍需遵守上游协议、第三方资源许可及本项目的学习交流声明。

## 致谢

- [makise2060/FluxTie](https://github.com/makise2060/FluxTie)：直接上游项目，贡献了 Material 3 体系、Jetpack Compose 架构重构、MaterialKolor 种子配色、流体启动与悬浮底栏等核心特性。
- [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite)：原作，奠定了贴吧第三方轻量客户端的基础架构。
- [zzc10086/TiebaLite](https://github.com/zzc10086/TiebaLite)：上游所基于的 4.0-dev 分支。
- [Lingyan000/fluxdo](https://github.com/Lingyan000/fluxdo)：外观与界面设计参考。
- [appshubcc/Bettbox](https://github.com/appshubcc/Bettbox)：设置页与底栏交互设计参考。
- **MeiloX**：iOS 风格的布局、转场与排版参考，首页顶栏、设置页 iOS Lists 布局、玻璃搜索工具栏与底部玻璃导航均对齐其实现。
- **Glasmio**：液态玻璃控件的移植来源，弹窗、Action Sheet、分段控件、开关、滑块与取色器均按其结构逐行移植。
- [aiotieba](https://github.com/Starry-OvO/aiotieba) 与 [tbclient.protobuf](https://github.com/n0099/tbclient.protobuf)：贴吧协议研究参考。

## License

[GPL-3.0](LICENSE) © 原作者、FluxTie 及 MioTie 贡献者。
