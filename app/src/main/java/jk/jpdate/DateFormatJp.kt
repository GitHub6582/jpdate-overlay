package jk.jpdate

import java.util.Calendar
import java.util.Locale

/** 日本語の年月日文字列を作る（端末のロケールに依存しない） */
object DateFormatJp {
    private val WEEKDAYS = arrayOf("日", "月", "火", "水", "木", "金", "土")

    fun format(showYear: Boolean, showWeekday: Boolean, now: Calendar = Calendar.getInstance()): String {
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        val d = now.get(Calendar.DAY_OF_MONTH)
        val w = WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1]
        val base = if (showYear) {
            String.format(Locale.JAPAN, "%d年%d月%d日", y, m, d)
        } else {
            String.format(Locale.JAPAN, "%d月%d日", m, d)
        }
        return if (showWeekday) "$base($w)" else base
    }
}
