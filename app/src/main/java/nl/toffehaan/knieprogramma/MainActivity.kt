package nl.toffehaan.knieprogramma

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val BG = 0xFF0F172A.toInt()
private const val CARD = 0xFF1E293B.toInt()
private const val ACCENT = 0xFF38BDF8.toInt()
private const val WHITE = 0xFFF8FAFC.toInt()
private const val MUTED = 0xFFCBD5E1.toInt()
private const val GREEN = 0xFF4ADE80.toInt()
private const val RED = 0xFFEF4444.toInt()

private data class Exercise(
    val name: String,
    val dose: String,
    val videoId: String,
    val hint: String = "",
    val altText: String? = null,
    val altVideoId: String? = null
)

private data class Phase(
    val number: Int,
    val title: String,
    val badge: String,
    val description: String,
    val exercises: List<Exercise>
)

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var root: FrameLayout
    private var currentPhase = 0
    private var exerciseIndex = 0
    private var showingAlternative = false
    private var videoView: WebView? = null
    private var videoFailed = false
    private var downX = 0f
    private var downY = 0f

    private val phases = listOf(
        Phase(1, "Kalmeren & activeren", "dagelijks · ±15 min", "Isometrisch werk en basis-activatie. Elke dag, ook op loop dagen ('s avonds prima).", listOf(
            Exercise("Quadriceps-setting", "3 × 10 (5 sec vasthouden)", "5TUK4uT2nnw"),
            Exercise("Spanish squat", "5 × 30 sec", "3igyh6eqGvc", altText = "Geen elastiek? Doe de wall sit: knieën 60–70°, rug tegen de muur, 5 × 30 sec.", altVideoId = "JaZNYM3zAP0"),
            Exercise("Glute bridge", "3 × 10", "zN391XBTbhQ"),
            Exercise("Clamshell", "2 × 10 per zijde", "zN391XBTbhQ"),
            Exercise("Rechte-beenheffing", "3 × 10", "IOJZM3m1sfQ")
        )),
        Phase(2, "Versterken", "3× per week · ±20–25 min", "Kracht rond knie, heup en romp. Op rust dagen of ná een loopje — nooit vlak vóór een loop.", listOf(
            Exercise("Step-down", "3 × 8 per been (langzaam omlaag!)", "rvI7OxBQqS4"),
            Exercise("Eénbeen opstaan van stoel", "3 × 8 per been", "bfLZAmA4RiI"),
            Exercise("Monster walks", "3 × 10 per richting", "XtRc8BO-IZ0", altText = "Geen elastiek? Zijliggende beenheffing 3 × 10 per zijde.", altVideoId = "UmmBtOG2N_s"),
            Exercise("Copenhagen (adductoren)", "2 × 8 (korte hefhoogte)", "nSvwoF1QGi0"),
            Exercise("Eénbeen-kuitvering", "3 × 12", "IphGZ8OlfYg"),
            Exercise("Side plank met beenlift", "2 × 8 per zijde", "Lf_O3UQI8r0")
        )),
        Phase(3, "Veerkracht", "2–3× per week · klein blok", "Alleen starten als de knie 2+ weken volledig stil is. Piekbelasting leren opvangen.", listOf(
            Exercise("Pogo hops (twee benen)", "3 × 20 (later: één been)", "lz6BM6WyJ0k"),
            Exercise("Sprongetjes vooruit, zachte landing", "3 × 10", "sGGegRzf1AE")
        )),
        Phase(4, "Oefeningen Fysio", "volgens schema van je fysiotherapeut", "Aanvullende oefeningen vanuit de fysiotherapie.", listOf(
            Exercise("Single leg bridge", "3 × 10", "AVAXhy6pl7o"),
            Exercise("Banded clam shell", "3 × 10 per kant", "Hj-2r7OlXuE"),
            Exercise("Side lying straight leg hip abduction", "3 × 10 per kant", "nwvkWrn2hRg", hint = "Eventueel uitvoeren met een weerstandsband boven de knieën.")
        ))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.navigationBarColor = BG
        window.statusBarColor = BG
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        prefs = getSharedPreferences("knieprogramma", MODE_PRIVATE)
        root = FrameLayout(this).apply { setBackgroundColor(BG) }
        setContentView(root)
        showHome()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun text(value: String, size: Float, color: Int = WHITE, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun button(label: String, color: Int, textColor: Int = WHITE, action: () -> Unit): Button = Button(this).apply {
        text = label; setTextColor(textColor); textSize = 15f; isAllCaps = false
        background = rounded(color, 14)
        minHeight = dp(52); setPadding(dp(16), dp(8), dp(16), dp(8))
        setOnClickListener { action() }
    }

    private fun column(padding: Int = 20): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
    }

    private fun addGap(parent: LinearLayout, height: Int = 12) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun showHome() {
        stopVideo()
        root.removeAllViews()
        val scroll = ScrollView(this)
        val content = column(20)
        content.addView(text("🦵 Knieprogramma", 29f, WHITE, true))
        addGap(content, 4)
        content.addView(text("Jouw oefeningen, stap voor stap", 15f, MUTED))
        addGap(content, 22)

        phases.forEachIndexed { index, phase ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(17), dp(18), dp(17))
                background = rounded(CARD, 18)
                isClickable = true
                isFocusable = true
                setOnClickListener { startPhase(index) }
            }
            card.addView(text(if (phase.number == 4) "EXTRA PROGRAMMA" else "FASE ${phase.number}", 12f, ACCENT, true))
            addGap(card, 5)
            card.addView(text(phase.title, 21f, WHITE, true))
            addGap(card, 7)
            val badge = text(phase.badge, 12f, ACCENT, true).apply {
                setPadding(dp(10), dp(6), dp(10), dp(6)); background = rounded(0xFF0B3045.toInt(), 20)
            }
            card.addView(badge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addGap(card, 10)
            card.addView(text(phase.description, 14f, MUTED))
            addGap(card, 12)
            card.addView(text("▶ Start (${phase.exercises.size} oefeningen)", 15f, GREEN, true))
            addGap(card, 5)
            val progressKey = if (phase.number == 4) "last_fysio" else "last_${phase.number}"
            val last = prefs.getString(progressKey, null)
            card.addView(text(if (last == null) "Laatst gedaan: Nog niet gedaan" else "Laatst gedaan: $last", 12f, 0xFF94A3B8.toInt()))
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        }
        addGap(content, 4)
        content.addView(button("🚦 Pijn-regels", 0xFF334155.toInt()) { showPainRules() }, LinearLayout.LayoutParams(-1, dp(54)))
        addGap(content, 18)
        content.addView(text("Persoonlijk oefenprogramma · gegevens blijven op dit toestel", 11f, 0xFF64748B.toInt()))
        scroll.addView(content)
        root.addView(scroll)
    }

    private fun startPhase(index: Int) {
        currentPhase = index; exerciseIndex = 0; showingAlternative = false
        showPlayer()
    }

    private fun showPlayer() {
        stopVideo()
        root.removeAllViews()
        videoFailed = false
        val phase = phases[currentPhase]
        val exercise = phase.exercises[exerciseIndex]
        val page = column(18)
        page.addView(button("✕  Naar hoofdmenu", 0xFF334155.toInt()) { showHome() }, LinearLayout.LayoutParams(-1, dp(48)))
        addGap(page, 15)
        page.addView(text("Oefening ${exerciseIndex + 1}/${phase.exercises.size} · Fase ${phase.number}", 13f, ACCENT, true))
        addGap(page, 8)
        page.addView(text(exercise.name, 25f, WHITE, true))
        addGap(page, 5)
        page.addView(text(exercise.dose, 16f, MUTED))
        if (exercise.hint.isNotBlank()) {
            addGap(page, 5)
            page.addView(text(exercise.hint, 13f, ACCENT))
        }
        addGap(page, 16)

        val videoFrame = FrameLayout(this).apply { background = rounded(Color.BLACK, 14); clipToOutline = true }
        val web = WebView(this)
        videoView = web
        web.setBackgroundColor(Color.BLACK)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webChromeClient = WebChromeClient()
        web.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; false }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX; val dy = event.y - downY
                    if (dx < -dp(60) && abs(dx) > abs(dy) * 1.3f) { nextExercise(); true } else false
                }
                else -> false
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) showVideoFallback(videoFrame, if (showingAlternative) exercise.altVideoId ?: exercise.videoId else exercise.videoId)
            }
        }
        videoFrame.addView(web, FrameLayout.LayoutParams(-1, -1))
        videoFrame.minimumHeight = dp(190)
        page.addView(videoFrame, LinearLayout.LayoutParams(-1, 0, 1f))
        loadVideo(web, if (showingAlternative) exercise.altVideoId ?: exercise.videoId else exercise.videoId)

        addGap(page, 12)
        page.addView(text("↔ Swipe of tik om verder te gaan", 13f, MUTED).apply { gravity = Gravity.CENTER })
        val fallbackLink = text("Video laadt niet? Open op YouTube ↗", 12f, ACCENT, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(5))
            setOnClickListener {
                val id = if (showingAlternative) exercise.altVideoId ?: exercise.videoId else exercise.videoId
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id")))
            }
        }
        page.addView(fallbackLink, LinearLayout.LayoutParams(-1, -2))
        addGap(page, 7)
        if (exercise.altText != null && exercise.altVideoId != null) {
            page.addView(text(exercise.altText, 13f, MUTED))
            addGap(page, 6)
            page.addView(button(if (showingAlternative) "▶ Bekijk originele video" else "▶ Bekijk alternatief", 0xFF334155.toInt()) {
                showingAlternative = !showingAlternative; showPlayer()
            }, LinearLayout.LayoutParams(-1, dp(48)))
            addGap(page, 8)
        }
        page.addView(button("Volgende oefening →", GREEN, BG) { nextExercise() }, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(page)

        // The WebView touch listener above detects left swipes while leaving taps to YouTube.
    }

    private fun loadVideo(web: WebView, id: String) {
        val html = """<!doctype html><html style="height:100%"><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"></head><body style="margin:0;background:#000;height:100%;overflow:hidden"><iframe width="100%" height="100%" style="position:absolute;inset:0;border:0" src="https://www.youtube-nocookie.com/embed/$id?autoplay=1&loop=1&playlist=$id&rel=0&playsinline=1" title="Oefening" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></body></html>"""
        web.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
    }

    private fun showVideoFallback(frame: FrameLayout, id: String) {
        if (videoFailed) return
        videoFailed = true
        frame.removeAllViews()
        val fallback = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        fallback.addView(text("Video kon niet worden geladen", 16f, WHITE, true).apply { gravity = Gravity.CENTER })
        val link = button("Open op YouTube ↗", ACCENT, BG) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id")))
        }
        val lp = LinearLayout.LayoutParams(-1, dp(50)); lp.topMargin = dp(14)
        fallback.addView(link, lp)
        frame.addView(fallback, FrameLayout.LayoutParams(-1, -1))
    }

    private fun nextExercise() {
        if (exerciseIndex < phases[currentPhase].exercises.lastIndex) {
            exerciseIndex++; showingAlternative = false; showPlayer()
        } else showEnd()
    }

    private fun showEnd() {
        stopVideo()
        root.removeAllViews()
        val page = column(24).apply { gravity = Gravity.CENTER }
        page.addView(text("🎉", 66f).apply { gravity = Gravity.CENTER })
        addGap(page, 16)
        val completedTitle = if (phases[currentPhase].number == 4) "Oefeningen Fysio klaar!" else "Fase ${phases[currentPhase].number} klaar!"
        page.addView(text(completedTitle, 29f, WHITE, true).apply { gravity = Gravity.CENTER })
        addGap(page, 10)
        page.addView(text("Alle ${phases[currentPhase].exercises.size} oefeningen gedaan.", 16f, MUTED).apply { gravity = Gravity.CENTER })
        addGap(page, 36)
        page.addView(button("⏹ Stop – naar hoofdmenu", RED) {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale("nl", "NL")).format(Date())
            val progressKey = if (phases[currentPhase].number == 4) "last_fysio" else "last_${phases[currentPhase].number}"
            prefs.edit().putString(progressKey, date).apply()
            showHome()
        }, LinearLayout.LayoutParams(-1, dp(56)))
        addGap(page, 12)
        page.addView(button("↻ Nog een keer", 0xFF334155.toInt()) { startPhase(currentPhase) }, LinearLayout.LayoutParams(-1, dp(54)))
        root.addView(page, FrameLayout.LayoutParams(-1, -1))
    }

    private fun showPainRules() {
        val dialog = Dialog(this)
        val panel = column(20).apply { background = rounded(CARD, 22) }
        panel.addView(text("🚦 Pijn-regels", 23f, WHITE, true))
        addGap(panel, 14)
        val rules = listOf(
            "Pijn 0–3/10, volgende ochtend niet erger" to "✅ Doorgaan",
            "Pijn >3/10 tijdens de oefening" to "⚠️ Niet verzwaren, lichtere versie",
            "'s Ochtends duidelijk gevoeliger" to "⚠️ Eén niveautje terug",
            "Scherpe pijn of zwelling" to "❌ Stoppen en laten beoordelen (fysio)"
        )
        rules.forEachIndexed { i, pair ->
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); background = rounded(0xFF26364B.toInt(), 12) }
            item.addView(text(pair.first, 14f, WHITE, true))
            addGap(item, 5)
            item.addView(text(pair.second, 14f, if (i == 3) 0xFFFCA5A5.toInt() else GREEN))
            panel.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
        addGap(panel, 4)
        panel.addView(button("Sluiten", ACCENT, BG) { dialog.dismiss() }, LinearLayout.LayoutParams(-1, dp(50)))
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            attributes = attributes.apply { width = WindowManager.LayoutParams.MATCH_PARENT; dimAmount = 0.65f }
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun stopVideo() {
        videoView?.apply { stopLoading(); loadUrl("about:blank"); onPause(); removeAllViews(); destroy() }
        videoView = null
    }

    @Deprecated("Deprecated in Android 13; retained for minSdk 24 compatibility")
    override fun onBackPressed() { showHome() }

    override fun onDestroy() { stopVideo(); super.onDestroy() }
}
