package com.example.notificacionesapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notificacionesapp.util.AmountSettings
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class AmountSettingsActivity : AppCompatActivity() {

    private lateinit var amountLimitSwitch: SwitchMaterial
    private lateinit var amountInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var amountSettings: AmountSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amount_settings)

        amountSettings = AmountSettings(this)

        initViews()
        loadSettings()

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun initViews() {
        amountLimitSwitch = findViewById(R.id.amountLimitSwitch)
        amountInput = findViewById(R.id.amountInput)
        saveButton = findViewById(R.id.saveAmountButton)
    }

    private fun loadSettings() {
        amountLimitSwitch.isChecked = amountSettings.isAmountLimitEnabled()
        amountInput.setText(amountSettings.getAmountThreshold().toString())
    }

    private fun saveSettings() {
        try {
            val isEnabled = amountLimitSwitch.isChecked
            val threshold = amountInput.text.toString().toInt()

            amountSettings.setAmountLimitEnabled(isEnabled)
            amountSettings.setAmountThreshold(threshold)

            val message = if (isEnabled) {
                "Límite activado: $threshold pesos"
            } else {
                "Límite de monto desactivado"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}