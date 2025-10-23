/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.gamebar.R

class GameBarLogActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamebar_log)
        title = "GameBar Log Monitor"
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, GameBarLogFragment())
                .commit()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gamebar_log_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logging_parameters -> {
                showLoggingParametersDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLoggingParametersDialog() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Create checkboxes for each parameter
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        
        val checkboxes = mutableMapOf<String, CheckBox>()
        
        val parameters = listOf(
            Pair(GameBarLogFragment.PREF_LOG_FPS, "FPS"),
            Pair(GameBarLogFragment.PREF_LOG_FRAME_TIME, "Frame Time"),
            Pair(GameBarLogFragment.PREF_LOG_BATTERY_TEMP, "Battery Temperature"),
            Pair(GameBarLogFragment.PREF_LOG_CPU_USAGE, "CPU Usage"),
            Pair(GameBarLogFragment.PREF_LOG_CPU_CLOCK, "CPU Clock"),
            Pair(GameBarLogFragment.PREF_LOG_CPU_TEMP, "CPU Temperature"),
            Pair(GameBarLogFragment.PREF_LOG_RAM, "RAM Usage"),
            Pair(GameBarLogFragment.PREF_LOG_RAM_SPEED, "RAM Frequency"),
            Pair(GameBarLogFragment.PREF_LOG_RAM_TEMP, "RAM Temperature"),
            Pair(GameBarLogFragment.PREF_LOG_GPU_USAGE, "GPU Usage"),
            Pair(GameBarLogFragment.PREF_LOG_GPU_CLOCK, "GPU Clock"),
            Pair(GameBarLogFragment.PREF_LOG_GPU_TEMP, "GPU Temperature")
        )
        
        parameters.forEach { (key, label) ->
            val cb = CheckBox(this).apply {
                text = label
                isChecked = prefs.getBoolean(key, true)
                setPadding(16, 16, 16, 16)
            }
            checkboxes[key] = cb
            container.addView(cb)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Logging Parameters")
            .setMessage("Select which parameters to include in logs:")
            .setView(container)
            .setPositiveButton("Apply") { _, _ ->
                val editor = prefs.edit()
                checkboxes.forEach { (key, checkbox) ->
                    editor.putBoolean(key, checkbox.isChecked)
                }
                editor.apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
