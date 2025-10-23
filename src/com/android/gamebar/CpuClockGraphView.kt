/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.android.gamebar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class CpuClockGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Color palette for CPU cores (8 distinct colors)
    private val coreColors = arrayOf(
        "#FF5252", // Red
        "#FF9800", // Orange
        "#FFEB3B", // Yellow
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#9C27B0", // Purple
        "#E91E63", // Pink
        "#00BCD4"  // Cyan
    )

    private val corePaints = mutableListOf<Paint>()
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30FFFFFF")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 20f
    }

    // Data: Map of coreIndex -> List<Pair<timestamp, clockMhz>>
    private var clockData: Map<Int, List<Pair<Long, Double>>> = emptyMap()
    private var maxClock = 3000.0  // Dynamic max
    private var numCores = 8
    
    private val padding = 80f
    private val topPadding = 60f
    private val bottomPadding = 120f  // Extra space for legend

    init {
        // Initialize paints for each core
        for (color in coreColors) {
            corePaints.add(Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.parseColor(color)
                strokeWidth = 2.5f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            })
        }
    }

    fun setData(data: Map<Int, List<Pair<Long, Double>>>) {
        this.clockData = data
        this.numCores = data.keys.size
        
        // Calculate dynamic max (round up to nearest 500)
        if (data.isNotEmpty()) {
            val dataMax = data.values.flatten().maxOfOrNull { it.second } ?: 3000.0
            maxClock = ((dataMax / 500).toInt() + 1) * 500.0
        }
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (clockData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val graphWidth = width - 2 * padding
        val graphHeight = height - topPadding - bottomPadding

        drawGrid(canvas, graphWidth, graphHeight)
        drawGraphs(canvas, graphWidth, graphHeight)
        drawTitle(canvas)
        drawLegend(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val message = "No CPU clock data available"
        val textWidth = textPaint.measureText(message)
        canvas.drawText(
            message,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // Calculate clock steps
        val step = (maxClock / 4).toInt()
        val clockSteps = (0..4).map { it * step }
        
        for (clock in clockSteps) {
            val y = (topPadding + graphHeight * (1 - clock / maxClock)).toFloat()
            
            canvas.drawLine(padding, y, padding + graphWidth, y, gridPaint)
            val label = "${clock}MHz"
            canvas.drawText(label, padding - 75f, y + 8f, textPaint)
        }

        // Y-axis label
        canvas.save()
        canvas.rotate(-90f, 15f, height / 2f)
        val yLabel = "Clock Speed (MHz)"
        val yLabelWidth = textPaint.measureText(yLabel)
        canvas.drawText(yLabel, (width - yLabelWidth) / 2, 30f, textPaint)
        canvas.restore()

        // Time labels
        if (clockData.isNotEmpty() && clockData.values.first().isNotEmpty()) {
            val firstData = clockData.values.first()
            val startTime = firstData.first().first
            val endTime = firstData.last().first
            val duration = endTime - startTime

            canvas.drawText("0s", padding, height - bottomPadding + 25f, textPaint)
            
            val middleTime = formatDuration(duration / 2)
            canvas.drawText(middleTime, padding + graphWidth / 2 - 30f, height - bottomPadding + 25f, textPaint)
            
            val endTimeStr = formatDuration(duration)
            val endX = padding + graphWidth - textPaint.measureText(endTimeStr)
            canvas.drawText(endTimeStr, endX, height - bottomPadding + 25f, textPaint)
        }

        val xLabel = "Time"
        val xLabelWidth = textPaint.measureText(xLabel)
        canvas.drawText(xLabel, (width - xLabelWidth) / 2, height - bottomPadding + 55f, textPaint)
    }

    private fun drawGraphs(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        clockData.forEach { (coreIndex, data) ->
            if (data.size < 2) return@forEach
            
            val paint = corePaints[coreIndex % corePaints.size]
            val startTime = data.first().first
            val endTime = data.last().first
            val timeDuration = max(endTime - startTime, 1L)

            val path = Path()

            data.forEachIndexed { index, (timestamp, clock) ->
                val x = padding + ((timestamp - startTime).toFloat() / timeDuration) * graphWidth
                val y = (topPadding + graphHeight * (1 - clock / maxClock)).toFloat()

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            canvas.drawPath(path, paint)
        }
    }

    private fun drawTitle(canvas: Canvas) {
        val title = "CPU Clock Speed vs Time (Per Core)"
        val titleWidth = textPaint.measureText(title)
        canvas.drawText(title, (width - titleWidth) / 2, topPadding - 15f, textPaint)
    }

    private fun drawLegend(canvas: Canvas) {
        val legendY = height - bottomPadding + 85f
        val legendStartX = padding
        val itemWidth = (width - 2 * padding) / 4  // 4 items per row
        
        for (i in 0 until numCores) {
            val col = i % 4
            val row = i / 4
            val x = legendStartX + col * itemWidth
            val y = legendY + row * 30f
            
            val paint = corePaints[i % corePaints.size]
            
            // Draw color box
            canvas.drawRect(x, y - 12f, x + 20f, y, paint)
            
            // Draw label
            canvas.drawText("CPU$i", x + 25f, y, legendTextPaint)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        return if (seconds < 60) {
            "${seconds}s"
        } else {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}m ${remainingSeconds}s"
        }
    }
}
