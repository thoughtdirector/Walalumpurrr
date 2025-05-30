package com.example.notificacionesapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var userDetails: Map<String, Any?> = HashMap()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun setupUI() {
        userDetails = HashMap()

        binding.adminCard.visibility = View.GONE

        loadUserProfile()

        setupButtons()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Show email
            binding.userEmailText.text = currentUser.email ?: "Sin email"

            // Get additional data from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document != null && document.exists()) {
                            // Guardar datos para uso posterior - CORREGIDO
                            val documentData = document.data
                            userDetails = if (documentData != null) {
                                HashMap(documentData)
                            } else {
                                HashMap()
                            }

                            // Show name - CORREGIDO
                            val firstName = document.getString("firstName") ?: ""
                            val lastName = document.getString("lastName") ?: ""
                            val fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                "$firstName $lastName".trim()
                            } else {
                                "Sin nombre"
                            }
                            binding.userNameText.text = fullName

                            // Show phone - CORREGIDO
                            val phone = document.getString("phone") ?: "No disponible"
                            binding.userPhoneText.text = "Teléfono: $phone"

                            // Show role and configure UI based on role - CORREGIDO
                            val role = document.getString("role") ?: "user"
                            binding.userRoleText.text = "Rol: ${roleToSpanish(role)}"

                            // Configure admin section visibility
                            if (role == "admin") {
                                binding.adminCard.visibility = View.VISIBLE
                            } else {
                                binding.adminCard.visibility = View.GONE
                            }
                        } else {
                            // Document doesn't exist - set defaults
                            userDetails = HashMap()
                            binding.userNameText.text = "Sin nombre"
                            binding.userPhoneText.text = "Teléfono: No disponible"
                            binding.userRoleText.text = "Rol: Usuario"
                            binding.adminCard.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing user data: ${e.message}")
                        // Set safe defaults
                        userDetails = HashMap()
                        binding.userNameText.text = "Error al cargar nombre"
                        binding.userPhoneText.text = "Teléfono: Error al cargar"
                        binding.userRoleText.text = "Rol: Usuario"
                        binding.adminCard.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user data from Firestore: ${e.message}")
                    // Set safe defaults on error
                    userDetails = HashMap()
                    binding.userNameText.text = "Error al cargar datos"
                    binding.userPhoneText.text = "Teléfono: Error al cargar"
                    binding.userRoleText.text = "Rol: Usuario"
                    binding.adminCard.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No current user - set defaults
            userDetails = HashMap()
            binding.userEmailText.text = "Sin email"
            binding.userNameText.text = "Sin nombre"
            binding.userPhoneText.text = "Teléfono: No disponible"
            binding.userRoleText.text = "Rol: Usuario"
            binding.adminCard.visibility = View.GONE
        }
    }

    private fun roleToSpanish(role: String): String {
        return when (role.lowercase()) {
            "admin" -> "Administrador"
            "employee" -> "Empleado"
            else -> "Usuario"
        }
    }

    private fun setupButtons() {
        // Botón de editar perfil
        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        // Botón de crear empleado
        binding.createEmployeeButton.setOnClickListener {
            showCreateEmployeeDialog()
        }

        // Botón de gestionar empleados
        binding.manageEmployeesButton.setOnClickListener {
            showManageEmployeesDialog()
        }

        // Botón de cambiar contraseña
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        // Botón de eliminar cuenta
        binding.deleteAccountButton.setOnClickListener {
            showDeleteAccountDialog()
        }

        // Botón de cerrar sesión
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar Perfil")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        // Campos a editar - CORREGIDO para evitar ClassCastException
        val firstNameInput = EditText(requireContext())
        firstNameInput.hint = "Nombre"
        val firstName = userDetails["firstName"]
        firstNameInput.setText(if (firstName is String) firstName else "")
        layout.addView(firstNameInput)

        val lastNameInput = EditText(requireContext())
        lastNameInput.hint = "Apellido"
        val lastName = userDetails["lastName"]
        lastNameInput.setText(if (lastName is String) lastName else "")
        layout.addView(lastNameInput)

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Teléfono"
        val phone = userDetails["phone"]
        phoneInput.setText(if (phone is String) phone else "")
        layout.addView(phoneInput)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { _, _ ->
            val newFirstName = firstNameInput.text.toString().trim()
            val newLastName = lastNameInput.text.toString().trim()
            val newPhone = phoneInput.text.toString().trim()

            if (newFirstName.isNotEmpty() && newLastName.isNotEmpty()) {
                updateUserProfile(newFirstName, newLastName, newPhone)
            } else {
                Toast.makeText(requireContext(), "Nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun updateUserProfile(firstName: String, lastName: String, phone: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val updates = hashMapOf<String, Any>(
                "firstName" to firstName,
                "lastName" to lastName,
                "phone" to phone
            )

            db.collection("users").document(currentUser.uid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                    loadUserProfile() // Reload data
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al actualizar el perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showCreateEmployeeDialog() {
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Crear Nuevo Empleado")

            val inputLayout = LinearLayout(requireContext())
            inputLayout.orientation = LinearLayout.VERTICAL
            inputLayout.setPadding(20, 20, 20, 20)

            val emailInput = EditText(requireContext())
            emailInput.hint = "Correo Electrónico"
            inputLayout.addView(emailInput)

            val firstNameInput = EditText(requireContext())
            firstNameInput.hint = "Nombre"
            inputLayout.addView(firstNameInput)

            val lastNameInput = EditText(requireContext())
            lastNameInput.hint = "Apellido"
            inputLayout.addView(lastNameInput)

            val phoneInput = EditText(requireContext())
            phoneInput.hint = "Teléfono"
            inputLayout.addView(phoneInput)

            val birthDateInput = EditText(requireContext())
            birthDateInput.hint = "Fecha de Nacimiento (YYYY-MM-DD)"
            inputLayout.addView(birthDateInput)

            builder.setView(inputLayout)

            builder.setPositiveButton("Crear") { _, _ ->
                val email = emailInput.text.toString()
                val firstName = firstNameInput.text.toString()
                val lastName = lastNameInput.text.toString()
                val phone = phoneInput.text.toString()
                val birthDate = birthDateInput.text.toString()

                if (email.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty() && phone.isNotEmpty() && birthDate.isNotEmpty()) {
                    mainActivity.createEmployeeAccount(email, firstName, lastName, phone, birthDate)
                } else {
                    Toast.makeText(requireContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }
    }

    private fun showManageEmployeesDialog() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Buscar empleados asociados a este administrador
            db.collection("users")
                .whereEqualTo("adminId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(requireContext(), "No tienes empleados registrados", Toast.LENGTH_SHORT).show()
                    } else {
                        val employees = ArrayList<Map<String, Any>>()
                        for (document in documents) {
                            val employeeData = document.data
                            // Añadir el ID del empleado a los datos
                            val employeeWithId = HashMap(employeeData)
                            employeeWithId["uid"] = document.id
                            employees.add(employeeWithId)
                        }
                        showEmployeesList(employees)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al cargar empleados: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEmployeesList(employees: List<Map<String, Any>>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tus Empleados")

        // Crear lista de nombres de empleados para mostrar
        val employeeNames = Array(employees.size) { index ->
            val employee = employees[index]
            val firstName = employee["firstName"] as? String ?: ""
            val lastName = employee["lastName"] as? String ?: ""
            val email = employee["email"] as? String ?: "Sin email"
            "$firstName $lastName ($email)"
        }

        builder.setItems(employeeNames) { _, which ->
            // Mostrar opciones para el empleado seleccionado
            val selectedEmployee = employees[which]
            showEmployeeOptionsDialog(selectedEmployee)
        }

        builder.setNegativeButton("Cerrar", null)
        builder.show()
    }

    private fun showEmployeeOptionsDialog(employee: Map<String, Any>) {
        val employeeId = employee["uid"] as? String ?: return
        val firstName = employee["firstName"] as? String ?: ""
        val lastName = employee["lastName"] as? String ?: ""

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("$firstName $lastName")

        val options = arrayOf("Editar información", "Eliminar empleado")

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showEditEmployeeDialog(employeeId, employee)
                1 -> showDeleteEmployeeConfirmDialog(employeeId, "$firstName $lastName")
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showEditEmployeeDialog(employeeId: String, employeeData: Map<String, Any>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar Empleado")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        // Campos a editar
        val firstNameInput = EditText(requireContext())
        firstNameInput.hint = "Nombre"
        firstNameInput.setText(employeeData["firstName"] as? String ?: "")
        layout.addView(firstNameInput)

        val lastNameInput = EditText(requireContext())
        lastNameInput.hint = "Apellido"
        lastNameInput.setText(employeeData["lastName"] as? String ?: "")
        layout.addView(lastNameInput)

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Teléfono"
        phoneInput.setText(employeeData["phone"] as? String ?: "")
        layout.addView(phoneInput)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { _, _ ->
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val phone = phoneInput.text.toString()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                updateEmployeeProfile(employeeId, firstName, lastName, phone)
            } else {
                Toast.makeText(requireContext(), "Nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun updateEmployeeProfile(employeeId: String, firstName: String, lastName: String, phone: String) {
        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to phone
        )

        db.collection("users").document(employeeId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Empleado actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al actualizar el empleado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteEmployeeConfirmDialog(employeeId: String, employeeName: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Eliminar Empleado")
        builder.setMessage("¿Estás seguro de que deseas eliminar a $employeeName? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar") { _, _ ->
            deleteEmployee(employeeId)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun deleteEmployee(employeeId: String) {
        // Eliminar de Firestore
        db.collection("users").document(employeeId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Empleado eliminado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al eliminar el empleado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cambiar Contraseña")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val currentPasswordInput = EditText(requireContext())
        currentPasswordInput.hint = "Contraseña actual"
        currentPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(currentPasswordInput)

        val newPasswordInput = EditText(requireContext())
        newPasswordInput.hint = "Nueva contraseña"
        newPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(newPasswordInput)

        val confirmPasswordInput = EditText(requireContext())
        confirmPasswordInput.hint = "Confirmar nueva contraseña"
        confirmPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(confirmPasswordInput)

        builder.setView(layout)

        builder.setPositiveButton("Cambiar") { _, _ ->
            val currentPassword = currentPasswordInput.text.toString()
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            changePassword(currentPassword, newPassword)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            // Reautenticar usuario
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // Cambiar contraseña
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error al actualizar contraseña: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error de autenticación. Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Eliminar Cuenta")
        builder.setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer y perderás todos tus datos.")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val passwordInput = EditText(requireContext())
        passwordInput.hint = "Introduce tu contraseña para confirmar"
        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(passwordInput)

        builder.setView(layout)

        builder.setPositiveButton("Eliminar Cuenta") { _, _ ->
            val password = passwordInput.text.toString()

            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Debes introducir tu contraseña para confirmar", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            deleteAccount(password)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            // Reautenticar usuario
            val credential = EmailAuthProvider.getCredential(user.email!!, password)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // Eliminar datos de Firestore
                    db.collection("users").document(user.uid)
                        .delete()
                        .addOnSuccessListener {
                            // Eliminar cuenta de autenticación
                            user.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()

                                    // Cerrar sesión y volver a la página principal
                                    (activity as? MainActivity)?.logoutUser()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Error al eliminar cuenta: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error al eliminar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error de autenticación. Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showLogoutConfirmDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cerrar Sesión")
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?")

        builder.setPositiveButton("Cerrar Sesión") { _, _ ->
            // Cerrar sesión
            (activity as? MainActivity)?.logoutUser()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}