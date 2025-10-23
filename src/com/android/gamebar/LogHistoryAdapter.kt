/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.gamebar.R

class LogHistoryAdapter(
    private val logFiles: List<GameBarLogFragment.LogFile>,
    private val onItemClick: (GameBarLogFragment.LogFile, View) -> Unit
) : RecyclerView.Adapter<LogHistoryAdapter.LogFileViewHolder>() {

    inner class LogFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val fileInfo: TextView = itemView.findViewById(R.id.tv_file_info)

        fun bind(logFile: GameBarLogFragment.LogFile) {
            fileName.text = logFile.name
            fileInfo.text = "${logFile.size} • ${logFile.lastModified}"

            itemView.setOnClickListener {
                onItemClick(logFile, itemView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogFileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_file, parent, false)
        return LogFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogFileViewHolder, position: Int) {
        holder.bind(logFiles[position])
    }

    override fun getItemCount(): Int = logFiles.size
}
