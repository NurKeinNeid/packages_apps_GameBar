/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object GameBarGpuInfo {

    private const val GPU_USAGE_PATH = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
    private const val GPU_CLOCK_PATH = "/sys/class/kgsl/kgsl-3d0/gpuclk"
    private const val GPU_TEMP_PATH = "/sys/class/kgsl/kgsl-3d0/temp"

    fun getGpuUsage(): String {
        val line = readLine(GPU_USAGE_PATH) ?: return "N/A"
        val cleanLine = line.replace("%", "").trim()
        return try {
            val value = cleanLine.toInt()
            value.toString()
        } catch (e: NumberFormatException) {
            "N/A"
        }
    }

    fun getGpuClock(): String {
        val line = readLine(GPU_CLOCK_PATH) ?: return "N/A"
        val cleanLine = line.trim()
        return try {
            val hz = cleanLine.toLong()
            val mhz = hz / 1_000_000
            mhz.toString()
        } catch (e: NumberFormatException) {
            "N/A"
        }
    }

    fun getGpuTemp(): String {
        val line = readLine(GPU_TEMP_PATH) ?: return "N/A"
        val cleanLine = line.trim()
        return try {
            val raw = cleanLine.toFloat()
            val celsius = raw / 1000f
            String.format("%.1f", celsius)
        } catch (e: NumberFormatException) {
            "N/A"
        }
    }

    private fun readLine(path: String): String? {
        return try {
            BufferedReader(FileReader(path)).use { it.readLine() }
        } catch (e: IOException) {
            null
        }
    }
}
