// app/src/main/java/com/example/notificacionesapp/fragments/RegisterFragment.kt
package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentRegisterBinding
import com.example.notificacionesapp.firebase.FirebaseManager

class RegisterFragment : BaseFragment<FragmentRegisterBinding>() {

    private lateinit var firebaseManager: FirebaseManager

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRegisterBinding {
        return FragmentRegisterBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseManager = FirebaseManager()
    }

    override fun setupUI() {
        // Configurar spinner de roles
        val roles = arrayOf("Administrador", "Empleado")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.roleSpinner.adapter = adapter

        // Manejar visibilidad de campo de admin ID
        binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val isEmployee = position == 1 // "Empleado" en posición 1
                binding.adminIdLayout.visibility = if (isEmployee) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }

        // Configurar botón de registro
        binding.registerButton.setOnClickListener {
            registerUser()
        }

        // Configurar enlace a login
        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun registerUser() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        val isAdmin = binding.roleSpinner.selectedItemPosition == 0
        val role = if (isAdmin) "admin" else "employee"
        val adminId = binding.adminIdInput.text.toString().trim()

        // Validaciones
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Por favor completa todos los campos")
            return
        }

        if (password != confirmPassword) {
            showError("Las contraseñas no coinciden")
            return
        }

        if (!isAdmin && adminId.isEmpty()) {
            showError("Por favor ingresa el ID del administrador")
            return
        }

        // Mostrar progreso
        binding.progressBar.visibility = View.VISIBLE
        binding.registerButton.isEnabled = false

        // Registrar usuario
        firebaseManager.registerUser(email, password, name, role, object : FirebaseManager.AuthListener {
            override fun onSuccess() {
                if (!isAdmin && adminId.isNotEmpty()) {
                    // Vincular empleado a admin
                    val employeeId = firebaseManager.currentUser?.uid ?: ""
                    firebaseManager.linkEmployeeToAdmin(employeeId, adminId, object : FirebaseManager.OperationListener {
                        override fun onSuccess() {
                            requireActivity().runOnUiThread {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            }
                        }

                        override fun onError(message: String) {
                            requireActivity().runOnUiThread {
                                binding.progressBar.visibility = View.GONE
                                binding.registerButton.isEnabled = true
                                showError("Error al vincular con administrador: $message")
                            }
                        }
                    })
                } else {
                    requireActivity().runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                }
            }

            override fun onError(message: String) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.registerButton.isEnabled = true
                    showError(message)
                }
            }
        })
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}