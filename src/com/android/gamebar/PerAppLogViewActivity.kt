/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.gamebar.R

class PerAppLogViewActivity : CollapsingToolbarBaseActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamebar_log)
        
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Unknown App"
        
        title = "Logs for $appName"
        
        if (savedInstanceState == null) {
            val fragment = PerAppLogViewFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, packageName)
                    putString(EXTRA_APP_NAME, appName)
                }
            }
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit()
        }
    }
}
