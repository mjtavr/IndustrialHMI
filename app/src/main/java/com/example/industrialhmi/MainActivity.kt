
package com.example.industrialhmi

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.app.PendingIntent
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.widget.Toast
import android.graphics.*
import android.view.View
import android.view.WindowManager

import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

import kotlin.math.*

class MainActivity : Activity() {

    private lateinit var usbManager: UsbManager
    private lateinit var hmi: Hmi

    private var serialPort: UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var ioManager: SerialInputOutputManager? = null

    private val permissionAction =
        "com.example.industrialhmi.USB_PERMISSION"

    private val rxBuffer = ArrayList<Int>()

    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == permissionAction) {

                val granted = intent.getBooleanExtra(
                   "permission",
                    false
                )

                if (granted) {
                    connectUsb()
                } else {
                    showMessage("USB permission denied")
                }
            }

            if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                connectUsb()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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

        usbManager =
            getSystemService(Context.USB_SERVICE) as UsbManager

        hmi = Hmi(this)
        setContentView(hmi)

        val filter = IntentFilter().apply {
            addAction(permissionAction)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                usbReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        connectUsb()
    }

    override fun onDestroy() {
        closeUsb()

        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // پیدا کردن FT232 و درخواست مجوز USB
    private fun connectUsb() {

        if (serialPort != null) return

        val drivers =
            UsbSerialProber.getDefaultProber()
                .findAllDrivers(usbManager)

        if (drivers.isEmpty()) {
            return
        }

        val driver = drivers[0]
        val device = driver.device

        if (!usbManager.hasPermission(device)) {

            val intent = Intent(permissionAction).apply {
                setPackage(packageName)
            }

            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_MUTABLE
                else
                    0

            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                flags
            )

            usbManager.requestPermission(
                device,
                permissionIntent
            )

            return
        }

        openSerialPort(driver)
    }

    // باز کردن پورت سریال
    private fun openSerialPort(
        driver: com.hoho.android.usbserial.driver.UsbSerialDriver
    ) {

        try {

            val connection =
                usbManager.openDevice(driver.device)

            if (connection == null) {
                showMessage("USB connection failed")
                return
            }

            val port = driver.ports[0]

            port.open(connection)

            port.setParameters(
                115200,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )

            usbConnection = connection
            serialPort = port

            ioManager = SerialInputOutputManager(
                port,
                object : SerialInputOutputManager.Listener {

                    override fun onNewData(data: ByteArray) {
                        parseIncoming(data)
                    }

                    override fun onRunError(e: Exception) {
                        showMessage("USB serial error: ${e.message}")
                    }
                }
            )

            ioManager?.start()

            showMessage("FT232 connected")

        } catch (e: Exception) {

            closeUsb()
            showMessage("USB error: ${e.message}")
        }
    }

    // بستن ارتباط
    private fun closeUsb() {

        try {
            ioManager?.stop()
        } catch (_: Exception) {
        }

        ioManager = null

        try {
            serialPort?.close()
        } catch (_: Exception) {
        }

        serialPort = null

        try {
            usbConnection?.close()
        } catch (_: Exception) {
        }

        usbConnection = null
    }

    // دریافت اطلاعات؛ ممکن است هر بار فقط بخشی از فریم برسد
    private fun parseIncoming(data: ByteArray) {

        synchronized(rxBuffer) {

            for (b in data) {
                rxBuffer.add(b.toInt() and 0xFF)
            }

            while (rxBuffer.size >= 15) {

                // پیدا کردن سرآیند AA 55
                if (rxBuffer[0] != 0xAA ||
                    rxBuffer[1] != 0x55) {

                    rxBuffer.removeAt(0)
                    continue
                }

                // کنترل نسخه، نوع و طول
                if (rxBuffer[2] != 0x01 ||
                    rxBuffer[3] != 0x01 ||
                    rxBuffer[5] != 0x07) {

                    rxBuffer.removeAt(0)
                    continue
                }

                val frame = IntArray(15) {
                    rxBuffer[it]
                }

                val calculatedCrc =
                    crc16(frame, 2, 11)

                val receivedCrc =
                    (frame[13] shl 8) or frame[14]

                if (calculatedCrc != receivedCrc) {
                    rxBuffer.removeAt(0)
                    continue
                }

                // استخراج مقادیر علامت دار 16 بیتی
                val hRaw = signed16(frame[6], frame[7])
                val vRaw = signed16(frame[8], frame[9])

                val water = frame[10]
                val foam = frame[11]
                val status = frame[12]

                // اعتبارسنجی داده ها
                if (hRaw in 0..36000 &&
                    vRaw in -9000..9000 &&
                    water in 0..100 &&
                    foam in 0..100) {

                    val heading = hRaw / 100f
                    val vertical = vRaw / 100f

                    runOnUiThread {
                        hmi.updateData(
                            heading,
                            vertical,
                            water,
                            foam
                        )
                    }
                }

                // حذف فریم دریافت شده
                repeat(15) {
                    rxBuffer.removeAt(0)
                }
            }

            // جلوگیری از رشد بی نهایت بافر در صورت داده نامعتبر
            if (rxBuffer.size > 256) {
                rxBuffer.clear()
            }
        }
    }

    private fun signed16(high: Int, low: Int): Int {
        return ((high shl 8) or low).toShort().toInt()
    }

    // CRC-16/CCITT-FALSE
    private fun crc16(
        data: IntArray,
        start: Int,
        length: Int
    ): Int {

        var crc = 0xFFFF

        for (i in start until start + length) {

            crc = crc xor (data[i] shl 8)

            repeat(8) {

                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }

                crc = crc and 0xFFFF
            }
        }

        return crc
    }
}

