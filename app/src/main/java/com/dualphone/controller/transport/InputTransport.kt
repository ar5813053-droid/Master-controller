package com.dualphone.controller.transport

import com.dualphone.controller.core.GamepadState
import kotlinx.coroutines.flow.Flow

/**
 * Core transport abstraction for sending gamepad state between devices.
 * Implementations will cover Wi-Fi Hotspot (TCP/UDP), Wi-Fi Direct and Bluetooth RFCOMM.
 */
interface InputTransport {
    val isConnected: Flow<Boolean>
    suspend fun connect(targetId: String): Result<Unit>
    suspend fun disconnect()
    suspend fun sendState(state: GamepadState): Result<Unit>
    fun receiveState(): Flow<GamepadState>
}
