
package com.example.industrialhmi

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.*
import android.view.View
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

        setContentView(Hmi(this))
    }
}

class Hmi(context: Context) : View(context) {

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

        // دو صفحه گرد با شعاع یکسان
circularGauge(canvas, 195f, 240f, 185f)
rightArcGauge(canvas, 410f, 240f, 185f)
drawLevelColumns(canvas)

        canvas.restore()
    }

    // قاب سه بعدی مشترک برای دایره و قوس
    private fun draw3DBezel(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        val oval = RectF(
            cx - r,
            cy - r,
            cx + r,
            cy + r
        )

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND

        // لایه تیره بیرونی
        p.color = Color.rgb(5, 9, 12)
        p.strokeWidth = 16f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        // لایه بدنه فلزی
        p.color = Color.rgb(45, 57, 65)
        p.strokeWidth = 12f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        // لبه روشن
        p.color = Color.rgb(125, 143, 151)
        p.strokeWidth = 4f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        // لبه داخلی تیره
        p.color = Color.rgb(20, 29, 35)
        p.strokeWidth = 2f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        // نوار باریک روشن برای جلوه فلزی
        p.color = Color.rgb(85, 105, 114)
        p.strokeWidth = 1.2f
        c.drawArc(
            oval,
            startAngle + 2f,
            sweepAngle - 4f,
            false,
            p
        )

        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

    // درجه بندی مشترک دو صفحه گرد
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

            val outerR = r - 22f
            val major = degree % 30 == 0
            val innerR = if (major) r - 43f else r - 32f

            val x1 = cx + cos(a).toFloat() * outerR
            val y1 = cy + sin(a).toFloat() * outerR

            val x2 = cx + cos(a).toFloat() * innerR
            val y2 = cy + sin(a).toFloat() * innerR

            p.style = Paint.Style.STROKE
            p.color = if (major) white else mute
            p.strokeWidth = if (major) 2.7f else 1.4f

            c.drawLine(x1, y1, x2, y2, p)

            if (major) {
                val labelR = r - 62f

                val tx = cx + cos(a).toFloat() * labelR
                val ty = cy + sin(a).toFloat() * labelR

                p.style = Paint.Style.FILL
                p.color = white
                p.textSize = 15f
                p.typeface = Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
                p.textAlign = Paint.Align.CENTER

                val label = if (degree > 0) "+$degree" else "$degree"

                c.drawText(label, tx, ty + 5f, p)
            }

            degree -= step
        }

        p.style = Paint.Style.FILL
    }

    // صفحه گرد کامل سمت چپ
    private fun circularGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float
    ) {
        draw3DBezel(c, cx, cy, r, 0f, 360f)

        p.style = Paint.Style.FILL
        p.color = Color.rgb(19, 27, 33)
        c.drawCircle(cx, cy, r - 17f, p)

        drawTicks(c, cx, cy, r, 170, -180, 10)

        // عقربه
        val angle = Math.toRadians(-circularValue.toDouble())
        val needleLength = r - 70f

        val nx = cx + cos(angle).toFloat() * needleLength
        val ny = cy + sin(angle).toFloat() * needleLength

        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.strokeCap = Paint.Cap.ROUND
        p.color = teal

        c.drawLine(cx, cy, nx, ny, p)

        p.style = Paint.Style.FILL
        p.color = white
        c.drawCircle(cx, cy, 10f, p)

        p.color = teal
        c.drawCircle(cx, cy, 5f, p)

        drawDigitalValue(c, cx, cy + 65f, circularValue, teal)
    }

    // صفحه قوسی سمت راست با قاب سه بعدی مشابه چپ
    private fun rightArcGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float
    ) {
        // فقط قوس 120 درجه؛ بدون دایره کامل
        draw3DBezel(c, cx, cy, r, -90f, 120f)

        drawTicks(c, cx, cy, r, 90, -30, 10)

        // عقربه
        val angle = Math.toRadians(-arcValue.toDouble())
        val needleLength = r - 70f

        val nx = cx + cos(angle).toFloat() * needleLength
        val ny = cy + sin(angle).toFloat() * needleLength

        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.strokeCap = Paint.Cap.ROUND
        p.color = blue

        c.drawLine(cx, cy, nx, ny, p)

        p.style = Paint.Style.FILL
        p.color = white
        c.drawCircle(cx, cy, 10f, p)

        p.color = blue
        c.drawCircle(cx, cy, 5f, p)

        drawDigitalValue(c, cx + 60f, cy + 65f, arcValue, blue)
    }

    // نمایشگر عدد دیجیتال
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
            cx - 48f,
            cy - 23f,
            cx + 48f,
            cy + 23f,
            7f,
            7f,
            p
        )

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = edge

        c.drawRoundRect(
            cx - 48f,
            cy - 23f,
            cx + 48f,
            cy + 23f,
            7f,
            7f,
            p
        )

        p.style = Paint.Style.FILL
        p.color = color
        p.textSize = 24f
        p.typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
        p.textAlign = Paint.Align.CENTER

        c.drawText(value.toInt().toString(), cx, cy + 8f, p)
    }

    // ستون های سطح آب و فوم
    private fun drawLevelColumns(c: Canvas) {

        val top = 45f
        val bottom = 435f

        // قطر بیشتر ستون ها
        val barWidth = 48f

        // فاصله دو ستون
        val waterX = 665f
        val foamX = 765f

        drawLevelBar(
            c, waterX, top, bottom,
            barWidth, waterLevel, blue
        )

        drawLevelBar(
            c, foamX, top, bottom,
            barWidth, foamLevel, teal
        )

        // نشانگرهای مشترک هر دو ستون
        val marks = listOf(
            1.00f to "FULL",
            0.75f to "3/4",
            0.50f to "1/2",
            0.25f to "1/4",
            0.00f to "EMPTY"
        )

        val centerX = (waterX + foamX) / 2f

        // خطوط فقط بین ستون ها
        val gapLeft = waterX + barWidth / 2f + 2f
        val gapRight = foamX - barWidth / 2f - 2f

        for ((ratio, label) in marks) {

            val y = bottom - (bottom - top) * ratio

            val textHalfWidth = when (label) {
                "EMPTY" -> 20f
                "FULL" -> 16f
                else -> 13f
            }

            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.3f
            p.color = mute

            c.drawLine(
                gapLeft,
                y,
                centerX - textHalfWidth - 3f,
                y,
                p
            )

            c.drawLine(
                centerX + textHalfWidth + 3f,
                y,
                gapRight,
                y,
                p
            )

            // زمینه نوشته
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
            p.typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            p.textAlign = Paint.Align.CENTER

            c.drawText(label, centerX, y + 4f, p)
        }

        // عنوان ستون ها
        p.style = Paint.Style.FILL
        p.color = blue
        p.textSize = 14f
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )

        c.drawText("WATER", waterX, 24f, p)

        p.color = teal
        c.drawText("FOAM", foamX, 24f, p)

        // درصدها
        p.color = white
        p.textSize = 15f

        c.drawText(
            "${waterLevel.toInt()}%",
            waterX,
            463f,
            p
        )

        c.drawText(
            "${foamLevel.toInt()}%",
            foamX,
            463f,
            p
        )
    }

    // رسم ستون سطح
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

        c.drawRoundRect(
            left,
            top,
            right,
            bottom,
            6f,
            6f,
            p
        )

        // بخش پرشده
        val fillTop = bottom -
                (bottom - top) * (level / 100f)

        p.color = color

        c.drawRoundRect(
            left + 3f,
            fillTop,
            right - 3f,
            bottom - 3f,
            4f,
            4f,
            p
        )

        // قاب ستون
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = edge

        c.drawRoundRect(
            left,
            top,
            right,
            bottom,
            6f,
            6f,
            p
        )

        p.style = Paint.Style.FILL
    }
}
