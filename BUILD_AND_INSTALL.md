# ChatDemo：构建与安装 Android APK

## 重要说明
本交付包含 Android 工程源码和测试服务器源码。当前执行环境没有 Android SDK、Gradle 或 Android 构建工具，因此这里不能诚实地声称已编译出 APK，也没有伪造 APK 文件。请按下面步骤在安装了 Android Studio 的电脑上构建。

## 1. 环境
- Android Studio（建议最新版稳定版）
- Android SDK Platform 35
- JDK 17
- Gradle 8.9 或兼容版本

## 2. 构建
1. 解压 `ChatDemo-source.zip`。
2. Android Studio 打开 `ChatDemo/android`。
3. 等待 Gradle Sync；按提示安装 Android SDK Platform 35 和 Build Tools。
4. 在 `app/build.gradle.kts` 中确认 `BASE_URL`：
   - Android 模拟器连接电脑：`http://10.0.2.2:3000/`
   - 真机连接测试服务器：改为服务器 HTTPS URL，例如 `https://test.example.com/`
5. 修改测试环境配置，不要在公开发行 APK 中嵌入固定上传密钥。
6. 选择 Build → Build Bundle(s) / APK(s) → Build APK(s)。

输出位置：
`android/app/build/outputs/apk/debug/app-debug.apk`

也可在项目根目录运行：
- macOS/Linux：`./build-apk.sh`
- Windows：`build-apk.bat`

前提是系统已安装并配置 Gradle。

## 3. 启动测试服务器
```bash
cd server
cp .env.example .env
```
编辑 `.env`，设置足够长且随机的 `UPLOAD_API_KEY` 与 `ADMIN_TOKEN`，然后：
```bash
npm install
npm start
```
管理页：`http://服务器地址:3000/admin`

## 4. 安装到 Android 手机
- 将 `app-debug.apk` 复制到手机。
- 在手机上打开 APK。
- 若系统提示，允许本次安装来源安装应用。
- 安装后打开 ChatDemo 测试版。

## 5. 测试流程
- 聊天页使用本地模拟会话。
- 通讯录页点击“导入通讯录”，在 Android 系统权限弹窗中选择允许。应用显示联系人后，再点击“确认上传”才会发送到测试服务器。
- 发现页点击“选择照片”，在系统照片选择器中挑选照片，再点击“确认上传所选照片”。
- 管理后台使用 `ADMIN_TOKEN` 查看和删除测试记录。

## 6. 真机联网与安全
- 真机必须能访问服务器；不要使用模拟器专用的 `10.0.2.2`。
- 正式或跨公网测试请配置 HTTPS。
- 客户端内置 API key 可被提取；正式方案应增加登录、服务端签发短期令牌、限流、审计和数据保留策略。
- 仅处理测试参与者明确授权的数据；不要导入未同意联系人的通讯录。
