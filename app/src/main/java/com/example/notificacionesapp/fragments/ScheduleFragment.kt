package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TimePicker
import android.widget.Toast
import com.example.notificacionesapp.R
import com.example.notificacionesapp.ScheduleManager
import com.example.notificacionesapp.databinding.FragmentScheduleBinding
import java.util.Calendar

class ScheduleFragment : BaseFragment<FragmentScheduleBinding>() {

    private lateinit var scheduleManager: ScheduleManager

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentScheduleBinding {
        return FragmentScheduleBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        scheduleManager = ScheduleManager(requireContext())

        // Configurar TimePickers para formato de 24 horas
        binding.startTimePicker.setIs24HourView(true)
        binding.endTimePicker.setIs24HourView(true)

        // Cargar configuración guardada
        loadSavedSchedule()

        // Configurar botón de guardar
        binding.saveButton.setOnClickListener {
            saveSchedule()
        }
    }

    private fun loadSavedSchedule() {
        binding.scheduleSwitch.isChecked = scheduleManager.isScheduleEnabled()

        // Obtener la hora de inicio y fin
        binding.startTimePicker.hour = scheduleManager.getStartHour()
        binding.startTimePicker.minute = scheduleManager.getStartMinute()

        binding.endTimePicker.hour = scheduleManager.getEndHour()
        binding.endTimePicker.minute = scheduleManager.getEndMinute()

        // Cargar estado de los días
        binding.mondayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.MONDAY)
        binding.tuesdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.TUESDAY)
        binding.wednesdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.WEDNESDAY)
        binding.thursdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.THURSDAY)
        binding.fridayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.FRIDAY)
        binding.saturdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.SATURDAY)
        binding.sundayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.SUNDAY)
    }

    private fun saveSchedule() {
        val isEnabled = binding.scheduleSwitch.isChecked

        val startHour = binding.startTimePicker.hour
        val startMinute = binding.startTimePicker.minute

        val endHour = binding.endTimePicker.hour
        val endMinute = binding.endTimePicker.minute

        // Guardar configuración de horario
        scheduleManager.saveScheduleSettings(
            isEnabled,
            startHour,
            startMinute,
            endHour,
            endMinute
        )

        // Guardar configuración de días
        scheduleManager.saveDaySettings(
            binding.mondayCheckBox.isChecked,
            binding.tuesdayCheckBox.isChecked,
            binding.wednesdayCheckBox.isChecked,
            binding.thursdayCheckBox.isChecked,
            binding.fridayCheckBox.isChecked,
            binding.saturdayCheckBox.isChecked,
            binding.sundayCheckBox.isChecked
        )

        // Mostrar mensaje de confirmación
        val message = if (isEnabled) {
            "Horario guardado: Activo de $startHour:$startMinute a $endHour:$endMinute"
        } else {
            "Horario programado desactivado"
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}