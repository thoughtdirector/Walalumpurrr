package com.example.notificacionesapp.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentSettingsLiteBinding
import com.example.notificacionesapp.util.AmountSettings
import com.example.notificacionesapp.util.CurrencyTextWatcher
import java.text.NumberFormat
import java.util.Locale

class SettingsFragmentLite : BaseFragment<FragmentSettingsLiteBinding>() {

    private var amountSettings: AmountSettings? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsLiteBinding {
        return FragmentSettingsLiteBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        amountSettings = AmountSettings(requireContext())

        loadSettings()

        // Aplicar formateador de miles al campo de monto
        binding.inputAmountThreshold.addTextChangedListener(CurrencyTextWatcher())

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        try {
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

            // Si no existe configuración previa, establecer valores por defecto:
            // Nequi y DaviPlata encendidos, filtro de monto apagado.
            // Se guarda inmediatamente (commit síncrono) para que persistan
            // desde la primera instalación y no queden solo "en apariencia".
            if (!appPrefs.contains("app_nequi") || !appPrefs.contains("app_daviplata")) {
                appPrefs.edit().apply {
                    putBoolean("app_nequi", true)
                    putBoolean("app_daviplata", true)
                }.commit()
            }

            // Bancos activos (por defecto encendidos)
            binding.checkNequi.isChecked = appPrefs.getBoolean("app_nequi", true)
            binding.checkDaviPlata.isChecked = appPrefs.getBoolean("app_daviplata", true)

            // Filtro por monto (por defecto apagado)
            binding.checkAmountLimit.isChecked = amountSettings?.isAmountLimitEnabled() ?: false
            val threshold = amountSettings?.getAmountThreshold() ?: 100000
            val formatted = NumberFormat.getInstance(Locale.getDefault()).format(threshold)
            binding.inputAmountThreshold.setText(formatted)
        } catch (e: Exception) {
            Log.w("SettingsFragmentLite", "Error al cargar configuración: ${e.message}")
            Toast.makeText(requireContext(), "Error al cargar configuración", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        try {
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

            // Guardar bancos activos
            appPrefs.edit().apply {
                putBoolean("app_nequi", binding.checkNequi.isChecked)
                putBoolean("app_daviplata", binding.checkDaviPlata.isChecked)
            }.commit()

            // Guardar filtro por monto
            amountSettings?.setAmountLimitEnabled(binding.checkAmountLimit.isChecked)

            val rawText = binding.inputAmountThreshold.text?.toString()?.replace(Regex("[^0-9]"), "") ?: ""
            val threshold = if (rawText.isNotEmpty()) rawText.toLong().coerceIn(1, Int.MAX_VALUE.toLong()).toInt() else 100000
            amountSettings?.setAmountThreshold(threshold)

            Toast.makeText(requireContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SettingsFragmentLite", "Error al guardar configuración: ${e.message}")
            Toast.makeText(requireContext(), "Error al guardar configuración", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): SettingsFragmentLite {
            return SettingsFragmentLite()
        }
    }
}
