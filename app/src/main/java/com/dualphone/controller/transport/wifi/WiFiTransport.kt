package com.dualphone.controller.transport.wifi

import com.dualphone.controller.transport.InputTransport

/**
 * Marker interface for Wi-Fi based transports (Hotspot TCP/UDP or Wi-Fi Direct).
 * Concrete implementations will be added in later milestones.
 */
interface WiFiTransport : InputTransport {
    suspend fun startHotspot(): Result<String>
    suspend fun stopHotspot()
    suspend fun discoverPeers(): Result<List<String>>
}
