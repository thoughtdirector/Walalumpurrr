package com.example.notificacionesapp.fragments

import android.app.Activity
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
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentAccountBinding
import com.example.notificacionesapp.model.FirestoreUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var viewFlipper: ViewFlipper
    private var isLoginMode = true
    private val db = Firebase.firestore

    // Google Sign In
    private lateinit var googleSignInClient: GoogleSignInClient

    // Registro para manejar el resultado de la actividad de inicio de sesión de Google
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign In fue exitoso, autenticar con Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In falló
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
        auth = FirebaseAuth.getInstance()

        // Configurar Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
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

        // Añadir el botón de Google Sign In
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
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
        val role = "admin" // First user is admin

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
                        // Create user for Firestore
                        val firestoreUser = FirestoreUser(
                            uid = uid,
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            birthDate = birthDate,
                            email = email,
                            role = role
                        )

                        // Save to Firestore
                        db.collection("users")
                            .document(uid)
                            .set(firestoreUser)
                            .addOnSuccessListener {
                                Log.d(TAG, "User data written to Firestore")

                                // Create session
                                val mainActivity = activity as? MainActivity
                                mainActivity?.createUserSession(uid, email, role)

                                // Load Home fragment
                                mainActivity?.homeFragment = HomeFragment()
                                mainActivity?.loadFragment(mainActivity.homeFragment!!)
                                mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error writing user data to Firestore", e)
                                Toast.makeText(requireContext(), "Error al guardar los datos del usuario.", Toast.LENGTH_SHORT).show()
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

                    // Obtener el rol del usuario de Firestore y guardar sesión
                    user?.uid?.let { uid ->
                        db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener { document ->
                                val role = document.getString("role") ?: "user"

                                // Crear sesión local con SessionManager
                                val mainActivity = activity as? MainActivity
                                mainActivity?.createUserSession(uid, email, role)

                                // Cargar fragmento Home
                                mainActivity?.homeFragment = HomeFragment()
                                mainActivity?.loadFragment(mainActivity.homeFragment!!)
                                mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error obteniendo rol de usuario: ${e.message}")

                                // Si falla, usar un rol predeterminado
                                val mainActivity = activity as? MainActivity
                                mainActivity?.createUserSession(uid, email, "user")

                                // Cargar fragmento Home
                                mainActivity?.homeFragment = HomeFragment()
                                mainActivity?.loadFragment(mainActivity.homeFragment!!)
                                mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
                            }
                    }
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

    // Método para iniciar sesión con Google
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // Método para autenticar con Firebase usando la cuenta de Google
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), "Inicio de sesión con Google exitoso.", Toast.LENGTH_SHORT).show()

                    // Verificar si es un usuario nuevo (primera vez que inicia sesión con Google)
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    user?.uid?.let { uid ->
                        if (isNewUser) {
                            // Si es un usuario nuevo, guardar datos básicos en Firestore
                            val firestoreUser = FirestoreUser(
                                uid = uid,
                                firstName = (user.displayName?.split(" ")?.firstOrNull() ?: ""),
                                lastName = (user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""),
                                email = (user.email ?: ""),
                                phone = (user.phoneNumber ?: ""),
                                role = "user"
                            )

                            db.collection("users").document(uid)
                                .set(firestoreUser)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Google user data written to Firestore")

                                    // Crear sesión local con SessionManager
                                    val mainActivity = activity as? MainActivity
                                    mainActivity?.createUserSession(uid, user.email ?: "", "user")

                                    // Cargar fragmento Home
                                    mainActivity?.homeFragment = HomeFragment()
                                    mainActivity?.loadFragment(mainActivity.homeFragment!!)
                                    mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error writing Google user data to Firestore", e)
                                    Toast.makeText(requireContext(), "Error al guardar los datos del usuario.", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // Si es un usuario existente, obtener su rol
                            db.collection("users").document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    val role = document.getString("role") ?: "user"

                                    // Crear sesión local con SessionManager
                                    val mainActivity = activity as? MainActivity
                                    mainActivity?.createUserSession(uid, user.email ?: "", role)

                                    // Cargar fragmento Home
                                    mainActivity?.homeFragment = HomeFragment()
                                    mainActivity?.loadFragment(mainActivity.homeFragment!!)
                                    mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error obteniendo rol de usuario Google: ${e.message}")

                                    // Si falla, usar un rol predeterminado
                                    val mainActivity = activity as? MainActivity
                                    mainActivity?.createUserSession(uid, user.email ?: "", "user")

                                    // Cargar fragmento Home
                                    mainActivity?.homeFragment = HomeFragment()
                                    mainActivity?.loadFragment(mainActivity.homeFragment!!)
                                    mainActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
                                }
                        }
                    }
                } else {
                    // Si el inicio de sesión falla
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(requireContext(), "Error de autenticación con Google: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val TAG = "AccountFragment"
    }
}