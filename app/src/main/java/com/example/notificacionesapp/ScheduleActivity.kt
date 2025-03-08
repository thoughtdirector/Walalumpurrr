package com.example.notificacionesapp

import android.os.Bundle
import android.widget.CheckBox
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class ScheduleActivity : AppCompatActivity() {

    private lateinit var scheduleSwitch: SwitchMaterial
    private lateinit var startTimePicker: TimePicker
    private lateinit var endTimePicker: TimePicker
    private lateinit var saveButton: MaterialButton

    private lateinit var mondayCheckBox: CheckBox
    private lateinit var tuesdayCheckBox: CheckBox
    private lateinit var wednesdayCheckBox: CheckBox
    private lateinit var thursdayCheckBox: CheckBox
    private lateinit var fridayCheckBox: CheckBox
    private lateinit var saturdayCheckBox: CheckBox
    private lateinit var sundayCheckBox: CheckBox

    private lateinit var scheduleManager: ScheduleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        scheduleManager = ScheduleManager(this)

        initViews()
        loadSavedSchedule()

        saveButton.setOnClickListener {
            saveSchedule()
        }
    }

    private fun initViews() {
        scheduleSwitch = findViewById(R.id.scheduleSwitch)
        startTimePicker = findViewById(R.id.startTimePicker)
        endTimePicker = findViewById(R.id.endTimePicker)
        saveButton = findViewById(R.id.saveButton)

        mondayCheckBox = findViewById(R.id.mondayCheckBox)
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox)
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox)
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox)
        fridayCheckBox = findViewById(R.id.fridayCheckBox)
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox)
        sundayCheckBox = findViewById(R.id.sundayCheckBox)

        // Configurar TimePickers para formato de 24 horas
        startTimePicker.setIs24HourView(true)
        endTimePicker.setIs24HourView(true)
    }

    private fun loadSavedSchedule() {
        scheduleSwitch.isChecked = scheduleManager.isScheduleEnabled()

        // Obtener la hora de inicio y fin
        startTimePicker.hour = scheduleManager.getStartHour()
        startTimePicker.minute = scheduleManager.getStartMinute()

        endTimePicker.hour = scheduleManager.getEndHour()
        endTimePicker.minute = scheduleManager.getEndMinute()

        // Cargar estado de los días
        mondayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.MONDAY)
        tuesdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.TUESDAY)
        wednesdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.WEDNESDAY)
        thursdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.THURSDAY)
        fridayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.FRIDAY)
        saturdayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.SATURDAY)
        sundayCheckBox.isChecked = scheduleManager.isDayEnabled(Calendar.SUNDAY)
    }

    private fun saveSchedule() {
        val isEnabled = scheduleSwitch.isChecked

        val startHour = startTimePicker.hour
        val startMinute = startTimePicker.minute

        val endHour = endTimePicker.hour
        val endMinute = endTimePicker.minute

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
            mondayCheckBox.isChecked,
            tuesdayCheckBox.isChecked,
            wednesdayCheckBox.isChecked,
            thursdayCheckBox.isChecked,
            fridayCheckBox.isChecked,
            saturdayCheckBox.isChecked,
            sundayCheckBox.isChecked
        )

        // Mostrar mensaje de confirmación
        val message = if (isEnabled) {
            "Horario guardado: Activo de $startHour:$startMinute a $endHour:$endMinute"
        } else {
            "Horario programado desactivado"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
