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
import kotlin.math.min

class FpsGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, 800f,
            Color.parseColor("#804CAF50"),
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

    private val lowLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var fpsData: List<Pair<Long, Double>> = emptyList()
    private var avgFps: Double = 0.0
    private var fps1PercentLow: Double = 0.0
    private val maxFps = 144.0  // Absolute max for display
    private val functionalMaxFps = 120.0  // Treat 120 as 100%
    private val minFps = 0.0
    
    private val padding = 80f
    private val topPadding = 40f
    private val bottomPadding = 80f  // Increased for axis label

    fun setData(data: List<Pair<Long, Double>>, avg: Double, low1Percent: Double) {
        this.fpsData = data
        this.avgFps = avg
        this.fps1PercentLow = low1Percent
        
        // Update gradient shader with actual view height
        post {
            fillPaint.shader = LinearGradient(
                0f, topPadding, 0f, height - bottomPadding,
                Color.parseColor("#804CAF50"),
                Color.parseColor("#104CAF50"),
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (fpsData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val graphWidth = width - 2 * padding
        val graphHeight = height - topPadding - bottomPadding

        // Draw grid and labels
        drawGrid(canvas, graphWidth, graphHeight)

        // Draw average line
        drawAverageLine(canvas, graphWidth, graphHeight)

        // Draw 1% low line
        draw1PercentLowLine(canvas, graphWidth, graphHeight)

        // Draw FPS graph
        drawGraph(canvas, graphWidth, graphHeight)

        // Draw legend
        drawLegend(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val message = "No data to display"
        val textWidth = textPaint.measureText(message)
        canvas.drawText(
            message,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // Draw horizontal grid lines (FPS) - using 120 as functional max (100%)
        val fpsSteps = listOf(0, 30, 60, 90, 120, 144)
        
        for (fps in fpsSteps) {
            // Map FPS values so 120 appears at 100% height
            val normalizedFps = if (fps <= 120) {
                fps.toFloat()
            } else {
                // Squeeze 120-144 into the remaining space
                120f + (fps - 120) * 0.2f
            }
            val y = (topPadding + graphHeight * (1 - normalizedFps / (functionalMaxFps.toFloat() + 4.8f))).toFloat()
            
            // Grid line
            canvas.drawLine(padding, y, padding + graphWidth, y, gridPaint)
            
            // Label
            val label = "$fps"
            canvas.drawText(label, padding - 60f, y + 10f, textPaint)
        }

        // Draw Y-axis label (far left, separate from numbers)
        canvas.save()
        canvas.rotate(-90f, 15f, height / 2f)
        val yLabel = "FPS"
        val yLabelWidth = textPaint.measureText(yLabel)
        canvas.drawText(yLabel, (width - yLabelWidth) / 2, 30f, textPaint)  // Position at x=30 (far left)
        canvas.restore()

        // Draw time labels on X-axis (higher up - first line)
        if (fpsData.isNotEmpty()) {
            val startTime = fpsData.first().first
            val endTime = fpsData.last().first
            val duration = endTime - startTime

            // Draw start time
            canvas.drawText("0s", padding, height - bottomPadding + 25f, textPaint)

            // Draw middle time
            val middleTime = formatDuration(duration / 2)
            val middleX = padding + graphWidth / 2
            canvas.drawText(middleTime, middleX - 30f, height - bottomPadding + 25f, textPaint)

            // Draw end time
            val endTimeStr = formatDuration(duration)
            val endX = padding + graphWidth - textPaint.measureText(endTimeStr)
            canvas.drawText(endTimeStr, endX, height - bottomPadding + 25f, textPaint)
        }

        // Draw X-axis label (second line - below time values)
        val xLabel = "Time"
        val xLabelWidth = textPaint.measureText(xLabel)
        canvas.drawText(xLabel, (width - xLabelWidth) / 2, height - bottomPadding + 55f, textPaint)
    }

    private fun drawGraph(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        if (fpsData.size < 2) return

        val startTime = fpsData.first().first
        val endTime = fpsData.last().first
        val timeDuration = max(endTime - startTime, 1L)

        val path = Path()
        val fillPath = Path()

        fpsData.forEachIndexed { index, (timestamp, fps) ->
            val x = padding + ((timestamp - startTime).toFloat() / timeDuration) * graphWidth
            
            // Normalize FPS using 120 as functional max
            val normalizedFps = max(minFps, min(maxFps, fps))
            val mappedFps = if (normalizedFps <= 120.0) {
                normalizedFps.toFloat()
            } else {
                // Squeeze 120-144 range into remaining space
                120f + (normalizedFps.toFloat() - 120f) * 0.2f
            }
            val y = (topPadding + graphHeight * (1 - mappedFps / (functionalMaxFps.toFloat() + 4.8f))).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - bottomPadding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Complete fill path
        val lastX = padding + graphWidth
        fillPath.lineTo(lastX, height - bottomPadding)
        fillPath.close()

        // Draw fill
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)
    }

    private fun drawAverageLine(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // Map average line position
        val normalizedAvg = max(minFps, min(maxFps, avgFps))
        val mappedAvg = if (normalizedAvg <= 120.0) {
            normalizedAvg.toFloat()
        } else {
            120f + (normalizedAvg.toFloat() - 120f) * 0.2f
        }
        val y = (topPadding + graphHeight * (1 - mappedAvg / (functionalMaxFps.toFloat() + 4.8f))).toFloat()
        
        canvas.drawLine(padding, y, padding + graphWidth, y, avgLinePaint)
    }

    private fun draw1PercentLowLine(canvas: Canvas, graphWidth: Float, graphHeight: Float) {
        // Map 1% low line position
        val normalizedLow = max(minFps, min(maxFps, fps1PercentLow))
        val mappedLow = if (normalizedLow <= 120.0) {
            normalizedLow.toFloat()
        } else {
            120f + (normalizedLow.toFloat() - 120f) * 0.2f
        }
        val y = (topPadding + graphHeight * (1 - mappedLow / (functionalMaxFps.toFloat() + 4.8f))).toFloat()
        
        canvas.drawLine(padding, y, padding + graphWidth, y, lowLinePaint)
    }

    private fun drawLegend(canvas: Canvas) {
        val legendX = padding
        val legendY = 20f
        val lineLength = 40f
        val spacing = 150f

        // FPS line
        canvas.drawLine(legendX, legendY, legendX + lineLength, legendY, linePaint)
        canvas.drawText("FPS", legendX + lineLength + 10f, legendY + 8f, textPaint.apply { textSize = 24f })

        // Average line
        canvas.drawLine(legendX + spacing, legendY, legendX + spacing + lineLength, legendY, avgLinePaint)
        canvas.drawText("Avg", legendX + spacing + lineLength + 10f, legendY + 8f, textPaint)

        // 1% Low line
        canvas.drawLine(legendX + spacing * 2, legendY, legendX + spacing * 2 + lineLength, legendY, lowLinePaint)
        canvas.drawText("1% Low", legendX + spacing * 2 + lineLength + 10f, legendY + 8f, textPaint)

        // Reset text size
        textPaint.textSize = 28f
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%dh%dm", hours, minutes % 60)
            minutes > 0 -> String.format("%dm%ds", minutes, seconds % 60)
            else -> String.format("%ds", seconds)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 800
        val desiredHeight = 600

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}
