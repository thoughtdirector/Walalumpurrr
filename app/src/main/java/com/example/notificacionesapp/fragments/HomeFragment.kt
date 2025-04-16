package com.example.notificacionesapp.fragments

import android.animation.AnimatorInflater
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private var scheduleManager: ScheduleManager? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleManager = (activity as MainActivity).scheduleManager
    }

    override fun setupUI() {
        // Aplicar animación al ícono
        applyIconAnimation()

        // Configurar el switch de encendido
        setupPowerSwitch()

        // Configurar botones
        setupButtons()

        // Actualizar información en la UI
        updateUI()
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
                val mainActivity = activity as MainActivity
                if (!mainActivity.isNotificationServiceEnabled()) {
                    mainActivity.promptNotificationAccess()
                    binding.powerSwitch.isChecked = false
                } else {
                    // Iniciar servicio explícitamente a través de MainActivity
                    mainActivity.toggleNotificationService(true)
                    updateStatusText()
                }
            } else {
                // Detener servicio explícitamente a través de MainActivity
                (activity as MainActivity).toggleNotificationService(false)
                updateStatusText()
            }
        }
    }

    private fun setupButtons() {
        // Botón para probar servicio
        binding.serviceButton.setOnClickListener {
            val mainActivity = activity as MainActivity
            if (!mainActivity.isNotificationServiceEnabled()) {
                mainActivity.promptNotificationAccess()
            } else if (binding.powerSwitch.isChecked) {
                Toast.makeText(requireContext(), getString(R.string.testing_voice_service), Toast.LENGTH_SHORT).show()
                mainActivity.testTTS(getString(R.string.service_working_correctly))
            } else {
                Toast.makeText(requireContext(), getString(R.string.activate_service_first), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateUI() {
        updateStatus()
        updateScheduleInfo()
        updateDaysChips()
    }

    fun updateServiceState(active: Boolean) {
        updateSwitchWithoutTrigger(active)
        updateStatusText()
    }

    private fun updateDaysChips() {
        scheduleManager?.let { manager ->
            // Actualizar el estado de los chips según la configuración actual
            binding.chipLun.isChecked = manager.isDayEnabled(Calendar.MONDAY)
            binding.chipMar.isChecked = manager.isDayEnabled(Calendar.TUESDAY)
            binding.chipMie.isChecked = manager.isDayEnabled(Calendar.WEDNESDAY)
            binding.chipJue.isChecked = manager.isDayEnabled(Calendar.THURSDAY)
            binding.chipVie.isChecked = manager.isDayEnabled(Calendar.FRIDAY)
            binding.chipSab.isChecked = manager.isDayEnabled(Calendar.SATURDAY)
            binding.chipDom.isChecked = manager.isDayEnabled(Calendar.SUNDAY)

            // Desactivar la interacción con los chips (solo informativos)
            binding.chipLun.isClickable = false
            binding.chipMar.isClickable = false
            binding.chipMie.isClickable = false
            binding.chipJue.isClickable = false
            binding.chipVie.isClickable = false
            binding.chipSab.isClickable = false
            binding.chipDom.isClickable = false
        }
    }

    private fun updateStatusText() {
        try {
            scheduleManager?.let { manager ->
                if (manager.isScheduleEnabled()) {
                    val activeNow = manager.shouldServiceBeActive()
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
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error en updateStatusText: ${e.message}")
        }
    }

    private fun updateStatus() {
        val mainActivity = activity as MainActivity
        val notificationServiceEnabled = mainActivity.isNotificationServiceEnabled()

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

    fun updateScheduleInfo() {
        scheduleManager?.let { manager ->
            if (manager.isScheduleEnabled()) {
                val startTime = formatTime(manager.getStartHour(), manager.getStartMinute())
                val endTime = formatTime(manager.getEndHour(), manager.getEndMinute())

                binding.activeTimeText.text = "$startTime a $endTime"

                val nextEvent = manager.getNextScheduledEvent()
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
}