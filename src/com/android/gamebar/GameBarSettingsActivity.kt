/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.gamebar.R

class GameBarSettingsActivity : CollapsingToolbarBaseActivity() {
    
    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
        private const val REQUEST_CODE_OPEN_CSV = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_bar)
        title = getString(R.string.game_bar_title)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.overlay_permission_granted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.overlay_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_OPEN_CSV -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.also { uri ->
                        handleExternalLogFile(uri)
                    }
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gamebar_settings_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_log_monitor -> {
                try {
                    startActivity(Intent(this, GameBarLogActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                true
            }
            R.id.menu_open_external_log -> {
                openExternalLogFile()
                true
            }
            R.id.menu_user_guide -> {
                try {
                    val url = getString(R.string.game_bar_user_guide_url)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open user guide: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun openExternalLogFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
        }
        
        try {
            startActivityForResult(intent, REQUEST_CODE_OPEN_CSV)
        } catch (e: Exception) {
            Toast.makeText(this, "File picker not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleExternalLogFile(uri: Uri) {
        try {
            // Copy file content to temporary location
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = java.io.File(cacheDir, "temp_external_log.csv")
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Open analytics activity with this file
            val intent = Intent(this, LogAnalyticsActivity::class.java).apply {
                putExtra(LogAnalyticsActivity.EXTRA_LOG_FILE_PATH, tempFile.absolutePath)
                putExtra(LogAnalyticsActivity.EXTRA_LOG_FILE_NAME, uri.lastPathSegment ?: "External Log")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
