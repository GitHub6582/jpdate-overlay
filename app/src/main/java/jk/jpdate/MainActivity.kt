package jk.jpdate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 設定画面。
 * - オーバーレイ権限
 * - 表示ON/OFF
 * - 文字サイズ・不透明度・背景の濃さ
 * - 年・曜日の表示
 * - 起動時に自動開始
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var enableSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(0xFF121212.toInt())
        }

        root.addView(TextView(this).apply {
            text = "日本語日付"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "画面の好きな位置に、日本語の年月日を常時表示します。\n表示中の日付をドラッグすると位置を変えられます。"
            textSize = 13f
            setTextColor(0xFFB0B0B0.toInt())
            setPadding(0, 0, 0, dp(16))
        })

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFE0E0E0.toInt())
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(status)

        // 権限ボタン
        root.addView(Button(this).apply {
            text = "他のアプリの上に表示する許可"
            setOnClickListener { openOverlaySettings() }
        })

        if (Build.VERSION.SDK_INT >= 33) {
            root.addView(Button(this).apply {
                text = "通知の許可（常駐に必要）"
                setOnClickListener {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1
                    )
                }
            })
        }

        // ON/OFF
        enableSwitch = Switch(this).apply {
            text = "日付を表示する"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.enabled
            setPadding(0, dp(12), 0, dp(8))
            setOnCheckedChangeListener { _, on ->
                if (on) {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        isChecked = false
                        Toast.makeText(this@MainActivity, "先に「他のアプリの上に表示」を許可してください", Toast.LENGTH_LONG).show()
                        openOverlaySettings()
                        return@setOnCheckedChangeListener
                    }
                    prefs.enabled = true
                    DateOverlayService.start(this@MainActivity)
                } else {
                    prefs.enabled = false
                    DateOverlayService.stop(this@MainActivity)
                }
                refreshStatus()
            }
        }
        root.addView(enableSwitch)

        // 自動開始
        root.addView(Switch(this).apply {
            text = "端末の起動後に自動で表示"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.autoStart
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, on -> prefs.autoStart = on }
        })

        // 年・曜日
        root.addView(Switch(this).apply {
            text = "年を表示（2026年…）"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.showYear
            setPadding(0, dp(8), 0, dp(4))
            setOnCheckedChangeListener { _, on ->
                prefs.showYear = on
                DateOverlayService.refresh(this@MainActivity)
            }
        })
        root.addView(Switch(this).apply {
            text = "曜日を表示（水）"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.showWeekday
            setPadding(0, dp(4), 0, dp(12))
            setOnCheckedChangeListener { _, on ->
                prefs.showWeekday = on
                DateOverlayService.refresh(this@MainActivity)
            }
        })

        // 文字サイズ
        val sizeLabel = TextView(this).apply {
            text = "文字サイズ: ${prefs.textSp.toInt()}sp"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 13f
        }
        root.addView(sizeLabel)
        root.addView(SeekBar(this).apply {
            max = 18 // 10〜28
            progress = (prefs.textSp - 10f).toInt().coerceIn(0, 18)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val sp = 10f + p
                    sizeLabel.text = "文字サイズ: ${sp.toInt()}sp"
                    if (fromUser) {
                        prefs.textSp = sp
                        DateOverlayService.refresh(this@MainActivity)
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        // 文字の不透明度
        val alphaLabel = TextView(this).apply {
            text = "文字の濃さ: ${(prefs.alpha * 100).toInt()}%"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(alphaLabel)
        root.addView(SeekBar(this).apply {
            max = 80 // 20〜100
            progress = ((prefs.alpha - 0.2f) * 100f).toInt().coerceIn(0, 80)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val a = 0.2f + p / 100f
                    alphaLabel.text = "文字の濃さ: ${(a * 100).toInt()}%"
                    if (fromUser) {
                        prefs.alpha = a
                        DateOverlayService.refresh(this@MainActivity)
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        // 背景の濃さ
        val bgLabel = TextView(this).apply {
            text = "背景の濃さ: ${(prefs.bgAlpha * 100).toInt()}%"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(bgLabel)
        root.addView(SeekBar(this).apply {
            max = 80 // 0〜80
            progress = (prefs.bgAlpha * 100f).toInt().coerceIn(0, 80)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val a = p / 100f
                    bgLabel.text = "背景の濃さ: ${(a * 100).toInt()}%"
                    if (fromUser) {
                        prefs.bgAlpha = a
                        DateOverlayService.refresh(this@MainActivity)
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        root.addView(TextView(this).apply {
            text = "\n表示中の日付を指でドラッグすると、好きな位置に置けます。位置は次回起動時にも残ります。"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
        })

        setContentView(root)
        maybeRequestNotif()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        enableSwitch.isChecked = prefs.enabled && Settings.canDrawOverlays(this)
        refreshStatus()
    }

    private fun refreshStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val running = prefs.enabled && overlay
        status.text = buildString {
            append(if (overlay) "✓ オーバーレイ許可: OK" else "✗ オーバーレイ許可: 未許可")
            append("\n")
            append(if (running) "✓ 表示中" else "— 非表示")
            append("\nプレビュー: ")
            append(DateFormatJp.format(prefs.showYear, prefs.showWeekday))
        }
    }

    private fun openOverlaySettings() {
        val i = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(i)
    }

    private fun maybeRequestNotif() {
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!ok) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()
}
