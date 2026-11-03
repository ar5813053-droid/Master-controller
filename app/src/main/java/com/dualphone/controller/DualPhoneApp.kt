package com.dualphone.controller

import android.app.Application
import com.dualphone.controller.database.AppDatabase
import com.dualphone.controller.database.LayoutRepository
import com.dualphone.controller.hid.AccessibilityTouchMappingBackend
import com.dualphone.controller.hid.CompositeInputBackend
import com.dualphone.controller.hid.LiveInputTesterBackend
import com.dualphone.controller.preferences.SettingsRepository
import com.dualphone.controller.transport.ConnectionManager
import com.dualphone.controller.transport.bluetooth.BluetoothRfcommTransport
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

    lateinit var wifiTransport: TcpUdpInputTransport
        private set

    lateinit var bluetoothTransport: BluetoothRfcommTransport
        private set

    lateinit var wifiDirectManager: WiFiDirectManager
        private set

    lateinit var connectionManager: ConnectionManager
        private set

    lateinit var testerBackend: LiveInputTesterBackend
        private set

    lateinit var accessibilityBackend: AccessibilityTouchMappingBackend
        private set

    lateinit var inputBackend: CompositeInputBackend
        private set

    val transport: TcpUdpInputTransport get() = wifiTransport

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        val db = AppDatabase.getInstance(this)
        layoutRepository = LayoutRepository(db.layoutDao())
        wifiTransport = TcpUdpInputTransport(this)
        bluetoothTransport = BluetoothRfcommTransport(this)
        wifiDirectManager = WiFiDirectManager(this)
        connectionManager = ConnectionManager(
            this, wifiTransport, bluetoothTransport, wifiDirectManager
        )
        testerBackend = LiveInputTesterBackend()
        accessibilityBackend = AccessibilityTouchMappingBackend()
        inputBackend = CompositeInputBackend(testerBackend, accessibilityBackend)
        testerBackend.setActive(true)

        applicationScope.launch {
            layoutRepository.ensureDefaultExists()
        }
    }

    override fun onTerminate() {
        try {
            connectionManager.shutdown()
            applicationScope.launch {
                try { inputBackend.releaseAll() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        }
        super.onTerminate()
    }
}
