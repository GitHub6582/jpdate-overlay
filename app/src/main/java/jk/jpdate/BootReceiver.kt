package jk.jpdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** 端末起動後、設定がONならオーバーレイを自動開始 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (prefs.autoStart && prefs.enabled && Settings.canDrawOverlays(context)) {
            DateOverlayService.start(context)
        }
    }
}
