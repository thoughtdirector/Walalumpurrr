package com.example.notificacionesapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notificacionesapp.util.AmountSettings
import com.example.notificacionesapp.util.CurrencyTextWatcher
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class AmountSettingsActivity : AppCompatActivity() {

    private lateinit var amountLimitSwitch: SwitchMaterial
    private lateinit var amountInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var amountSettings: AmountSettings
    private lateinit var currencyTextWatcher: CurrencyTextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amount_settings)

        amountSettings = AmountSettings(this)

        initViews()
        setupCurrencyInput()
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

    private fun setupCurrencyInput() {
        // Configurar el TextWatcher para formatear en tiempo real
        currencyTextWatcher = CurrencyTextWatcher(amountInput)
        amountInput.addTextChangedListener(currencyTextWatcher)

        // Configurar hint con formato de ejemplo
        amountInput.hint = "Ej: 100.000"
    }

    private fun loadSettings() {
        // Cargar el estado del switch
        amountLimitSwitch.isChecked = amountSettings.isAmountLimitEnabled()

        // Cargar y formatear el valor del límite
        val currentThreshold = amountSettings.getAmountThreshold()
        currencyTextWatcher.setNumericValue(currentThreshold)

        // Opcional: mostrar información del límite actual
        if (amountSettings.isAmountLimitEnabled()) {
            Toast.makeText(this, amountSettings.getLimitInfo(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        try {
            val isEnabled = amountLimitSwitch.isChecked

            // Obtener el valor numérico del campo de entrada
            val threshold = currencyTextWatcher.getNumericValue()

            // Validar que el threshold sea mayor a 0 si está habilitado
            if (isEnabled && threshold <= 0) {
                Toast.makeText(this, "Por favor, ingrese un monto válido mayor a 0", Toast.LENGTH_SHORT).show()
                return
            }

            // Guardar la configuración
            amountSettings.setAmountLimitEnabled(isEnabled)
            amountSettings.setAmountThreshold(threshold)

            // Mostrar mensaje de confirmación
            val message = if (isEnabled) {
                "Límite activado: ${CurrencyTextWatcher.formatValue(threshold)} pesos"
            } else {
                "Límite de monto desactivado"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el text watcher para evitar memory leaks
        amountInput.removeTextChangedListener(currencyTextWatcher)
    }
}