// app/src/main/java/com/example/notificacionesapp/fragments/StatsFragment.kt
package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentStatsBinding

class StatsFragment : BaseFragment<FragmentStatsBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStatsBinding {
        return FragmentStatsBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Mostrar mensaje de funcionalidad en desarrollo
        Toast.makeText(requireContext(), "Módulo de estadísticas en desarrollo", Toast.LENGTH_SHORT).show()
    }
}