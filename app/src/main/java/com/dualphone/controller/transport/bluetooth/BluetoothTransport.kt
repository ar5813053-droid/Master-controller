package com.dualphone.controller.transport.bluetooth

import com.dualphone.controller.transport.InputTransport

/**
 * Bluetooth RFCOMM transport interface.
 * App-to-app RFCOMM is the chosen architecture (no Bluetooth HID in this version).
 */
interface BluetoothTransport : InputTransport {
    suspend fun startAdvertising(serviceName: String): Result<Unit>
    suspend fun stopAdvertising()
    suspend fun pair(deviceAddress: String): Result<Unit>
}
