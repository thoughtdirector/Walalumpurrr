package com.example.notificacionesapp.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class CurrencyTextWatcher(private val editText: EditText) : TextWatcher {

    private var current = ""
    private val formatter: NumberFormat = DecimalFormat("#,###").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = '.' }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // No necesitamos hacer nada aquí
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // No necesitamos hacer nada aquí
    }

    override fun afterTextChanged(editable: Editable?) {
        if (editable.toString() != current) {
            editText.removeTextChangedListener(this)

            // Limpiar la cadena de cualquier carácter que no sea un dígito
            val cleanString = editable.toString().replace("[^\\d]".toRegex(), "")

            // Formatear la cadena si no está vacía
            if (cleanString.isNotEmpty()) {
                try {
                    val parsed = cleanString.toLong()
                    val formatted = formatter.format(parsed)

                    current = formatted
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                } catch (e: NumberFormatException) {
                    // Si hay un error, mantener el texto anterior
                    editText.setText(current)
                    editText.setSelection(current.length)
                }
            } else {
                current = ""
            }

            editText.addTextChangedListener(this)
        }
    }

    /**
     * Obtiene el valor numérico sin formato del EditText
     */
    fun getNumericValue(): Long {
        return try {
            val cleanString = editText.text.toString().replace("[^\\d]".toRegex(), "")
            if (cleanString.isNotEmpty()) cleanString.toLong() else 0L
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /**
     * Establece un valor numérico y lo formatea automáticamente
     */
    fun setNumericValue(value: Long) {
        current = formatter.format(value)
        editText.setText(current)
        editText.setSelection(current.length)
    }

    companion object {
        /**
         * Método estático para convertir una cadena formateada a número
         */
        fun parseFormattedValue(formattedValue: String): Long {
            return try {
                val cleanString = formattedValue.replace("[^\\d]".toRegex(), "")
                if (cleanString.isNotEmpty()) cleanString.toLong() else 0L
            } catch (e: NumberFormatException) {
                0L
            }
        }

        /**
         * Método estático para formatear un número con separadores de miles
         */
        fun formatValue(value: Long): String {
            val formatter = DecimalFormat("#,###").apply {
                decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = '.' }
            }
            return formatter.format(value)
        }

        /**
         * Método para formatear valores de texto que vienen de notificaciones
         * Ejemplo: "123456" -> "123.456"
         */
        fun formatNotificationAmount(amount: String?): String {
            if (amount.isNullOrBlank()) return "0"

            return try {
                val cleanString = amount.replace("[^\\d]".toRegex(), "")
                if (cleanString.isNotEmpty()) {
                    val value = cleanString.toLong()
                    formatValue(value)
                } else {
                    "0"
                }
            } catch (e: Exception) {
                amount
            }
        }
    }
}