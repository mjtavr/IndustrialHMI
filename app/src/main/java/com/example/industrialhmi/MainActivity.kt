
package com.example.industrialhmi

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.graphics.*
import android.view.View
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        setContentView(Hmi(this))
    }
}

class Hmi(context: android.content.Context) : View(context) {

    private val bg = Color.rgb(16, 22, 27)
    private val pane = Color.rgb(27, 36, 43)
    private val edge = Color.rgb(76, 94, 104)
    private val white = Color.rgb(230, 237, 240)
    private val mute = Color.rgb(135, 153, 163)
    private val teal = Color.rgb(0, 210, 190)
    private val blue = Color.rgb(45, 155, 230)
    private val dark = Color.rgb(8, 13, 17)

    // مقادیر آزمایشی
    var circularValue = 65f
    var arcValue = 25f
    var waterLevel = 72f
    var foamLevel = 38f

    // ابعاد مشترک دو گیج
    private val gaugeRadius = 185f

    private val circleX = 203f
    private val arcX = 400f
    private val gaugeY = 245f

    private fun paint(
        color: Int,
        width: Float = 1f,
        style: Paint.Style = Paint.Style.FILL
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = width
            this.style = style
            strokeCap = Paint.Cap.ROUND
        }
    }

    private fun text(
        c: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int = white,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        val tp = paint(color).apply {
            textSize = size
            textAlign = align
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
        }
        c.drawText(value, x, y, tp)
    }

    private fun panel(
        c: Canvas,
        l: Float,
        t: Float,
        r: Float,
        b: Float
    ) {
        c.drawRoundRect(
            l, t, r, b, 15f, 15f,
            paint(dark)
        )

        c.drawRoundRect(
            l + 2, t + 2, r - 2, b - 2,
            13f, 13f,
            paint(pane)
        )

        c.drawRoundRect(
            l, t, r, b, 15f, 15f,
            paint(edge, 1.5f, Paint.Style.STROKE)
        )
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        c.save()

        // مقیاس کل رابط برای نمایشگر 800x480
        val sx = width / 800f
        val sy = height / 480f
        c.scale(sx, sy)

        c.drawColor(bg)

        // HEADER
        c.drawRect(
            0f, 0f, 800f, 43f,
            paint(Color.rgb(22, 31, 37))
        )

        c.drawLine(
            0f, 43f, 800f, 43f,
            paint(edge, 1.5f)
        )

        text(
            c, "INDUSTRIAL CONTROL SYSTEM",
            22f, 29f, 18f, white
        )

        text(
            c, "SYSTEM ONLINE",
            775f, 28f, 13f, teal,
            Paint.Align.RIGHT
        )

        // پنل اصلی
        panel(c, 7f, 48f, 600f, 449f)

        // گیج دایره‌ای
        circularGauge(
            c, circleX, gaugeY,
            gaugeRadius, circularValue
        )

        // گیج قوسی
        rightArcGauge(
            c, arcX, gaugeY,
            gaugeRadius, arcValue
        )

        // ستون‌های سطح
        drawLevelColumns(c)

        // FOOTER
        c.drawRect(
            0f, 454f, 800f, 480f,
            paint(Color.rgb(22, 31, 37))
        )

        c.drawLine(
            0f, 454f, 800f, 454f,
            paint(edge, 1f)
        )

        text(c, "POWER ON", 18f, 473f, 12f, teal)

        text(
            c, "CONTROLLER READY",
            400f, 473f, 12f, mute,
            Paint.Align.CENTER
        )

        text(
            c, "24.0 V",
            780f, 473f, 12f, white,
            Paint.Align.RIGHT
        )

        c.restore()
    }

    // ------------------------------------------------
    // درجه‌بندی مشترک دو گیج
    // فاصله تقسیمات و طول خطوط در هر دو یکسان است
    // ------------------------------------------------

    private fun drawTicks(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        startDegree: Int,
        endDegree: Int,
        step: Int
    ) {

        var degree = startDegree

        while (degree >= endDegree) {

            val a = Math.toRadians(-degree.toDouble())

            // درجه‌های اصلی هر 30 درجه
            val major = degree % 30 == 0

            val outerR = r - 20f

            val innerR = if (major) {
                r - 40f
            } else {
                r - 30f
            }

            val x1 = cx + cos(a).toFloat() * outerR
            val y1 = cy + sin(a).toFloat() * outerR

            val x2 = cx + cos(a).toFloat() * innerR
            val y2 = cy + sin(a).toFloat() * innerR

            c.drawLine(
                x1, y1, x2, y2,
                paint(
                    if (major) white else mute,
                    if (major) 2.5f else 1.3f
                )
            )

            // عددگذاری هر 30 درجه
            if (major) {

                val labelR = r - 58f

                val lx = cx + cos(a).toFloat() * labelR
                val ly = cy + sin(a).toFloat() * labelR + 5f

                val label = when {
                    degree > 0 -> "+$degree"
                    degree < 0 -> "$degree"
                    else -> "0"
                }

                text(
                    c, label, lx, ly,
                    14f, white, Paint.Align.CENTER
                )
            }

            degree -= step
        }
    }

    // ------------------------------------------------
    // قاب کامل گیج دایره‌ای
    // ------------------------------------------------

    private fun circularGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        value: Float
    ) {

        // قاب بیرونی
        c.drawCircle(
            cx, cy, r + 8f,
            paint(Color.rgb(8, 12, 15))
        )

        c.drawCircle(
            cx, cy, r + 5f,
            paint(
                Color.rgb(112, 129, 137),
                5f,
                Paint.Style.STROKE
            )
        )

        // صفحه
        c.drawCircle(
            cx, cy, r,
            paint(Color.rgb(13, 21, 27))
        )

        c.drawCircle(
            cx, cy, r - 5f,
            paint(edge, 1.5f, Paint.Style.STROKE)
        )

        c.drawCircle(
            cx, cy, r - 15f,
            paint(
                Color.rgb(45, 60, 68),
                1f,
                Paint.Style.STROKE
            )
        )

        // تقسیمات از -180 تا +170
        drawTicks(
            c, cx, cy, r,
            170, -180, 10
        )

        // عقربه
        val v = value.coerceIn(-180f, 180f)
        val a = Math.toRadians(-v.toDouble())

        val nx = cx + cos(a).toFloat() * (r - 55f)
        val ny = cy + sin(a).toFloat() * (r - 55f)

        c.drawLine(
            cx, cy, nx, ny,
            paint(teal, 5f)
        )

        // محور عقربه
        c.drawCircle(
            cx, cy, 12f,
            paint(Color.rgb(125, 145, 153))
        )

        c.drawCircle(
            cx, cy, 7f,
            paint(teal)
        )

        // نمایش دیجیتال
        c.drawRoundRect(
            cx - 48f, cy + 48f,
            cx + 48f, cy + 83f,
            6f, 6f,
            paint(Color.rgb(3, 12, 16))
        )

        text(
            c,
            String.format(Locale.US, "%+.1f", value),
            cx, cy + 72f,
            22f, teal, Paint.Align.CENTER
        )

        text(
            c, "MAIN",
            cx, cy - 112f,
            13f, mute, Paint.Align.CENTER
        )
    }

    // ------------------------------------------------
    // گیج قوسی سمت راست
    // +90 تا -30 درجه
    // ------------------------------------------------

    private fun rightArcGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        value: Float
    ) {

        val rect = RectF(
            cx - r, cy - r,
            cx + r, cy + r
        )

        // قاب بیرونی قوس
        c.drawArc(
            RectF(
                cx - r - 7f,
                cy - r - 7f,
                cx + r + 7f,
                cy + r + 7f
            ),
            -90f, 120f, false,
            paint(
                Color.rgb(8, 12, 15),
                13f,
                Paint.Style.STROKE
            )
        )

        // قاب فلزی
        c.drawArc(
            RectF(
                cx - r - 4f,
                cy - r - 4f,
                cx + r + 4f,
                cy + r + 4f
            ),
            -90f, 120f, false,
            paint(
                Color.rgb(112, 129, 137),
                6f,
                Paint.Style.STROKE
            )
        )

        // خط داخلی
        c.drawArc(
            rect,
            -90f, 120f, false,
            paint(
                Color.rgb(45, 60, 68),
                2f,
                Paint.Style.STROKE
            )
        )

        // همان درجه‌بندی گیج دایره‌ای
        drawTicks(
            c, cx, cy, r,
            90, -30, 10
        )

        // عقربه
        val v = value.coerceIn(-30f, 90f)
        val a = Math.toRadians(-v.toDouble())

        val nx = cx + cos(a).toFloat() * (r - 55f)
        val ny = cy + sin(a).toFloat() * (r - 55f)

        c.drawLine(
            cx, cy, nx, ny,
            paint(teal, 5f)
        )

        c.drawCircle(
            cx, cy, 12f,
            paint(Color.rgb(125, 145, 153))
        )

        c.drawCircle(
            cx, cy, 7f,
            paint(teal)
        )

        // نمایش دیجیتال
        c.drawRoundRect(
            cx + 25f, cy + 35f,
            cx + 111f, cy + 70f,
            6f, 6f,
            paint(Color.rgb(3, 12, 16))
        )

        text(
            c,
            String.format(Locale.US, "%+.1f", value),
            cx + 68f, cy + 58f,
            18f, teal, Paint.Align.CENTER
        )

        text(
            c, "PRESSURE",
            cx + 68f, cy + 91f,
            12f, mute, Paint.Align.CENTER
        )
    }

    // ------------------------------------------------
    // دو ستون سطح با مقیاس مشترک
    // ------------------------------------------------

    private fun drawLevelColumns(c: Canvas) {

        val leftX = 665f
        val rightX = 775f

        val top = 105f
        val bottom = 415f

        val barWidth = 38f

        text(
            c, "WATER",
            leftX, 78f, 14f, white,
            Paint.Align.CENTER
        )

        text(
            c, "FOAM",
            rightX, 78f, 14f, white,
            Paint.Align.CENTER
        )

        // بدنه ستون‌ها
        drawLevelBar(
            c, leftX, top, bottom,
            barWidth, waterLevel, blue
        )

        drawLevelBar(
            c, rightX, top, bottom,
            barWidth, foamLevel, teal
        )

        // چهار خط مرجع مشترک
        val marks = listOf(
            Pair(100f, "FULL"),
            Pair(0.75f, "3/4"),
            Pair(0.50f, "1/2"),
            Pair(0f, "EMPTY")
        )

        for ((ratio, label) in marks) {

            val y = if (ratio == 100f) {
                top
            } else {
                bottom - (bottom - top) * ratio
            }

            // خط مرجع روی هر دو ستون
            c.drawLine(
                leftX - barWidth / 2f - 2f,
                y,
                leftX + barWidth / 2f + 2f,
                y,
                paint(white, 1.5f)
            )

            c.drawLine(
                rightX - barWidth / 2f - 2f,
                y,
                rightX + barWidth / 2f + 2f,
                y,
                paint(white, 1.5f)
            )

            // خطوط کوتاه بین ستون‌ها
            c.drawLine(
                leftX + barWidth / 2f + 4f,
                y,
                697f,
                y,
                paint(mute, 1f)
            )

            c.drawLine(
                743f,
                y,
                rightX - barWidth / 2f - 4f,
                y,
                paint(mute, 1f)
            )

            // برچسب مشترک وسط
            c.drawRoundRect(
                697f, y - 10f,
                743f, y + 10f,
                3f, 3f,
                paint(pane)
            )

            text(
                c, label,
                720f, y + 4f,
                11f, white,
                Paint.Align.CENTER
            )
        }

        // درصدها
        text(
            c, "${waterLevel.toInt()}%",
            leftX, 440f, 13f, blue,
            Paint.Align.CENTER
        )

        text(
            c, "${foamLevel.toInt()}%",
            rightX, 440f, 13f, teal,
            Paint.Align.CENTER
        )
    }

    // رسم بدنه و پرشدگی ستون
    private fun drawLevelBar(
        c: Canvas,
        x: Float,
        top: Float,
        bottom: Float,
        w: Float,
        value: Float,
        color: Int
    ) {

        // زمینه
        c.drawRoundRect(
            x - w / 2f,
            top,
            x + w / 2f,
            bottom,
            6f, 6f,
            paint(Color.rgb(5, 11, 15))
        )

        // سطح پرشدگی
        val v = value.coerceIn(0f, 100f)
        val fillHeight = (bottom - top) * v / 100f

        c.drawRoundRect(
            x - w / 2f + 4f,
            bottom - fillHeight,
            x + w / 2f - 4f,
            bottom - 4f,
            3f, 3f,
            paint(color)
        )

        // قاب
        c.drawRoundRect(
            x - w / 2f,
            top,
            x + w / 2f,
            bottom,
            6f, 6f,
            paint(edge, 1.5f, Paint.Style.STROKE)
        )
    }
}
