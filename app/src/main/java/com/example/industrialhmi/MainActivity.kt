
package com.example.industrialhmi

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.View
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // مقادیر نمونه؛ بعداً به ورودی واقعی متصل می‌شوند
    var circularValue = 65f
    var arcValue = 25f
    var waterLevel = 72f
    var foamLevel = 38f

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
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        c.drawText(value, x, y, tp)
    }

    private fun panel(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        c.drawRoundRect(
            l, t, r, b, 15f, 15f,
            paint(dark)
        )
        c.drawRoundRect(
            l + 2, t + 2, r - 2, b - 2, 13f, 13f,
            paint(pane)
        )
        c.drawRoundRect(
            l, t, r, b, 15f, 15f,
            paint(edge, 1.5f, Paint.Style.STROKE)
        )
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        c.drawColor(bg)

        // Header
        c.drawRect(0f, 0f, 800f, 43f, paint(Color.rgb(22, 31, 37)))
        c.drawLine(0f, 43f, 800f, 43f, paint(edge, 1.5f))

        text(c, "INDUSTRIAL CONTROL SYSTEM", 22f, 29f, 18f, white)
        text(c, "SYSTEM ONLINE", 775f, 28f, 13f, teal, Paint.Align.RIGHT)

        // پنل اصلی گیج‌ها
        panel(c, 7f, 50f, 532f, 449f)

        // گیج دایره‌ای سمت چپ
        circularGauge(c, 178f, 240f, 160f, circularValue)

        // گیج قوسی سمت راست
        rightArcGauge(c, 358f, 240f, 160f, arcValue)

        // نشانگرهای سطح
        levelGauge(c, 640f, 90f, 407f, "WATER", waterLevel, blue)
        levelGauge(c, 735f, 90f, 407f, "FOAM", foamLevel, teal)

        // Footer
        c.drawRect(0f, 454f, 800f, 480f, paint(Color.rgb(22, 31, 37)))
        c.drawLine(0f, 454f, 800f, 454f, paint(edge, 1f))
        text(c, "POWER  ON", 18f, 473f, 12f, teal)
        text(c, "CONTROLLER READY", 400f, 473f, 12f, mute, Paint.Align.CENTER)
        text(c, "24.0 V", 780f, 473f, 12f, white, Paint.Align.RIGHT)
    }

    // گیج دایره‌ای کامل
    private fun circularGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        value: Float
    ) {
        val rect = RectF(cx-r, cy-r, cx+r, cy+r)

        // بدنه و قاب فلزی
        c.drawCircle(cx, cy, r + 8f, paint(Color.rgb(8, 12, 15)))
        c.drawCircle(cx, cy, r + 5f, paint(Color.rgb(112, 129, 137), 5f, Paint.Style.STROKE))
        c.drawCircle(cx, cy, r, paint(Color.rgb(13, 21, 27)))
        c.drawCircle(cx, cy, r - 5f, paint(edge, 1.5f, Paint.Style.STROKE))

        // حلقه داخلی
        c.drawCircle(cx, cy, r - 15f, paint(Color.rgb(45, 60, 68), 1f, Paint.Style.STROKE))

        // درجه‌بندی 0 تا 360
        for (i in 0 until 60) {
            val degree = i * 6f
            val a = Math.toRadians((degree - 90f).toDouble())

            val major = i % 5 == 0
            val r1 = if (major) r - 18f else r - 13f
            val r2 = r - 31f

            val x1 = cx + cos(a).toFloat() * r1
            val y1 = cy + sin(a).toFloat() * r1
            val x2 = cx + cos(a).toFloat() * r2
            val y2 = cy + sin(a).toFloat() * r2

            c.drawLine(
                x1, y1, x2, y2,
                paint(if (major) white else mute, if (major) 2.5f else 1.2f)
            )

            if (major) {
                val labelR = r - 49f
                val lx = cx + cos(a).toFloat() * labelR
                val ly = cy + sin(a).toFloat() * labelR + 5f
                text(c, "${i / 5 * 30}", lx, ly, 12f, mute, Paint.Align.CENTER)
            }
        }

        // عقربه
        val v = value.coerceIn(0f, 360f)
        val a = Math.toRadians((v - 90f).toDouble())
        val nx = cx + cos(a).toFloat() * (r - 55f)
        val ny = cy + sin(a).toFloat() * (r - 55f)

        c.drawLine(cx, cy, nx, ny, paint(teal, 5f))
        c.drawCircle(cx, cy, 12f, paint(Color.rgb(125, 145, 153)))
        c.drawCircle(cx, cy, 7f, paint(teal))

        // نمایش دیجیتال
        c.drawRoundRect(
            cx - 47f, cy + 43f, cx + 47f, cy + 76f,
            6f, 6f, paint(Color.rgb(3, 12, 16))
        )
        text(
            c,
            String.format(Locale.US, "%.1f", value),
            cx, cy + 66f, 22f, teal, Paint.Align.CENTER
        )

        text(c, "MAIN", cx, cy - 105f, 13f, mute, Paint.Align.CENTER)
    }

    // گیج قوسی سمت راست با بازه +90 تا -30 درجه
    private fun rightArcGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        value: Float
    ) {
        val rect = RectF(cx-r, cy-r, cx+r, cy+r)

        // فقط قوس قاب فلزی؛ دایره کامل رسم نمی‌شود
        c.drawArc(
            RectF(cx-r-7f, cy-r-7f, cx+r+7f, cy+r+7f),
            -90f, 120f, false,
            paint(Color.rgb(8, 12, 15), 13f, Paint.Style.STROKE)
        )

        c.drawArc(
            RectF(cx-r-4f, cy-r-4f, cx+r+4f, cy+r+4f),
            -90f, 120f, false,
            paint(Color.rgb(112, 129, 137), 6f, Paint.Style.STROKE)
        )

        c.drawArc(
            rect,
            -90f, 120f, false,
            paint(Color.rgb(45, 60, 68), 2f, Paint.Style.STROKE)
        )

        // درجات از +90 تا -30 با فاصله 10 درجه
        for (i in 0..12) {
            val degree = 90 - i * 10

            // تبدیل زاویه ریاضی به زاویه Canvas
            val a = Math.toRadians(-degree.toDouble())

            val major = i % 3 == 0
            val r1 = r - 17f
            val r2 = if (major) r - 38f else r - 29f

            val x1 = cx + cos(a).toFloat() * r1
            val y1 = cy + sin(a).toFloat() * r1
            val x2 = cx + cos(a).toFloat() * r2
            val y2 = cy + sin(a).toFloat() * r2

            c.drawLine(
                x1, y1, x2, y2,
                paint(if (major) white else mute, if (major) 2.5f else 1.3f)
            )

            if (major) {
                val labelR = r - 59f
                val lx = cx + cos(a).toFloat() * labelR
                val ly = cy + sin(a).toFloat() * labelR + 5f

                val label = when {
                    degree > 0 -> "+$degree"
                    degree < 0 -> "$degree"
                    else -> "0"
                }

                text(c, label, lx, ly, 14f, white, Paint.Align.CENTER)
            }
        }

        // عقربه؛ زاویه مثبت بالا و منفی پایین
        val v = value.coerceIn(-30f, 90f)
        val a = Math.toRadians(-v.toDouble())

        val nx = cx + cos(a).toFloat() * (r - 48f)
        val ny = cy + sin(a).toFloat() * (r - 48f)

        c.drawLine(cx, cy, nx, ny, paint(teal, 5f))
        c.drawCircle(cx, cy, 12f, paint(Color.rgb(125, 145, 153)))
        c.drawCircle(cx, cy, 7f, paint(teal))

        // نمایش دیجیتال داخل محدوده قوس
        c.drawRoundRect(
            cx + 23f, cy + 34f, cx + 111f, cy + 68f,
            6f, 6f, paint(Color.rgb(3, 12, 16))
        )

        text(
            c,
            String.format(Locale.US, "%+.1f", value),
            cx + 67f, cy + 57f, 18f, teal, Paint.Align.CENTER
        )

        text(c, "PRESSURE", cx + 66f, cy + 91f, 12f, mute, Paint.Align.CENTER)
    }

    // نمایشگر سطح آب و فوم
    private fun levelGauge(
        c: Canvas,
        x: Float,
        top: Float,
        bottom: Float,
        title: String,
        value: Float,
        color: Int
    ) {
        text(c, title, x, 68f, 14f, white, Paint.Align.CENTER)

        val w = 48f
        val h = bottom - top

        c.drawRoundRect(
            x-w/2, top, x+w/2, bottom,
            7f, 7f, paint(Color.rgb(5, 11, 15))
        )

        c.drawRoundRect(
            x-w/2, top, x+w/2, bottom,
            7f, 7f, paint(edge, 1.5f, Paint.Style.STROKE)
        )

        val v = value.coerceIn(0f, 100f)
        val fillHeight = h * v / 100f

        c.drawRoundRect(
            x-w/2+5f,
            bottom-fillHeight,
            x+w/2-5f,
            bottom-5f,
            4f, 4f,
            paint(color)
        )

        text(
            c,
            "${v.toInt()}%",
            x, bottom+22f, 14f, white, Paint.Align.CENTER
        )
    }
}
