
package com.example.industrialhmi

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Typeface
import kotlin.math.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            5894 or 1024 or 512 or 4096
        setContentView(Hmi(this))
    }
}

class Hmi(ctx: Context) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bg = Color.rgb(16, 22, 27)
    private val pane = Color.rgb(27, 36, 43)
    private val edge = Color.rgb(76, 94, 104)
    private val white = Color.rgb(231, 238, 241)
    private val mute = Color.rgb(139, 157, 166)
    private val teal = Color.rgb(65, 174, 157)
    private val blue = Color.rgb(25, 158, 190)
    private val dark = Color.rgb(10, 16, 20)

    private fun paint(
        color: Int,
        stroke: Float = 1f,
        style: Paint.Style = Paint.Style.FILL
    ) {
        p.color = color
        p.strokeWidth = stroke
        p.style = style
        p.strokeCap = Paint.Cap.ROUND
    }

    private fun text(
        c: Canvas,
        s: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        paint(color)
        p.textSize = size
        p.typeface = if (bold)
            Typeface.create("sans-serif", Typeface.BOLD)
        else
            Typeface.create("sans-serif", Typeface.NORMAL)

        c.drawText(s, x, y, p)
    }

    private fun centeredText(
        c: Canvas,
        s: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        paint(color)
        p.textSize = size
        p.typeface = if (bold)
            Typeface.DEFAULT_BOLD
        else
            Typeface.DEFAULT

        c.drawText(s, x - p.measureText(s) / 2f, y, p)
    }

    private fun rect(
        c: Canvas,
        l: Float,
        t: Float,
        r: Float,
        b: Float,
        color: Int,
        border: Int? = null,
        radius: Float = 6f
    ) {
        paint(color)
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, p)

        if (border != null) {
            paint(border, 1.5f, Paint.Style.STROKE)
            c.drawRoundRect(RectF(l, t, r, b), radius, radius, p)
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        c.save()
        c.scale(width / 800f, height / 480f)
        c.drawColor(bg)

        // HEADER
        rect(c, 8f, 7f, 792f, 39f, pane, edge, 8f)

        text(c, "14:37:25", 22f, 29f, 17f, white, true)
        centeredText(c, "SANGINKAR SANAT.CO",
            400f, 28f, 17f, white, true)

        text(c, "2024-04-30  Tue", 614f, 28f, 13f, mute)

        rect(c, 746f, 13f, 780f, 32f, teal)
        centeredText(c, "OK", 763f, 27f, 12f, white, true)

        // LEFT: VERTICAL GAUGE
        verticalGauge(
            c,
            155f,
            258f,
            130f,
            -90,
            90,
            0f
        )

        // CENTER: FULL 360 DEGREE GAUGE
        horizontalGauge(
            c,
            455f,
            239f,
            151f,
            0f
        )

        // RIGHT: LEVEL INDICATORS
        level(c, 660f, "WATER", 75, blue)
        level(c, 754f, "FOAM", 50, teal)

        // BOTTOM STATUS
        rect(c, 10f, 458f, 790f, 474f, pane, edge, 7f)

        text(c, "SYSTEM READY", 22f, 470f,
            10f, teal, true)

        text(c, "DEMO VALUES", 674f, 470f,
            10f, mute, true)

        c.restore()
    }

    // --------------------------------
    // VERTICAL GAUGE: -90 TO +90
    // --------------------------------

    private fun verticalGauge(
        c: Canvas,
        x: Float,
        y: Float,
        r: Float,
        min: Int,
        max: Int,
        value: Float
    ) {
        rect(c, 12f, 48f, 294f, 449f, pane, edge)

        centeredText(c, "VERTICAL ANGLE",
            x, 68f, 13f, white, true)

        // Upper semicircle
        val radius = r - 8f
        val arc = RectF(
            x - radius,
            y - radius,
            x + radius,
            y + radius
        )

        paint(edge, 2f, Paint.Style.STROKE)
        c.drawArc(arc, 180f, 180f, false, p)

        // Uniform scale and ticks
        for (i in 0..12) {

            val angle = Math.toRadians(180.0 + i * 15.0)

            val major = i % 2 == 0

            val inner = if (major) r - 27f else r - 18f
            val outer = r - 7f

            val x1 = x + cos(angle).toFloat() * inner
            val y1 = y + sin(angle).toFloat() * inner

            val x2 = x + cos(angle).toFloat() * outer
            val y2 = y + sin(angle).toFloat() * outer

            paint(
                if (major) white else mute,
                if (major) 2.2f else 1.1f
            )

            c.drawLine(x1, y1, x2, y2, p)

            if (major) {
                val v = min + (max - min) * i / 12
                val labelRadius = r - 48f

                val lx = x + cos(angle).toFloat() * labelRadius
                val ly = y + sin(angle).toFloat() * labelRadius

                centeredText(
                    c,
                    v.toString(),
                    lx,
                    ly + 4f,
                    11f,
                    mute
                )
            }
        }

        // Needle: zero points upward
        val needleAngle = Math.toRadians(
            270.0 + value.coerceIn(-90f, 90f)
        )

        paint(teal, 4f)

        c.drawLine(
            x, y,
            x + cos(needleAngle).toFloat() * (r - 40f),
            y + sin(needleAngle).toFloat() * (r - 40f),
            p
        )

        paint(white)
        c.drawCircle(x, y, 8f, p)

        paint(dark)
        c.drawCircle(x, y, 3f, p)

        // Digital value
        rect(c, x - 49f, y + 35f,
            x + 49f, y + 67f, dark, edge)

        centeredText(
            c,
            "%.1f°".format(value),
            x,
            y + 57f,
            18f,
            white,
            true
        )

        rect(c, x - 65f, 414f,
            x + 65f, 438f, pane, edge)

        centeredText(c, "VERTICAL",
            x, 431f, 12f, white, true)
    }

    // --------------------------------
    // HORIZONTAL GAUGE: 0 TO 360
    // --------------------------------

    private fun horizontalGauge(
        c: Canvas,
        x: Float,
        y: Float,
        r: Float,
        value: Float
    ) {
        rect(c, 301f, 48f, 615f, 449f, pane, edge)

        centeredText(c, "HORIZONTAL ANGLE",
            x, 68f, 13f, white, true)

        val radius = r - 7f

        val arc = RectF(
            x - radius,
            y - radius,
            x + radius,
            y + radius
        )

        // Complete 360-degree scale
        paint(teal, 6f, Paint.Style.STROKE)
        c.drawCircle(x, y, radius, p)

        // 36 divisions, every 10 degrees
        for (i in 0 until 36) {

            val degree = i * 10
            val angle = Math.toRadians(
                (degree - 90).toDouble()
            )

            val major = degree % 30 == 0

            val inner = if (major) r - 29f else r - 17f
            val outer = r - 6f

            val x1 = x + cos(angle).toFloat() * inner
            val y1 = y + sin(angle).toFloat() * inner

            val x2 = x + cos(angle).toFloat() * outer
            val y2 = y + sin(angle).toFloat() * outer

            paint(
                if (major) white else mute,
                if (major) 2.2f else 1.1f
            )

            c.drawLine(x1, y1, x2, y2, p)

            if (major) {

                val label = if (degree == 0)
                    "0/360"
                else
                    degree.toString()

                val labelRadius = r - 48f

                val lx = x + cos(angle).toFloat() * labelRadius
                val ly = y + sin(angle).toFloat() * labelRadius

                centeredText(
                    c,
                    label,
                    lx,
                    ly + 4f,
                    10f,
                    mute
                )
            }
        }

        // Needle: zero at top, clockwise increasing
        val needleAngle = Math.toRadians(
            (value - 90.0)
        )

        paint(teal, 4f)

        c.drawLine(
            x,
            y,
            x + cos(needleAngle).toFloat() * (r - 43f),
            y + sin(needleAngle).toFloat() * (r - 43f),
            p
        )

        paint(white)
        c.drawCircle(x, y, 9f, p)

        paint(dark)
        c.drawCircle(x, y, 3.5f, p)

        // Digital value
        rect(c, x - 50f, y + 28f,
            x + 50f, y + 61f, dark, edge)

        centeredText(
            c,
            "%.1f°".format(value),
            x,
            y + 51f,
            19f,
            white,
            true
        )

        rect(c, x - 70f, 414f,
            x + 70f, 438f, pane, edge)

        centeredText(c, "HORIZONTAL",
            x, 431f, 12f, white, true)
    }

    // --------------------------------
    // WATER / FOAM LEVEL INDICATORS
    // --------------------------------

    private fun level(
        c: Canvas,
        x: Float,
        title: String,
        percent: Int,
        color: Int
    ) {
        centeredText(c, title,
            x, 58f, 12f, white, true)

        rect(c, x - 36f, 70f,
            x + 36f, 444f, pane, edge)

        val top = 87f
        val bottom = 408f

        // Scale
        for (i in 0..4) {

            val yy = bottom - (bottom - top) * i / 4f

            paint(mute, 1f)

            c.drawLine(
                x - 40f, yy,
                x - 35f, yy,
                p
            )

            text(
                c,
                "${i * 25}",
                x - 65f,
                yy + 4f,
                9f,
                mute
            )
        }

        // Bar background
        rect(c, x - 19f, top,
            x + 19f, bottom, dark, white, 4f)

        val safePercent = percent.coerceIn(0, 100)

        val fillY =
            bottom - (bottom - top) * safePercent / 100f

        rect(
            c,
            x - 17f,
            fillY,
            x + 17f,
            bottom,
            color,
            null,
            3f
        )

        // Percentage display
        rect(c, x - 31f, 414f,
            x + 31f, 438f, dark, edge)

        centeredText(
            c,
            "$safePercent%",
            x,
            431f,
            13f,
            white,
            true
        )
    }
}
