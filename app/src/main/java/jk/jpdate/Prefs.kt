package jk.jpdate

import android.content.Context
import android.content.SharedPreferences

/** 表示位置・見た目の保存 */
class Prefs(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.getSharedPreferences("jpdate", Context.MODE_PRIVATE)

    /** オーバーレイが有効か */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** 画面上の X 座標（px）。未設定は -1 */
    var x: Int
        get() = sp.getInt("x", -1)
        set(v) = sp.edit().putInt("x", v).apply()

    /** 画面上の Y 座標（px）。未設定は -1 */
    var y: Int
        get() = sp.getInt("y", -1)
        set(v) = sp.edit().putInt("y", v).apply()

    /** 文字サイズ（sp） */
    var textSp: Float
        get() = sp.getFloat("text_sp", 14f)
        set(v) = sp.edit().putFloat("text_sp", v.coerceIn(10f, 28f)).apply()

    /** 不透明度 0.2〜1.0 */
    var alpha: Float
        get() = sp.getFloat("alpha", 0.85f)
        set(v) = sp.edit().putFloat("alpha", v.coerceIn(0.2f, 1f)).apply()

    /** 背景の濃さ 0〜1（0=ほぼ透明） */
    var bgAlpha: Float
        get() = sp.getFloat("bg_alpha", 0.35f)
        set(v) = sp.edit().putFloat("bg_alpha", v.coerceIn(0f, 0.8f)).apply()

    /** 年を出すか */
    var showYear: Boolean
        get() = sp.getBoolean("show_year", true)
        set(v) = sp.edit().putBoolean("show_year", v).apply()

    /** 曜日を出すか */
    var showWeekday: Boolean
        get() = sp.getBoolean("show_weekday", true)
        set(v) = sp.edit().putBoolean("show_weekday", v).apply()

    /** 起動時（再起動後）に自動で出すか */
    var autoStart: Boolean
        get() = sp.getBoolean("auto_start", true)
        set(v) = sp.edit().putBoolean("auto_start", v).apply()
}
