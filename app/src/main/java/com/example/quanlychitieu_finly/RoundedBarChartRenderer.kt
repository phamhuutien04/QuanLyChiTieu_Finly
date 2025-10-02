package com.example.quanlychitieu_finly

import android.graphics.Canvas
import android.graphics.Path
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    override fun drawDataSet(c: Canvas, dataSet: com.github.mikephil.charting.interfaces.datasets.IBarDataSet, index: Int) {
        val buffer = mBarBuffers[index]
        mRenderPaint.color = dataSet.color
        val radius = 30f
        for (j in 0 until buffer.buffer.size step 4) {
            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]
            val path = Path().apply {
                addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW)
            }
            c.drawPath(path, mRenderPaint)
        }
    }
}
