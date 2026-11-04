package com.dualphone.controller.transport

/**
 * Parses pairing strings produced by the receiver.
 *
 * Wi-Fi format:  IP:TCP_PORT:TOKEN
 *   TOKEN is 8-char uppercase hex (as shown in UI / QR). Decimal tokens are also accepted.
 * Bluetooth:     BT:AA:BB:CC:DD:EE:FF:TOKEN  or  AA-BB-CC-DD-EE-FF:TOKEN
 */
object PairingParser {

    data class WifiPairing(
        val host: String,
        val tcpPort: Int,
        val token: Int,
        val tokenDisplay: String
    )

    data class BluetoothPairing(
        val address: String,
        val token: Int,
        val tokenDisplay: String
    )

    /** True if string looks like BT MAC+token or 8-char hex token (no IP dots). */
    fun looksLikeBluetooth(raw: String): Boolean {
        val t = raw.trim().replace(" ", "")
        if (t.startsWith("BT:", ignoreCase = true)) return true
        val parts = t.split(":")
        if (parts.size >= 7 && parts.take(6).all { part ->
            part.length in 1..2 && part.all { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' }
        }) return true
        if (!t.contains('.') && t.length == 8 && t.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            return true
        }
        return false
    }

    fun parseWifi(raw: String): Result<WifiPairing> {
        val cleaned = raw.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty pairing string"))
        }
        // Split from the right so IPv6-ish mistakes don't break; we require exactly 3 fields for IPv4
        val parts = cleaned.split(":")
        if (parts.size != 3) {
            return Result.failure(
                IllegalArgumentException("Expected IP:PORT:TOKEN (got ${parts.size} fields)")
            )
        }
        val host = parts[0]
        if (host.isBlank() || host.count { it == '.' } != 3) {
            return Result.failure(IllegalArgumentException("Invalid IPv4 address"))
        }
        val port = parts[1].toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid TCP port"))
        if (port !in 1..65535) {
            return Result.failure(IllegalArgumentException("TCP port out of range"))
        }
        val tokenResult = parseToken(parts[2])
        val token = tokenResult.getOrElse {
            return Result.failure(it)
        }
        return Result.success(WifiPairing(host, port, token, parts[2].uppercase()))
    }

    fun parseBluetooth(raw: String): Result<BluetoothPairing> {
        var cleaned = raw.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        if (cleaned.startsWith("BT:", ignoreCase = true)) {
            cleaned = cleaned.substring(3)
        }
        // Dash form: AA-BB-CC-DD-EE-FF:TOKEN
        if (cleaned.count { it == '-' } == 5 && cleaned.contains(":")) {
            val idx = cleaned.lastIndexOf(':')
            val address = cleaned.substring(0, idx).replace('-', ':').uppercase()
            val tokenStr = cleaned.substring(idx + 1)
            val token = parseToken(tokenStr).getOrElse { return Result.failure(it) }
            return Result.success(BluetoothPairing(address, token, tokenStr.uppercase()))
        }
        val parts = cleaned.split(":")
        if (parts.size < 7) {
            return Result.failure(IllegalArgumentException("BT format: AA:BB:CC:DD:EE:FF:TOKEN"))
        }
        val address = parts.take(6).joinToString(":").uppercase()
        val tokenStr = parts.drop(6).joinToString("") // in case of accidental split
        val token = parseToken(tokenStr).getOrElse { return Result.failure(it) }
        return Result.success(BluetoothPairing(address, token, tokenStr.uppercase()))
    }

    /**
     * Tokens are always displayed as 8-char hex. Prefer hex parse when length is 8
     * or when any A-F character is present, so all-digit hex is not misread as decimal.
     */
    fun parseToken(raw: String): Result<Int> {
        val t = raw.trim().uppercase()
        if (t.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty token"))
        }
        val preferHex = t.length == 8 || t.any { it in 'A'..'F' }
        if (preferHex) {
            val v = t.toLongOrNull(16)
                ?: return Result.failure(IllegalArgumentException("Invalid hex token"))
            return Result.success(v.toInt())
        }
        t.toIntOrNull()?.let { return Result.success(it) }
        t.toLongOrNull(16)?.let { return Result.success(it.toInt()) }
        return Result.failure(IllegalArgumentException("Invalid token"))
    }

    fun formatToken(token: Int): String = String.format("%08X", token)

    fun formatWifiPairing(host: String, tcpPort: Int, token: Int): String =
        "$host:$tcpPort:${formatToken(token)}"
}
