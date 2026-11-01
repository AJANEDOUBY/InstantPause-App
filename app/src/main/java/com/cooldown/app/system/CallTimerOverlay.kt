package com.cooldown.app.system

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
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
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(24)
        }

        runCatching { wm.addView(text, params) }
            .onSuccess { view = text }
            .onFailure { Log.w(TAG, "Pastille impossible à afficher", it) }
    }

    /**
     * Met à jour le texte de la pastille.
     *
     * Avec un [prefix], il est affiché en petite taille au-dessus du décompte : un
     * chronomètre seul ne dit pas à quoi il correspond, et passe donc inaperçu.
     */
    fun updateText(value: String, prefix: String? = null) {
        val current = view ?: return
        current.text = if (prefix.isNullOrEmpty()) {
            value
        } else {
            SpannableString("$prefix\n$value").apply {
                setSpan(
                    RelativeSizeSpan(PREFIX_SCALE),
                    0,
                    prefix.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    ForegroundColorSpan(PREFIX_COLOR),
                    0,
                    prefix.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
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

    /**
     * Superposition système. `TYPE_APPLICATION_OVERLAY` existe depuis Android 8 et le
     * projet cible au minimum Android 10 : aucun repli sur `TYPE_PHONE` n'est utile.
     */
    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "CallTimerOverlay"
        const val NORMAL_BACKGROUND = 0xCC1B1B1F.toInt()
        const val WARNING_BACKGROUND = 0xDDD97706.toInt()

        /** Le libellé reste lisible mais laisse la vedette au décompte. */
        const val PREFIX_SCALE = 0.62f
        const val PREFIX_COLOR = 0xCCFFFFFF.toInt()
    }
}
