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

    fun getAmountThreshold(): Long {
        return prefs.getLong(KEY_AMOUNT_THRESHOLD, DEFAULT_AMOUNT_THRESHOLD.toLong())
    }

    fun setAmountThreshold(threshold: Long) {
        prefs.edit().putLong(KEY_AMOUNT_THRESHOLD, threshold).apply()
    }

    /**
     * Obtiene el monto límite formateado con separadores de miles
     */
    fun getFormattedAmountThreshold(): String {
        return CurrencyTextWatcher.formatValue(getAmountThreshold())
    }

    /**
     * Verifica si el monto debe ser leído según el límite configurado
     * @param amount El monto como cadena (puede venir de notificaciones con diferentes formatos)
     * @return true si debe leerse, false si supera el límite
     */
    fun shouldReadAmount(amount: String?): Boolean {
        if (!isAmountLimitEnabled() || amount.isNullOrBlank()) {
            return true // Si no hay límite activado o no hay monto, leer siempre
        }

        try {
            // Usar el método de CurrencyTextWatcher para parsear el monto
            val numericAmount = CurrencyTextWatcher.parseFormattedValue(amount)
            val threshold = getAmountThreshold()

            // Log para debugging (opcional)
            android.util.Log.d("AmountSettings",
                "Comparando: $numericAmount <= $threshold = ${numericAmount <= threshold}")

            return numericAmount <= threshold
        } catch (e: Exception) {
            // Si hay algún error al convertir, permitir la lectura por seguridad
            android.util.Log.e("AmountSettings", "Error parsing amount: $amount", e)
            return true
        }
    }

    /**
     * Verifica si el monto debe ser leído (versión que acepta Long directamente)
     * @param amount El monto como número
     * @return true si debe leerse, false si supera el límite
     */
    fun shouldReadAmount(amount: Long): Boolean {
        if (!isAmountLimitEnabled()) {
            return true
        }
        return amount <= getAmountThreshold()
    }

    /**
     * Obtiene información del límite actual para mostrar en la UI
     */
    fun getLimitInfo(): String {
        return if (isAmountLimitEnabled()) {
            "Límite activo: ${getFormattedAmountThreshold()} pesos"
        } else {
            "Sin límite de monto"
        }
    }
}