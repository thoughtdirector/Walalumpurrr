package com.example.notificacionesapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.notificacionesapp.databinding.ActivityThemeBinding

class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isDarkMode = intent.getBooleanExtra("dark_mode", false)
        
        // Aplicar el tema
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Enviar resultado
        val resultIntent = Intent()
        resultIntent.putExtra("theme_applied", true)
        setResult(Activity.RESULT_OK, resultIntent)
        
        Toast.makeText(this, "Tema aplicado correctamente", Toast.LENGTH_SHORT).show()
        
        // Cerrar la actividad después de un breve delay
        binding.root.postDelayed({
            finish()
        }, 1000)
    }
}