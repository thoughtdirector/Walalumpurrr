package com.example.notificacionesapp.fragments

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.NotificationService
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentSettingsBinding
import com.example.notificacionesapp.util.AmountSettings
import com.example.notificacionesapp.util.CurrencyTextWatcher

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private lateinit var amountSettings: AmountSettings

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        amountSettings = AmountSettings(requireContext())

        // Configurar el TextWatcher para el campo de monto en el fragmento
        val currencyTextWatcher = CurrencyTextWatcher(binding.amountInput)
        binding.amountInput.addTextChangedListener(currencyTextWatcher)
        binding.amountInput.hint = "Ej: 100.000"

        // Cargar configuraciones guardadas
        loadSettings(currencyTextWatcher)

        // Configurar botones
        binding.saveSettingsButton.setOnClickListener {
            saveSettings(currencyTextWatcher)
        }

        binding.testServiceButton.setOnClickListener {
            testService()
        }
    }

    private fun loadSettings(currencyTextWatcher: CurrencyTextWatcher) {
        try {
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

            // Cargar configuración de apps
            binding.nequiSwitch.isChecked = appPrefs.getBoolean("app_nequi", true)
            binding.daviplataSwitch.isChecked = appPrefs.getBoolean("app_daviplata", true)
            binding.bancolombiaSwitch.isChecked = appPrefs.getBoolean("app_bancolombia", true)
            binding.whatsappSwitch.isChecked = appPrefs.getBoolean("app_whatsapp", true)

            // Cargar configuración de montos
            binding.amountLimitSwitch.isChecked = amountSettings.isAmountLimitEnabled()
            currencyTextWatcher.setNumericValue(amountSettings.getAmountThreshold())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al cargar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings(currencyTextWatcher: CurrencyTextWatcher) {
        try {
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

            // Guardar configuración de apps
            appPrefs.edit().apply {
                putBoolean("app_nequi", binding.nequiSwitch.isChecked)
                putBoolean("app_daviplata", binding.daviplataSwitch.isChecked)
                putBoolean("app_bancolombia", binding.bancolombiaSwitch.isChecked)
                putBoolean("app_whatsapp", binding.whatsappSwitch.isChecked)
            }.apply()

            // Guardar configuración de montos
            val isAmountLimitEnabled = binding.amountLimitSwitch.isChecked
            val threshold = currencyTextWatcher.getNumericValue()

            // Validar que el threshold sea mayor a 0 si está habilitado
            if (isAmountLimitEnabled && threshold <= 0) {
                Toast.makeText(requireContext(), "Por favor, ingrese un monto válido mayor a 0", Toast.LENGTH_SHORT).show()
                return
            }

            amountSettings.setAmountLimitEnabled(isAmountLimitEnabled)
            amountSettings.setAmountThreshold(threshold)

            // Notificar al servicio sobre los cambios
            val serviceIntent = Intent(requireContext(), NotificationService::class.java)
            serviceIntent.action = NotificationService.ACTION_UPDATE_APP_SETTINGS
            requireContext().startService(serviceIntent)

            // Mostrar mensaje de confirmación para la configuración de montos
            if (isAmountLimitEnabled) {
                val formattedAmount = CurrencyTextWatcher.formatValue(threshold)
                Toast.makeText(requireContext(), "Configuración guardada. Límite: $formattedAmount pesos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al guardar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testService() {
        if (NotificationService.isServiceActive) {
            Toast.makeText(requireContext(), getString(R.string.testing_voice_service), Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.tts?.speak(
                getString(R.string.service_working_correctly),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "test_id"
            )
        } else {
            Toast.makeText(requireContext(), getString(R.string.activate_service_first), Toast.LENGTH_SHORT).show()
        }
    }
}