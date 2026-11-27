package com.russoviller.cryogenic.cmod

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupMenu
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

    // ---------------------------------------------------------------- API 2

    private fun rounded(view: View, radiusDp: Float) {
        val r = dp(radiusDp).toFloat()
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) = outline.setRoundRect(0, 0, v.width, v.height, r)
        }
        view.clipToOutline = true
    }

    override fun image(bitmap: Bitmap?, widthDp: Float, heightDp: Float): ImageView = ImageView(context).apply {
        setImageBitmap(bitmap)
        scaleType = ImageView.ScaleType.CENTER_CROP
        adjustViewBounds = true
        layoutParams = LinearLayout.LayoutParams(
            if (widthDp > 0) dp(widthDp) else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (heightDp > 0) dp(heightDp) else ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rounded(this, 14f)
    }

    override fun chip(text: CharSequence, selected: Boolean, onClick: View.OnClickListener?): TextView = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (selected) getBackgroundColor() else getTextColor())
        setPadding(dp(12f), dp(5f), dp(12f), dp(5f))
        val bg = if (selected) gradientBackground(14f) else GradientDrawable().apply {
            cornerRadius = dp(14f).toFloat()
            setColor(alpha(getAccentColor(), 0.12f))
            setStroke(dp(1f), alpha(getAccentColor(), 0.55f))
        }
        background = if (onClick != null) ripple(bg, 14f) else bg
        if (onClick != null) {
            isClickable = true
            setOnClickListener { v -> guard { onClick.onClick(v) } }
        }
    }

    override fun checkbox(text: CharSequence, checked: Boolean, onChange: CompoundButton.OnCheckedChangeListener?): CompoundButton =
        CheckBox(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(getTextColor())
            isChecked = checked
            buttonTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(getAccentColor(), alpha(CryoGlassColors.Frost.toArgb(), 0.7f))
            )
            minHeight = dp(40f)
            setOnCheckedChangeListener(onChange?.let { l -> CompoundButton.OnCheckedChangeListener { b, c -> guard { l.onCheckedChanged(b, c) } } })
        }

    override fun choice(options: Array<String>, selected: Int, onChoice: CryoModUi.ChoiceListener?): TextView {
        var current = selected.coerceIn(0, (options.size - 1).coerceAtLeast(0))
        return baseButton(options.getOrElse(current) { "" } + "  ▾", null, primary = false).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setTypeface(typeface, Typeface.NORMAL)
            setOnClickListener { anchor ->
                val menu = PopupMenu(context, anchor)
                options.forEachIndexed { i, o -> menu.menu.add(0, i, i, o) }
                menu.setOnMenuItemClickListener { item ->
                    current = item.itemId
                    text = options[current] + "  ▾"
                    if (onChoice != null) guard { onChoice.onChoice(current, options[current]) }
                    true
                }
                menu.show()
            }
        }
    }

    override fun listItem(icon: Bitmap?, title: CharSequence, subtitle: CharSequence?, onClick: View.OnClickListener?): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = dp(10f)
            setPadding(p, p, p, p)
            val bg = glassBackground(16f)
            background = if (onClick != null) ripple(bg, 16f) else bg
            if (icon != null) {
                addView(image(icon, 40f, 40f).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(12f) })
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(textView(title, 15f, getTextColor(), bold = true))
                if (!subtitle.isNullOrEmpty()) addView(caption(subtitle))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (onClick != null) {
                isClickable = true
                setOnClickListener { v -> guard { onClick.onClick(v) } }
            }
        }

    override fun grid(columns: Int): GridLayout = GridLayout(context).apply {
        columnCount = columns.coerceIn(1, 12)
        useDefaultMargins = true
        alignmentMode = GridLayout.ALIGN_BOUNDS
    }

    override fun dialog(title: CharSequence?, content: View): Runnable {
        val d = Dialog(context)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = card().apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(24f).toFloat()
                setColor(getBackgroundColor())
                setStroke(dp(1f), alpha(getAccentColor(), 0.6f))
            }
            if (!title.isNullOrEmpty()) addView(this@CmodUiKit.title(title))
            (content.parent as? ViewGroup)?.removeView(content)
            addView(content)
        }
        d.setContentView(FrameLayout(context).apply {
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            addView(box, FrameLayout.LayoutParams(dp(420f).coerceAtMost(context.resources.displayMetrics.widthPixels - dp(32f)), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        })
        d.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        d.show()
        return Runnable { runCatching { d.dismiss() } }
    }

    override fun confirm(title: CharSequence?, message: CharSequence?, onResult: CryoModUi.ConfirmListener?) {
        var answered = false
        lateinit var close: Runnable
        fun answer(ok: Boolean) {
            if (answered) return
            answered = true
            close.run()
            if (onResult != null) guard { onResult.onResult(ok) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            dividerDrawable = spacer(10f, vertical = true)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            if (!message.isNullOrEmpty()) addView(text(message))
            addView(row().apply {
                addView(secondaryButton(context.getString(android.R.string.cancel)) { answer(false) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(button(context.getString(android.R.string.ok)) { answer(true) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            })
        }
        close = dialog(title, col)
    }

    override fun prompt(title: CharSequence?, hint: CharSequence?, initial: CharSequence?, onResult: CryoModUi.TextListener?) {
        var answered = false
        lateinit var close: Runnable
        val field = input(hint).apply { setText(initial) }
        fun answer(text: String?) {
            if (answered) return
            answered = true
            close.run()
            if (onResult != null) guard { onResult.onText(text) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            dividerDrawable = spacer(10f, vertical = true)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            addView(field)
            addView(row().apply {
                addView(secondaryButton(context.getString(android.R.string.cancel)) { answer(null) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(button(context.getString(android.R.string.ok)) { answer(field.text.toString()) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            })
        }
        close = dialog(title, col)
    }
}
