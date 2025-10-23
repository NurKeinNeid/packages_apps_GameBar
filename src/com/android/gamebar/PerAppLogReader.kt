/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.Serializable
import kotlin.math.pow
import kotlin.math.sqrt

data class LogAnalytics(
    val fpsStats: FpsStatistics,
    val cpuStats: CpuStatistics,
    val gpuStats: GpuStatistics,
    val sessionDuration: String,
    val totalSamples: Int,
    val appName: String,
    val sessionDate: String,
    val fpsTimeData: List<Pair<Long, Double>>,  // Timestamp in millis, FPS value
    val frameTimeData: List<Pair<Long, Double>>,  // Frame time over time
    val cpuUsageTimeData: List<Pair<Long, Double>>,  // CPU usage over time
    val cpuTempTimeData: List<Pair<Long, Double>>,   // CPU temp over time
    val cpuClockTimeData: Map<Int, List<Pair<Long, Double>>>,  // Per-core clock speeds over time
    val gpuUsageTimeData: List<Pair<Long, Double>>,  // GPU usage over time
    val gpuTempTimeData: List<Pair<Long, Double>>,   // GPU temp over time
    val gpuClockTimeData: List<Pair<Long, Double>>   // GPU clock speed over time
) : Serializable

data class FpsStatistics(
    val maxFps: Double,
    val minFps: Double,
    val avgFps: Double,
    val variance: Double,
    val standardDeviation: Double,
    val fps1PercentLow: Double,  // 1% low - worst 1% of frames
    val fps0_1PercentLow: Double, // 0.1% low - worst 0.1% of frames
    val smoothnessPercentage: Double  // Percentage of frames >= 45 FPS
) : Serializable

data class CpuStatistics(
    val maxUsage: Double,
    val minUsage: Double,
    val avgUsage: Double,
    val maxTemp: Double,
    val minTemp: Double,
    val avgTemp: Double
) : Serializable

data class GpuStatistics(
    val maxUsage: Double,
    val minUsage: Double,
    val avgUsage: Double,
    val maxClock: Double,
    val minClock: Double,
    val avgClock: Double,
    val maxTemp: Double,
    val minTemp: Double,
    val avgTemp: Double
) : Serializable

class PerAppLogReader {

    companion object {
        private const val TAG = "PerAppLogReader"
        
        // CSV column indices (updated format)
        private const val COL_DATETIME = 0
        private const val COL_PACKAGE_NAME = 1
        private const val COL_FPS = 2
        private const val COL_FRAME_TIME = 3
        private const val COL_BATTERY_TEMP = 4
        private const val COL_CPU_USAGE = 5
        private const val COL_CPU_CLOCK = 6
        private const val COL_CPU_TEMP = 7
        private const val COL_RAM_USAGE = 8
        private const val COL_RAM_SPEED = 9
        private const val COL_RAM_TEMP = 10
        private const val COL_GPU_USAGE = 11
        private const val COL_GPU_CLOCK = 12
        private const val COL_GPU_TEMP = 13
    }

