
package com.example.industrialhmi

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.graphics.*
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlin.math.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        setContentView(Hmi())
    }
}

class Hmi : View(this.context) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bg = Color.rgb(16, 22, 27)
    private val pane = Color.rgb(27, 36, 43)
    private val edge = Color.rgb(76, 94, 104)
    private val white = Color.rgb(230, 237, 240)
    private val mute = Color.rgb(135, 153, 163)
    private val teal = Color.rgb(0, 210, 190)
    private val blue = Color.rgb(45, 155, 230)
    private val dark = Color.rgb(8, 13, 17)

    private val circularValue = 65f
    private val arcValue = 25f
    private val waterLevel = 72f
    private val foamLevel = 38f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sx = width / 800f
        val sy = height / 480f

        canvas.save()
        canvas.scale(sx, sy)

        p.style = Paint.Style.FILL
        p.color = bg
        canvas.drawRect(0f, 0f, 800f, 480f, p)

        drawHeader(canvas)
        drawPanel(canvas)

        // هر دو صفحه‌گرد دارای شعاع یکسان هستند
        circularGauge(canvas, 203f, 245f, 195f)
        rightArcGauge(canvas, 400f, 245f, 195f)

        drawLevelColumns(canvas)
        drawFooter(canvas)

        canvas.restore()
    }

    private fun drawHeader(c: Canvas) {
        p.style = Paint.Style.FILL
        p.color = white
        p.textSize = 16f
        p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        c.drawText("INDUSTRIAL MONITORING SYSTEM", 400f, 24f, p)

        p.color = teal
        p.textSize = 11f
        p.typeface = Typeface.DEFAULT
        c.drawText("LIVE DATA", 690f, 24f, p)
    }

    private fun drawPanel(c: Canvas) {
        p.style = Paint.Style.FILL
        p.color = pane
        c.drawRoundRect(5f, 39f, 600f, 452f, 18f, 18f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = edge
        c.drawRoundRect(5f, 39f, 600f, 452f, 18f, 18f, p)
    }

    private fun drawFooter(c: Canvas) {
        p.style = Paint.Style.FILL
        p.color = mute
        p.textSize = 11f
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT
        c.drawText("SYSTEM NORMAL     |     SENSOR STATUS: OK", 400f, 471f, p)

        p.color = teal
        c.drawCircle(590f, 466f, 4f, p)
    }

    // قاب سه‌بعدی دایره کامل
    private fun draw3DCircle(c: Canvas, cx: Float, cy: Float, r: Float) {
        p.style = Paint.Style.STROKE

        p.color = dark
        p.strokeWidth = 14f
        c.drawCircle(cx, cy, r, p)

        p.color = Color.rgb(45, 57, 65)
        p.strokeWidth = 9f
        c.drawCircle(cx, cy, r - 3f, p)

        p.color = Color.rgb(115, 132, 140)
        p.strokeWidth = 3f
        c.drawCircle(cx, cy, r - 8f, p)

        p.color = Color.rgb(20, 29, 35)
        p.strokeWidth = 2f
        c.drawCircle(cx, cy, r - 13f, p)

        p.style = Paint.Style.FILL
    }

    // قاب سه‌بعدی قوس؛ با همان ضخامت و ظاهر قاب دایره
    private fun draw3DArc(c: Canvas, cx: Float, cy: Float, r: Float) {
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND

        p.color = dark
        p.strokeWidth = 14f
        c.drawArc(oval, -90f, 120f, false, p)

        p.color = Color.rgb(45, 57, 65)
        p.strokeWidth = 9f
        c.drawArc(oval, -90f, 120f, false, p)

        p.color = Color.rgb(115, 132, 140)
        p.strokeWidth = 3f
        c.drawArc(oval, -90f, 120f, false, p)

        p.color = Color.rgb(20, 29, 35)
        p.strokeWidth = 2f
        c.drawArc(oval, -90f, 120f, false, p)

        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

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

            val outerR = r - 20f
            val major = degree % 30 == 0
            val innerR = if (major) r - 40f else r - 30f

            val x1 = cx + cos(a).toFloat() * outerR
            val y1 = cy + sin(a).toFloat() * outerR
            val x2 = cx + cos(a).toFloat() * innerR
            val y2 = cy + sin(a).toFloat() * innerR

            p.style = Paint.Style.STROKE
            p.color = if (major) white else mute
            p.strokeWidth = if (major) 2.5f else 1.3f
            c.drawLine(x1, y1, x2, y2, p)

            if (major) {
                val labelR = r - 58f
                val tx = cx + cos(a).toFloat() * labelR
                val ty = cy + sin(a).toFloat() * labelR

                p.style = Paint.Style.FILL
                p.color = white
                p.textSize = 14f
                p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                p.textAlign = Paint.Align.CENTER

                val label = if (degree > 0) "+$degree" else "$degree"
                c.drawText(label, tx, ty + 5f, p)
            }

            degree -= step
        }

        p.style = Paint.Style.FILL
    }

    private fun circularGauge(c: Canvas, cx: Float, cy: Float, r: Float) {
        draw3DCircle(c, cx, cy, r)

        p.style = Paint.Style.FILL
        p.color = Color.rgb(19, 27, 33)
        c.drawCircle(cx, cy, r - 15f, p)

        // درجه‌بندی مشترک با قوس سمت راست
        drawTicks(c, cx, cy, r, 170, -180, 10)

        // عقربه
        val angle = Math.toRadians(-circularValue.toDouble())
        val needleLength = r - 65f
        val nx = cx + cos(angle).toFloat() * needleLength
        val ny = cy + sin(angle).toFloat() * needleLength

        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.strokeCap = Paint.Cap.ROUND
        p.color = teal
        c.drawLine(cx, cy, nx, ny, p)

        p.style = Paint.Style.FILL
        p.color = white
        c.drawCircle(cx, cy, 9f, p)
        p.color = teal
        c.drawCircle(cx, cy, 5f, p)

        // نمایش عدد
        drawDigitalValue(c, cx, cy + 53f, circularValue, teal)
    }

    private fun rightArcGauge(c: Canvas, cx: Float, cy: Float, r: Float) {
        draw3DArc(c, cx, cy, r)

        // سطح داخلی قوس
        p.style = Paint.Style.STROKE
        p.color = Color.rgb(19, 27, 33)
        p.strokeWidth = 1f

        // درجه‌بندی همانند صفحه‌گرد چپ
        drawTicks(c, cx, cy, r, 90, -30, 10)

        // عقربه قوس
        val angle = Math.toRadians(-arcValue.toDouble())
        val needleLength = r - 65f
        val nx = cx + cos(angle).toFloat() * needleLength
        val ny = cy + sin(angle).toFloat() * needleLength

        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.strokeCap = Paint.Cap.ROUND
        p.color = blue
        c.drawLine(cx, cy, nx, ny, p)

        p.style = Paint.Style.FILL
        p.color = white
        c.drawCircle(cx, cy, 9f, p)
        p.color = blue
        c.drawCircle(cx, cy, 5f, p)

        drawDigitalValue(c, cx + 57f, cy + 54f, arcValue, blue)

        p.color = mute
        p.textSize = 11f
        p.textAlign = Paint.Align.CENTER
        c.drawText("PRESSURE", cx + 57f, cy + 88f, p)
    }

    private fun drawDigitalValue(
        c: Canvas,
        cx: Float,
        cy: Float,
        value: Float,
        color: Int
    ) {
        p.style = Paint.Style.FILL
        p.color = dark
        c.drawRoundRect(
            cx - 47f, cy - 22f,
            cx + 47f, cy + 22f,
            7f, 7f, p
        )

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = edge
        c.drawRoundRect(
            cx - 47f, cy - 22f,
            cx + 47f, cy + 22f,
            7f, 7f, p
        )

        p.style = Paint.Style.FILL
        p.color = color
        p.textSize = 23f
        p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        c.drawText(value.toInt().toString(), cx, cy + 8f, p)
    }

    private fun drawLevelColumns(c: Canvas) {
        val top = 105f
        val bottom = 415f
        val barWidth = 36f

        // ستون‌ها کمی به هم نزدیک‌تر شده‌اند
        val waterX = 670f
        val foamX = 770f

        drawLevelBar(
            c, waterX, top, bottom, barWidth,
            waterLevel, blue
        )

        drawLevelBar(
            c, foamX, top, bottom, barWidth,
            foamLevel, teal
        )

        // برچسب‌های مشترک و خطوط فقط در فاصله بین دو ستون
        val marks = listOf(
            1.00f to "FULL",
            0.75f to "3/4",
            0.50f to "1/2",
            0.25f to "1/4",
            0.00f to "EMPTY"
        )

        val centerX = (waterX + foamX) / 2f
        val gapLeft = waterX + barWidth / 2f + 2f
        val gapRight = foamX - barWidth / 2f - 2f

        for ((ratio, label) in marks) {
            val y = bottom - (bottom - top) * ratio

            // خطوط کوتاه در فضای خالی بین ستون‌ها
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.2f
            p.color = mute

            val textHalfWidth = when (label) {
                "EMPTY" -> 20f
                "FULL" -> 16f
                else -> 13f
            }

            c.drawLine(gapLeft, y, centerX - textHalfWidth - 3f, y, p)
            c.drawLine(centerX + textHalfWidth + 3f, y, gapRight, y, p)

            // زمینه کوچک برای خوانایی نوشته
            p.style = Paint.Style.FILL
            p.color = pane
            c.drawRect(
                centerX - textHalfWidth - 2f,
                y - 10f,
                centerX + textHalfWidth + 2f,
                y + 10f,
                p
            )

            p.color = white
            p.textSize = 11f
            p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            p.textAlign = Paint.Align.CENTER
            c.drawText(label, centerX, y + 4f, p)
        }

        // عنوان ستون‌ها
        p.style = Paint.Style.FILL
        p.color = blue
        p.textSize = 13f
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        c.drawText("WATER", waterX, 87f, p)

        p.color = teal
        c.drawText("FOAM", foamX, 87f, p)

        // درصدها
        p.color = white
        p.textSize = 14f
        c.drawText("${waterLevel.toInt()}%", waterX, 440f, p)
        c.drawText("${foamLevel.toInt()}%", foamX, 440f, p)
    }

    private fun drawLevelBar(
        c: Canvas,
        cx: Float,
        top: Float,
        bottom: Float,
        w: Float,
        level: Float,
        color: Int
    ) {
        val left = cx - w / 2f
        val right = cx + w / 2f

        // زمینه ستون
        p.style = Paint.Style.FILL
        p.color = dark
        c.drawRoundRect(left, top, right, bottom, 5f, 5f, p)

        // بخش پرشده
        val fillTop = bottom - (bottom - top) * (level / 100f)
        p.color = color
        c.drawRoundRect(left + 3f, fillTop, right - 3f, bottom - 3f, 3f, 3f, p)

        // قاب
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = edge
        c.drawRoundRect(left, top, right, bottom, 5f, 5f, p)

        p.style = Paint.Style.FILL
    }
}
