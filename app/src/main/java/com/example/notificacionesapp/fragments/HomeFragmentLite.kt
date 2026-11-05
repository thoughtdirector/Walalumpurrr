package com.example.notificacionesapp.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.notificacionesapp.MainActivityLite
import com.example.notificacionesapp.NotificationService
import com.example.notificacionesapp.SessionAlarmManager
import com.example.notificacionesapp.SessionExpirationReceiver
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentHomeLiteBinding

class HomeFragmentLite : BaseFragment<FragmentHomeLiteBinding>() {

    private var activationTimestamp: Long = 0
    private var haloPulse: ObjectAnimator? = null

    // Manejador de alarma de expiración (patrón ScheduleManager de la versión principal).
    private lateinit var sessionAlarmManager: SessionAlarmManager

    // Ticker de UI: solo actualiza la pantalla mientras el fragmento es visible.
    // No reinicia el contador ni mantiene temporizadores en segundo plano.
    private var sessionTickerHandler: Handler? = null
    private var sessionTickerRunnable: Runnable? = null

    companion object {
        private const val TAG = "HomeFragmentLite"
        // 4 horas de duración de sesión (producción).
        private const val SESSION_DURATION_MS = 4L * 60 * 60 * 1000
        private const val PREFS_NAME = "lite_session_prefs"
        private const val KEY_ACTIVATION_TIME = "activation_timestamp"
        private const val REQUEST_CODE_SESSION_ALARM = 1001
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeLiteBinding {
        return FragmentHomeLiteBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionAlarmManager = SessionAlarmManager(requireContext())
        activationTimestamp = sessionAlarmManager.getActivationTime()
    }

    override fun onResume() {
        super.onResume()
        // Al volver a la pantalla sincroniza con el estado real del servicio,
        // chequea expiración (incluso si ocurrió en segundo plano) y refresca el contador.
        checkExpirationAndSync()
        announceExpiredIfNeeded()
        startSessionTicker()
        updateUI()
    }

    override fun onStop() {
        super.onStop()
        stopSessionTicker()
    }

    override fun setupUI() {
        setupPowerButton()
        setupSoundButton()
        updateUI()
    }

    private fun loadSessionState() {
        activationTimestamp = sessionAlarmManager.getActivationTime()
    }

    private fun saveSessionState(timestamp: Long) {
        sessionAlarmManager.saveActivationTime(timestamp)
    }

    private fun clearSessionState() {
        sessionAlarmManager.clearActivationTime()
        activationTimestamp = 0
    }

    private fun isSessionExpired(): Boolean {
        if (activationTimestamp == 0L) return false
        val now = System.currentTimeMillis()
        return (now - activationTimestamp) >= SESSION_DURATION_MS
    }

    private fun getRemainingTimeMs(): Long {
        if (activationTimestamp == 0L) return 0L
        val elapsed = System.currentTimeMillis() - activationTimestamp
        return (SESSION_DURATION_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Si la sesión expiró mientras la app estaba en segundo plano/cerrada, el
     * SessionExpirationReceiver ya detuvo el servicio y dejó un flag. Aquí se
     * anuncia por TTS al usuario y se limpia la sesión para que el contador
     * muestre "sin iniciar".
     */
    private fun announceExpiredIfNeeded() {
        val prefs = requireContext().getSharedPreferences(SessionAlarmManager.PREFS_NAME, Context.MODE_PRIVATE)
        val expiredNotified = prefs.getBoolean(SessionAlarmManager.KEY_EXPIRED_NOTIFIED, false)
        if (!expiredNotified || activationTimestamp == 0L) return

        prefs.edit().remove(SessionAlarmManager.KEY_EXPIRED_NOTIFIED).apply()
        clearSessionState()

        val mainActivity = activity as? MainActivityLite ?: return
        mainActivity.testTTS(getString(R.string.lite_service_disconnected))
    }

    /**
     * Sincroniza la UI con la fuente de verdad (NotificationService.isServiceActive).
     * Si el servicio está activo pero la sesión expiró, lo detiene y avisa por TTS.
     * No reinicia el contador ni detiene el servicio solo por navegar entre pantallas.
     */
    private fun checkExpirationAndSync() {
        val mainActivity = activity as? MainActivityLite ?: return

        val serviceActive = NotificationService.isServiceActive
        val expired = activationTimestamp != 0L && isSessionExpired()

        if (serviceActive && expired) {
            binding.btnPower.tag = null
            clearSessionState()
            mainActivity.toggleNotificationService(false)
            mainActivity.testTTS(getString(R.string.lite_service_disconnected))
        }

        // Sincroniza la etiqueta visual con el estado real del servicio.
        val isActive = serviceActive && !expired && activationTimestamp != 0L
        binding.btnPower.tag = if (isActive) "ACTIVE" else null
    }

    private fun setupPowerButton() {
        binding.btnPower.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val mainActivity = activity as? MainActivityLite ?: return@setOnClickListener
            val willActivate = binding.btnPower.tag?.toString() != "ACTIVE"

            if (willActivate) {
                // Encender servicio
                if (!mainActivity.isNotificationServiceEnabled()) {
                    mainActivity.promptNotificationAccess()
                    return@setOnClickListener
                }
                binding.btnPower.tag = "ACTIVE"
                activationTimestamp = System.currentTimeMillis()
                saveSessionState(activationTimestamp)
                mainActivity.toggleNotificationService(true)
                mainActivity.testTTS(getString(R.string.service_activated))
                sessionAlarmManager.scheduleExpirationAlarm(activationTimestamp, SESSION_DURATION_MS)
            } else {
                // Apagar servicio
                binding.btnPower.tag = null
                clearSessionState()
                sessionAlarmManager.cancelExpirationAlarm()
                mainActivity.toggleNotificationService(false)
                mainActivity.testTTS(getString(R.string.service_deactivated))
            }

            animatePowerPress()
            updateUI()
        }
    }

    private fun animatePowerPress() {
        val button = binding.btnPower
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(90)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun setupSoundButton() {
        binding.btnSound.setOnClickListener {
            val mainActivity = activity as? MainActivityLite ?: return@setOnClickListener
            if (NotificationService.isServiceActive && !isSessionExpired()) {
                mainActivity.testTTS(getString(R.string.service_working_correctly))
            } else {
                Toast.makeText(requireContext(), getString(R.string.activate_service_first), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateUI() {
        if (!isAdded) return
        try {
            updateStatus()
            updateSessionInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error en updateUI: ${e.message}")
        }
    }

    /**
     * Fuente de verdad: NotificationService.isServiceActive.
     * Nunca sobrescribe ese estado estático desde la etiqueta de un botón,
     * para evitar que al recrearse la vista (Home <-> Info) el servicio se apague
     * por error y el botón quede desincronizado.
     */
    private fun updateStatus() {
        val mainActivity = (activity as? MainActivityLite) ?: return

        val expired = activationTimestamp != 0L && isSessionExpired()
        val serviceActive = NotificationService.isServiceActive

        // Si el servicio sigue activo pero la sesión expiró, lo detiene y avisa.
        if (serviceActive && expired) {
            binding.btnPower.tag = null
            clearSessionState()
            mainActivity.toggleNotificationService(false)
            mainActivity.testTTS(getString(R.string.lite_service_disconnected))
        }

        val isActive = serviceActive && !expired && activationTimestamp != 0L

        val statusTextRes: Int
        val statusColorRes: Int
        when {
            isActive -> {
                statusTextRes = R.string.lite_status_active
                statusColorRes = R.color.status_active
            }
            expired -> {
                statusTextRes = R.string.lite_status_expired
                statusColorRes = R.color.status_waiting
            }
            else -> {
                statusTextRes = R.string.lite_status_inactive
                statusColorRes = R.color.status_inactive
            }
        }

        val statusColor = ContextCompat.getColor(requireContext(), statusColorRes)
        binding.statusText.setText(statusTextRes)
        binding.statusText.setTextColor(statusColor)
        binding.statusDot.backgroundTintList = ColorStateList.valueOf(statusColor)
        binding.btnPower.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (isActive) R.color.status_active else R.color.status_inactive
        )
        binding.btnPower.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        setHaloPulsing(isActive)
    }

    private fun setHaloPulsing(active: Boolean) {
        val halo = binding.powerHalo
        if (active) {
            if (haloPulse == null) {
                haloPulse = ObjectAnimator.ofFloat(halo, View.ALPHA, 1f, 0.4f).apply {
                    duration = 1500L
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
            }
            if (haloPulse?.isStarted != true) {
                haloPulse?.start()
            }
        } else {
            haloPulse?.cancel()
            halo.alpha = 1f
        }
    }

    private fun updateSessionInfo() {
        if (activationTimestamp == 0L) {
            binding.sessionInfoText.setText(R.string.lite_session_not_started)
            return
        }

        if (isSessionExpired()) {
            binding.sessionInfoText.setText(R.string.session_expired)
            return
        }

        val remainingMs = getRemainingTimeMs()
        val remainingHours = remainingMs / (1000 * 60 * 60)
        val remainingMinutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
        binding.sessionInfoText.text = getString(R.string.session_time_remaining, remainingHours, remainingMinutes)
    }

    fun updateServiceState(active: Boolean) {
        binding.btnPower.tag = if (active) "ACTIVE" else null
        updateUI()
    }

    private fun startSessionTicker() {
        stopSessionTicker()
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        runnable = object : Runnable {
            override fun run() {
                if (isAdded) {
                    updateUI()
                }
                handler.postDelayed(runnable!!, 1000)
            }
        }
        sessionTickerHandler = handler
        sessionTickerRunnable = runnable
        handler.post(runnable!!)
    }

    private fun stopSessionTicker() {
        sessionTickerHandler?.removeCallbacks(sessionTickerRunnable!!)
        sessionTickerHandler = null
        sessionTickerRunnable = null
    }

    override fun onDestroyView() {
        stopSessionTicker()
        haloPulse?.cancel()
        haloPulse = null
        super.onDestroyView()
    }
}
