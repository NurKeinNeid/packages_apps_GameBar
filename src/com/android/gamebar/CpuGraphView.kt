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

class CpuGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")  // Blue for CPU
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, 600f,
            Color.parseColor("#802196F3"),
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

    private var cpuData: List<Pair<Long, Double>> = emptyList()
    private var avgCpu: Double = 0.0
    private val maxCpu = 100.0
    private val minCpu = 0.0
    
    private val padding = 80f
    private val topPadding = 40f
    private val bottomPadding = 80f

    fun setData(data: List<Pair<Long, Double>>, avg: Double) {
        this.cpuData = data
        this.avgCpu = avg
        
        post {
            fillPaint.shader = LinearGradient(
                0f, topPadding, 0f, height - bottomPadding,
                Color.parseColor("#802196F3"),
                Color.parseColor("#102196F3"),
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (cpuData.isEmpty()) {
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
        val message = "No CPU data available"
        val textWidth = textPaint.measureText(message)
        canvas.drawText(
            message,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        val cpuSteps = listOf(0, 25, 50, 75, 100)
        
        for (cpu in cpuSteps) {
            val y = (topPadding + graphHeight * (1 - cpu / maxCpu.toFloat())).toFloat()
            
            canvas.drawLine(padding, y, padding + graphWidth, y, gridPaint)
            val label = "$cpu%"
            canvas.drawText(label, padding - 70f, y + 10f, textPaint)
        }

        // Y-axis label
        canvas.save()
        canvas.rotate(-90f, 15f, height / 2f)
        val yLabel = "CPU Usage (%)"
        val yLabelWidth = textPaint.measureText(yLabel)
        canvas.drawText(yLabel, (width - yLabelWidth) / 2, 30f, textPaint)
        canvas.restore()

        // Time labels on X-axis
        if (cpuData.isNotEmpty()) {
            val startTime = cpuData.first().first
            val endTime = cpuData.last().first
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
        val y = (topPadding + graphHeight * (1 - avgCpu / maxCpu)).toFloat()
        canvas.drawLine(padding, y, padding + graphWidth, y, avgLinePaint)
    }

    private fun drawGraph(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        if (cpuData.size < 2) return

        val startTime = cpuData.first().first
        val endTime = cpuData.last().first
        val timeDuration = max(endTime - startTime, 1L)

        val path = Path()
        val fillPath = Path()

        cpuData.forEachIndexed { index, (timestamp, cpu) ->
            val x = padding + ((timestamp - startTime).toFloat() / timeDuration) * graphWidth
            val y = (topPadding + graphHeight * (1 - cpu / maxCpu)).toFloat()

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
        val title = "CPU Usage vs Time"
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
