package com.example.quickbillmate.ui.changelog

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.util.AppVersion

/** 升级更新说明页：读取上次已读版本并展示累积说明，页面显示即标记已读。 */
class ChangelogViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    val versionCode: Int = AppVersion.code(app)

    var sections by mutableStateOf<List<VersionChange>>(emptyList())
        private set

    init {
        val lastSeen = repo.settings.lastSeenVersionCode
        sections = VersionChangelog.sectionsFor(
            lastSeen = lastSeen,
            current = versionCode,
            currentVersionName = AppVersion.name(app),
        )
        // 显示即标记：本版本说明展示过就不再重复打扰
        if (lastSeen < versionCode) {
            repo.settings.lastSeenVersionCode = versionCode
        }
    }
}
