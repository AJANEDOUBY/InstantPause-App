package com.cooldown.app.ui.pause

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooldown.app.system.ProtectionAlertNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class PauseActivity : ComponentActivity() {

    @Inject lateinit var notifier: ProtectionAlertNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // ⭐ Demande le déverrouillage automatique pour rendre l'écran interactif immédiatement
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)

        // ⭐ Annule les rappels UNIQUEMENT si l'utilisateur a tapé la notification.
        // Quand l'écran est ouvert automatiquement par PauseForegroundService, la
        // notification et les rappels doivent rester actifs.
        if (intent.getBooleanExtra(EXTRA_FROM_USER, false)) {
            notifier.dismissBlockedCall()
        }

        setContent {
            MaterialTheme {
                SensoryPauseScreen(
                    onClose = {
                        notifier.dismissBlockedCall()
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_FROM_USER, false)) {
            notifier.dismissBlockedCall()
        }
    }

    companion object {
        private const val EXTRA_NUMBER = "extra_target_number"
        private const val EXTRA_FROM_USER = "extra_from_user"

        /**
         * Actions distinctes par origine.
         *
         * Indispensable : `PendingIntent.filterEquals()` ignore les extras. Sans
         * action différente, le PendingIntent de l'écran automatique et celui de
         * la notification (même request code + FLAG_UPDATE_CURRENT) désignent le
         * MÊME enregistrement : le dernier créé écrase les extras de l'autre.
         * L'écran ouvert automatiquement héritait alors de `fromUser = true` et
         * annulait la notification et les rappels.
         */
        private const val ACTION_AUTO = "com.cooldown.app.action.PAUSE_AUTO"
        private const val ACTION_FROM_USER = "com.cooldown.app.action.PAUSE_FROM_USER"

        fun intent(context: Context, number: String, fromUser: Boolean = false): Intent =
            Intent(context, PauseActivity::class.java).apply {
                action = if (fromUser) ACTION_FROM_USER else ACTION_AUTO
                putExtra(EXTRA_NUMBER, number)
                putExtra(EXTRA_FROM_USER, fromUser)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
    }
}

private const val CYCLE_MS = 8_000
private const val MIN_SCALE = 0.6f
private const val MAX_SCALE = 1.2f
private const val MIN_DWELL_MS = 4_000L

@Composable
fun SensoryPauseScreen(onClose: () -> Unit) {
    val context = LocalContext.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    val phase = remember { Animatable(0f) }
    var lastPeak by remember { mutableStateOf(Peak.NONE) }
    var canClose by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(MIN_DWELL_MS)
        canClose = true
    }

    LaunchedEffect(Unit) {
        while (true) {
            phase.animateTo(1f, tween(CYCLE_MS / 2, easing = LinearOutSlowInEasing))
            phase.animateTo(0f, tween(CYCLE_MS / 2, easing = LinearOutSlowInEasing))
        }
    }

    // Dérivé de l'animation : ne change que quelques fois par cycle,
    // contrairement à `phase.value` qui change à chaque frame.
    val currentPeak = when {
        phase.value > 0.98f -> Peak.INHALED
        phase.value < 0.02f -> Peak.EXHALED
        else -> Peak.NONE
    }

    LaunchedEffect(currentPeak) {
        if (currentPeak == Peak.NONE || currentPeak == lastPeak) return@LaunchedEffect
        lastPeak = currentPeak
        if (!vibrator.hasVibrator()) return@LaunchedEffect
        vibrator.vibrate(
            VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * phase.value
    val cue = if (phase.value < 0.5f) "Inspirer doucement…" else "Expirer lentement…"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Prenez un instant.",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Cet appel peut attendre demain.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFCBD5E1)
            )
            Spacer(Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f + 0.2f * phase.value)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((120 * scale).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                )
            }

            Spacer(Modifier.height(60.dp))
            Text(
                text = cue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF60A5FA)
            )
            Spacer(Modifier.height(48.dp))

            if (canClose) {
                TextButton(onClick = onClose) {
                    Text(
                        text = "Fermer",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private enum class Peak { NONE, INHALED, EXHALED }