    /**
     * Read and analyze a log file
     */
    fun analyzeLogFile(logFilePath: String): LogAnalytics? {
        return try {
            val file = File(logFilePath)
            if (!file.exists() || !file.canRead()) {
                Log.e(TAG, "Log file does not exist or cannot be read: $logFilePath")
                return null
            }

            val fpsValues = mutableListOf<Double>()
            val fpsTimeData = mutableListOf<Pair<Long, Double>>()
            val frameTimeValues = mutableListOf<Double>()
            val frameTimeData = mutableListOf<Pair<Long, Double>>()
            val cpuUsageValues = mutableListOf<Double>()
            val cpuUsageTimeData = mutableListOf<Pair<Long, Double>>()
            val cpuTempValues = mutableListOf<Double>()
            val cpuTempTimeData = mutableListOf<Pair<Long, Double>>()
            val cpuClockTimeData = mutableMapOf<Int, MutableList<Pair<Long, Double>>>()
            val gpuUsageValues = mutableListOf<Double>()
            val gpuUsageTimeData = mutableListOf<Pair<Long, Double>>()
            val gpuClockValues = mutableListOf<Double>()
            val gpuClockTimeData = mutableListOf<Pair<Long, Double>>()
            val gpuTempValues = mutableListOf<Double>()
            val gpuTempTimeData = mutableListOf<Pair<Long, Double>>()
            var firstTimestamp: String? = null
            var lastTimestamp: String? = null
            var packageName = ""
            var lineCount = 0
            var sessionStartTimeMs: Long = 0

            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine()
                
                // Skip header
                if (line != null && line.contains("DateTime")) {
                    line = reader.readLine()
                }

                // Read data lines
                while (line != null) {
                    lineCount++
                    val columns = line.split(",")
                    
                    if (columns.size >= COL_FPS + 1) {
                        try {
                            // Extract timestamp
                            val timestampStr = columns[COL_DATETIME].trim()
                            
                            // Extract FPS
                            val fpsStr = columns[COL_FPS].trim()
                            if (fpsStr.isNotEmpty() && fpsStr != "N/A" && fpsStr != "-") {
                                val fps = fpsStr.toDoubleOrNull()
                                if (fps != null && fps > 0) {
                                    fpsValues.add(fps)
                                    
                                    // Parse timestamp and calculate relative time
                                    try {
                                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        val timestamp = dateFormat.parse(timestampStr)?.time ?: 0L
                                        
                                        if (sessionStartTimeMs == 0L) {
                                            sessionStartTimeMs = timestamp
                                        }
                                        
                                        // Store relative time in milliseconds
                                        val relativeTime = timestamp - sessionStartTimeMs
                                        fpsTimeData.add(Pair(relativeTime, fps))
                                    } catch (e: Exception) {
                                        // If timestamp parsing fails, use sequential time
                                        fpsTimeData.add(Pair(lineCount * 1000L, fps))
                                    }
                                }
                            }
                            
                            // Extract Frame Time
                            if (columns.size > COL_FRAME_TIME) {
                                val frameTimeStr = columns[COL_FRAME_TIME].trim()
                                if (frameTimeStr.isNotEmpty() && frameTimeStr != "N/A" && frameTimeStr != "-") {
                                    val frameTime = frameTimeStr.toDoubleOrNull()
                                    if (frameTime != null && frameTime > 0) {
                                        frameTimeValues.add(frameTime)
                                        val relativeTime = if (sessionStartTimeMs > 0) {
                                            fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                        } else {
                                            lineCount * 1000L
                                        }
                                        frameTimeData.add(Pair(relativeTime, frameTime))
                                    }
                                }
                            }
                            
                            // Extract CPU Usage
                            if (columns.size > COL_CPU_USAGE) {
                                val cpuUsageStr = columns[COL_CPU_USAGE].trim()
                                if (cpuUsageStr.isNotEmpty() && cpuUsageStr != "N/A") {
                                    val cpuUsage = cpuUsageStr.toDoubleOrNull()
                                    if (cpuUsage != null && cpuUsage >= 0) {
                                        cpuUsageValues.add(cpuUsage)
                                        val relativeTime = if (sessionStartTimeMs > 0) {
                                            fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                        } else {
                                            lineCount * 1000L
                                        }
                                        cpuUsageTimeData.add(Pair(relativeTime, cpuUsage))
                                    }
                                }
                            }
                            
                            // Extract CPU Temp
                            if (columns.size > COL_CPU_TEMP) {
                                val cpuTempStr = columns[COL_CPU_TEMP].trim()
                                if (cpuTempStr.isNotEmpty() && cpuTempStr != "N/A") {
                                    val cpuTemp = cpuTempStr.toDoubleOrNull()
                                    if (cpuTemp != null && cpuTemp > 0) {
                                        cpuTempValues.add(cpuTemp)
                                        val relativeTime = if (sessionStartTimeMs > 0) {
                                            fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                        } else {
                                            lineCount * 1000L
                                        }
                                        cpuTempTimeData.add(Pair(relativeTime, cpuTemp))
                                    }
                                }
                            }
                            
                            // Extract CPU Clock (multi-core data)
                            if (columns.size > COL_CPU_CLOCK) {
                                val cpuClockStr = columns[COL_CPU_CLOCK].trim()
                                if (cpuClockStr.isNotEmpty() && cpuClockStr != "N/A") {
                                    // Parse format: "cpu0: 1800 MHz; cpu1: 2000 MHz; ..."
                                    val relativeTime = if (sessionStartTimeMs > 0) {
                                        fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                    } else {
                                        lineCount * 1000L
                                    }
                                    
                                    cpuClockStr.split(";").forEachIndexed { index, coreData ->
                                        try {
                                            // Extract MHz value from "cpuX: YYYY MHz"
                                            val mhzMatch = Regex("(\\d+)\\s*MHz").find(coreData)
                                            if (mhzMatch != null) {
                                                val mhz = mhzMatch.groupValues[1].toDouble()
                                                if (!cpuClockTimeData.containsKey(index)) {
                                                    cpuClockTimeData[index] = mutableListOf()
                                                }
                                                cpuClockTimeData[index]?.add(Pair(relativeTime, mhz))
                                            }
                                        } catch (e: Exception) {
                                            // Skip malformed core data
                                        }
                                    }
                                }
                            }
                            
                            // Extract GPU Usage
                            if (columns.size > COL_GPU_USAGE) {
                                val gpuUsageStr = columns[COL_GPU_USAGE].trim()
                                if (gpuUsageStr.isNotEmpty() && gpuUsageStr != "N/A") {
                                    val gpuUsage = gpuUsageStr.toDoubleOrNull()
                                    if (gpuUsage != null && gpuUsage >= 0) {
                                        gpuUsageValues.add(gpuUsage)
                                        val relativeTime = if (sessionStartTimeMs > 0) {
                                            fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                        } else {
                                            lineCount * 1000L
                                        }
                                        gpuUsageTimeData.add(Pair(relativeTime, gpuUsage))
                                    }
                                }
                            }
                            
                            // Extract GPU Clock
                            if (columns.size > COL_GPU_CLOCK) {
                                val gpuClockStr = columns[COL_GPU_CLOCK].trim()
                                if (gpuClockStr.isNotEmpty() && gpuClockStr != "N/A") {
                                    val gpuClock = gpuClockStr.toDoubleOrNull()
                                    if (gpuClock != null && gpuClock > 0) {
                                        gpuClockValues.add(gpuClock)
                                        val relativeTime = if (sessionStartTimeMs > 0) {
                                            fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                        } else {
                                            lineCount * 1000L
                                        }
                                        gpuClockTimeData.add(Pair(relativeTime, gpuClock))
                                    }
                                }
                            }
                            
                            // Extract GPU Temp
                            if (columns.size > COL_GPU_TEMP) {
                                val gpuTempStr = columns[COL_GPU_TEMP].trim()
                                if (gpuTempStr.isNotEmpty() && gpuTempStr != "N/A") {
                                    val gpuTemp = gpuTempStr.toDoubleOrNull()
                                    if (gpuTemp != null && gpuTemp > 0) {
                                        gpuTempValues.add(gpuTemp)
                                        val relativeTime = if (sessionStartTimeMs > 0) {
                                            fpsTimeData.lastOrNull()?.first ?: (lineCount * 1000L)
                                        } else {
                                            lineCount * 1000L
                                        }
                                        gpuTempTimeData.add(Pair(relativeTime, gpuTemp))
                                    }
                                }
                            }

                            // Extract timestamps for session duration
                            if (firstTimestamp == null) {
                                firstTimestamp = timestampStr
                                packageName = columns[COL_PACKAGE_NAME].trim()
                            }
                            lastTimestamp = timestampStr
                            
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing line $lineCount: $line", e)
                        }
                    }
                    
                    line = reader.readLine()
                }
            }

            if (fpsValues.isEmpty()) {
                Log.w(TAG, "No valid FPS data found in log file")
                return null
            }

            // Calculate FPS statistics
            val fpsStats = calculateFpsStatistics(fpsValues)
            
            // Calculate CPU statistics
            val cpuStats = calculateCpuStatistics(cpuUsageValues, cpuTempValues)
            
            // Calculate GPU statistics
            val gpuStats = calculateGpuStatistics(gpuUsageValues, gpuClockValues, gpuTempValues)
            
            // Calculate session duration
            val sessionDuration = calculateSessionDuration(firstTimestamp, lastTimestamp)
            
            // Extract session date from filename
            val sessionDate = extractSessionDate(file.name)

            LogAnalytics(
                fpsStats = fpsStats,
                cpuStats = cpuStats,
                gpuStats = gpuStats,
                sessionDuration = sessionDuration,
                totalSamples = fpsValues.size,
                appName = packageName,
                sessionDate = sessionDate,
                fpsTimeData = fpsTimeData,
                frameTimeData = frameTimeData,
                cpuUsageTimeData = cpuUsageTimeData,
                cpuTempTimeData = cpuTempTimeData,
                cpuClockTimeData = cpuClockTimeData,
                gpuUsageTimeData = gpuUsageTimeData,
                gpuTempTimeData = gpuTempTimeData,
                gpuClockTimeData = gpuClockTimeData
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing log file: $logFilePath", e)
            null
        }
    }

