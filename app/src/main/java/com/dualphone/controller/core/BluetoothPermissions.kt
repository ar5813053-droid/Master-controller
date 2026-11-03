package com.dualphone.controller.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothPermissions {

    fun requiredRuntimePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            // Install-time BLUETOOTH is enough on most pre-31; location helps discovery
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasAll(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return requiredRuntimePermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
        // Pre-31: BLUETOOTH is normal/install-time
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
            PackageManager.PERMISSION_GRANTED ||
            // Some devices grant it at install; treat missing location as soft
            true
    }

    fun missing(context: Context): List<String> {
        return requiredRuntimePermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun userMessage(context: Context): String {
        val miss = missing(context)
        return if (miss.isEmpty()) {
            "Bluetooth ready"
        } else {
            "Bluetooth permission required: ${miss.joinToString { it.substringAfterLast('.') }}. Grant in system settings if permanently denied."
        }
    }
}
