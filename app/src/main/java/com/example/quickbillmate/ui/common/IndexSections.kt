package com.example.quickbillmate.ui.common

/** 首页时间索引：把单据日期 yyyy-MM-dd 转为分区键 yyyy-MM；空日期归入“其他”。 */
fun monthKey(date: String): String = if (date.isBlank()) "其他" else date.take(7)

/** 分区气泡文本：yyyy-MM → “yyyy年M月”。 */
fun monthBubble(key: String): String =
    if (key == "其他") {
        "其他"
    } else if (key.length >= 7) {
        "${key.substring(0, 4)}年${key.substring(5, 7).trimStart('0')}月"
    } else {
        key
    }

/** 首页月份分区：按单据列表中出现的顺序去重。 */
fun monthSections(keys: List<String>): List<IndexSection> =
    keys.distinct().map { IndexSection(it, monthBubble(it)) }

/**
 * 字母索引分区：可选收藏星标（置顶）→ A-Z → #。
 * letters 为每项的分区键（"#" 表示无法归入字母）；收藏星标独立置顶，
 * 星标仅在 hasFavorites 为 true 时出现，收藏项仍保留在各自字母分区中。
 */
fun letterSections(letters: List<String>, hasFavorites: Boolean = false): List<IndexSection> {
    val distinct = letters.toSet()
    val star = if (hasFavorites) listOf(IndexSection("♥", "收藏")) else emptyList()
    val alpha = distinct.filter { it != "#" }.sorted().map { IndexSection(it, it) }
    val other = if ("#" in distinct) listOf(IndexSection("#", "#")) else emptyList()
    return star + alpha + other
}
