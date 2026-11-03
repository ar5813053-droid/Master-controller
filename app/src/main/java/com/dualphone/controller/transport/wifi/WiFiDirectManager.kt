package com.dualphone.controller.transport.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class WifiDirectPeer(
    val deviceName: String,
    val deviceAddress: String,
    val status: Int
)

data class WifiDirectConnectionInfo(
    val isGroupOwner: Boolean,
    val groupOwnerAddress: String?,
    val isConnected: Boolean
)

/**
 * Thin wrapper around Android WifiP2pManager.
 * Transport layer still uses TcpUdpInputTransport after P2P group is formed.
 */
class WiFiDirectManager(private val context: Context) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager

    private var channel: WifiP2pManager.Channel? = null

    private val _peers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())
    val peers: StateFlow<List<WifiDirectPeer>> = _peers.asStateFlow()

    private val _connectionInfo = MutableStateFlow(WifiDirectConnectionInfo(false, null, false))
    val connectionInfo: StateFlow<WifiDirectConnectionInfo> = _connectionInfo.asStateFlow()

    private val _available = MutableStateFlow(manager != null)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _available.value = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    requestConnectionInfo()
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> { /* ignore */ }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun initialize(): Boolean {
        if (manager == null) {
            _statusMessage.value = "Wi-Fi Direct not available on this device"
            _available.value = false
            return false
        }
        channel = manager.initialize(context, Looper.getMainLooper(), null)
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
        _statusMessage.value = "Wi-Fi Direct ready"
        return true
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        val ch = channel ?: return
        val mgr = manager ?: return
        _statusMessage.value = "Discovering peers…"
        mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _statusMessage.value = "Discovery started"
            }
            override fun onFailure(reason: Int) {
                _statusMessage.value = "Discovery failed (code $reason)"
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        val ch = channel ?: return
        val mgr = manager ?: return
        mgr.requestPeers(ch) { peerList ->
            _peers.value = peerList.deviceList.map {
                WifiDirectPeer(it.deviceName ?: "Unknown", it.deviceAddress, it.status)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToPeer(deviceAddress: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val ch = channel
            val mgr = manager
            if (ch == null || mgr == null) {
                cont.resume(Result.failure(IllegalStateException("Wi-Fi Direct unavailable")))
                return@suspendCancellableCoroutine
            }
            val config = WifiP2pConfig().apply {
                this.deviceAddress = deviceAddress
            }
            mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _statusMessage.value = "Connecting…"
                    cont.resume(Result.success(Unit))
                }
                override fun onFailure(reason: Int) {
                    _statusMessage.value = "Connect failed (code $reason)"
                    cont.resume(Result.failure(IllegalStateException("Connect failed: $reason")))
                }
            })
        }

    @SuppressLint("MissingPermission")
    private fun requestConnectionInfo() {
        val ch = channel ?: return
        val mgr = manager ?: return
        mgr.requestConnectionInfo(ch) { info: WifiP2pInfo? ->
            if (info == null || !info.groupFormed) {
                _connectionInfo.value = WifiDirectConnectionInfo(false, null, false)
                return@requestConnectionInfo
            }
            val ownerAddr = info.groupOwnerAddress?.hostAddress
            _connectionInfo.value = WifiDirectConnectionInfo(
                isGroupOwner = info.isGroupOwner,
                groupOwnerAddress = ownerAddr,
                isConnected = true
            )
            _statusMessage.value = if (info.isGroupOwner) {
                "Group owner – $ownerAddr"
            } else {
                "Connected to group owner $ownerAddr"
            }
        }
    }

    fun disconnect() {
        val ch = channel ?: return
        val mgr = manager ?: return
        mgr.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionInfo.value = WifiDirectConnectionInfo(false, null, false)
                _statusMessage.value = "Disconnected"
            }
            override fun onFailure(reason: Int) {
                _statusMessage.value = "Remove group failed ($reason)"
            }
        })
    }

    fun release() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            receiverRegistered = false
        }
        channel = null
    }
}
