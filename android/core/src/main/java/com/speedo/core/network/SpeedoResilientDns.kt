package com.speedo.core.network

import android.util.Log
import okhttp3.Dns
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException

/**
 * SpeedoResilientDns
 * ==================
 * Multi-tier resilient DNS resolver engineered to eliminate 'UnknownHostException'
 * on Indian 5G SA networks (Jio 5G, Airtel 5G) where carrier DNS servers fail
 * IPv6 NAT64 synthesis or throttle/block Railway cloud domains.
 *
 * Resolution Tiers:
 * 1. Primary: Native Android System DNS (0ms overhead under normal conditions)
 * 2. Secondary: Public DNS-over-HTTPS (Google 8.8.8.8 & Cloudflare 1.1.1.1)
 * 3. Tertiary: Static Edge IP Fallback (verified Railway Edge proxy at 69.46.46.7
 *    plus RFC 6052 Well-Known NAT64 Prefix for IPv6-only cellular networks)
 */
object SpeedoResilientDns : Dns {
    private const val TAG = "SpeedoResilientDns"
    private const val RAILWAY_HOST = "web-production-5d826.up.railway.app"

    private val RAILWAY_FALLBACK_IPS: List<InetAddress> by lazy {
        val list = mutableListOf<InetAddress>()
        try {
            // Direct IPv4 Railway Edge IP
            list.add(InetAddress.getByAddress(RAILWAY_HOST, byteArrayOf(69.toByte(), 46.toByte(), 46.toByte(), 7.toByte())))
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating IPv4 fallback: ${e.message}")
        }
        try {
            // RFC 6052 Well-Known NAT64 Prefix (64:ff9b::/96) -> 64:ff9b::69.46.46.7
            // Allows pure IPv6 5G networks to route directly via carrier PLAT gateway
            list.add(InetAddress.getByName("64:ff9b::452e:2e07"))
        } catch (_: Exception) {
            // IPv6 not supported on device; safe to ignore
        }
        list
    }

    override fun lookup(hostname: String): List<InetAddress> {
        // 0. Bypass for raw IP addresses and localhost
        if (hostname.matches(Regex("^[0-9.]+$")) || hostname == "localhost" || hostname.contains(":")) {
            return try {
                listOf(InetAddress.getByName(hostname))
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }

        // Tier 1: Try Standard System DNS
        try {
            val systemAddresses = Dns.SYSTEM.lookup(hostname)
            if (systemAddresses.isNotEmpty()) {
                return systemAddresses
            }
        } catch (e: Exception) {
            Log.w(TAG, "System DNS failed for $hostname (${e.message}). Trying DoH fallback...")
        }

        // Tier 2: Public DNS-over-HTTPS (Google & Cloudflare)
        val dohAddresses = resolveViaDoh(hostname)
        if (dohAddresses.isNotEmpty()) {
            Log.i(TAG, "Resolved $hostname via DoH: $dohAddresses")
            return dohAddresses
        }

        // Tier 3: Static Edge IP Fallback for Railway Cloud Domain
        if (hostname.equals(RAILWAY_HOST, ignoreCase = true) && RAILWAY_FALLBACK_IPS.isNotEmpty()) {
            Log.i(TAG, "Using resilient static edge fallback for $RAILWAY_HOST")
            return RAILWAY_FALLBACK_IPS
        }

        throw UnknownHostException("SpeedoResilientDns: Unable to resolve host '$hostname' across all tiers")
    }

    private fun resolveViaDoh(hostname: String): List<InetAddress> {
        val result = mutableListOf<InetAddress>()

        // Try Google DoH
        try {
            val url = URL("https://dns.google/resolve?name=" + hostname + "&type=A")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Speedo-Android/1.0")
            }
            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val answers = root.optJSONArray("Answer")
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val item = answers.getJSONObject(i)
                        val type = item.optInt("type")
                        val ip = item.optString("data")
                        if (type == 1 && ip.isNotBlank()) {
                            try {
                                result.add(InetAddress.getByName(ip))
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Google DoH query failed: ${e.message}")
        }

        if (result.isNotEmpty()) return result

        // Try Cloudflare DoH
        try {
            val url = URL("https://cloudflare-dns.com/dns-query?name=" + hostname + "&type=A")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Accept", "application/dns-json")
                setRequestProperty("User-Agent", "Speedo-Android/1.0")
            }
            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val answers = root.optJSONArray("Answer")
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val item = answers.getJSONObject(i)
                        val type = item.optInt("type")
                        val ip = item.optString("data")
                        if (type == 1 && ip.isNotBlank()) {
                            try {
                                result.add(InetAddress.getByName(ip))
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Cloudflare DoH query failed: ${e.message}")
        }

        return result
    }
}
