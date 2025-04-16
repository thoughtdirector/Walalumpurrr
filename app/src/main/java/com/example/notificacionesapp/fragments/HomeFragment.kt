package com.example.notificacionesapp.fragments

import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.NotificationService
import com.example.notificacionesapp.R
import com.example.notificacionesapp.ScheduleManager
import com.example.notificacionesapp.databinding.FragmentHomeBinding
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var scheduleManager: ScheduleManager
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == MainActivity.updateStatusAction) {
                val serviceState = intent.getBooleanExtra("service_state", NotificationService.isServiceActive)
                val scheduleActivated = intent.getBooleanExtra("schedule_activated", false)

                // Actualizar la UI basado en el estado recibido
                updateSwitchWithoutTrigger(serviceState)

                if (scheduleActivated) {
                    // Mostrar un Toast informativo sobre la activación/desactivación por horario
                    val message = if (serviceState) {
                        getString(R.string.service_activated_by_schedule)
                    } else {
                        getString(R.string.service_deactivated_by_schedule)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                updateStatus()
                updateNextScheduleInfo()
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Inicializar ScheduleManager
        scheduleManager = ScheduleManager(requireContext())

        // Aplicar animación al ícono
        applyIconAnimation()

        // Configurar el switch de encendido
        setupPowerSwitch()

        // Configurar botones
        setupButtons()

        // Actualizar información en la UI
        updateStatus()
        updateNextScheduleInfo()
        updateDaysChips()
    }

    private fun applyIconAnimation() {
        try {
            val pulseAnimator = AnimatorInflater.loadAnimator(requireContext(), R.animator.pulse_animation)
            pulseAnimator.setTarget(binding.appIcon)
            pulseAnimator.start()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error al aplicar animación: ${e.message}")
        }
    }

    private fun setupPowerSwitch() {
        binding.powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isNotificationServiceEnabled()) {
                    promptNotificationAccess()
                    binding.powerSwitch.isChecked = false
                } else {
                    // Iniciar servicio explícitamente
                    val intent = Intent(requireContext(), NotificationService::class.java)
                    intent.action = NotificationService.ACTION_START_SERVICE

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            requireContext().startForegroundService(intent)
                        } else {
                            requireContext().startService(intent)
                        }

                        NotificationService.isServiceActive = true
                        updateStatusText()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error al iniciar servicio: ${e.message}")
                        Toast.makeText(requireContext(), "Error al iniciar servicio", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Detener servicio explícitamente
                val intent = Intent(requireContext(), NotificationService::class.java)
                intent.action = NotificationService.ACTION_STOP_SERVICE

                try {
                    requireContext().startService(intent)

                    NotificationService.isServiceActive = false
                    updateStatusText()
                    (activity as? MainActivity)?.tts?.speak(
                        getString(R.string.service_deactivated),
                        android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                        null,
                        "switch_off"
                    )
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error al detener servicio: ${e.message}")
                }
            }
        }
    }

    private fun setupButtons() {
        // Botón para probar servicio
        binding.serviceButton.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                promptNotificationAccess()
            } else if (binding.powerSwitch.isChecked) {
                Toast.makeText(requireContext(), getString(R.string.testing_voice_service), Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.tts?.speak(
                    getString(R.string.service_working_correctly),
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                    null,
                    "test_id"
                )
            } else {
                Toast.makeText(requireContext(), getString(R.string.activate_service_first), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDaysChips() {
        // Actualizar el estado de los chips según la configuración actual
        binding.chipLun.isChecked = scheduleManager.isDayEnabled(Calendar.MONDAY)
        binding.chipMar.isChecked = scheduleManager.isDayEnabled(Calendar.TUESDAY)
        binding.chipMie.isChecked = scheduleManager.isDayEnabled(Calendar.WEDNESDAY)
        binding.chipJue.isChecked = scheduleManager.isDayEnabled(Calendar.THURSDAY)
        binding.chipVie.isChecked = scheduleManager.isDayEnabled(Calendar.FRIDAY)
        binding.chipSab.isChecked = scheduleManager.isDayEnabled(Calendar.SATURDAY)
        binding.chipDom.isChecked = scheduleManager.isDayEnabled(Calendar.SUNDAY)

        // Desactivar la interacción con los chips (solo informativos)
        binding.chipLun.isClickable = false
        binding.chipMar.isClickable = false
        binding.chipMie.isClickable = false
        binding.chipJue.isClickable = false
        binding.chipVie.isClickable = false
        binding.chipSab.isClickable = false
        binding.chipDom.isClickable = false
    }

    private fun updateStatusText() {
        try {
            if (scheduleManager.isScheduleEnabled()) {
                val activeNow = scheduleManager.shouldServiceBeActive()
                if (activeNow && binding.powerSwitch.isChecked) {
                    binding.statusText.text = getString(R.string.service_active_scheduled)
                    binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_active))
                    binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_active))
                } else if (!activeNow) {
                    binding.statusText.text = getString(R.string.service_waiting)
                    binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_waiting))
                    binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_waiting))
                } else {
                    binding.statusText.text = getString(R.string.service_inactive)
                    binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_inactive))
                    binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_inactive))
                }
                NotificationService.isServiceActive = activeNow && binding.powerSwitch.isChecked
            } else {
                if (binding.powerSwitch.isChecked) {
                    binding.statusText.text = getString(R.string.service_active_listening)
                    binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_active))
                    binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_active))
                } else {
                    binding.statusText.text = getString(R.string.service_inactive)
                    binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_inactive))
                    binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_inactive))
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error en updateStatusText: ${e.message}")
        }
    }

    private fun updateStatus() {
        val notificationServiceEnabled = isNotificationServiceEnabled()

        if (notificationServiceEnabled) {
            binding.powerSwitch.isEnabled = true

            // Restaurar el estado del switch si el servicio estaba activo
            updateSwitchWithoutTrigger(NotificationService.isServiceActive)

            // Verificar el estado según el horario programado
            updateStatusText()
        } else {
            binding.statusText.text = getString(R.string.service_unavailable)
            binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_inactive))
            binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_inactive))
            updateSwitchWithoutTrigger(false)
            binding.powerSwitch.isEnabled = false
            NotificationService.isServiceActive = false
        }
    }

    private fun updateNextScheduleInfo() {
        if (scheduleManager.isScheduleEnabled()) {
            val startTime = formatTime(scheduleManager.getStartHour(), scheduleManager.getStartMinute())
            val endTime = formatTime(scheduleManager.getEndHour(), scheduleManager.getEndMinute())

            binding.activeTimeText.text = "$startTime a $endTime"

            val nextEvent = scheduleManager.getNextScheduledEvent()
            val nextEventTime = if (nextEvent != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormat.format(nextEvent.time)
            } else {
                getString(R.string.no_scheduled)
            }

            binding.nextUpdateText.text = nextEventTime

            // Actualizar el texto detallado
            binding.nextScheduleInfoText.text = "Próxima actualización: $nextEventTime"
        } else {
            binding.activeTimeText.text = "No programado"
            binding.nextUpdateText.text = "No programado"
            binding.nextScheduleInfoText.text = ""
        }

        // Actualizar el estado de los chips de días
        updateDaysChips()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun updateSwitchWithoutTrigger(checked: Boolean) {
        val currentState = binding.powerSwitch.isChecked

        if (currentState != checked) {
            // Desactivar temporalmente el listener
            binding.powerSwitch.setOnCheckedChangeListener(null)

            // Establecer el nuevo estado
            binding.powerSwitch.isChecked = checked

            // Reactivar el listener original
            setupPowerSwitch()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = requireContext().packageName
        val flat = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun promptNotificationAccess() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.notification_access_message))
            .setPositiveButton(getString(R.string.configure)) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            val filterStatus = IntentFilter(MainActivity.updateStatusAction)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(statusReceiver, filterStatus, Context.RECEIVER_NOT_EXPORTED)
            } else {
                requireContext().registerReceiver(statusReceiver, filterStatus)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error al registrar receivers: ${e.message}")
        }
        updateStatus()
        updateNextScheduleInfo()
        updateDaysChips()
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Ignorar si los receptores no están registrados
            Log.e("HomeFragment", "Error al desregistrar receivers: ${e.message}")
        }
    }
}