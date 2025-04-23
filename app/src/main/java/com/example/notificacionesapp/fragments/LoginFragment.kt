package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentLoginBinding
import com.example.notificacionesapp.firebase.FirebaseManager

class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private lateinit var firebaseManager: FirebaseManager

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseManager = FirebaseManager()

        // Verificar si ya hay sesión
        if (firebaseManager.currentUser != null) {
            navigateToMain()
        }
    }

    override fun setupUI() {
        // Configurar botón de login
        binding.loginButton.setOnClickListener {
            loginUser()
        }

        // Configurar enlace a registro
        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun loginUser() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        // Validaciones
        if (email.isEmpty() || password.isEmpty()) {
            showError("Por favor completa todos los campos")
            return
        }

        // Mostrar progreso
        binding.progressBar.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        // Login
        firebaseManager.loginUser(email, password, object : FirebaseManager.AuthListener {
            override fun onSuccess() {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    navigateToMain()
                }
            }

            override fun onError(message: String) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    showError(message)
                }
            }
        })
    }

    private fun navigateToMain() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}