package com.example.quickbillmate.util

import android.icu.text.Transliterator
import java.text.Normalizer

/**
 * 取汉字拼音首字母，用于列表 A-Z 索引。
 * 英文/数字开头直接取首字符；中文通过 ICU 音译得到拼音首字母；无法识别归入 "#"。
 */
object Pinyin {
    private val hanLatin: Transliterator? by lazy {
        runCatching { Transliterator.getInstance("Han-Latin") }.getOrNull()
    }

    private val latinAscii: Transliterator? by lazy {
        runCatching { Transliterator.getInstance("Latin-ASCII") }.getOrNull()
    }

    fun firstLetter(name: String): String {
        val first = name.trim().firstOrNull() ?: return "#"
        if (first.isLetter() && first.code < 128) return first.uppercaseChar().toString()
        val han = hanLatin ?: return "#"
        return try {
            var latin = han.transliterate(first.toString())
            latinAscii?.let { latin = it.transliterate(latin) }
            val plain = Normalizer.normalize(latin, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
            plain.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        } catch (_: Exception) {
            "#"
        }
    }

    /** 字母索引排序键：A-Z 按字母序，# 固定排最后。 */
    fun letterSortKey(letter: String): String = if (letter == "#") "{" else letter

    /**
     * 按拼音首字母排序（收藏优先 → A-Z → # 最后）；
     * 同字母保持原相对顺序（稳定排序）。
     */
    fun <T> sortByPinyinLetter(
        items: List<T>,
        favoriteOf: (T) -> Boolean,
        letterOf: (T) -> String,
    ): List<T> = items.sortedWith(
        compareByDescending<T> { favoriteOf(it) }.thenBy { letterSortKey(letterOf(it)) }
    )
}
