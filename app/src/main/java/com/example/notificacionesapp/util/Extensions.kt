package com.example.notificacionesapp.util

import android.widget.EditText
import android.widget.TextView

/**
 * Funciones de extensión para simplificar el formateo de montos en toda la aplicación
 */

/**
 * Formatea un número como moneda colombiana con separador de miles (punto)
 * Ejemplo: 1234567 -> "1.234.567"
 */
fun Long.toFormattedCurrency(): String {
    return CurrencyTextWatcher.formatValue(this)
}

/**
 * Formatea un string de número como moneda colombiana
 * Ejemplo: "1234567" -> "1.234.567"
 */
fun String.toFormattedCurrency(): String {
    return CurrencyTextWatcher.formatNotificationAmount(this)
}

/**
 * Parsea un string formateado a número
 * Ejemplo: "1.234.567" -> 1234567
 */
fun String.parseFormattedCurrency(): Long {
    return CurrencyTextWatcher.parseFormattedValue(this)
}

/**
 * Establece un TextWatcher de moneda a un EditText
 */
fun EditText.setupCurrencyWatcher(): CurrencyTextWatcher {
    val watcher = CurrencyTextWatcher(this)
    this.addTextChangedListener(watcher)
    this.hint = "Ej: 100.000"
    return watcher
}

/**
 * Muestra un monto formateado en un TextView
 */
fun TextView.setFormattedAmount(amount: Long) {
    this.text = amount.toFormattedCurrency()
}

/**
 * Muestra un monto formateado en un TextView desde un String
 */
fun TextView.setFormattedAmount(amount: String) {
    this.text = amount.toFormattedCurrency()
}
