package com.example.quickbillmate.render

import com.example.quickbillmate.data.db.StylePreset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StyleParams(
    val paperWidthDp: Int = 800,
    val titleFontSizeSp: Int = 22,
    val titleBold: Boolean = true,
    val titleLetterSpacing: Int = 6,
    val titleUnderline: Boolean = false,
    val titleUnderlineColor: String = "#000000",
    val headerFontSizeSp: Int = 12,
    val bodyFontSizeSp: Int = 13,
    val tableBorderWidthPx: Int = 1,
    val tableBorderColor: String = "#000000",
    val headerBgColor: String = "#E8E8E8",
    val headerTextColor: String = "#000000",
    val totalRowBgColor: String = "#E8E8E8",
    val fontFamily: String = "system_serif",
    val watermarkEnabled: Boolean = true,
    val watermarkText: String = "— 快贝智单 QuickBillMate —",
    val watermarkFontSizeSp: Int = 11,
    val watermarkColor: String = "#666666",
    val footerTextSizeSp: Int = 13,
    val infoLabelWidthPx: Int = 90,
    val amountBold: Boolean = false,
) {
    fun toJson(): String = StylePresets.json.encodeToString(this)

    companion object {
        fun fromJson(json: String): StyleParams =
            try {
                StylePresets.json.decodeFromString<StyleParams>(json)
            } catch (_: Exception) {
                StyleParams()
            }
    }
}

data class BuiltinPreset(
    val key: String,
    val name: String,
    val params: StyleParams,
)

object StylePresets {
    val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    val classic = BuiltinPreset("classic", "经典单据", StyleParams())

    val modern = BuiltinPreset(
        key = "modern",
        name = "简洁现代",
        params = StyleParams(
            titleLetterSpacing = 2,
            headerBgColor = "#0DB8942E",
            headerTextColor = "#333333",
            totalRowBgColor = "#FFFFFF",
            fontFamily = "system_sans",
            tableBorderWidthPx = 0,
        ),
    )

    val business = BuiltinPreset(
        key = "business",
        name = "商务蓝",
        params = StyleParams(
            titleUnderline = true,
            titleUnderlineColor = "#1F4E79",
            headerBgColor = "#1F4E79",
            headerTextColor = "#FFFFFF",
            totalRowBgColor = "#DCE6F1",
            amountBold = true,
        ),
    )

    val builtIns: List<BuiltinPreset> = listOf(classic, modern, business)

    fun builtInByKey(key: String): BuiltinPreset? = builtIns.firstOrNull { it.key == key }

    fun builtInName(key: String): String = builtInByKey(key)?.name ?: key

    /** 解析单据引用的预设：自定义优先，找不到则回退内置，再回退经典。 */
    fun resolve(presetKey: String?, customPresets: List<StylePreset>): StyleParams {
        val key = presetKey ?: "classic"
        val customId = key.removePrefix("custom:").toLongOrNull()
        if (customId != null) {
            customPresets.firstOrNull { it.id == customId }?.let {
                return StyleParams.fromJson(it.paramsJson)
            }
        }
        return builtInByKey(key)?.params ?: StyleParams()
    }

    fun customKey(id: Long): String = "custom:$id"
}
