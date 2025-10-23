/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.gamebar.R

class PerAppLogAdapter(
    private val context: Context,
    private var apps: List<ApplicationInfo>,
    private val onSwitchChanged: (String, Boolean) -> Unit,
    private val onViewLogsClicked: (String, String) -> Unit // packageName, appName
) : RecyclerView.Adapter<PerAppLogAdapter.PerAppLogViewHolder>() {

    private val packageManager = context.packageManager
    private val perAppLogManager = PerAppLogManager.getInstance()
    private var enabledApps = setOf<String>()
    private var currentlyLoggingApps = setOf<String>()

    fun updateApps(newApps: List<ApplicationInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    fun updateEnabledApps(enabled: Set<String>) {
        enabledApps = enabled
        notifyDataSetChanged()
    }

    fun updateCurrentlyLoggingApps(logging: Set<String>) {
        currentlyLoggingApps = logging
        notifyDataSetChanged()
    }
    
    fun refreshLogFileStates() {
        // Force refresh of the adapter to update log file states
        notifyDataSetChanged()
    }

    inner class PerAppLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val packageName: TextView = itemView.findViewById(R.id.tv_package_name)
        private val viewLogsIcon: ImageView = itemView.findViewById(R.id.iv_view_logs)
        private val loggingSwitch: Switch = itemView.findViewById(R.id.switch_per_app_logging)

        fun bind(app: ApplicationInfo) {
            val appLabel = app.loadLabel(packageManager).toString()
            
            appIcon.setImageDrawable(app.loadIcon(packageManager))
            appName.text = appLabel
            packageName.text = app.packageName
            
            // Update switch state
            loggingSwitch.setOnCheckedChangeListener(null) // Prevent recursive calls
            loggingSwitch.isChecked = enabledApps.contains(app.packageName)
            
            // Add visual indicator if currently logging
            if (currentlyLoggingApps.contains(app.packageName)) {
                appName.setTextColor(context.getColor(R.color.gamebar_green))
                packageName.text = "${app.packageName} • LOGGING"
            } else {
                appName.setTextColor(context.getColor(R.color.app_name_text_selector))
                packageName.text = app.packageName
            }

            // Set switch listener
            loggingSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged(app.packageName, isChecked)
            }

            // Always enable view logs icon - the log view will handle empty state
            viewLogsIcon.alpha = 1.0f
            viewLogsIcon.isEnabled = true
            
            // Set view logs click listener - always navigate to log view
            viewLogsIcon.setOnClickListener {
                onViewLogsClicked(app.packageName, appLabel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerAppLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_per_app_log, parent, false)
        return PerAppLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: PerAppLogViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    fun filter(query: String): List<ApplicationInfo> {
        return if (query.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                val label = app.loadLabel(packageManager).toString().lowercase()
                val pkg = app.packageName.lowercase()
                label.contains(query.lowercase()) || pkg.contains(query.lowercase())
            }
        }
    }
}