// ======================================================
// HMI DISPLAY
// ======================================================

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

    @Volatile private var circularValue = 65f
    @Volatile private var arcValue = 25f
    @Volatile private var waterLevel = 72f
    @Volatile private var foamLevel = 38f

    fun updateData(
        heading: Float,
        vertical: Float,
        water: Int,
        foam: Int
    ) {
        circularValue = heading.coerceIn(0f, 360f)
        arcValue = vertical.coerceIn(-90f, 90f)
        waterLevel = water.coerceIn(0, 100).toFloat()
        foamLevel = foam.coerceIn(0, 100).toFloat()

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sx = width / 800f
        val sy = height / 480f

        canvas.save()
        canvas.scale(sx, sy)

        p.style = Paint.Style.FILL
        p.color = bg
        canvas.drawRect(0f, 0f, 800f, 480f, p)

        circularGauge(canvas, 195f, 240f, 185f)
        rightArcGauge(canvas, 410f, 240f, 185f)

        drawLevelColumns(canvas)

        canvas.restore()
    }

    private fun draw3DBezel(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        val oval = RectF(
            cx - r, cy - r,
            cx + r, cy + r
        )

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND

        p.color = Color.rgb(5, 9, 12)
        p.strokeWidth = 16f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        p.color = Color.rgb(45, 57, 65)
        p.strokeWidth = 12f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        p.color = Color.rgb(125, 143, 151)
        p.strokeWidth = 4f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

        p.color = Color.rgb(20, 29, 35)
        p.strokeWidth = 2f
        c.drawArc(oval, startAngle, sweepAngle, false, p)

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

                val label =
                    if (degree > 0) "+$degree" else "$degree"

                c.drawText(label, tx, ty + 5f, p)
            }

            degree -= step
        }

        p.style = Paint.Style.FILL
    }

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

        // تبدیل زاویه 0..360 به بازه -180..180 برای عقربه
        val needleDegree =
            if (circularValue > 180f)
                circularValue - 360f
            else
                circularValue

        val angle = Math.toRadians(-needleDegree.toDouble())
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

        drawDigitalValue(
            c, cx, cy + 65f,
            circularValue, teal
        )
    }

    private fun rightArcGauge(
        c: Canvas,
        cx: Float,
        cy: Float,
        r: Float
    ) {
        draw3DBezel(c, cx, cy, r, -90f, 120f)

        drawTicks(c, cx, cy, r, 90, -30, 10)

        // این قوس از +90 تا -30 درجه نمایش داده می شود
        val needleDegree = arcValue.coerceIn(-30f, 90f)

        val angle = Math.toRadians(-needleDegree.toDouble())
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

        drawDigitalValue(
            c, cx + 60f, cy + 65f,
            arcValue, blue
        )
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
            cx - 48f, cy - 23f,
            cx + 48f, cy + 23f,
            7f, 7f, p
        )

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = edge

        c.drawRoundRect(
            cx - 48f, cy - 23f,
            cx + 48f, cy + 23f,
            7f, 7f, p
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

    private fun drawLevelColumns(c: Canvas) {

        val top = 45f
        val bottom = 435f
        val barWidth = 48f

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

            val textHalfWidth = when (label) {
                "EMPTY" -> 20f
                "FULL" -> 16f
                else -> 13f
            }

            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.3f
            p.color = mute

            c.drawLine(
                gapLeft, y,
                centerX - textHalfWidth - 3f, y, p
            )

            c.drawLine(
                centerX + textHalfWidth + 3f, y,
                gapRight, y, p
            )

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

        p.color = white
        p.textSize = 15f

        c.drawText(
            "${waterLevel.toInt()}%",
            waterX, 463f, p
        )

        c.drawText(
            "${foamLevel.toInt()}%",
            foamX, 463f, p
        )
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

        p.style = Paint.Style.FILL
        p.color = dark

        c.drawRoundRect(
            left, top, right, bottom,
            6f, 6f, p
        )

        val fillTop =
            bottom - (bottom - top) * (level / 100f)

        p.color = color

        c.drawRoundRect(
            left + 3f,
            fillTop,
            right - 3f,
            bottom - 3f,
            4f, 4f, p
        )

        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = edge

        c.drawRoundRect(
            left, top, right, bottom,
            6f, 6f, p
        )

        p.style = Paint.Style.FILL
    }
}
