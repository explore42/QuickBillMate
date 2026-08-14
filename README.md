# 快贝智单（QuickBillMate）

<p align="center"><img src="docs/logo.png" width="96" alt="快贝智单 Logo"></p>

快贝智单 QuickBillMate，面向小微商户和建材/日杂销售场景的 **便捷销售单据生成器** 安卓软件，免费，开源，无需联网，体积很小（3.3MB）。
在手机上填写客户与单据信息、勾选商品，生成销售单据的高清图片，即可保存或分享给微信好友。
点击页面右侧的Releases，下载第一个APK安装包到手机上，安装后即可使用（如提示“未知来源”，选择允许本次安装即可）。

## 核心功能

单据图片导出示例（二维码和电话号码仅为示意）：

![单据图片导出示例](docs/screenshots/bill_exported.png)

- **单据**：新建 / 编辑 / 实时预览。
- **导出图片**：可以预览与导出单据图片，一键保存到相册或分享。
- **微信二维码**：在设置中上传收款码图片，方形裁剪后显示在所有单据左上角。
- **单据样式**：内置 4 套预设（经典单据、经典单据（简洁）、简洁现代、商务蓝），支持自定义。
- **商品库**：增删改查、收藏、按字母索引，支持 JSON 批量导入 / 导出（文件或剪贴板）。
- **客户库**：增删改查、收藏、多电话支持、通讯录导入、拼音排序、按字母索引。
- **更多**：客户电话拨打，深色 / 浅色主题……

## APP界面预览

|                单据列表                |           单据编辑（实时预览）           |                 单据详情                 |
| :------------------------------------: | :--------------------------------------: | :--------------------------------------: |
| ![单据列表](docs/screenshots/home.png) | ![单据编辑](docs/screenshots/editor.png) | ![单据详情](docs/screenshots/detail.png) |

|                  商品库                  |                  设置                  |
| :--------------------------------------: | :------------------------------------: |
| ![商品库](docs/screenshots/products.png) | ![设置](docs/screenshots/settings.png) |

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Room（SQLite）数据存储
- minSdk 29 / targetSdk 37

## 监控与合规

- 应用不申请联网权限。
- 崩溃监控采用**本地日志**方案：`filesDir/crash_logs/` 记录崩溃堆栈（时间、应用版本、设备信息、异常堆栈），不包含任何业务数据。
- 性能监控（启动耗时、卡顿率、APM）暂未接入，后续按需评估。

## 数据库与升级

- 数据库启用 Room 迁移机制。
- 数据库使用 TRUNCATE journal 模式，系统备份/恢复时主库文件保持一致。

## 构建与运行

使用 Android Studio 打开仓库后直接运行，或在命令行执行：

```bash
./gradlew assembleDebug   # 构建 debug APK
./gradlew installDebug    # 构建并安装到已连接的设备/模拟器
```

安装包名：`com.example.quickbillmate`

## 开发文档

- [设计文档](Design.md)
- [商品 JSON 导入模板](docs/products_template.json)

## 许可证

[Apache License 2.0](LICENSE)
