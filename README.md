# 快贝智单（QuickBillMate）

快贝智单是一款面向小微商户和建材/日杂销售场景的 **Android 销售清单生成器**。在手机上填写客户与单据信息、勾选商品，即可实时预览销售清单，并一键导出为高清 PNG 图片——保存到相册或直接分享给微信、QQ 等。

## 核心功能

- **单据**：新建 / 编辑 / 实时预览，按“收藏 → 时间”分组，支持搜索与多选批量操作（复制、编辑、导出、删除）。
- **导出图片**：预览与导出同源（Compose 布局引擎渲染），一键保存到相册或分享。
- **微信二维码**：在设置中上传收款码图片，方形裁剪后显示在所有单据左上角，预览与导出图片同步生效；可随时更换或移除。
- **图片样式**：内置 4 套预设（经典单据、经典单据（简洁）、简洁现代、商务蓝），支持自定义预设并配置列顺序、列宽、字体、颜色等参数。
- **商品库**：增删改查、收藏、按字母索引，支持 JSON 批量导入 / 导出（文件或剪贴板）。
- **客户库**：增删改查、收藏、多电话支持、通讯录导入、拼音排序、按字母索引。
- **更多**：长按多选与分组全选、单据详情多电话逐个拨打、深色 / 浅色主题。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Room（SQLite）数据存储
- Navigation Compose
- kotlinx.serialization、Coroutines
- minSdk 29 / targetSdk 37

## 构建与运行

使用 Android Studio 打开仓库后直接运行，或在命令行执行：

```bash
./gradlew assembleDebug   # 构建 debug APK
./gradlew installDebug    # 构建并安装到已连接的设备/模拟器
```

安装包名：`com.example.quickbillmate`

## 发布构建

发布签名通过仓库根目录的 `keystore.properties` 提供（已被 `.gitignore` 忽略，不会提交）。复制 [keystore.properties.example](keystore.properties.example) 为 `keystore.properties` 并填入真实密钥后执行：

```bash
./gradlew assembleRelease   # 构建已签名的 release APK（开启 R8 最小化）
```

未配置 `keystore.properties` 时，`assembleRelease` 会回退使用 debug 签名并打印警告，仅用于开发验证；正式发布前请配置真实密钥。

## 相关文档

- [设计文档](Design.md)
- [商品 JSON 导入模板](docs/products_template.json)

## 许可证

[Apache License 2.0](LICENSE)
