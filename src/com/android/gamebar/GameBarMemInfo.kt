/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object GameBarMemInfo {

    fun getRamUsage(): String {
        var memTotal = 0L
        var memAvailable = 0L

        try {
            BufferedReader(FileReader("/proc/meminfo")).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    line?.let {
                        when {
                            it.startsWith("MemTotal:") -> memTotal = parseMemValue(it)
                            it.startsWith("MemAvailable:") -> memAvailable = parseMemValue(it)
                        }
                        if (memTotal > 0 && memAvailable > 0) {
                            return@use
                        }
                    }
                }
            }
        } catch (e: IOException) {
            return "N/A"
        }

        if (memTotal == 0L) {
            return "N/A"
        }

        val usedKb = memTotal - memAvailable
        val usedMb = usedKb / 1024
        return usedMb.toString()
    }

    private fun parseMemValue(line: String): Long {
        val parts = line.split("\\s+".toRegex())
        if (parts.size < 3) {
            return 0L
        }
        return try {
            parts[1].toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    fun getRamSpeed(): String {
        val path = "/sys/devices/system/cpu/bus_dcvs/DDR/cur_freq"
        try {
            BufferedReader(FileReader(path)).use { br ->
                val line = br.readLine()
                if (!line.isNullOrEmpty()) {
                    try {
                        val khz = line.trim().toInt()
                        val mhz = khz / 1000f
                        return if (mhz >= 1000) {
                            String.format("%.3f GHz", mhz / 1000f)
                        } else {
                            String.format("%.0f MHz", mhz)
                        }
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
        } catch (e: IOException) {
            // ignore
        }
        return "N/A"
    }

    fun getRamTemp(): String {
        val path = "/sys/class/thermal/thermal_zone27/temp"
        try {
            BufferedReader(FileReader(path)).use { br ->
                val line = br.readLine()
                if (!line.isNullOrEmpty()) {
                    try {
                        val raw = line.trim().toInt()
                        val celsius = raw / 1000f
                        return String.format("%.1f°C", celsius)
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
        } catch (e: IOException) {
            // ignore
        }
        return "N/A"
    }
}
