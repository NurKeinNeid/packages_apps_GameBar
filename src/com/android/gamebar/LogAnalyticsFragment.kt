/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.gamebar.R

class LogAnalyticsFragment : Fragment() {

    private var analytics: LogAnalytics? = null
    private var logFileName: String = "Log"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val logFilePath = arguments?.getString("LOG_FILE_PATH") ?: ""
        logFileName = arguments?.getString("LOG_FILE_NAME") ?: "Log"
        analytics = arguments?.getSerializable("ANALYTICS_DATA") as? LogAnalytics
        
        if (analytics == null) {
            Toast.makeText(requireContext(), "Failed to load analytics data", Toast.LENGTH_SHORT).show()
            return inflater.inflate(R.layout.dialog_log_analytics, container, false)
        }
        
        return inflater.inflate(R.layout.dialog_log_analytics, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val analyticsData = analytics ?: return
        
        // Get all views from the layout
        val sessionInfoText = view.findViewById<TextView>(R.id.tv_session_info)
        val fpsGraphView = view.findViewById<FpsGraphView>(R.id.fps_graph_view)
        val frameTimeGraphView = view.findViewById<FrameTimeGraphView>(R.id.frame_time_graph_view)
        val maxFpsText = view.findViewById<TextView>(R.id.tv_max_fps)
        val minFpsText = view.findViewById<TextView>(R.id.tv_min_fps)
        val avgFpsText = view.findViewById<TextView>(R.id.tv_avg_fps)
        val varianceText = view.findViewById<TextView>(R.id.tv_variance)
        val stdDevText = view.findViewById<TextView>(R.id.tv_std_dev)
        val smoothnessText = view.findViewById<TextView>(R.id.tv_smoothness)
        val fps1PercentText = view.findViewById<TextView>(R.id.tv_1percent_low)
        val fps01PercentText = view.findViewById<TextView>(R.id.tv_01percent_low)
        
        // Set session info
        sessionInfoText.text = buildString {
            appendLine("📅 ${analyticsData.sessionDate}")
            appendLine("⏱️  ${analyticsData.sessionDuration}")
            appendLine("📊 ${analyticsData.totalSamples} samples")
            append("📁 $logFileName")
        }
        
        // Set FPS statistics
        maxFpsText.text = String.format("Max FPS:      %.1f", analyticsData.fpsStats.maxFps)
        minFpsText.text = String.format("Min FPS:      %.1f", analyticsData.fpsStats.minFps)
        avgFpsText.text = String.format("Avg FPS:      %.1f", analyticsData.fpsStats.avgFps)
        varianceText.text = String.format("Variance:     %.2f", analyticsData.fpsStats.variance)
        stdDevText.text = String.format("Std Dev:      %.2f", analyticsData.fpsStats.standardDeviation)
        smoothnessText.text = String.format("Smoothness:   %.1f%%", analyticsData.fpsStats.smoothnessPercentage)
        fps1PercentText.text = String.format("1%% Low:       %.1f FPS", analyticsData.fpsStats.fps1PercentLow)
        fps01PercentText.text = String.format("0.1%% Low:     %.1f FPS", analyticsData.fpsStats.fps0_1PercentLow)
        
        // Set FPS graph data
        fpsGraphView.setData(
            analyticsData.fpsTimeData,
            analyticsData.fpsStats.avgFps,
            analyticsData.fpsStats.fps1PercentLow
        )
        
        // Set Frame Time graph data
        val avgFrameTime = if (analyticsData.fpsStats.avgFps > 0) {
            1000.0 / analyticsData.fpsStats.avgFps
        } else {
            0.0
        }
        frameTimeGraphView.setData(analyticsData.frameTimeData, avgFrameTime)
        
        // Get CPU graph views
        val cpuUsageGraphView = view.findViewById<CpuGraphView>(R.id.cpu_usage_graph_view)
        val cpuTempGraphView = view.findViewById<CpuTempGraphView>(R.id.cpu_temp_graph_view)
        val cpuClockGraphView = view.findViewById<CpuClockGraphView>(R.id.cpu_clock_graph_view)
        
        // Get CPU statistics views
        val maxCpuUsageText = view.findViewById<TextView>(R.id.tv_max_cpu_usage)
        val minCpuUsageText = view.findViewById<TextView>(R.id.tv_min_cpu_usage)
        val avgCpuUsageText = view.findViewById<TextView>(R.id.tv_avg_cpu_usage)
        val maxCpuTempText = view.findViewById<TextView>(R.id.tv_max_cpu_temp)
        val minCpuTempText = view.findViewById<TextView>(R.id.tv_min_cpu_temp)
        val avgCpuTempText = view.findViewById<TextView>(R.id.tv_avg_cpu_temp)
        
        // Set CPU graph data
        cpuUsageGraphView.setData(analyticsData.cpuUsageTimeData, analyticsData.cpuStats.avgUsage)
        cpuTempGraphView.setData(analyticsData.cpuTempTimeData, analyticsData.cpuStats.avgTemp)
        cpuClockGraphView.setData(analyticsData.cpuClockTimeData)
        
        // Set CPU statistics
        maxCpuUsageText.text = String.format("Max Usage:    %.0f%%", analyticsData.cpuStats.maxUsage)
        minCpuUsageText.text = String.format("Min Usage:    %.0f%%", analyticsData.cpuStats.minUsage)
        avgCpuUsageText.text = String.format("Avg Usage:    %.1f%%", analyticsData.cpuStats.avgUsage)
        maxCpuTempText.text = String.format("Max Temp:     %.1f°C", analyticsData.cpuStats.maxTemp)
        minCpuTempText.text = String.format("Min Temp:     %.1f°C", analyticsData.cpuStats.minTemp)
        avgCpuTempText.text = String.format("Avg Temp:     %.1f°C", analyticsData.cpuStats.avgTemp)
        
        // Get GPU graph views
        val gpuUsageGraphView = view.findViewById<GpuUsageGraphView>(R.id.gpu_usage_graph_view)
        val gpuTempGraphView = view.findViewById<GpuTempGraphView>(R.id.gpu_temp_graph_view)
        val gpuClockGraphView = view.findViewById<GpuClockGraphView>(R.id.gpu_clock_graph_view)
        
        // Get GPU statistics views
        val maxGpuUsageText = view.findViewById<TextView>(R.id.tv_max_gpu_usage)
        val minGpuUsageText = view.findViewById<TextView>(R.id.tv_min_gpu_usage)
        val avgGpuUsageText = view.findViewById<TextView>(R.id.tv_avg_gpu_usage)
        val maxGpuClockText = view.findViewById<TextView>(R.id.tv_max_gpu_clock)
        val minGpuClockText = view.findViewById<TextView>(R.id.tv_min_gpu_clock)
        val avgGpuClockText = view.findViewById<TextView>(R.id.tv_avg_gpu_clock)
        val maxGpuTempText = view.findViewById<TextView>(R.id.tv_max_gpu_temp)
        val minGpuTempText = view.findViewById<TextView>(R.id.tv_min_gpu_temp)
        val avgGpuTempText = view.findViewById<TextView>(R.id.tv_avg_gpu_temp)
        
        // Set GPU graph data
        gpuUsageGraphView.setData(analyticsData.gpuUsageTimeData, analyticsData.gpuStats.avgUsage)
        gpuTempGraphView.setData(analyticsData.gpuTempTimeData, analyticsData.gpuStats.avgTemp)
        gpuClockGraphView.setData(analyticsData.gpuClockTimeData, analyticsData.gpuStats.avgClock)
        
        // Set GPU statistics
        maxGpuUsageText.text = String.format("Max Usage:    %.0f%%", analyticsData.gpuStats.maxUsage)
        minGpuUsageText.text = String.format("Min Usage:    %.0f%%", analyticsData.gpuStats.minUsage)
        avgGpuUsageText.text = String.format("Avg Usage:    %.1f%%", analyticsData.gpuStats.avgUsage)
        maxGpuClockText.text = String.format("Max Clock:    %.0f MHz", analyticsData.gpuStats.maxClock)
        minGpuClockText.text = String.format("Min Clock:    %.0f MHz", analyticsData.gpuStats.minClock)
        avgGpuClockText.text = String.format("Avg Clock:    %.0f MHz", analyticsData.gpuStats.avgClock)
        maxGpuTempText.text = String.format("Max Temp:     %.1f°C", analyticsData.gpuStats.maxTemp)
        minGpuTempText.text = String.format("Min Temp:     %.1f°C", analyticsData.gpuStats.minTemp)
        avgGpuTempText.text = String.format("Avg Temp:     %.1f°C", analyticsData.gpuStats.avgTemp)
    }
}
