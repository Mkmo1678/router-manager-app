# 路由器管理 Android APP

一个极简的 Android WebView 壳应用，把你的路由器管理平台（React + NestJS 全栈项目）包装成可安装的 APK。

## 原理

- APP 内嵌入一个全屏 WebView，打开你输入的服务器地址
- 前端页面和 API 都由你的后端（NestJS）提供，APP 不做任何业务逻辑
- 支持**多路由器管理**：可保存多个路由器（名称+地址），一键切换
- 自动记住上次使用的路由器，下次打开直接进入
- 支持返回键回退、页面缩放、HTTP 明文访问

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

推送后 GitHub 会自动运行 Actions（`.github/workflows/build.yml`），约 3-5 分钟完成。

也可以手动触发：仓库页面 → Actions → Build Debug APK → Run workflow。

### 第三步：下载 APK

编译完成后，在 Actions 运行页面底部 **Artifacts** 区域下载 `router-manager-apk`，解压得到 `app-debug.apk`。

### 第四步：安装到手机

把 APK 传到手机，点击安装（需要开启"允许安装未知来源应用"）。

## 本地编译（可选）

如果你装了 Android Studio：

1. 用 Android Studio 打开本项目目录
2. 等待 Gradle 同步完成
3. 菜单 Build → Build Bundle(s) / APK(s) → Build APK(s)
4. 生成路径：`app/build/outputs/apk/debug/app-debug.apk`

## 使用方法

1. 确保你的路由器管理平台后端已启动，且手机能访问（同一 WiFi 下用 `http://路由器IP:端口`）
2. 首次打开 APP 会弹出"添加路由器"对话框，输入名称（如"家里"）和地址（如 `http://192.168.1.1:3000`），点保存
3. 点右上角菜单（三个点）可进行以下操作：
   - **切换路由器**：从已保存的列表中选择，当前使用的前面有 ✓ 标记
   - **添加路由器**：新增一个路由器地址
   - **删除当前路由器**：移除当前正在使用的路由器
4. 下次打开 APP 自动加载上次使用的路由器

## 自定义

| 项目 | 文件 | 说明 |
|------|------|------|
| APP 名称 | `app/src/main/res/values/strings.xml` | 修改 `app_name` |
| 包名 | `app/build.gradle.kts` + `AndroidManifest.xml` | 修改 `applicationId` 和 `namespace` |
| 图标颜色 | `app/src/main/res/values/colors.xml` | 修改 `ic_launcher_background` |
| 图标图案 | `app/src/main/res/drawable/ic_launcher_foreground.xml` | 修改矢量路径 |
| 版本号 | `app/build.gradle.kts` | 修改 `versionCode` / `versionName` |
| 最低安卓版本 | `app/build.gradle.kts` | 修改 `minSdk`（当前 26 = Android 8.0） |

## 注意事项

- Debug APK 未签名，只能自己安装测试，不能上架应用商店
- 如需正式签名 Release 版，需要生成 keystore 并配置签名
- 如果后端是 HTTPS 自签名证书，APP 会提示证书错误，需在 WebView 中额外处理
- 后端必须允许跨域或与前端同源（本项目原本就是同源部署，直接访问后端地址即可）
