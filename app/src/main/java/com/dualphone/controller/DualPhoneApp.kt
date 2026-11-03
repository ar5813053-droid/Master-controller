package com.dualphone.controller

import android.app.Application
import com.dualphone.controller.database.AppDatabase
import com.dualphone.controller.database.LayoutRepository
import com.dualphone.controller.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DualPhoneApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var layoutRepository: LayoutRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        val db = AppDatabase.getInstance(this)
        layoutRepository = LayoutRepository(db.layoutDao())

        applicationScope.launch {
            layoutRepository.ensureDefaultExists()
        }
    }
}
