package com.cooldown.app.system

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * Pastille flottante du minuteur.
 *
 * Volontairement **non touchable** : elle ne doit pas intercepter les gestes de
 * l'écran d'appel. Si la superposition est refusée (permission retirée), tout
 * échoue en silence : le minuteur continue dans la notification.
 */
class CallTimerOverlay(private val context: Context) {

    private val windowManager: WindowManager? =
        context.getSystemService(WindowManager::class.java)

    private var view: TextView? = null

    fun show() {
        if (view != null) return
        val wm = windowManager ?: return

        val text = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val horizontal = dp(20)
            val vertical = dp(10)
            setPadding(horizontal, vertical, horizontal, vertical)
            background = pill(WARNING_BACKGROUND)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(24)
        }

        runCatching { wm.addView(text, params) }
            .onSuccess { view = text }
            .onFailure { Log.w(TAG, "Pastille impossible à afficher", it) }
    }

    fun updateText(value: String) {
        view?.text = value
    }

    fun updateWarning(warning: Boolean) {
        view?.background = pill(if (warning) WARNING_BACKGROUND else NORMAL_BACKGROUND)
    }

    fun hide() {
        val wm = windowManager ?: return
        val current = view ?: return
        view = null
        runCatching { wm.removeView(current) }
            .onFailure { Log.w(TAG, "Pastille déjà retirée", it) }
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(28).toFloat()
        setColor(color)
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "CallTimerOverlay"
        const val NORMAL_BACKGROUND = 0xCC1B1B1F.toInt()
        const val WARNING_BACKGROUND = 0xDDD97706.toInt()
    }
}
