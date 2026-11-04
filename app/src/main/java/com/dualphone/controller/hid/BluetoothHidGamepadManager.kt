package com.dualphone.controller.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.dualphone.controller.core.BluetoothPermissions
import com.dualphone.controller.core.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Registers this phone as a Bluetooth HID Gamepad (HID Device role).
 * The host (gaming phone) pairs via system Bluetooth settings — no receiver app required.
 *
 * Requires API 28+ and OEM support for [BluetoothHidDevice].
 */
@SuppressLint("MissingPermission")
class BluetoothHidGamepadManager(private val context: Context) {

    enum class Status {
        UNAVAILABLE,
        BLUETOOTH_OFF,
        NEED_PERMISSION,
        IDLE,
        REGISTERING,
        REGISTERED,
        CONNECTED,
        ERROR
    }

    data class UiState(
        val status: Status = Status.IDLE,
        val message: String = "HID idle",
        val hostName: String? = null,
        val hostAddress: String? = null,
        val reportsSent: Long = 0,
        val lastError: String? = null,
        val profileReady: Boolean = false,
        val appRegistered: Boolean = false
    )

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _ui = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _ui.asStateFlow()

    private val executor = Executors.newSingleThreadExecutor()
    private var hidDevice: BluetoothHidDevice? = null
    private val registered = AtomicBoolean(false)
    private val connectedDevice = AtomicReference<BluetoothDevice?>(null)
    private val reportsSent = AtomicLong(0)
    private val lastReport = AtomicReference(HidReportEncoder.neutral())
    private val running = AtomicBoolean(false)

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE || proxy !is BluetoothHidDevice) return
            hidDevice = proxy
            _ui.value = _ui.value.copy(profileReady = true, message = "HID profile ready")
            Log.i(TAG, "HID_DEVICE profile connected")
            registerAppInternal()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                registered.set(false)
                connectedDevice.set(null)
                _ui.value = _ui.value.copy(
                    profileReady = false,
                    appRegistered = false,
                    status = Status.IDLE,
                    message = "HID profile disconnected"
                )
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.i(TAG, "onAppStatusChanged registered=$registered plugged=${pluggedDevice?.address}")
            this@BluetoothHidGamepadManager.registered.set(registered)
            _ui.value = _ui.value.copy(
                appRegistered = registered,
                status = if (registered) Status.REGISTERED else Status.IDLE,
                message = if (registered) {
                    "Registered as HID Gamepad — pair from the gaming phone Bluetooth settings"
                } else {
                    "HID app unregistered"
                }
            )
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.i(TAG, "onConnectionStateChanged state=$state device=${device?.address}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice.set(device)
                    _ui.value = _ui.value.copy(
                        status = Status.CONNECTED,
                        hostName = device?.name,
                        hostAddress = device?.address,
                        message = "Connected to ${device?.name ?: device?.address}"
                    )
                    // Send neutral so host sees a live device
                    sendReport(GamepadState.EMPTY, force = true)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice.get()?.address == device?.address) {
                        connectedDevice.set(null)
                    }
                    _ui.value = _ui.value.copy(
                        status = if (registered.get()) Status.REGISTERED else Status.IDLE,
                        hostName = null,
                        hostAddress = null,
                        message = "Host disconnected"
                    )
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                hidDevice?.replyReport(device, type, id, lastReport.get())
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {}
        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            connectedDevice.set(null)
            _ui.value = _ui.value.copy(
                status = if (registered.get()) Status.REGISTERED else Status.IDLE,
                message = "Virtual cable unplugged"
            )
        }
    }

    fun refreshStatus() {
        when {
            adapter == null ->
                _ui.value = UiState(Status.UNAVAILABLE, "Bluetooth hardware unavailable")
            !BluetoothPermissions.hasAll(context) ->
                _ui.value = _ui.value.copy(status = Status.NEED_PERMISSION, message = "Bluetooth permission required")
            adapter?.isEnabled != true ->
                _ui.value = _ui.value.copy(status = Status.BLUETOOTH_OFF, message = "Enable Bluetooth")
            registered.get() && connectedDevice.get() != null ->
                _ui.value = _ui.value.copy(status = Status.CONNECTED)
            registered.get() ->
                _ui.value = _ui.value.copy(status = Status.REGISTERED)
            hidDevice != null ->
                _ui.value = _ui.value.copy(status = Status.IDLE, message = "HID profile ready — start registration")
            else ->
                _ui.value = _ui.value.copy(status = Status.IDLE, message = "Start HID Gamepad to register")
        }
    }

    fun start() {
        if (adapter == null) {
            _ui.value = UiState(Status.UNAVAILABLE, "Bluetooth HID not available on this device")
            return
        }
        if (!BluetoothPermissions.hasAll(context)) {
            _ui.value = _ui.value.copy(status = Status.NEED_PERMISSION, message = "Grant Bluetooth permissions")
            return
        }
        if (adapter?.isEnabled != true) {
            _ui.value = _ui.value.copy(status = Status.BLUETOOTH_OFF, message = "Turn on Bluetooth")
            return
        }
        if (running.getAndSet(true)) {
            if (hidDevice != null && !registered.get()) registerAppInternal()
            return
        }
        _ui.value = _ui.value.copy(status = Status.REGISTERING, message = "Binding HID profile…")
        val ok = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        if (!ok) {
            running.set(false)
            _ui.value = _ui.value.copy(
                status = Status.ERROR,
                message = "getProfileProxy(HID_DEVICE) failed — OEM may not support HID Device role",
                lastError = "getProfileProxy returned false"
            )
        }
    }

    private fun registerAppInternal() {
        val hid = hidDevice ?: return
        if (registered.get()) return
        _ui.value = _ui.value.copy(status = Status.REGISTERING, message = "Registering HID Gamepad app…")
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "DualPhone Gamepad",
                "DualPhone Bluetooth HID Gamepad",
                "DualPhone",
                BluetoothHidDevice.SUBCLASS1_GAMEPAD,
                HidReportDescriptor.COMPACT_DESCRIPTOR
            )
            val inQos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
            )
            val outQos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
            )
            val accepted = hid.registerApp(sdp, null, inQos, executor, hidCallback)
            if (!accepted) {
                _ui.value = _ui.value.copy(
                    status = Status.ERROR,
                    message = "registerApp rejected — this OEM may block third-party HID Device",
                    lastError = "registerApp returned false"
                )
            }
            // Real success is confirmed in onAppStatusChanged(registered=true)
        } catch (e: SecurityException) {
            _ui.value = _ui.value.copy(
                status = Status.NEED_PERMISSION,
                message = "Bluetooth permission denied",
                lastError = e.message
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(
                status = Status.ERROR,
                message = "registerApp error: ${e.message}",
                lastError = e.message
            )
        }
    }

    /**
     * Encode and send a HID input report to the connected host.
     * Safe to call from any thread; no-ops if not connected.
     */
    fun sendReport(state: GamepadState, force: Boolean = false): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice.get() ?: return false
        if (!registered.get()) return false
        val report = HidReportEncoder.encode(state)
        if (!force && HidReportEncoder.reportsEqual(report, lastReport.get())) {
            return true
        }
        return try {
            val ok = hid.sendReport(device, HidReportDescriptor.REPORT_ID.toInt() and 0xFF, report)
            if (ok) {
                lastReport.set(report)
                val n = reportsSent.incrementAndGet()
                if (n % 30L == 0L) {
                    _ui.value = _ui.value.copy(reportsSent = n)
                }
            }
            ok
        } catch (e: Exception) {
            Log.w(TAG, "sendReport failed: ${e.message}")
            false
        }
    }

    fun releaseAll() {
        sendReport(GamepadState.EMPTY, force = true)
    }

    fun stop() {
        running.set(false)
        try {
            releaseAll()
            hidDevice?.unregisterApp()
        } catch (_: Exception) {
        }
        registered.set(false)
        connectedDevice.set(null)
        try {
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        } catch (_: Exception) {
        }
        hidDevice = null
        _ui.value = UiState(status = Status.IDLE, message = "HID stopped")
    }

    fun isConnected(): Boolean = connectedDevice.get() != null && registered.get()

    companion object {
        private const val TAG = "BtHidGamepad"
    }
}
