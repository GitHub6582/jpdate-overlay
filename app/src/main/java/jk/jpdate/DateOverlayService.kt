package jk.jpdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.math.abs

/**
 * 画面上に日本語の年月日を常時表示するフォアグラウンドサービス。
 * ドラッグで任意の位置へ移動でき、位置は保存される。
 */
class DateOverlayService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var wm: WindowManager
    private var label: TextView? = null
    private var lp: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastDay = -1

    private val tick = object : Runnable {
        override fun run() {
            refreshText()
            // 日付が変わった直後にすぐ更新できるよう、次の分境界付近でも動く
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) {
            attachOverlay()
            handler.post(tick)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                prefs.enabled = false
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                applyAppearance()
                refreshText()
            }
            else -> {
                prefs.enabled = true
                if (label == null && Settings.canDrawOverlays(this)) {
                    attachOverlay()
                    handler.post(tick)
                } else {
                    applyAppearance()
                    refreshText()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        detachOverlay()
        super.onDestroy()
    }

    private fun attachOverlay() {
        if (label != null) return

        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
            isClickable = true
            isFocusable = false
        }
        applyBg(tv)
        label = tv

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val dm = resources.displayMetrics
            x = if (prefs.x >= 0) prefs.x else (dm.widthPixels * 0.05f).toInt()
            y = if (prefs.y >= 0) prefs.y else (dm.heightPixels * 0.02f).toInt()
        }
        lp = params

        setupDrag(tv, params)
        applyAppearance()
        refreshText()

        try {
            wm.addView(tv, params)
        } catch (_: Exception) {
            label = null
            lp = null
        }
    }

    private fun detachOverlay() {
        label?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        label = null
        lp = null
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        view.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - downX).toInt()
                    val dy = (ev.rawY - downY).toInt()
                    if (!moved && (abs(dx) > 6 || abs(dy) > 6)) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        prefs.x = params.x
                        prefs.y = params.y
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun applyAppearance() {
        val tv = label ?: return
        tv.textSize = prefs.textSp
        tv.alpha = prefs.alpha
        applyBg(tv)
    }

    private fun applyBg(tv: TextView) {
        val a = (prefs.bgAlpha * 255).toInt().coerceIn(0, 200)
        val bg = GradientDrawable().apply {
            setColor(Color.argb(a, 20, 20, 20))
            cornerRadius = dp(8).toFloat()
        }
        tv.background = bg
    }

    private fun refreshText() {
        val tv = label ?: return
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR) * 1000
        val text = DateFormatJp.format(prefs.showYear, prefs.showWeekday, cal)
        if (tv.text?.toString() != text) {
            tv.text = text
        }
        lastDay = day
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val chId = "jpdate_overlay"
        if (nm.getNotificationChannel(chId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    chId,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
            )
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, DateOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, chId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(open)
            .addAction(0, "停止", stop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        const val NOTIF_ID = 42
        const val ACTION_STOP = "jk.jpdate.STOP"
        const val ACTION_REFRESH = "jk.jpdate.REFRESH"

        fun start(ctx: Context) {
            val i = Intent(ctx, DateOverlayService::class.java)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            val i = Intent(ctx, DateOverlayService::class.java).setAction(ACTION_STOP)
            ctx.startService(i)
        }

        fun refresh(ctx: Context) {
            val i = Intent(ctx, DateOverlayService::class.java).setAction(ACTION_REFRESH)
            ctx.startService(i)
        }
    }
}
