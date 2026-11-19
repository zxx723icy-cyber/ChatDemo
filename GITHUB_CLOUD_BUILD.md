# 不用电脑：用 GitHub Actions 云端编译 APK

## 你需要
- Android 手机
- GitHub 账号
- ChatDemo 项目 ZIP（先在手机上下载并解压/上传文件）

## 操作步骤（手机浏览器也可）
1. 登录 https://github.com ，创建一个新仓库，例如 `ChatDemo`。建议设为 **Private（私有）**。
2. 将本项目源码上传到仓库根目录。注意要上传 `ChatDemo/` 里面的内容，而不是再套一层 ChatDemo 文件夹。必须包含 `.github/workflows/android-apk.yml` 和 `android/`。
3. 打开仓库的 **Actions** 标签页。如果首次使用，按提示启用 Actions。
4. 选择 **Build ChatDemo APK** 工作流，点击 **Run workflow**。也可以直接向 `main` 分支提交文件触发构建。
5. 等待工作流完成，打开该次运行页面，在 **Artifacts** 区域下载 `ChatDemo-debug-apk`。
6. 解压下载的 Artifact，得到 `app-debug.apk`，点击安装。若 Android 提示，允许浏览器/文件管理器本次安装未知来源应用。

## 注意
- 本仓库目前的示例 API 地址为 Android 模拟器地址 `10.0.2.2`，真机不能用它访问你电脑以外的服务器。安装 APK 前需要把 `android/app/build.gradle.kts` 中的 `BASE_URL` 改成你自己的可访问 HTTPS 测试服务器地址，再提交重新构建。
- 不要把真实联系人、真实照片或任何私人数据放进 GitHub 仓库或构建日志。
- 目前示例 APK 是 debug 构建，仅供测试；正式发布应使用签名配置、HTTPS、用户登录和短期令牌。
- GitHub Actions 只负责云端编译，不会自动替你创建服务器、托管 API 或管理后台。
