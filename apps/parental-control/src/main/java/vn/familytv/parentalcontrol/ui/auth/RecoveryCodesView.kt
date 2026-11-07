package vn.familytv.parentalcontrol.ui.auth

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

class RecoveryCodesView(context: Context) : LinearLayout(context) {
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()

    private val grid = GridLayout(context).apply {
        columnCount = 2
        rowCount = 3
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        addView(grid)
    }

    fun setCodes(codes: List<String>) {
        grid.removeAllViews()
        for (i in codes.indices) {
            val code = codes[i]
            val formatted = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
            val tv = TextView(context).apply {
                text = "${i + 1}.  $formatted"
                textSize = 22f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#60A5FA"))
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(dp(16), dp(10), dp(16), dp(10))
                val lp = GridLayout.LayoutParams().apply {
                    setMargins(dp(8), dp(6), dp(8), dp(6))
                }
                layoutParams = lp
            }
            grid.addView(tv)
        }
    }
}
