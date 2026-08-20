package com.example.quickbillmate.ui.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.DefaultInfoValues
import com.example.quickbillmate.util.AppVersion

/** 首次安装引导页：读取/写入默认信息并置位引导完成标志。 */
class OnboardingViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    var defaults by mutableStateOf(repo.settings.defaultsSnapshot())
        private set

    fun updateDefaults(values: DefaultInfoValues) {
        defaults = values
    }

    fun complete() {
        repo.settings.applyDefaults(defaults)
        repo.settings.onboardingCompleted = true
        repo.settings.lastSeenVersionCode = AppVersion.code(app)
    }

    fun skip() {
        repo.settings.onboardingCompleted = true
        repo.settings.lastSeenVersionCode = AppVersion.code(app)
    }
}
