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

class GpuClockGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00BCD4")  // Cyan for GPU clock
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, 600f,
            Color.parseColor("#8000BCD4"),
            Color.parseColor("#00000000"),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30FFFFFF")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val avgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9800")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var clockData: List<Pair<Long, Double>> = emptyList()
    private var avgClock: Double = 0.0
    private var maxClock = 1000.0  // Dynamic max based on data
    private var minClock = 0.0
    
    private val padding = 80f
    private val topPadding = 40f
    private val bottomPadding = 80f

    fun setData(data: List<Pair<Long, Double>>, avg: Double) {
        this.clockData = data
        this.avgClock = avg
        
        // Calculate dynamic max (round up to nearest 100)
        if (data.isNotEmpty()) {
            val dataMax = data.maxOf { it.second }
            maxClock = ((dataMax / 100).toInt() + 1) * 100.0
        }
        
        post {
            fillPaint.shader = LinearGradient(
                0f, topPadding, 0f, height - bottomPadding,
                Color.parseColor("#8000BCD4"),
                Color.parseColor("#1000BCD4"),
                Shader.TileMode.CLAMP
            )
            invalidate()
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
        drawAverageLine(canvas, graphWidth, graphHeight)
        drawGraph(canvas, graphWidth, graphHeight)
        drawTitle(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val message = "No GPU clock data available"
        val textWidth = textPaint.measureText(message)
        canvas.drawText(
            message,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // Calculate clock steps based on maxClock
        val step = (maxClock / 4).toInt()
        val clockSteps = (0..4).map { it * step }
        
        for (clock in clockSteps) {
            val y = (topPadding + graphHeight * (1 - clock / maxClock)).toFloat()
            
            canvas.drawLine(padding, y, padding + graphWidth, y, gridPaint)
            val label = "${clock}MHz"
            canvas.drawText(label, padding - 75f, y + 10f, textPaint)
        }

        // Y-axis label
        canvas.save()
        canvas.rotate(-90f, 15f, height / 2f)
        val yLabel = "GPU Clock (MHz)"
        val yLabelWidth = textPaint.measureText(yLabel)
        canvas.drawText(yLabel, (width - yLabelWidth) / 2, 30f, textPaint)
        canvas.restore()

        // Time labels
        if (clockData.isNotEmpty()) {
            val startTime = clockData.first().first
            val endTime = clockData.last().first
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

    private fun drawAverageLine(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        val y = (topPadding + graphHeight * (1 - avgClock / maxClock)).toFloat()
        canvas.drawLine(padding, y, padding + graphWidth, y, avgLinePaint)
    }

    private fun drawGraph(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        if (clockData.size < 2) return

        val startTime = clockData.first().first
        val endTime = clockData.last().first
        val timeDuration = max(endTime - startTime, 1L)

        val path = Path()
        val fillPath = Path()

        clockData.forEachIndexed { index, (timestamp, clock) ->
            val x = padding + ((timestamp - startTime).toFloat() / timeDuration) * graphWidth
            val y = (topPadding + graphHeight * (1 - clock / maxClock)).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - bottomPadding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(padding + graphWidth, height - bottomPadding)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }

    private fun drawTitle(canvas: Canvas) {
        val title = "GPU Clock Speed vs Time"
        val titleWidth = textPaint.measureText(title)
        canvas.drawText(title, (width - titleWidth) / 2, topPadding - 10f, textPaint)
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
