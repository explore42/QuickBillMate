package com.example.quickbillmate.ui.changelog

/** 单个版本的升级说明条目。 */
data class VersionChange(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val changes: List<String>,
)

/**
 * 应用内升级说明注册表：每个发布版本维护一条，随 APK 分发，不联网。
 */
object VersionChangelog {

    /** 全部已发布版本说明，按 versionCode 升序维护。 */
    val entries: List<VersionChange> = listOf(
        VersionChange(
            versionCode = 5,
            versionName = "1.2.0",
            title = "v1.2.0 主要更新",
            changes = listOf(
                "修复：软键盘弹出时弹窗不再被顶出屏幕或留出大空隙，底部输入更顺手",
                "优化：预置单位弹窗支持即时保存，删除需二次确认",
                "新增：首次安装引导页，引导填写默认信息",
                "新增：设置页数据导入导出——单据、商品、客户、默认信息可备份与迁移",
                "新增：首页数据报表——按时间、客户、商品统计单据与金额",
                "新增：升级更新说明，升级后第一时间了解新功能",
            ),
        ),
    )

    /**
     * 需要展示的条目：versionCode 落在 `(lastSeen, current]` 区间内，最新在前。
     */
    fun entriesFor(
        lastSeen: Int,
        current: Int,
        all: List<VersionChange> = entries,
    ): List<VersionChange> =
        all.filter { it.versionCode in (lastSeen + 1)..current }
            .sortedByDescending { it.versionCode }

    /**
     * 最终展示分段：优先注册表条目；区间内无条目但确实升级过时，给出通用说明兜底。
     */
    fun sectionsFor(
        lastSeen: Int,
        current: Int,
        currentVersionName: String,
        all: List<VersionChange> = entries,
    ): List<VersionChange> {
        val matched = entriesFor(lastSeen, current, all)
        if (matched.isNotEmpty()) return matched
        return if (lastSeen < current && current > 0) {
            listOf(
                VersionChange(
                    versionCode = current,
                    versionName = currentVersionName,
                    title = "已更新至 v${currentVersionName.ifBlank { "新版本" }}",
                    changes = emptyList(),
                )
            )
        } else {
            emptyList()
        }
    }
}
