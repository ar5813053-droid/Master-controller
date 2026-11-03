package com.dualphone.controller.receiver

import com.dualphone.controller.core.GamepadState
import kotlinx.coroutines.flow.Flow

/**
 * Receiver side abstraction. Will host the service that accepts incoming input streams
 * and forwards them to a GameInputBackend in later milestones.
 */
interface InputReceiver {
    val isRunning: Flow<Boolean>
    val incomingState: Flow<GamepadState>
    suspend fun start(): Result<Unit>
    suspend fun stop()
}
