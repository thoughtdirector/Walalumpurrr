package com.example.notificacionesapp.fragments

import android.app.DatePickerDialog
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
import com.example.notificacionesapp.R
import java.util.*

class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var viewFlipper: ViewFlipper
    private var isLoginMode = true
    private val database = Firebase.database.reference

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
        viewFlipper = binding.viewFlipper

        viewFlipper.displayedChild = 0

        binding.loginButton.setOnClickListener {
            login()
        }

        binding.registerButton.setOnClickListener {
            register()
        }

        binding.registerPrompt.setOnClickListener {
            toggleMode()
        }

        binding.forgotPasswordButton.setOnClickListener {
            forgotPassword()
        }

        // Date Picker
        binding.birthDateEditText.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDayOfMonth)
                binding.birthDateEditText.setText("$selectedYear-$formattedMonth-$formattedDay")
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        if (isLoginMode) {
            viewFlipper.displayedChild = 0
            binding.registerPrompt.text = "¿No tienes una cuenta? Regístrate"
        } else {
            viewFlipper.displayedChild = 1
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
        val role = "admin"

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), "Registro exitoso.", Toast.LENGTH_SHORT).show()

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
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthException -> {
                            val errorCode = (task.exception as FirebaseAuthException).errorCode
                            when (errorCode) {
                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo electrónico ya está en uso."
                                "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
                                "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil."
                                else -> "Error de registro: ${task.exception?.message}"
                            }
                        }
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
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                    val direction = R.id.action_accountFragment_to_homeFragment
                    findNavController().navigate(direction)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthException -> {
                            val errorCode = (task.exception as FirebaseAuthException).errorCode
                            when (errorCode) {
                                "ERROR_INVALID_CREDENTIAL" -> "Correo electrónico o contraseña incorrectos."
                                "ERROR_USER_NOT_FOUND" -> "No hay ningún usuario registrado con este correo electrónico."
                                else -> "Error de inicio de sesión: ${task.exception?.message}"
                            }
                        }
                        else -> "Error de inicio de sesión: ${task.exception?.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun forgotPassword() {
        val email = binding.loginEmailEditText.text.toString()

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