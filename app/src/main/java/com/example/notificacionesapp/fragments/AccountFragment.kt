package com.example.notificacionesapp.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.notificacionesapp.databinding.FragmentAccountBinding

class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccountBinding {
        return FragmentAccountBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Configurar los botones de la pantalla de cuenta
        binding.loginButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "La funcionalidad de cuenta estará disponible próximamente",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.registerPrompt.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "El registro de usuarios estará disponible próximamente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}