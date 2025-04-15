package com.example.notificacionesapp.util

import android.content.Context
import android.content.SharedPreferences

class AmountSettings(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "amount_settings"
        private const val KEY_AMOUNT_LIMIT_ENABLED = "amount_limit_enabled"
        private const val KEY_AMOUNT_THRESHOLD = "amount_threshold"
        private const val DEFAULT_AMOUNT_THRESHOLD = 100000 // Por defecto, 100,000 pesos
    }

    fun isAmountLimitEnabled(): Boolean {
        return prefs.getBoolean(KEY_AMOUNT_LIMIT_ENABLED, false)
    }

    fun setAmountLimitEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOUNT_LIMIT_ENABLED, enabled).apply()
    }

    fun getAmountThreshold(): Int {
        return prefs.getInt(KEY_AMOUNT_THRESHOLD, DEFAULT_AMOUNT_THRESHOLD)
    }

    fun setAmountThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_AMOUNT_THRESHOLD, threshold).apply()
    }

    fun shouldReadAmount(amount: String?): Boolean {
        if (!isAmountLimitEnabled() || amount.isNullOrBlank()) {
            return true // Si no hay límite activado o no hay monto, leer siempre
        }

        try {
            // Convertir el monto a un número
            val numericAmount = amount.replace("[^0-9]".toRegex(), "").toInt()
            return numericAmount <= getAmountThreshold()
        } catch (e: Exception) {
            // Si hay algún error al convertir, permitir la lectura por seguridad
            return true
        }
    }
}