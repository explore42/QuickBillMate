package com.example.quickbillmate.navigation

import android.net.Uri

/** 外部应用通过 ACTION_VIEW 打开 JSON 时待处理的导入文件（进程内单例）。 */
object PendingImport {
    var uri: Uri? = null
}
