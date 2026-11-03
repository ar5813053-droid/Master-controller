package com.dualphone.controller

import android.app.Application
import com.dualphone.controller.database.AppDatabase
import com.dualphone.controller.database.LayoutRepository
import com.dualphone.controller.preferences.SettingsRepository
import com.dualphone.controller.transport.wifi.TcpUdpInputTransport
import com.dualphone.controller.transport.wifi.WiFiDirectManager
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

    lateinit var transport: TcpUdpInputTransport
        private set

    lateinit var wifiDirectManager: WiFiDirectManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        val db = AppDatabase.getInstance(this)
        layoutRepository = LayoutRepository(db.layoutDao())
        transport = TcpUdpInputTransport(this)
        wifiDirectManager = WiFiDirectManager(this)

        applicationScope.launch {
            layoutRepository.ensureDefaultExists()
        }
    }

    override fun onTerminate() {
        try {
            transport.shutdown()
            wifiDirectManager.release()
        } catch (_: Exception) {
        }
        super.onTerminate()
    }
}
