/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar

import android.content.Context

/**
 * Centralized configuration for GameBar hardware paths and conversion factors.
 * All values are loaded from resources to support device-specific overlays.
 */
object GameBarConfig {
    
    private lateinit var context: Context
    
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }
    
    // FPS paths
    val fpsSysfsPath: String
        get() = context.getString(R.string.config_fps_sysfs_path)
    
    // Battery configuration
    val batteryTempPath: String
        get() = context.getString(R.string.config_battery_temp_path)
    val batteryTempDivider: Int
        get() = context.resources.getInteger(R.integer.config_battery_temp_divider)
    
    // CPU configuration
    val cpuBasePath: String
        get() = context.getString(R.string.config_cpu_base_path)
    val cpuTempPath: String
        get() = context.getString(R.string.config_cpu_temp_path)
    val cpuTempDivider: Int
        get() = context.resources.getInteger(R.integer.config_cpu_temp_divider)
    
    // GPU configuration
    val gpuUsagePath: String
        get() = context.getString(R.string.config_gpu_usage_path)
    val gpuClockPath: String
        get() = context.getString(R.string.config_gpu_clock_path)
    val gpuTempPath: String
        get() = context.getString(R.string.config_gpu_temp_path)
    val gpuTempDivider: Int
        get() = context.resources.getInteger(R.integer.config_gpu_temp_divider)
    val gpuClockDivider: Int
        get() = context.resources.getInteger(R.integer.config_gpu_clock_divider)
    
    // RAM configuration
    val ramFreqPath: String
        get() = context.getString(R.string.config_ram_freq_path)
    val ramTempPath: String
        get() = context.getString(R.string.config_ram_temp_path)
    val ramTempDivider: Int
        get() = context.resources.getInteger(R.integer.config_ram_temp_divider)
    
    // Proc filesystem paths
    val procStatPath: String
        get() = context.getString(R.string.config_proc_stat_path)
    val procMeminfoPath: String
        get() = context.getString(R.string.config_proc_meminfo_path)
}
