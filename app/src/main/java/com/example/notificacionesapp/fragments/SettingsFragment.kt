package com.example.notificacionesapp.fragments

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
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
import java.text.NumberFormat
import java.util.Locale

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

        loadSettings()

        // Formatear monto con puntos cada 3 dígitos mientras el usuario escribe
        binding.amountInput.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val raw = s?.toString()?.replace(Regex("[^0-9]"), "") ?: ""
                if (raw.isNotEmpty()) {
                    try {
                        val formatted = NumberFormat.getIntegerInstance(Locale("es", "CO")).format(raw.toLong())
                        binding.amountInput.removeTextChangedListener(this)
                        binding.amountInput.setText(formatted)
                        binding.amountInput.setSelection(formatted.length)
                        binding.amountInput.addTextChangedListener(this)
                    } catch (_: Exception) {}
                }
                isEditing = false
            }
        })

        binding.saveSettingsButton.setOnClickListener {
            saveSettings()
        }

        binding.testServiceButton.setOnClickListener {
            testService()
        }
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

            // Cargar mute local
            binding.muteLocalSwitch.isChecked = appPrefs.getBoolean("mute_local_tts", false)

            // Cargar configuración de montos (con manejo seguro de tipos)
            try {
                binding.amountLimitSwitch.isChecked = amountSettings.isAmountLimitEnabled()
                val threshold = amountSettings.getAmountThreshold()
                val formatted = NumberFormat.getIntegerInstance(Locale("es", "CO")).format(threshold)
                binding.amountInput.setText(formatted)
            } catch (e: Exception) {
                binding.amountLimitSwitch.isChecked = false
                binding.amountInput.setText("100.000")
                Log.w("SettingsFragment", "Error al cargar amount settings: ${e.message}")
            }
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
                putBoolean("mute_local_tts", binding.muteLocalSwitch.isChecked)
            }.commit()  // commit() síncrono para que el servicio lea los valores actualizados

            // Guardar configuración de montos
            amountSettings.setAmountLimitEnabled(binding.amountLimitSwitch.isChecked)
            try {
                val rawText = binding.amountInput.text?.toString()?.replace(Regex("[^0-9]"), "") ?: ""
                val threshold = if (rawText.isNotEmpty()) rawText.toLong().coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                               else 100000
                amountSettings.setAmountThreshold(threshold)
            } catch (e: Exception) {
                Log.w("SettingsFragment", "Error al parsear monto: ${e.message}")
                amountSettings.setAmountThreshold(100000)
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