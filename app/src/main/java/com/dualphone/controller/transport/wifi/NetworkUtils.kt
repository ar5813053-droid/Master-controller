package com.dualphone.controller.transport.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Best-effort local IPv4 used for LAN / hotspot communication.
     * Does not assume a fixed hotspot gateway IP.
     */
    fun getLocalIpAddress(context: Context): String? {
        // Prefer active network link addresses
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return scanInterfaces()
            val lp: LinkProperties? = cm.getLinkProperties(network)
            lp?.linkAddresses?.forEach { addr ->
                val ia = addr.address
                if (ia is Inet4Address && !ia.isLoopbackAddress) {
                    return ia.hostAddress
                }
            }
        } catch (_: Exception) {
        }
        return scanInterfaces()
    }

    private fun scanInterfaces(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        // Prefer private ranges typical of hotspot / LAN
                        if (host.startsWith("192.168.") ||
                            host.startsWith("10.") ||
                            host.startsWith("172.")
                        ) {
                            return host
                        }
                    }
                }
            }
            // Fallback: any non-loopback IPv4
            val interfaces2 = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces2) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
    }
}
