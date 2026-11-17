package com.example.notificacionesapp.util

import android.text.Editable
import android.text.TextWatcher
import java.text.NumberFormat
import java.util.Locale

/**
 * Formatea un número con separadores de miles mientras el usuario escribe.
 * Ejemplo: 100000 → 100.000
 * Guarda el valor numérico limpio en SharedPreferences; solo la vista está formateada.
 */
class CurrencyTextWatcher : TextWatcher {

    private var isFormatting = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting || s == null || s.length == 0) return
        isFormatting = true

        val raw = s.filter { it.isDigit() }
        if (raw.length > 0) {
            val value = raw.toString().toLong()
            val formatted = NumberFormat.getInstance(Locale.getDefault()).format(value)
            s.clear()
            s.append(formatted)
        }
        isFormatting = false
    }
}
