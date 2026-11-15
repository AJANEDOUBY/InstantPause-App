package com.instantpause.app.system

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import com.instantpause.app.R
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Compte à rebours de [LIMIT_SECONDS] secondes pour un appel passé vers un contact
 * du cercle de confiance.
 *
 * À l'échéance, le service **raccroche** via `TelecomManager.endCall()`, autorisé par
 * la permission `ANSWER_PHONE_CALLS`. Si le constructeur refuse (ou si la permission
 * a été retirée), [hangUp] échoue sans crash : la pastille et la notification
 * continuent d'afficher 0:00 et l'utilisateur doit raccrocher lui-même. C'est le
 * seul cas où la limite redevient un simple rappel visuel.
 */
class CallTimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: CallTimerOverlay? = null
    private var deadline = 0L
    private var warned = false
    private var expiredShown = false
    private var lastNotifiedSecond = -1

    private val audioManager: AudioManager?
        get() = getSystemService(AudioManager::class.java)

    /** Vrai dès qu'une communication a réellement été observée (voir [callIsOver]). */
    private var callWasLive = false
    private var idleTicks = 0

    /** Le décompte est décalé une seule fois, au moment où la ligne s'établit. */
    private var countdownStartedOnConnect = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlay = CallTimerOverlay(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(
            getString(
                R.string.call_timer_line,
                getString(R.string.call_timer_prefix),
                CallTimerText.countdown(LIMIT_SECONDS, LIMIT_SECONDS)
            )
        )

        deadline = SystemClock.elapsedRealtime() + (LIMIT_SECONDS * 1_000L)
        warned = false
        expiredShown = false
        lastNotifiedSecond = -1
        callWasLive = false
        idleTicks = 0
        countdownStartedOnConnect = false

        overlay?.show()
        overlay?.updateText(
            CallTimerText.countdown(LIMIT_SECONDS, LIMIT_SECONDS),
            prefix = getString(R.string.call_timer_prefix),
        )

        handler.removeCallbacks(tick)
        handler.post(tick)

        return START_NOT_STICKY
    }

    private val tick = object : Runnable {
        override fun run() {
            if (callIsOver()) {
                Log.i(TAG, "Appel terminé — arrêt du minuteur")
                stopSelf()
                return
            }

            // La sonnerie ne doit pas manger les 3 minutes : au premier signe de
            // communication établie, on repart de 3 minutes pleines. On ne fait que
            // **repousser** l'échéance, jamais l'avancer.
            if (callWasLive && !countdownStartedOnConnect) {
                countdownStartedOnConnect = true
                deadline = maxOf(
                    deadline,
                    SystemClock.elapsedRealtime() + (LIMIT_SECONDS * 1_000L)
                )
                // La sonnerie a pu consommer l'alerte des 30 s : on la ré-arme pour la
                // vraie fin de conversation.
                warned = false
                lastNotifiedSecond = -1
                overlay?.updateWarning(warning = false)
            }

            val leftMs = deadline - SystemClock.elapsedRealtime()
            val leftSec = ((leftMs + 999) / 1000).coerceAtLeast(0L).toInt()

            if (leftSec > 0) {
                overlay?.updateText(
                    CallTimerText.countdown(leftSec, LIMIT_SECONDS),
                    prefix = getString(R.string.call_timer_prefix),
                )

                if (leftSec <= WARN_SECONDS && !warned) {
                    warned = true
                    overlay?.updateWarning(true)
                    vibrate(200)
                }

                // Rafraîchit la notification sans spammer : une fois par tranche de 30 s.
                if (leftSec % 30 == 0 && leftSec != lastNotifiedSecond) {
                    lastNotifiedSecond = leftSec
                    updateNotification(
                        getString(
                            R.string.call_timer_line,
                            getString(R.string.call_timer_prefix),
                            CallTimerText.countdown(leftSec, LIMIT_SECONDS)
                        )
                    )
                }

                handler.postDelayed(this, 500L)
            } else {
                if (!expiredShown) {
                    expiredShown = true
                    if (hangUp()) {
                        // La ligne est coupée par le système : la pastille s'efface au
                        // prochain relevé (l'audio repasse en mode normal).
                        Log.i(TAG, "Temps écoulé — appel coupé")
                        overlay?.updateText(getString(R.string.call_time_up_cut))
                        overlay?.updateWarning(true)
                        updateNotification(getString(R.string.call_time_up_cut))
                    } else {
                        // Permission absente ou constructeur récalcitrant : on ne peut
                        // que prévenir. L'utilisateur raccroche lui-même.
                        Log.w(TAG, "Raccrochage refusé par le système")
                        overlay?.updateText(getString(R.string.call_time_up_hang_up))
                        overlay?.updateWarning(true)
                        updateNotification(getString(R.string.call_time_up_hang_up))
                    }
                    vibrate(600)
                    handler.postDelayed({ stopSelf() }, END_GRACE_MS)
                }
                // La pastille reste, mais s'efface dès que l'appel est raccroché.
                handler.postDelayed(this, 500L)
            }
        }
    }

    /**
     * Raccroche l'appel en cours.
     *
     * Android n'autorise une application tierce à couper une ligne qu'avec la permission
     * [Manifest.permission.ANSWER_PHONE_CALLS] (c'est elle qui ouvre
     * [TelecomManager.endCall]). Sans elle, ou si le constructeur refuse, on renvoie
     * `false` : le minuteur reste un rappel visuel, il ne coupe rien.
     *
     * `endCall` est déprécié depuis Android 10 au profit d'`InCallService`, réservé à
     * l'application de téléphone par défaut — hors de question de prendre ce rôle.
     */
    @Suppress("DEPRECATION")
    private fun hangUp(): Boolean {
        if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val telecom = getSystemService(TelecomManager::class.java) ?: return false
        return runCatching { telecom.endCall() }
            .onFailure { Log.w(TAG, "endCall() a échoué", it) }
            .getOrDefault(false)
    }

    /**
     * Détecte le raccrochage sans permission téléphonie : pendant une communication,
     * Android impose le mode audio [AudioManager.MODE_IN_CALL] (ou
     * MODE_IN_COMMUNICATION), puis le remet à MODE_NORMAL une fois la ligne coupée.
     *
     * On ne s'arrête qu'après avoir **vu** la communication s'établir — sinon la
     * numérotation (mode encore NORMAL) arrêterait le minuteur aussitôt — et après
     * [IDLE_TICKS_TO_STOP] relevés d'affilée, pour ignorer les micro-coupures.
     */
    private fun callIsOver(): Boolean {
        val mode = runCatching { audioManager?.mode }.getOrNull() ?: return false
        val live = mode == AudioManager.MODE_IN_CALL ||
                mode == AudioManager.MODE_IN_COMMUNICATION

        if (live) {
            callWasLive = true
            idleTicks = 0
            return false
        }

        if (!callWasLive) return false
        idleTicks++
        return idleTicks >= IDLE_TICKS_TO_STOP
    }

    private fun startInForeground(content: String) {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(content),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )
    }

    private fun buildNotification(content: String): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_allowed_call_title))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun updateNotification(content: String) {
        runCatching {
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIF_ID, buildNotification(content))
        }
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        // Recréé à chaque démarrage : c'est le seul moyen de mettre à jour le nom
        // du canal quand l'utilisateur change la langue de l'appareil.
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_call_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_call_timer_desc)
                setShowBadge(false)
            }
        )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        overlay?.hide()
        overlay = null
        super.onDestroy()
    }

    private fun vibrate(durationMs: Long) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.takeIf { it.hasVibrator() }?.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    companion object {
        private const val TAG = "CallTimerService"
        private const val CHANNEL_ID = "cooldown_timer"
        private const val NOTIF_ID = 4207

        /** Durée maximale d'un appel vers un contact du cercle de confiance. */
        const val LIMIT_SECONDS = 180

        /** Seuil d'alerte avant la fin. */
        private const val WARN_SECONDS = 30

        /** Délai laissé à l'utilisateur pour raccrocher avant l'arrêt du service. */
        private const val END_GRACE_MS = 30_000L

        /** Relevés consécutifs « audio revenu à la normale » avant de conclure au raccrochage. */
        private const val IDLE_TICKS_TO_STOP = 4

        fun start(context: Context) {
            val intent = Intent(context, CallTimerService::class.java)
            runCatching { context.startForegroundService(intent) }
        }
    }
}
