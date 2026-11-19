# ChatDemo（仿聊天界面的透明授权测试项目）

这是一个用于内部测试的 Android 原生 Kotlin + Jetpack Compose 示例。它不是微信，也不隶属于腾讯。
通讯录与照片均采用**用户主动操作**：通讯录需要系统权限并在应用内再次确认；照片使用 Android Photo Picker，由用户逐张/多张选择。不会后台扫描、静默上传或自动同步。

## 项目结构
- `android/`：Android Studio 工程（Kotlin、Compose、OkHttp）
- `server/`：Node.js + Express 测试 API、JSON 文件存储、简单管理页面
- `docker-compose.yml`：可选容器启动方式

## 运行后端
```bash
cd server
cp .env.example .env
npm install
npm start
```
默认监听 `0.0.0.0:3000`。请修改 `.env` 中的 `UPLOAD_API_KEY` 和 `ADMIN_TOKEN`。
管理页：`http://localhost:3000/admin`，API 健康检查：`GET /api/health`。

开发环境 Android 模拟器访问宿主机请使用 `http://10.0.2.2:3000`；
真机请将 `BASE_URL` 改为测试服务器的 HTTPS 地址，并确保手机可访问。
不要把 API key 硬编码进公开发行包。此示例为内网/测试脚手架，正式环境应使用登录态短期令牌、HTTPS、速率限制、审计日志、数据保留期限和删除接口。

## 运行 Android
用 Android Studio 打开 `android/`，等待 Gradle 同步后运行 `app`。
最低 Android 版本 API 26。示例使用 Android Photo Picker，不申请整个媒体库权限。
通讯录功能在用户点击“导入通讯录”后请求 `READ_CONTACTS`，展示数量和用途，再由用户明确点击“确认上传所列联系人”上传。
照片功能由系统选择器返回用户选中的 URI，用户点击“上传所选照片”后才上传。

## API
- `GET /api/health`
- `POST /api/contacts`，请求头 `X-Upload-Key`，JSON `{ "contacts": [{ "name": "...", "phones": ["..."] }] }`
- `POST /api/photos`，请求头 `X-Upload-Key`，multipart 字段 `photos`
- `GET /api/admin/records`，`Authorization: Bearer <ADMIN_TOKEN>`
- `DELETE /api/admin/records/:id`，`Authorization: Bearer <ADMIN_TOKEN>`

后端只用于你拥有/获准管理的测试环境。仅导入测试参与者明确同意提供的数据。


## APK 构建
本源码包不包含预编译 APK。请参阅项目根目录 `BUILD_AND_INSTALL.md`；本次运行环境未安装 Android SDK/Gradle，无法在此处完成 APK 编译。
