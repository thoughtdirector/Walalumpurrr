package com.example.notificacionesapp.presentation.fragments

import android.animation.AnimatorInflater
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.notificacionesapp.R
import com.example.notificacionesapp.core.auth.AuthManager
import com.example.notificacionesapp.databinding.FragmentHomeBinding
import com.example.notificacionesapp.domain.model.Schedule
import com.example.notificacionesapp.presentation.viewmodel.HomeViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Home Fragment with improved architecture using ViewModel
 */
@AndroidEntryPoint
class HomeFragmentNew : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Apply animation to icon
        applyIconAnimation()

        // Setup power switch
        setupPowerSwitch()

        // Setup buttons
        setupButtons()
    }

    private fun observeViewModel() {
        // Observe schedule data
        homeViewModel.schedule.observe(viewLifecycleOwner) { schedule ->
            updateScheduleInfo(schedule)
        }

        // Observe schedule active state
        homeViewModel.isScheduleActive.observe(viewLifecycleOwner) { isActive ->
            updateStatusText(isActive)
        }

        // Observe loading state
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Handle loading state if needed
        }

        // Observe error state
        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                homeViewModel.clearError()
            }
        }
    }

    private fun applyIconAnimation() {
        try {
            val pulseAnimator = AnimatorInflater.loadAnimator(requireContext(), R.animator.pulse_animation)
            pulseAnimator.setTarget(binding.appIcon)
            pulseAnimator.start()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error applying animation: ${e.message}")
        }
    }

    private fun setupPowerSwitch() {
        binding.powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val mainActivity = requireActivity() as? com.example.notificacionesapp.presentation.MainActivityNew
                if (mainActivity?.isNotificationServiceEnabled() != true) {
                    mainActivity?.promptNotificationAccess()
                    binding.powerSwitch.isChecked = false
                } else {
                    // Start service through MainActivity
                    mainActivity?.toggleNotificationService(true)
                    updateStatusText(true)
                }
            } else {
                // Stop service through MainActivity
                (requireActivity() as? com.example.notificacionesapp.presentation.MainActivityNew)?.toggleNotificationService(false)
                updateStatusText(false)
            }
        }
    }

    private fun setupButtons() {
        // Button to test service
        binding.serviceButton.setOnClickListener {
            val mainActivity = requireActivity() as? com.example.notificacionesapp.presentation.MainActivityNew
            if (mainActivity?.isNotificationServiceEnabled() != true) {
                mainActivity?.promptNotificationAccess()
            } else if (binding.powerSwitch.isChecked) {
                Toast.makeText(requireContext(), getString(R.string.testing_voice_service), Toast.LENGTH_SHORT).show()
                mainActivity?.testTTS(getString(R.string.service_working_correctly))
            } else {
                Toast.makeText(requireContext(), getString(R.string.activate_service_first), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateUI() {
        if (isAdded) {
            try {
                homeViewModel.loadSchedule()
                updateDaysChips()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in updateUI: ${e.message}")
            }
        }
    }

    fun updateServiceState(active: Boolean) {
        updateSwitchWithoutTrigger(active)
        updateStatusText(active)
    }

    private fun updateDaysChips() {
        val schedule = homeViewModel.schedule.value
        schedule?.let { sched ->
            // Update chip states according to current configuration
            binding.chipLun.isChecked = sched.isDayEnabled(Calendar.MONDAY)
            binding.chipMar.isChecked = sched.isDayEnabled(Calendar.TUESDAY)
            binding.chipMie.isChecked = sched.isDayEnabled(Calendar.WEDNESDAY)
            binding.chipJue.isChecked = sched.isDayEnabled(Calendar.THURSDAY)
            binding.chipVie.isChecked = sched.isDayEnabled(Calendar.FRIDAY)
            binding.chipSab.isChecked = sched.isDayEnabled(Calendar.SATURDAY)
            binding.chipDom.isChecked = sched.isDayEnabled(Calendar.SUNDAY)

            // Disable interaction with chips (informational only)
            binding.chipLun.isClickable = false
            binding.chipMar.isClickable = false
            binding.chipMie.isClickable = false
            binding.chipJue.isClickable = false
            binding.chipVie.isClickable = false
            binding.chipSab.isClickable = false
            binding.chipDom.isClickable = false
        }
    }

    private fun updateStatusText(isScheduleActive: Boolean) {
        try {
            val schedule = homeViewModel.schedule.value
            schedule?.let { sched ->
                if (sched.isEnabled) {
                    val activeNow = sched.isCurrentlyActive()
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
            Log.e("HomeFragment", "Error in updateStatusText: ${e.message}")
        }
    }

    private fun updateScheduleInfo(schedule: Schedule) {
        if (schedule.isEnabled) {
            val startTime = formatTime(schedule.startHour, schedule.startMinute)
            val endTime = formatTime(schedule.endHour, schedule.endMinute)

            binding.activeTimeText.text = "$startTime a $endTime"

            val nextEvent = schedule.getNextScheduledEvent()
            val nextEventTime = if (nextEvent != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormat.format(nextEvent.time)
            } else {
                getString(R.string.no_scheduled)
            }

            binding.nextUpdateText.text = nextEventTime
            binding.nextScheduleInfoText.text = "Próxima actualización: $nextEventTime"
        } else {
            binding.activeTimeText.text = "No programado"
            binding.nextUpdateText.text = "No programado"
            binding.nextScheduleInfoText.text = ""
        }

        // Update day chips state
        updateDaysChips()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun updateSwitchWithoutTrigger(checked: Boolean) {
        if (binding.powerSwitch == null) return

        val currentState = binding.powerSwitch.isChecked

        if (currentState != checked) {
            try {
                binding.powerSwitch.setOnCheckedChangeListener(null)
                binding.powerSwitch.isChecked = checked
                setupPowerSwitch()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in updateSwitchWithoutTrigger: ${e.message}")
            }
        }
    }

    fun updateScheduleInfo() {
        homeViewModel.loadSchedule()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
