package com.example.quickbillmate

import android.app.Application
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.QrImageStore
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.util.LocalCrashLogger

class QuickBillMateApp : Application() {
    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        LocalCrashLogger.init(this)
        val database = AppDatabase.get(this)
        repository = AppRepository(database, SettingsStore(this), QrImageStore(this))
    }
}
