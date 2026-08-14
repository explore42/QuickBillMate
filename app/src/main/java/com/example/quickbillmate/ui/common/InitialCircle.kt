package com.example.quickbillmate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text

/** 浅色圆形首字头像，用于商品/客户列表行，便于快速辨认。 */
@Composable
fun InitialCircle(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppThemeColors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppThemeTypography.titleSmall,
            color = AppThemeColors.onPrimaryContainer,
            maxLines = 1,
        )
    }
}
