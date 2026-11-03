package com.dualphone.controller.transport

/**
 * Explicit receiver lifecycle – UI must not invent Listening while stopped.
 */
enum class ReceiverState {
    STOPPED,
    STARTING,
    LISTENING,
    AUTHENTICATING,
    CONNECTED,
    RECONNECTING,
    ERROR
}
