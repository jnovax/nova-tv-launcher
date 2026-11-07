package vn.familytv.parentalcontrol.ui.auth

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import vn.familytv.parentalcontrol.R

class DpadPinPadView(
    context: Context,
    private val onPinComplete: (String) -> Unit
) : LinearLayout(context) {
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()

    private val pinBuilder = StringBuilder()
    private val slotViews = ArrayList<TextView>()

    private val slotsRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, dp(10))
    }

    private val grid = GridLayout(context).apply {
        columnCount = 3
        rowCount = 4
        alignmentMode = GridLayout.ALIGN_BOUNDS
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        for (i in 0 until 6) {
            val slot = TextView(context).apply {
                text = ""
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = createSlotBackground(false)
                val lp = LayoutParams(dp(40), dp(44)).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                layoutParams = lp
            }
            slotViews.add(slot)
            slotsRow.addView(slot)
        }
        addView(slotsRow, LayoutParams(dp(280), LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        val buttons = listOf(
            "1" to R.id.pin_btn_1, "2" to R.id.pin_btn_2, "3" to R.id.pin_btn_3,
            "4" to R.id.pin_btn_4, "5" to R.id.pin_btn_5, "6" to R.id.pin_btn_6,
            "7" to R.id.pin_btn_7, "8" to R.id.pin_btn_8, "9" to R.id.pin_btn_9,
            "⌫" to R.id.pin_btn_del, "0" to R.id.pin_btn_0, "✔" to R.id.pin_btn_ok
        )
        for ((btnText, idRes) in buttons) {
            val btn = Button(context).apply {
                id = idRes
                text = btnText
                textSize = 18f
                isAllCaps = false
                gravity = Gravity.CENTER
                background = context.getDrawable(R.drawable.tv_button)
                setTextColor(
                    ColorStateList(
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
                    )
                )
                setOnClickListener {
                    when (btnText) {
                        "⌫" -> deleteDigit()
                        "✔" -> if (pinBuilder.length == 6) onPinComplete(pinBuilder.toString())
                        else -> appendDigit(btnText[0])
                    }
                }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = dp(88)
                height = dp(44)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            btn.layoutParams = lp
            grid.addView(btn)
        }
        addView(grid, LayoutParams(dp(280), LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
    }

    private fun createSlotBackground(filled: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            if (filled) {
                setColor(Color.parseColor("#1D4ED8"))
                setStroke(dp(2), Color.parseColor("#60A5FA"))
            } else {
                setColor(Color.parseColor("#1E293B"))
                setStroke(dp(1), Color.parseColor("#475569"))
            }
        }
    }

    fun appendDigit(digit: Char) {
        if (pinBuilder.length < 6 && digit.isDigit()) {
            pinBuilder.append(digit)
            updateSlots()
            if (pinBuilder.length == 6) {
                onPinComplete(pinBuilder.toString())
            }
        }
    }

    fun deleteDigit() {
        if (pinBuilder.isNotEmpty()) {
            pinBuilder.deleteCharAt(pinBuilder.length - 1)
            updateSlots()
        }
    }

    fun clear() {
        pinBuilder.clear()
        updateSlots()
    }

    fun currentPin(): String = pinBuilder.toString()

    val firstButton: View
        get() = grid.getChildAt(0)

    private fun updateSlots() {
        for (i in 0 until 6) {
            if (i < pinBuilder.length) {
                slotViews[i].text = pinBuilder[i].toString()
                slotViews[i].background = createSlotBackground(true)
            } else {
                slotViews[i].text = ""
                slotViews[i].background = createSlotBackground(false)
            }
        }
    }

    fun setKeypadEnabled(enabled: Boolean) {
        for (i in 0 until grid.childCount) {
            grid.getChildAt(i).isEnabled = enabled
        }
    }
}
