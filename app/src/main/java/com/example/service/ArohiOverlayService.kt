package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ArohiApplication
import com.example.MainActivity
import com.example.engine.ArohiEmotion
import kotlinx.coroutines.launch

/**
 * Small, elegant, battery-conscious floating indicator shown while the
 * background assistant is running (only when Android overlay permission is
 * granted). Displays "Arohi" and "made with Shù Vrô"; the status dot color
 * follows the REAL assistant state. No continuous high-FPS animation — the
 * pulse only runs while AROHI is actively listening/speaking/executing.
 */
class ArohiOverlayService : android.app.Service() {

    companion object {
        var isActive: Boolean = false
            private set

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        fun startService(context: Context) {
            if (isActive) return
            if (!canDrawOverlays(context)) return
            try {
                context.startService(Intent(context, ArohiOverlayService::class.java))
            } catch (e: Exception) {
                // Foreground-service restrictions or missing permission — nothing fake
            }
        }

        fun stopService(context: Context) {
            try {
                context.stopService(Intent(context, ArohiOverlayService::class.java))
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var dotView: View? = null
    private var dotPulseRunning = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(9), dp(16), dp(9))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#E60A0E1A"))
                setStroke(dp(1), Color.parseColor("#3322D3EE"))
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        dotView = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply {
                rightMargin = dp(7)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#10B981"))
            }
        }
        topRow.addView(dotView)

        topRow.addView(TextView(this).apply {
            text = "Arohi"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        container.addView(topRow)

        container.addView(TextView(this).apply {
            text = "made with Shù Vrô"
            setTextColor(Color.parseColor("#CC94A3B8"))
            textSize = 10f
            setPadding(0, dp(1), 0, 0)
        })

        // Tap opens the main UI — real behavior, no fake actions
        container.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Ignored
            }
        }

        // Drag to reposition
        var initialRawX = 0f
        var initialRawY = 0f
        var initialParamX = 0
        var initialParamY = 0
        var moved = false
        container.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val params = container.layoutParams as? WindowManager.LayoutParams
                    if (params != null) {
                        initialRawX = event.rawX
                        initialRawY = event.rawY
                        initialParamX = params.x
                        initialParamY = params.y
                        moved = false
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialRawX
                    val dy = event.rawY - initialRawY
                    if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) moved = true
                    if (moved) {
                        val params = container.layoutParams as? WindowManager.LayoutParams
                        if (params != null) {
                            params.x = initialParamX + dx.toInt()
                            params.y = initialParamY + dy.toInt()
                            windowManager?.updateViewLayout(container, params)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(140)
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            stopSelf()
            return
        }
        overlayView = container
        isActive = true

        // Observe the REAL assistant state — pulse only while active
        val app = applicationContext as? ArohiApplication
        val emotionFlow = app?.emotionEngine?.currentEmotion
        if (app != null && emotionFlow != null) {
            app.applicationScope.launch {
                emotionFlow.collect { emotion -> applyState(emotion) }
            }
        }
        applyState(app?.emotionEngine?.currentEmotion?.value ?: ArohiEmotion.IDLE)
    }

    private fun applyState(emotion: ArohiEmotion) {
        val colorHex = when (emotion) {
            ArohiEmotion.LISTENING -> "#10B981"
            ArohiEmotion.SPEAKING -> "#22D3EE"
            ArohiEmotion.THINKING -> "#8B5CF6"
            ArohiEmotion.EXECUTING -> "#F59E0B"
            ArohiEmotion.ERROR, ArohiEmotion.CONCERNED -> "#EF4444"
            else -> "#10B981"
        }
        (dotView?.background as? GradientDrawable)?.setColor(Color.parseColor(colorHex))

        val shouldPulse = emotion == ArohiEmotion.LISTENING ||
            emotion == ArohiEmotion.SPEAKING ||
            emotion == ArohiEmotion.THINKING ||
            emotion == ArohiEmotion.EXECUTING

        if (shouldPulse) startPulse() else stopPulse()
    }

    private fun startPulse() {
        if (dotPulseRunning) return
        dotPulseRunning = true
        dotView?.animate()?.alpha(0.3f)?.setDuration(500)?.withEndAction {
            dotView?.animate()?.alpha(1f)?.setDuration(500)?.withEndAction {
                if (dotPulseRunning) startPulse()
            }
        }
    }

    private fun stopPulse() {
        dotPulseRunning = false
        dotView?.animate()?.cancel()
        dotView?.alpha = 1f
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        stopPulse()
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            // Ignored
        }
        overlayView = null
        windowManager = null
        isActive = false
        super.onDestroy()
    }
}
