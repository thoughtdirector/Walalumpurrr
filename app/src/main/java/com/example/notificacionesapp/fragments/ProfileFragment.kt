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
import androidx.core.content.ContextCompat
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private lateinit var auth: FirebaseAuth
    private val database = Firebase.firestore
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
        // Ocultar sección de admin por defecto
        binding.adminCard.visibility = View.GONE

        // Cargar información del usuario
        loadUserProfile()

        // Configurar botones
        setupButtons()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Show email
            binding.userEmailText.text = currentUser.email

            // Get additional data from Firestore
            Firebase.firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Show name
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        binding.userNameText.text = "$firstName $lastName"

                        // Show phone
                        val phone = document.getString("phone") ?: "No disponible"
                        binding.userPhoneText.text = "Teléfono: $phone"

                        // Show role and configure UI based on role
                        val role = document.getString("role") ?: "user"
                        binding.userRoleText.text = "Rol: ${roleToSpanish(role)}"

                        // Configure admin section visibility
                        if (role == "admin") {
                            binding.adminCard.visibility = View.VISIBLE
                        } else {
                            binding.adminCard.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user data from Firestore: ${e.message}")
                    Toast.makeText(requireContext(), "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                }
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

        // Campos a editar
        val firstNameInput = EditText(requireContext())
        firstNameInput.hint = "Nombre"
        firstNameInput.setText(userDetails["firstName"] as? String ?: "")
        layout.addView(firstNameInput)

        val lastNameInput = EditText(requireContext())
        lastNameInput.hint = "Apellido"
        lastNameInput.setText(userDetails["lastName"] as? String ?: "")
        layout.addView(lastNameInput)

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Teléfono"
        phoneInput.setText(userDetails["phone"] as? String ?: "")
        layout.addView(phoneInput)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { _, _ ->
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val phone = phoneInput.text.toString()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                updateUserProfile(firstName, lastName, phone)
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

            Firebase.firestore.collection("users").document(currentUser.uid)
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
            database.child("users").orderByChild("adminId").equalTo(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val employees = ArrayList<Map<String, Any>>()
                        for (employeeSnapshot in snapshot.children) {
                            val employeeData = employeeSnapshot.value as? Map<String, Any>
                            if (employeeData != null) {
                                // Añadir el ID del empleado a los datos
                                val employeeWithId = HashMap(employeeData)
                                employeeWithId["uid"] = employeeSnapshot.key ?: ""
                                employees.add(employeeWithId)
                            }
                        }

                        if (employees.isEmpty()) {
                            Toast.makeText(requireContext(), "No tienes empleados registrados", Toast.LENGTH_SHORT).show()
                        } else {
                            showEmployeesList(employees)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error al cargar empleados: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
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
        val updates = HashMap<String, Any>()
        updates["firstName"] = firstName
        updates["lastName"] = lastName
        updates["phone"] = phone

        database.child("users").child(employeeId).updateChildren(updates)
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
        // Eliminar de la base de datos
        database.child("users").child(employeeId).removeValue()
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
                    // Eliminar datos de la base de datos
                    database.child("users").child(user.uid).removeValue()
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