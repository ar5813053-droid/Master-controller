package com.dualphone.controller.transport

/**
 * Live diagnostics shared by controller and receiver sides.
 */
data class ConnectionDiagnostics(
    val isConnected: Boolean = false,
    val connectionType: String = "None",
    val remoteDeviceName: String = "",
    val remoteAddress: String = "",
    val sessionId: Int = 0,
    val packetsSent: Long = 0,
    val packetsReceived: Long = 0,
    val packetsLostEstimate: Long = 0,
    val updateRateHz: Float = 0f,
    val estimatedRttMs: Float? = null, // null = unavailable / not measured
    val lastPacketTimestampMs: Long = 0,
    val localIp: String = "",
    val tcpPort: Int = 0,
    val udpPort: Int = 0,
    val pairingTokenDisplay: String = "", // never the raw secret in production logs; UI only
    val statusMessage: String = "Disconnected"
)
