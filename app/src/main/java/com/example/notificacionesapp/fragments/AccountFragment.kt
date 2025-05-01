package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.navigation.fragment.findNavController
import com.example.notificacionesapp.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.navigation.NavDirections
import com.example.notificacionesapp.R

class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var viewFlipper: ViewFlipper
    private var isLoginMode = true // Estado para alternar entre inicio de sesión y registro
    private val database = Firebase.database.reference // Initialize Realtime Database

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccountBinding {
        return FragmentAccountBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun setupUI() {
        // Inicializar ViewFlipper
        viewFlipper = binding.viewFlipper

        binding.loginButton.setOnClickListener {
            if (isLoginMode) {
                login()
            } else {
                register()
            }
        }

        binding.registerPrompt.setOnClickListener {
            toggleMode()
        }

        binding.forgotPasswordButton.setOnClickListener {
            forgotPassword()
        }

        // Mostrar el formulario de inicio de sesión inicialmente
        viewFlipper.showPrevious()
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        if (isLoginMode) {
            viewFlipper.showPrevious() // Show login form
            binding.loginButton.text = "INICIAR SESIÓN"
            binding.registerPrompt.text = "¿No tienes una cuenta? Regístrate"
        } else {
            viewFlipper.showNext() // Show register form
            binding.loginButton.text = "REGISTRARSE"
            binding.registerPrompt.text = "¿Ya tienes una cuenta? Inicia sesión"
        }
    }

    private fun register() {
        val email = binding.registerEmailEditText.text.toString()
        val password = binding.registerPasswordEditText.text.toString()
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val phone = binding.phoneEditText.text.toString()
        val birthDate = binding.birthDateEditText.text.toString()
        val role = "admin" // Set role to "admin"

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), "Registro exitoso.", Toast.LENGTH_SHORT).show()

                    // Write user data to Realtime Database
                    user?.uid?.let { uid ->
                        val userData = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "phone" to phone,
                            "birthDate" to birthDate,
                            "role" to role
                        )
                        database.child("users").child(uid).setValue(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "User data written to database")
                                val direction = R.id.action_accountFragment_to_homeFragment
                                findNavController().navigate(direction)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error writing user data to database", e)
                                Toast.makeText(requireContext(), "Error al guardar los datos del usuario.", Toast.LENGTH_SHORT).show()
                                val direction = R.id.action_accountFragment_to_homeFragment
                                findNavController().navigate(direction)
                            }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    val errorCode = (task.exception as FirebaseAuthException).errorCode
                    val errorMessage = when (errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo electrónico ya está en uso."
                        "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
                        "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil."
                        else -> "Error de registro: ${task.exception?.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun login() {
        val email = binding.loginEmailEditText.text.toString()
        val password = binding.loginPasswordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Correo electrónico y contraseña son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                    val direction = R.id.action_accountFragment_to_homeFragment
                    findNavController().navigate(direction)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    val errorCode = (task.exception as FirebaseAuthException).errorCode
                    val errorMessage = when (errorCode) {
                        "ERROR_INVALID_CREDENTIAL" -> "Correo electrónico o contraseña incorrectos."
                        "ERROR_USER_NOT_FOUND" -> "No hay ningún usuario registrado con este correo electrónico."
                        else -> "Error de inicio de sesión: ${task.exception?.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun forgotPassword() {
        val email = binding.loginEmailEditText.text.toString() // Usar el email de inicio de sesión

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Correo electrónico es requerido", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Correo electrónico de restablecimiento de contraseña enviado.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al enviar correo electrónico de restablecimiento: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val TAG = "AccountFragment"
    }
}