# 路由器管理 Android APP

一个功能完善的 Android 多路由器后台管理工具，基于 WebView 实现，支持多路由器同时在线、后台多任务切换、密码自动填充、文件上传下载等。

## 功能特性

### 核心功能
- **多路由器管理**：保存无限个路由器（名称 + 管理地址 + 用户名 + 密码）
- **后台多任务**：同时打开多个路由器后台，登录状态保持，一键切换
- **密码自动填充**：保存用户名密码后，打开登录页自动填入，无需手动输入
- **文件上传/下载**：支持路由器固件升级、配置备份/恢复等需要文件操作的功能
- **UA 模式切换**：手机/电脑 UA 一键切换，默认电脑模式，适配需要 UAC/电脑网页的登录页

### 界面与个性化
- **玻璃拟态卡片 UI**：毛玻璃效果 + 大圆角 + 柔和阴影，精致高大上
- **自定义主题色**：7 种主题色可选（蓝/绿/橙/紫/红/青/灰蓝）
- **自定义背景壁纸**：相册选图，壁纸延伸到状态栏和导航栏
- **自定义首页标题**：首页名称可改
- **状态栏自动适配**：状态栏图标颜色随背景明暗自动切换（白字/黑字）
- **底部导航栏**：透明带阴影，选中按钮主题色高亮，自动隐藏/弹出

### 安全与隐私
- **每卡片独立 IP/密码隐藏**：眼睛按钮单独控制每个路由器的地址和密码显示/隐藏
- **登录状态保持**：WebView 池复用，切换不销毁，Cookie/Session 不丢失
- **本地存储**：所有数据保存在手机本地，不上传任何服务器

### 其他
- **版本号自动迭代**：每次编译版本号自动递增
- **关于页面更新日志**：App 内可查看完整版本更新记录
- **HTTP 明文支持**：支持 `http://` 内网地址访问
- **页面缩放**：支持双指缩放，适配不同路由器后台页面

## 一键编译（GitHub Actions，推荐）

不需要本地安装 Java / Android SDK。

### 第一步：推送到 GitHub

```bash
cd RouterManagerApp
git init
git add .
git commit -m "init router manager app"
git branch -M main
git remote add origin https://github.com/你的用户名/router-manager-app.git
git push -u origin main
```

### 第二步：触发编译

推送后 GitHub 会自动运行 Actions（`.github/workflows/build.yml`），约 1 分钟完成。

也可以手动触发：仓库页面 → Actions → Build Debug APK → Run workflow。

### 第三步：下载 APK

编译完成后，在 Actions 运行页面底部 **Artifacts** 区域下载 `router-manager-apk`，解压得到 `app-debug.apk`。

### 第四步：安装到手机

把 APK 传到手机，点击安装（需要开启"允许安装未知来源应用"）。

## 使用方法

1. 打开 APP，底部点「+」添加路由器
2. 填写名称（如"家里"）、管理地址（如 `http://192.168.1.1`），可选填用户名和密码
3. 点击路由器卡片进入管理界面，登录页会自动填充已保存的账号密码
4. 按返回键直接回到多任务页，登录状态保持
5. 底部「多任务」查看所有已打开的后台，点击切换或关闭
6. 卡片右侧眼睛按钮可隐藏/显示该路由器的地址和密码
7. 首页右上角齿轮可设置主题色、背景壁纸、UA 模式、首页标题

## 自定义配置

| 项目 | 文件 | 说明 |
|------|------|------|
| APP 名称 | `app/src/main/res/values/strings.xml` | 修改 `app_name` |
| 包名 | `app/build.gradle.kts` + `AndroidManifest.xml` | 修改 `applicationId` 和 `namespace` |
| 图标背景 | `app/src/main/res/drawable/ic_launcher_bg.xml` | 修改渐变色 |
| 图标图案 | `app/src/main/res/drawable/ic_launcher_foreground.xml` | 修改矢量路径 |
| 版本号 | `app/build.gradle.kts` | 自动基于 git commit 数递增 |
| 最低安卓版本 | `app/build.gradle.kts` | 修改 `minSdk`（当前 26 = Android 8.0） |

## 技术栈

- **语言**：Kotlin
- **最低 SDK**：Android 8.0 (API 26)
- **编译 SDK**：Android 14 (API 34)
- **构建工具**：Gradle 8.7
- **CI/CD**：GitHub Actions 自动编译
- **核心实现**：WebView + WebViewClient + WebChromeClient + DownloadManager

## 注意事项

- Debug APK 未签名，只能自己安装测试，不能上架应用商店
- 如需正式签名 Release 版，需要生成 keystore 并配置签名
- 路由器后台需手机能访问（同一 WiFi 或 VPN 下用 `http://路由器IP:端口`）
- 密码自动填充通过 JS 注入实现，兼容大多数标准登录表单
- 所有数据仅存储在本地，卸载 APP 会清除所有已保存的路由器信息

## 作者

burry默默
