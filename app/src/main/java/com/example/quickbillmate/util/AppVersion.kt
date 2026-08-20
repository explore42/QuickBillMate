package com.example.quickbillmate.util

import android.content.Context

/** 当前安装版本信息（升级说明/引导页共用）。 */
object AppVersion {
    fun code(context: Context): Int =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        }.getOrElse { 0 }

    fun name(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrElse { "" }
}