    /**
     * Calculate comprehensive FPS statistics
     */
    private fun calculateFpsStatistics(fpsValues: List<Double>): FpsStatistics {
        if (fpsValues.isEmpty()) {
            return FpsStatistics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val sortedFps = fpsValues.sorted()
        
        // Max and Min
        val maxFps = sortedFps.last()
        val minFps = sortedFps.first()
        
        // Average
        val avgFps = fpsValues.average()
        
        // Variance and Standard Deviation
        val variance = if (fpsValues.size > 1) {
            fpsValues.map { (it - avgFps).pow(2) }.sum() / fpsValues.size
        } else {
            0.0
        }
        val stdDev = sqrt(variance)
        
        // Calculate 1% low and 0.1% low
        val fps1PercentLow = calculatePercentileLow(sortedFps, 0.01)
        val fps0_1PercentLow = calculatePercentileLow(sortedFps, 0.001)
        
        // Calculate smoothness percentage (frames >= 45 FPS)
        val smoothFrames = fpsValues.count { it >= 45.0 }
        val smoothnessPercentage = (smoothFrames.toDouble() / fpsValues.size) * 100.0

        return FpsStatistics(
            maxFps = maxFps,
            minFps = minFps,
            avgFps = avgFps,
            variance = variance,
            standardDeviation = stdDev,
            fps1PercentLow = fps1PercentLow,
            fps0_1PercentLow = fps0_1PercentLow,
            smoothnessPercentage = smoothnessPercentage
        )
    }

    /**
     * Calculate CPU statistics
     */
    private fun calculateCpuStatistics(cpuUsageValues: List<Double>, cpuTempValues: List<Double>): CpuStatistics {
        val maxUsage = if (cpuUsageValues.isNotEmpty()) cpuUsageValues.maxOrNull() ?: 0.0 else 0.0
        val minUsage = if (cpuUsageValues.isNotEmpty()) cpuUsageValues.minOrNull() ?: 0.0 else 0.0
        val avgUsage = if (cpuUsageValues.isNotEmpty()) cpuUsageValues.average() else 0.0
        
        val maxTemp = if (cpuTempValues.isNotEmpty()) cpuTempValues.maxOrNull() ?: 0.0 else 0.0
        val minTemp = if (cpuTempValues.isNotEmpty()) cpuTempValues.minOrNull() ?: 0.0 else 0.0
        val avgTemp = if (cpuTempValues.isNotEmpty()) cpuTempValues.average() else 0.0
        
        return CpuStatistics(
            maxUsage = maxUsage,
            minUsage = minUsage,
            avgUsage = avgUsage,
            maxTemp = maxTemp,
            minTemp = minTemp,
            avgTemp = avgTemp
        )
    }
    
    /**
     * Calculate GPU statistics
     */
    private fun calculateGpuStatistics(
        gpuUsageValues: List<Double>,
        gpuClockValues: List<Double>,
        gpuTempValues: List<Double>
    ): GpuStatistics {
        val maxUsage = if (gpuUsageValues.isNotEmpty()) gpuUsageValues.maxOrNull() ?: 0.0 else 0.0
        val minUsage = if (gpuUsageValues.isNotEmpty()) gpuUsageValues.minOrNull() ?: 0.0 else 0.0
        val avgUsage = if (gpuUsageValues.isNotEmpty()) gpuUsageValues.average() else 0.0
        
        val maxClock = if (gpuClockValues.isNotEmpty()) gpuClockValues.maxOrNull() ?: 0.0 else 0.0
        val minClock = if (gpuClockValues.isNotEmpty()) gpuClockValues.minOrNull() ?: 0.0 else 0.0
        val avgClock = if (gpuClockValues.isNotEmpty()) gpuClockValues.average() else 0.0
        
        val maxTemp = if (gpuTempValues.isNotEmpty()) gpuTempValues.maxOrNull() ?: 0.0 else 0.0
        val minTemp = if (gpuTempValues.isNotEmpty()) gpuTempValues.minOrNull() ?: 0.0 else 0.0
        val avgTemp = if (gpuTempValues.isNotEmpty()) gpuTempValues.average() else 0.0
        
        return GpuStatistics(
            maxUsage = maxUsage,
            minUsage = minUsage,
            avgUsage = avgUsage,
            maxClock = maxClock,
            minClock = minClock,
            avgClock = avgClock,
            maxTemp = maxTemp,
            minTemp = minTemp,
            avgTemp = avgTemp
        )
    }
    
    /**
     * Calculate percentile low (e.g., 1% low = average of worst 1% of frames)
     */
    private fun calculatePercentileLow(sortedFps: List<Double>, percentile: Double): Double {
        if (sortedFps.isEmpty()) return 0.0
        
        val count = (sortedFps.size * percentile).toInt().coerceAtLeast(1)
        val worstFrames = sortedFps.take(count)
        
        return if (worstFrames.isNotEmpty()) {
            worstFrames.average()
        } else {
            sortedFps.first()
        }
    }

    /**
     * Calculate session duration from timestamps
     */
    private fun calculateSessionDuration(firstTimestamp: String?, lastTimestamp: String?): String {
        if (firstTimestamp == null || lastTimestamp == null) {
            return "Unknown"
        }

        return try {
            // Parse timestamps - format example: "2025-01-15 14:32:45"
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val startTime = dateFormat.parse(firstTimestamp)
            val endTime = dateFormat.parse(lastTimestamp)

            if (startTime != null && endTime != null) {
                val durationMs = endTime.time - startTime.time
                val seconds = (durationMs / 1000) % 60
                val minutes = (durationMs / (1000 * 60)) % 60
                val hours = (durationMs / (1000 * 60 * 60))

                when {
                    hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
                    minutes > 0 -> String.format("%dm %ds", minutes, seconds)
                    else -> String.format("%ds", seconds)
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating session duration", e)
            "Unknown"
        }
    }

    /**
     * Extract session date from filename
     * Filename format: packageName_GameBar_log_yyyyMMdd_HHmmss.csv
     */
    private fun extractSessionDate(fileName: String): String {
        return try {
            val pattern = Regex("_(\\d{8})_(\\d{6})\\.csv$")
            val match = pattern.find(fileName)
            
            if (match != null) {
                val dateStr = match.groupValues[1]
                val timeStr = match.groupValues[2]
                
                // Parse: yyyyMMdd_HHmmss
                val inputFormat = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                
                val date = inputFormat.parse(dateStr + timeStr)
                if (date != null) {
                    outputFormat.format(date)
                } else {
                    "Unknown Date"
                }
            } else {
                "Unknown Date"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting session date from filename: $fileName", e)
            "Unknown Date"
        }
    }

    /**
     * Format FPS statistics for display
     */
    fun formatFpsStats(stats: FpsStatistics): String {
        return buildString {
            appendLine("FPS Statistics:")
            appendLine("─────────────────────────")
            appendLine(String.format("Max FPS:     %.1f", stats.maxFps))
            appendLine(String.format("Min FPS:     %.1f", stats.minFps))
            appendLine(String.format("Avg FPS:     %.1f", stats.avgFps))
            appendLine(String.format("Variance:    %.2f", stats.variance))
            appendLine(String.format("Std Dev:     %.2f", stats.standardDeviation))
            appendLine(String.format("1%% Low:      %.1f", stats.fps1PercentLow))
            appendLine(String.format("0.1%% Low:    %.1f", stats.fps0_1PercentLow))
        }
    }
}
