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
import com.example.notificacionesapp.ThemeActivity
import com.example.notificacionesapp.databinding.FragmentSettingsBinding
import com.example.notificacionesapp.util.AmountSettings

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

        // Cargar configuraciones guardadas
        loadSettings()

        // Configurar botones
        binding.saveSettingsButton.setOnClickListener {
            saveSettings()
        }

        binding.testServiceButton.setOnClickListener {
            testService()
        }

        // El botón de crear empleado ya no está en este fragmento
        // Se ha movido al ProfileFragment
    }

    private fun loadSettings() {
        try {
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val themePrefs = requireContext().getSharedPreferences("theme_settings", Context.MODE_PRIVATE)

            // Cargar configuración de apps
            binding.nequiSwitch.isChecked = appPrefs.getBoolean("app_nequi", true)
            binding.daviplataSwitch.isChecked = appPrefs.getBoolean("app_daviplata", true)
            binding.bancolombiaSwitch.isChecked = appPrefs.getBoolean("app_bancolombia", true)
            binding.whatsappSwitch.isChecked = appPrefs.getBoolean("app_whatsapp", true)

            // Cargar configuración de tema
            binding.darkModeSwitch.isChecked = themePrefs.getBoolean("dark_mode", false)

            // Cargar configuración de montos
            binding.amountLimitSwitch.isChecked = amountSettings.isAmountLimitEnabled()
            binding.amountInput.setText(amountSettings.getAmountThreshold().toString())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al cargar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        try {
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val themePrefs = requireContext().getSharedPreferences("theme_settings", Context.MODE_PRIVATE)

            // Guardar configuración de apps
            appPrefs.edit().apply {
                putBoolean("app_nequi", binding.nequiSwitch.isChecked)
                putBoolean("app_daviplata", binding.daviplataSwitch.isChecked)
                putBoolean("app_bancolombia", binding.bancolombiaSwitch.isChecked)
                putBoolean("app_whatsapp", binding.whatsappSwitch.isChecked)
            }.apply()

            // Guardar configuración de montos
            amountSettings.setAmountLimitEnabled(binding.amountLimitSwitch.isChecked)
            try {
                val threshold = binding.amountInput.text.toString().toInt()
                amountSettings.setAmountThreshold(threshold)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error en el valor del monto. Usando valor predeterminado.", Toast.LENGTH_SHORT).show()
            }

            // Notificar al servicio sobre los cambios
            val serviceIntent = Intent(requireContext(), NotificationService::class.java)
            serviceIntent.action = NotificationService.ACTION_UPDATE_APP_SETTINGS
            requireContext().startService(serviceIntent)

            // Verificar si el tema ha cambiado
            val isDarkMode = binding.darkModeSwitch.isChecked
            val oldDarkMode = themePrefs.getBoolean("dark_mode", false)

            // Guardar la configuración del tema
            themePrefs.edit().putBoolean("dark_mode", isDarkMode).apply()

            if (oldDarkMode != isDarkMode) {
                // Usar la actividad puente para cambiar el tema de manera segura
                val themeIntent = Intent(requireContext(), ThemeActivity::class.java)
                themeIntent.putExtra("dark_mode", isDarkMode)
                startActivity(themeIntent)
                return // Salir del método para evitar mostrar el Toast aquí
            }

            Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
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