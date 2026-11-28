package com.russoviller.cryogenic.live

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.russoviller.cryogenic.R

/**
 * Botão flutuante da live quando o app não está na tela (igual ao menu da live do TikTok):
 * toca no botão para abrir o chat e os controles (microfone, volumes, bloquear tela, opacidade, encerrar).
 * Precisa da permissão "Sobrepor a outros apps"; sem ela não aparece.
 */
class LiveFloatingWindow(
    private val context: Context,
    private val actions: Actions
) {
    interface Actions {
        fun setMic(on: Boolean)
        fun setMicVolume(value: Int)
        fun setInternalVolume(value: Int)
        fun setPrivacy(on: Boolean)
        fun stopLive()
        fun openApp()
        fun saveOpacity(value: Int)
    }

    private val wm = context.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var opacity = 85

    /** botão fechado: pílula compacta (foto, espectadores, comentários) igual ao da live do TikTok */
    private lateinit var bubble: LinearLayout
    private lateinit var avatarView: android.widget.ImageView
    private lateinit var viewersText: TextView
    private lateinit var commentsText: TextView
    private lateinit var lockText: TextView
    private var unread = 0
    /** encostado na borda (minimizado): -1 esquerda, 1 direita, 0 solto */
    private var docked = 0
    private var avatar: android.graphics.Bitmap? = null
    private lateinit var collapsedBox: LinearLayout
    private lateinit var preview: TextView
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hidePreview = Runnable {
        if (root != null) {
            preview.visibility = View.GONE
            relayout()
        }
    }
    /** canto de cima/esquerda do botão na tela (o painel e a prévia se ajustam a partir dele) */
    private var anchorX = 0
    private var anchorY = 0
    private lateinit var panel: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var chatText: TextView
    private lateinit var chatScroll: ScrollView
    private lateinit var chatStateText: TextView
    private lateinit var newMessages: TextView
    private lateinit var body: LinearLayout
    private lateinit var chatBox: FrameLayout
    private lateinit var controlsScroll: ScrollView
    /** acompanha as mensagens novas; desliga quando o usuário sobe para ler as anteriores */
    private var stickToBottom = true
    private var lastChatId = -1L
    /** chat recebido com o painel fechado: só monta o texto quando abrir */
    private var pendingChat: List<LiveChatMessage>? = null
    private lateinit var micButton: Button
    private lateinit var privacyButton: Button
    private lateinit var micSeek: SeekBar
    private lateinit var internalSeek: SeekBar

    private var state = State()

    data class State(
        val status: LiveStatus = LiveStatus.IDLE,
        val startedAt: Long = 0L,
        val mic: Boolean = true,
        val hasMic: Boolean = false,
        val micVolume: Int = 100,
        val internalVolume: Int = 100,
        val privacy: Boolean = false,
        val viewers: String = "",
        val chatState: String = "",
        /** linha de diagnóstico do chat (mostrada enquanto o chat não funciona) */
        val chatDetail: String = ""
    )

    val showing: Boolean get() = root != null

    @androidx.annotation.VisibleForTesting
    internal val rootView: View? get() = root

    fun canShow(): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

    private fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, context.resources.displayMetrics).toInt()

    @SuppressLint("ClickableViewAccessibility")
    fun show(initialOpacity: Int) {
        if (root != null || !canShow()) return
        opacity = initialOpacity.coerceIn(30, 100)
        val frame = FrameLayout(context)

        avatarView = android.widget.ImageView(context).apply {
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#33FFFFFF")) }
            setImageBitmap(roundAvatar(avatar))
            layoutParams = LinearLayout.LayoutParams(dp(26f), dp(26f))
        }
        fun counter(icon: Int): TextView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            text = "0"
            val d = context.getDrawable(icon)?.mutate()?.apply { setBounds(0, 0, dp(14f), dp(14f)) }
            setCompoundDrawables(d, null, null, null)
            compoundDrawablePadding = dp(4f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(9f)
            }
        }
        viewersText = counter(R.drawable.ic_live_viewer)
        commentsText = counter(R.drawable.ic_live_comment)
        lockText = TextView(context).apply {
            text = "🔒"
            textSize = 12f
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(8f)
            }
        }
        bubble = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5f), dp(5f), dp(11f), dp(5f))
            background = GradientDrawable().apply {
                cornerRadius = dp(18f).toFloat()
                setColor(Color.parseColor("#D9161A1F"))
            }
            addView(avatarView)
            addView(viewersText)
            addView(commentsText)
            addView(lockText)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36f))
        }

        // prévia do último comentário (some depois de ~8 s), igual ao botão da live do TikTok
        preview = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxWidth = dp(230f)
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            background = GradientDrawable().apply {
                cornerRadius = dp(14f).toFloat()
                setColor(Color.parseColor("#E0101820"))
            }
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6f)
            }
        }
        collapsedBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(bubble)
            addView(preview)
        }

        panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(18f).toFloat()
                setColor(Color.parseColor("#F0101820"))
                setStroke(dp(1f), Color.parseColor("#8869D2E7"))
            }
            layoutParams = FrameLayout.LayoutParams(dp(300f), FrameLayout.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
        }
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        statusText = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val collapse = smallButton(context.getString(R.string.cryo_live_bubble_minimize)) { setExpanded(false) }
        header.addView(statusText)
        header.addView(collapse)
        panel.addView(header)

        chatStateText = TextView(context).apply {
            setTextColor(Color.parseColor("#B3FFFFFF"))
            textSize = 11f
            visibility = View.GONE
            setPadding(0, dp(2f), 0, dp(2f))
        }
        panel.addView(chatStateText)

        chatText = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, dp(6f), dp(6f), dp(6f))
            // o texto cresceu (mensagem nova): desce junto se estava acompanhando o fim
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (stickToBottom) scrollChatToBottom()
            }
        }
        chatScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
            isScrollbarFadingEnabled = false
            addView(chatText)
            setOnScrollChangeListener { _, _, _, _, _ ->
                // rolou para cima: para de acompanhar; voltou ao fim: acompanha de novo
                stickToBottom = isChatAtBottom()
                if (stickToBottom) newMessages.visibility = View.GONE
            }
        }
        newMessages = TextView(context).apply {
            text = context.getString(R.string.cryo_live_chat_new)
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(Color.parseColor("#FE2C55"))
            }
            visibility = View.GONE
            setOnClickListener {
                stickToBottom = true
                visibility = View.GONE
                scrollChatToBottom()
            }
        }
        chatBox = FrameLayout(context).apply {
            addView(chatScroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(newMessages, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(4f)
            })
        }

        // controles numa coluna própria com rolagem (na horizontal o painel fica baixo)
        val controls = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        micButton = smallButton("") { actions.setMic(!state.mic) }
        privacyButton = smallButton("") { actions.setPrivacy(!state.privacy) }
        row1.addView(micButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4f) })
        row1.addView(privacyButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4f) })
        controls.addView(row1)

        micSeek = labeledSeek(controls, context.getString(R.string.cryo_live_mic_volume), 100) { actions.setMicVolume(it) }
        internalSeek = labeledSeek(controls, context.getString(R.string.cryo_live_internal_volume), 100) { actions.setInternalVolume(it) }
        labeledSeek(controls, context.getString(R.string.cryo_live_bubble_opacity), 70, opacity - 30) {
            opacity = it + 30
            applyOpacity()
            actions.saveOpacity(opacity)
        }

        val row2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(smallButton(context.getString(R.string.cryo_live_bubble_open_app)) { actions.openApp() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4f) })
        row2.addView(smallButton(context.getString(R.string.cryo_live_stop), Color.parseColor("#FE2C55")) { actions.stopLive() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4f) })
        // afastado das barras: evita encerrar a live sem querer ao mexer na opacidade
        controls.addView(row2, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16f); bottomMargin = dp(4f) })
        controlsScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
            addView(controls)
        }

        body = LinearLayout(context)
        body.addView(chatBox)
        body.addView(controlsScroll)
        panel.addView(body)
        configurePanel()

        frame.addView(panel)
        frame.addView(collapsedBox, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12f)
            y = dp(120f)
        }
        anchorX = lp.x
        anchorY = lp.y

        // arrastar o botão; toque simples abre o painel
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = anchorX; startY = anchorY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (moved || dx * dx + dy * dy > dp(6f) * dp(6f)) {
                        if (!moved && docked != 0) setDocked(0)
                        moved = true
                        // livre: pode ir para qualquer ponto da tela
                        anchorX = (startX + dx).toInt()
                        anchorY = (startY + dy).toInt()
                        relayout()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!moved) {
                        // toque: minimizado volta ao normal; normal abre o chat
                        if (docked != 0) setDocked(0) else setExpanded(true)
                    } else {
                        // soltou encostado na borda esquerda/direita: minimiza
                        val screenW = context.resources.displayMetrics.widthPixels
                        val edge = dp(24f)
                        when {
                            anchorX <= edge -> setDocked(-1)
                            anchorX + bubble.width >= screenW - edge -> setDocked(1)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        root = frame
        params = lp
        applyOpacity()
        runCatching { wm.addView(frame, lp) }.onFailure { root = null }
        render()
        relayout()
    }

    /**
     * Posiciona a janela a partir do botão: na metade direita da tela a prévia/painel crescem para a
     * esquerda, para não sair da tela.
     */
    private fun relayout() {
        val r = root ?: return
        val lp = params ?: return
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        bubble.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val bubbleW = bubble.measuredWidth.coerceAtLeast(dp(40f))
        anchorX = anchorX.coerceIn(0, (screenW - bubbleW).coerceAtLeast(0))
        anchorY = anchorY.coerceIn(0, (screenH - dp(40f)).coerceAtLeast(0))
        val rightSide = anchorX + bubbleW / 2 > screenW / 2
        if (expanded) {
            configurePanel()
            val panelW = panel.layoutParams.width
            panel.measure(
                View.MeasureSpec.makeMeasureSpec(panelW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            lp.x = (if (rightSide) anchorX + bubbleW - panelW else anchorX).coerceIn(0, (screenW - panelW).coerceAtLeast(0))
            // o painel inteiro cabe na tela (na horizontal ela é baixa)
            lp.y = anchorY.coerceAtMost(screenH - panel.measuredHeight - dp(8f)).coerceAtLeast(0)
        } else if (docked != 0) {
            // minimizado: só metade da foto aparece na borda
            val peek = dp(18f)
            lp.x = if (docked < 0) -(bubbleW - peek) else screenW - peek
            lp.y = anchorY
        } else {
            collapsedBox.gravity = if (rightSide) Gravity.END else Gravity.START
            var width = bubbleW
            if (preview.visibility == View.VISIBLE) {
                preview.measure(
                    View.MeasureSpec.makeMeasureSpec(dp(230f), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                width = maxOf(bubbleW, preview.measuredWidth)
            }
            lp.x = if (rightSide) anchorX + bubbleW - width else anchorX
            lp.y = anchorY
        }
        runCatching { wm.updateViewLayout(r, lp) }
    }

    /**
     * Tamanho do painel aberto conforme a tela: deitada = chat à esquerda e controles à direita
     * (cabe na altura); em pé = chat em cima e controles embaixo.
     */
    private fun configurePanel() {
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val landscape = screenW > screenH
        val panelLp = panel.layoutParams as FrameLayout.LayoutParams
        if (landscape) {
            val bodyH = (screenH - dp(72f)).coerceIn(dp(120f), dp(300f))
            panelLp.width = minOf(dp(580f), screenW - dp(16f))
            body.orientation = LinearLayout.HORIZONTAL
            chatBox.layoutParams = LinearLayout.LayoutParams(0, bodyH, 1f)
            controlsScroll.layoutParams = LinearLayout.LayoutParams(dp(240f), bodyH).apply { marginStart = dp(10f) }
        } else {
            panelLp.width = minOf(dp(320f), screenW - dp(16f))
            body.orientation = LinearLayout.VERTICAL
            chatBox.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220f))
            controlsScroll.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        panel.layoutParams = panelLp
    }

    private fun isChatAtBottom(): Boolean {
        if (chatScroll.height == 0) return true
        return chatScroll.scrollY + chatScroll.height >= chatText.height - dp(24f)
    }

    private fun scrollChatToBottom() {
        chatScroll.post { chatScroll.scrollTo(0, (chatText.height - chatScroll.height).coerceAtLeast(0)) }
    }

    /** Minimiza na borda (-1 esquerda / 1 direita) ou volta ao normal (0). */
    private fun setDocked(side: Int) {
        docked = side
        val full = side == 0
        viewersText.visibility = if (full) View.VISIBLE else View.GONE
        commentsText.visibility = if (full) View.VISIBLE else View.GONE
        lockText.visibility = if (full && state.privacy) View.VISIBLE else View.GONE
        bubble.setPadding(dp(5f), dp(5f), if (full) dp(11f) else dp(5f), dp(5f))
        if (!full) {
            handler.removeCallbacks(hidePreview)
            preview.visibility = View.GONE
        } else {
            // volta um pouco para dentro da tela
            val screenW = context.resources.displayMetrics.widthPixels
            anchorX = if (anchorX < screenW / 2) dp(8f) else screenW
        }
        relayout()
    }

    /** Novo comentário: com o botão fechado mostra a prévia por ~8 segundos. */
    fun showPreview(message: LiveChatMessage) {
        if (root == null || expanded) return
        unread++
        commentsText.text = formatCount(unread)
        if (docked != 0) return
        val sb = SpannableStringBuilder()
        if (message.user.isNotEmpty()) {
            sb.append(message.user)
            sb.setSpan(ForegroundColorSpan(Color.parseColor("#69D2E7")), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(StyleSpan(Typeface.BOLD), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("  ")
        }
        sb.append(message.text)
        preview.text = sb
        preview.visibility = View.VISIBLE
        relayout()
        handler.removeCallbacks(hidePreview)
        handler.postDelayed(hidePreview, PREVIEW_MS)
    }

    fun hide() {
        handler.removeCallbacks(hidePreview)
        root?.let { runCatching { wm.removeViewImmediate(it) } }
        root = null
        expanded = false
    }

    private fun setExpanded(value: Boolean) {
        expanded = value
        panel.visibility = if (value) View.VISIBLE else View.GONE
        collapsedBox.visibility = if (value) View.GONE else View.VISIBLE
        if (value) {
            handler.removeCallbacks(hidePreview)
            preview.visibility = View.GONE
            // comentários vistos: zera o contador da pílula
            unread = 0
            commentsText.text = "0"
        }
        relayout()
        if (value) {
            pendingChat?.let { pendingChat = null; updateChat(it) }
            stickToBottom = true
            newMessages.visibility = View.GONE
            scrollChatToBottom()
            render()
        }
    }

    /** A cada segundo: só o relógio da live, e só com o painel aberto. */
    fun tick() {
        if (root != null && expanded) render()
    }

    private fun applyOpacity() {
        root?.alpha = opacity / 100f
    }

    /** Foto do perfil do TikTok (vinda da página da live); null = ícone do app. */
    fun setAvatar(bitmap: android.graphics.Bitmap?) {
        avatar = bitmap
        if (root == null) return
        avatarView.setImageBitmap(roundAvatar(bitmap))
    }

    /** Recorta a foto (ou o ícone do app) em círculo: funciona mesmo sem aceleração de hardware. */
    private fun roundAvatar(src: android.graphics.Bitmap?): android.graphics.Bitmap {
        val size = dp(26f).coerceAtLeast(1)
        val base = src ?: android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_live_avatar_default)
            ?: android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val out = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        val side = minOf(base.width, base.height)
        val srcRect = android.graphics.Rect((base.width - side) / 2, (base.height - side) / 2, (base.width + side) / 2, (base.height + side) / 2)
        canvas.drawBitmap(base, srcRect, android.graphics.Rect(0, 0, size, size), paint)
        return out
    }

    fun update(newState: State) {
        state = newState
        render()
    }

    fun updateChat(messages: List<LiveChatMessage>) {
        if (root == null) return
        if (!expanded) {
            // fechado: não monta/mede 150 linhas a cada mensagem enquanto o jogo roda
            pendingChat = messages
            return
        }
        val sb = SpannableStringBuilder()
        for (m in messages) {
            if (sb.isNotEmpty()) sb.append('\n')
            if (m.user.isNotEmpty()) {
                val start = sb.length
                sb.append(m.user)
                sb.setSpan(ForegroundColorSpan(Color.parseColor("#69D2E7")), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("  ")
            }
            sb.append(m.text)
        }
        if (sb.isEmpty()) sb.append(context.getString(R.string.cryo_live_chat_empty))
        val newestId = messages.lastOrNull()?.id ?: -1L
        val grew = newestId != lastChatId
        lastChatId = newestId
        // lido lá em cima: não puxa para baixo, só avisa que chegaram novas
        if (expanded && !stickToBottom && grew && messages.isNotEmpty()) newMessages.visibility = View.VISIBLE
        if (messages.isEmpty()) {
            stickToBottom = true
            newMessages.visibility = View.GONE
        }
        chatText.text = sb
        if (stickToBottom) scrollChatToBottom()
    }

    private fun render() {
        if (root == null) return
        val s = state
        statusText.text = when (s.status) {
            LiveStatus.LIVE -> "● " + context.getString(R.string.cryo_live_status_live) + "  " + formatElapsed(s.startedAt)
            LiveStatus.CONNECTING -> context.getString(R.string.cryo_live_status_connecting)
            LiveStatus.RECONNECTING -> context.getString(R.string.cryo_live_status_reconnecting)
            LiveStatus.ERROR -> context.getString(R.string.cryo_live_status_error)
            LiveStatus.IDLE -> context.getString(R.string.cryo_live_status_idle)
        }
        val chatLine = when (s.chatState) {
            TikTokLiveChat.STATE_OK -> ""
            TikTokLiveChat.STATE_LOADING -> context.getString(R.string.cryo_live_chat_loading)
            TikTokLiveChat.STATE_WAITING -> context.getString(R.string.cryo_live_chat_waiting)
            TikTokLiveChat.STATE_NOT_FOUND -> context.getString(R.string.cryo_live_chat_not_found)
            TikTokLiveChat.STATE_BLOCKED -> context.getString(R.string.cryo_live_chat_blocked)
            TikTokLiveChat.STATE_LIMIT -> context.getString(R.string.cryo_live_chat_limit)
            TikTokLiveChat.STATE_ENDED -> context.getString(R.string.cryo_live_chat_ended)
            TikTokLiveChat.STATE_SUSPENDED -> context.getString(R.string.cryo_live_chat_suspended)
            else -> context.getString(R.string.cryo_live_chat_help)
        }
        chatStateText.text = if (chatLine.isNotEmpty() && s.chatDetail.isNotEmpty()) chatLine + "\n" + s.chatDetail else chatLine
        chatStateText.visibility = if (chatLine.isEmpty()) View.GONE else View.VISIBLE
        micButton.isEnabled = s.hasMic
        micButton.text = context.getString(if (s.mic && s.hasMic) R.string.cryo_live_mic_on else R.string.cryo_live_mic_off)
        privacyButton.text = context.getString(if (s.privacy) R.string.cryo_live_privacy_on_short else R.string.cryo_live_privacy_off_short)
        micSeek.isEnabled = s.hasMic
        if (!micSeek.isPressed) micSeek.progress = s.micVolume
        if (!internalSeek.isPressed) internalSeek.progress = s.internalVolume
        val oldViewers = viewersText.text.toString()
        viewersText.text = s.viewers.ifEmpty { "0" }
        lockText.visibility = if (s.privacy && docked == 0) View.VISIBLE else View.GONE
        if (oldViewers != viewersText.text.toString()) relayout()
        // status do painel aberto também mostra os espectadores
        if (s.viewers.isNotEmpty() && s.status == LiveStatus.LIVE) statusText.append("  ·  👁 ${s.viewers}")
    }

    private fun smallButton(label: String, color: Int = Color.parseColor("#3369D2E7"), onClick: () -> Unit): Button =
        Button(context).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            setTextColor(Color.WHITE)
            minHeight = dp(36f)
            minimumHeight = dp(36f)
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(color)
            }
            setOnClickListener { onClick() }
        }

    private fun labeledSeek(parent: LinearLayout, label: String, max: Int, initial: Int = max, onChange: (Int) -> Unit): SeekBar {
        parent.addView(TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 11f
            setPadding(0, dp(6f), 0, 0)
        })
        val seek = SeekBar(context).apply {
            this.max = max
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onChange(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        parent.addView(seek)
        return seek
    }

    companion object {
        private const val PREVIEW_MS = 8000L

        /** 1234 -> "1,2K" (igual ao contador do TikTok) */
        fun formatCount(n: Int): String = when {
            n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f).replace(Regex("[.,]0M$"), "M")
            n >= 1_000 -> "%.1fK".format(n / 1_000f).replace(Regex("[.,]0K$"), "K")
            else -> n.toString()
        }

        fun formatElapsed(startedAt: Long): String {
            if (startedAt <= 0L) return ""
            val s = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
            val h = s / 3600
            val m = (s % 3600) / 60
            val sec = s % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
        }
    }
}
