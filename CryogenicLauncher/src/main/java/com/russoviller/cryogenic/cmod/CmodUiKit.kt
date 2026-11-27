package com.russoviller.cryogenic.cmod

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.russoviller.cryogenic.cmod.api.CryoModUi
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTheme

/** Peças de interface (Views) no estilo do Launcher para os Mods do Launcher. */
internal class CmodUiKit(
    private val context: Context,
    /** Roda os listeners do mod protegidos (erro do mod não fecha o launcher). */
    private val guard: (() -> Unit) -> Unit = { it() }
) : CryoModUi {

    private fun alpha(color: Int, a: Float): Int = Color.argb((a * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    override fun getAccentColor(): Int = CryoGlassColors.Ice.toArgb()
    override fun getAccentSoftColor(): Int = CryoGlassColors.IceSoft.toArgb()
    override fun getAuroraColor(): Int = CryoGlassColors.Aurora.toArgb()
    override fun getBackgroundColor(): Int = CryoGlassColors.Deep.toArgb()
    override fun getSurfaceColor(): Int = alpha(CryoGlassColors.Navy.toArgb(), 0.72f)
    override fun getTextColor(): Int = Color.WHITE
    override fun getSecondaryTextColor(): Int = alpha(CryoGlassColors.Frost.toArgb(), 0.82f)
    override fun getGradientColors(): IntArray =
        (CryoTheme.spec.gradient ?: listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora))
            .map { it.toArgb() }.toIntArray()

    override fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()

    override fun glassBackground(radiusDp: Float): Drawable = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(getSurfaceColor())
        setStroke(dp(1f), alpha(getAccentColor(), 0.45f))
    }

    override fun gradientBackground(radiusDp: Float): Drawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, getGradientColors().let { if (it.size < 2) intArrayOf(it.first(), it.first()) else it }).apply {
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1f), alpha(Color.WHITE, 0.35f))
        }

    private fun ripple(content: Drawable, radiusDp: Float): Drawable {
        val mask = GradientDrawable().apply { cornerRadius = dp(radiusDp).toFloat(); setColor(Color.WHITE) }
        return RippleDrawable(ColorStateList.valueOf(alpha(Color.WHITE, 0.25f)), content, mask)
    }

    private fun spacer(gapDp: Float, vertical: Boolean): Drawable = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        if (vertical) setSize(1, dp(gapDp)) else setSize(dp(gapDp), 1)
    }

    override fun column(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        dividerDrawable = spacer(8f, vertical = true)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        val p = dp(12f)
        setPadding(p, p, p, p)
    }

    override fun row(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        dividerDrawable = spacer(8f, vertical = false)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
    }

    override fun card(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        dividerDrawable = spacer(8f, vertical = true)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        background = glassBackground(18f)
        val p = dp(14f)
        setPadding(p, p, p, p)
    }

    override fun scroll(content: View): ScrollView = ScrollView(context).apply {
        isFillViewport = true
        isVerticalScrollBarEnabled = true
        (content.parent as? ViewGroup)?.removeView(content)
        addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun space(size: Float): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

    override fun divider(): View = View(context).apply {
        setBackgroundColor(alpha(getAccentColor(), 0.25f))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f).coerceAtLeast(1))
    }

    private fun textView(text: CharSequence, sp: Float, color: Int, bold: Boolean) = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    override fun title(text: CharSequence): TextView = textView(text, 20f, getTextColor(), bold = true)
    override fun text(text: CharSequence): TextView = textView(text, 14f, getSecondaryTextColor(), bold = false)
    override fun caption(text: CharSequence): TextView = textView(text, 12f, alpha(CryoGlassColors.Frost.toArgb(), 0.65f), bold = false)

    private fun baseButton(text: CharSequence, onClick: View.OnClickListener?, primary: Boolean) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (primary) getBackgroundColor() else getTextColor())
        minHeight = dp(46f)
        setPadding(dp(18f), dp(10f), dp(18f), dp(10f))
        background = ripple(if (primary) gradientBackground(23f) else glassBackground(23f), 23f)
        isClickable = true
        isFocusable = true
        setOnClickListener(onClick?.let { l -> View.OnClickListener { v -> guard { l.onClick(v) } } })
    }

    override fun button(text: CharSequence, onClick: View.OnClickListener?): TextView = baseButton(text, onClick, primary = true)
    override fun secondaryButton(text: CharSequence, onClick: View.OnClickListener?): TextView = baseButton(text, onClick, primary = false)

    override fun input(hint: CharSequence?): EditText = EditText(context).apply {
        this.hint = hint
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_TEXT
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(getTextColor())
        setHintTextColor(alpha(CryoGlassColors.Frost.toArgb(), 0.5f))
        background = glassBackground(23f)
        setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
        minHeight = dp(44f)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun switchButton(text: CharSequence, checked: Boolean, onChange: CompoundButton.OnCheckedChangeListener?): CompoundButton =
        android.widget.Switch(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(getTextColor())
            isChecked = checked
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(getAccentColor(), alpha(CryoGlassColors.Frost.toArgb(), 0.9f)))
            trackTintList = ColorStateList(states, intArrayOf(alpha(getAccentColor(), 0.55f), alpha(CryoGlassColors.Frost.toArgb(), 0.3f)))
            minHeight = dp(44f)
            setOnCheckedChangeListener(onChange?.let { l -> CompoundButton.OnCheckedChangeListener { b, c -> guard { l.onCheckedChanged(b, c) } } })
        }

    override fun slider(max: Int, value: Int, onChange: CryoModUi.ValueListener?): SeekBar = SeekBar(context).apply {
        this.max = max.coerceAtLeast(1)
        progress = value.coerceIn(0, this.max)
        progressTintList = ColorStateList.valueOf(getAccentColor())
        thumbTintList = ColorStateList.valueOf(getAccentColor())
        progressBackgroundTintList = ColorStateList.valueOf(alpha(CryoGlassColors.Frost.toArgb(), 0.35f))
        minimumHeight = dp(36f)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && onChange != null) guard { onChange.onValueChanged(progress) }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun progress(indeterminate: Boolean): ProgressBar =
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = indeterminate
            max = 100
            progressTintList = ColorStateList.valueOf(getAccentColor())
            indeterminateTintList = ColorStateList.valueOf(getAccentColor())
            progressBackgroundTintList = ColorStateList.valueOf(alpha(CryoGlassColors.Frost.toArgb(), 0.3f))
        }
}
