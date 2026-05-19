package com.example.notificacionesapp.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.core.auth.AuthManager
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.databinding.FragmentAccountBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    @Inject lateinit var authManager: AuthManager

    private lateinit var viewFlipper: ViewFlipper
    private var isLoginMode = true

    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(requireContext(), "Error en inicio de sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccountBinding {
        return FragmentAccountBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun setupUI() {
        viewFlipper = binding.viewFlipper
        viewFlipper.displayedChild = 0

        binding.loginButton.setOnClickListener { login() }
        binding.registerButton.setOnClickListener { register() }
        binding.registerPrompt.setOnClickListener { toggleMode() }
        binding.forgotPasswordButton.setOnClickListener { forgotPassword() }

        binding.birthDateEditText.setOnClickListener { showDatePickerDialog() }
        binding.birthDateInputLayout.setEndIconOnClickListener { showDatePickerDialog() }
        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val formattedMonth = String.format(Locale.getDefault(), "%02d", selectedMonth + 1)
                val formattedDay = String.format(Locale.getDefault(), "%02d", selectedDayOfMonth)
                binding.birthDateEditText.setText("$selectedYear-$formattedMonth-$formattedDay")
            },
            year, month, day
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

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
            || phone.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            when (val result = authManager.signUpWithEmailAndPassword(
                email, password, firstName, lastName, phone, birthDate, "admin"
            )) {
                is Result.Success -> {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = authManager.currentUser.value
                    if (user != null) {
                        Toast.makeText(requireContext(), "Registro exitoso.", Toast.LENGTH_SHORT).show()
                        navigateToHome(user.id, email, "admin")
                    }
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "Error de registro"
                    Log.w(TAG, "createUserWithEmail:failure", result.exception)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
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

        lifecycleScope.launch {
            when (val result = authManager.signInWithEmailAndPassword(email, password)) {
                is Result.Success -> {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = authManager.currentUser.value
                    if (user != null) {
                        Toast.makeText(requireContext(), "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                        navigateToHome(user.id, email, user.role.name.lowercase(), user.adminId)
                    }
                }
                is Result.Error -> {
                    val msg = result.exception.message ?: "Error de inicio de sesión"
                    Log.w(TAG, "signInWithEmail:failure", result.exception)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun forgotPassword() {
        val email = binding.loginEmailEditText.text.toString()

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Correo electrónico es requerido", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            when (val result = authManager.resetPassword(email)) {
                is Result.Success -> {
                    Toast.makeText(requireContext(),
                        "Correo electrónico de restablecimiento de contraseña enviado.", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error al enviar correo: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "Google sign in: ${account.id}")
        val idToken = account.idToken ?: return

        lifecycleScope.launch {
            when (val result = authManager.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    Log.d(TAG, "signInWithGoogle:success")
                    val user = authManager.currentUser.value
                    if (user != null) {
                        Toast.makeText(requireContext(), "Inicio de sesión con Google exitoso.", Toast.LENGTH_SHORT).show()
                        navigateToHome(user.id, user.email, user.role.name.lowercase(), user.adminId)
                    }
                }
                is Result.Error -> {
                    Log.w(TAG, "signInWithGoogle:failure", result.exception)
                    Toast.makeText(requireContext(),
                        "Error de autenticación con Google: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun navigateToHome(userId: String, email: String, role: String, adminId: String? = null) {
        val mainActivity = activity as? MainActivity
        mainActivity?.createUserSession(userId, email, role, adminId)
        mainActivity?.homeFragment = HomeFragment()
        mainActivity?.homeFragment?.let { fragment ->
            mainActivity?.loadFragment(fragment)
        }
        mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
    }

    companion object {
        private const val TAG = "AccountFragment"
    }
}
