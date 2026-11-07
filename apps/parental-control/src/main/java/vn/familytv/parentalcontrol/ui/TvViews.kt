package vn.familytv.parentalcontrol.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import vn.familytv.parentalcontrol.R

class TvViews(private val context: Context) {
    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun column(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    fun text(value: CharSequence, size: Float = 18f, color: Int = R.color.text_secondary): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(context.getColor(color))
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    fun title(value: CharSequence): TextView = text(value, 32f, R.color.text_primary).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isAccessibilityHeading = true
        setPadding(0, 0, 0, dp(12))
    }

    fun body(parent: LinearLayout, value: CharSequence) {
        parent.addView(text(value).apply { setPadding(0, 0, 0, dp(18)) })
    }

    fun button(
        parent: LinearLayout,
        label: CharSequence,
        viewId: Int,
        enabled: Boolean = true,
        action: () -> Unit,
    ): Button = Button(context).apply {
        id = viewId
        text = label
        textSize = 18f
        isAllCaps = false
        isEnabled = enabled
        gravity = Gravity.CENTER
        minimumHeight = dp(56)
        setPadding(dp(18), dp(12), dp(18), dp(12))
        background = context.getDrawable(R.drawable.tv_button)
        setTextColor(ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(),
            ),
            intArrayOf(
                context.getColor(R.color.text_disabled),
                context.getColor(R.color.focus_text),
                context.getColor(R.color.text_primary),
            ),
        ))
        setOnClickListener { action() }
        parent.addView(this, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(10) })
    }

    fun status(parent: LinearLayout, label: CharSequence, value: CharSequence, complete: Boolean) {
        val card = column().apply {
            background = context.getDrawable(R.drawable.status_card)
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        card.addView(text(label, 14f))
        card.addView(text(value, 20f, if (complete) R.color.success else R.color.warning))
        parent.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(10) })
    }

    fun focusLater(view: View) {
        view.post { view.requestFocus() }
    }
}
