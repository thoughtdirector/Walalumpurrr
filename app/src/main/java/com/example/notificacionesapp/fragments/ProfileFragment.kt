package com.example.notificacionesapp.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import java.util.Calendar
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.core.auth.AuthManager
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.model.isAdmin
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.example.notificacionesapp.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var authRepository: AuthRepository

    private var userDetails: Map<String, Any?> = HashMap()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        binding.adminCard.visibility = View.GONE
        loadUserProfile()
        setupButtons()
    }

    private fun loadUserProfile() {
        val user = authManager.currentUser.value
        if (user != null) {
            binding.userEmailText.text = user.email

            lifecycleScope.launch {
                when (val result = authRepository.getUserById(user.id)) {
                    is Result.Success -> {
                        val userData = result.data
                        binding.userNameText.text = "${userData.firstName} ${userData.lastName}"
                        binding.userPhoneText.text = "Teléfono: ${userData.phone}"
                        binding.userRoleText.text = "Rol: ${roleToSpanish(userData.role.name.lowercase())}"

                        if (userData.isAdmin()) {
                            binding.adminCard.visibility = View.VISIBLE
                        } else {
                            binding.adminCard.visibility = View.GONE
                        }
                    }
                    is Result.Error -> {
                        Toast.makeText(requireContext(),
                            "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Loading -> {}
                }
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
        binding.editProfileButton.setOnClickListener { showEditProfileDialog() }
        binding.createEmployeeButton.setOnClickListener { showCreateEmployeeDialog() }
        binding.manageEmployeesButton.setOnClickListener { showManageEmployeesDialog() }
        binding.changePasswordButton.setOnClickListener { showChangePasswordDialog() }
        binding.deleteAccountButton.setOnClickListener { showDeleteAccountDialog() }
        binding.logoutButton.setOnClickListener { showLogoutConfirmDialog() }
    }

    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar Perfil")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val firstNameInput = EditText(requireContext())
        firstNameInput.hint = "Nombre"
        firstNameInput.setText(userDetails["firstName"] as? String ?: "")
        layout.addView(firstNameInput)

        val lastNameInput = EditText(requireContext())
        lastNameInput.hint = "Apellido"
        lastNameInput.setText(userDetails["lastName"] as? String ?: "")
        layout.addView(lastNameInput)

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Telefono"
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
        val user = authManager.currentUser.value ?: return

        lifecycleScope.launch {
            val updatedUser = user.copy(firstName = firstName, lastName = lastName, phone = phone)
            when (val result = authRepository.updateUser(updatedUser)) {
                is Result.Success -> {
                    Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                    loadUserProfile()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error al actualizar el perfil: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun showCreateEmployeeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Crear Nuevo Empleado")

        val inputLayout = LinearLayout(requireContext())
        inputLayout.orientation = LinearLayout.VERTICAL
        inputLayout.setPadding(20, 20, 20, 20)

        val emailInput = EditText(requireContext())
        emailInput.hint = "Correo Electronico"
        inputLayout.addView(emailInput)

        val firstNameInput = EditText(requireContext())
        firstNameInput.hint = "Nombre"
        inputLayout.addView(firstNameInput)

        val lastNameInput = EditText(requireContext())
        lastNameInput.hint = "Apellido"
        inputLayout.addView(lastNameInput)

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Telefono"
        inputLayout.addView(phoneInput)

        val birthDateInput = EditText(requireContext())
        birthDateInput.hint = "Fecha de Nacimiento (YYYY-MM-DD)"
        birthDateInput.isFocusable = false
        birthDateInput.isClickable = true
        inputLayout.addView(birthDateInput)

        val calendar = Calendar.getInstance()
        birthDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val fm = String.format(Locale.getDefault(), "%02d", m + 1)
                    val fd = String.format(Locale.getDefault(), "%02d", d)
                    birthDateInput.setText("$y-$fm-$fd")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        builder.setView(inputLayout)

        builder.setPositiveButton("Crear") { _, _ ->
            val email = emailInput.text.toString()
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val phone = phoneInput.text.toString()
            val birthDate = birthDateInput.text.toString()

            if (email.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()
                && phone.isNotEmpty() && birthDate.isNotEmpty()) {
                (activity as? MainActivity)?.createEmployeeAccount(
                    email, firstName, lastName, phone, birthDate
                )
            } else {
                Toast.makeText(requireContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showManageEmployeesDialog() {
        val user = authManager.currentUser.value ?: return

        lifecycleScope.launch {
            when (val result = authRepository.getUsersByAdminId(user.id)) {
                is Result.Success -> {
                    val employees = result.data
                    if (employees.isEmpty()) {
                        Toast.makeText(requireContext(),
                            "No tienes empleados registrados", Toast.LENGTH_SHORT).show()
                    } else {
                        showEmployeesList(employees)
                    }
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error al cargar empleados: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun showEmployeesList(employees: List<User>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Tus Empleados")

        val employeeNames = Array(employees.size) { index ->
            val employee = employees[index]
            "${employee.firstName} ${employee.lastName} (${employee.email})"
        }

        builder.setItems(employeeNames) { _, which ->
            showEmployeeOptionsDialog(employees[which])
        }

        builder.setNegativeButton("Cerrar", null)
        builder.show()
    }

    private fun showEmployeeOptionsDialog(employee: User) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("${employee.firstName} ${employee.lastName}")

        val options = arrayOf("Editar informacion", "Eliminar empleado")

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showEditEmployeeDialog(employee)
                1 -> showDeleteEmployeeConfirmDialog(employee)
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showEditEmployeeDialog(employee: User) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar Empleado")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val firstNameInput = EditText(requireContext())
        firstNameInput.hint = "Nombre"
        firstNameInput.setText(employee.firstName)
        layout.addView(firstNameInput)

        val lastNameInput = EditText(requireContext())
        lastNameInput.hint = "Apellido"
        lastNameInput.setText(employee.lastName)
        layout.addView(lastNameInput)

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Telefono"
        phoneInput.setText(employee.phone)
        layout.addView(phoneInput)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { _, _ ->
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val phone = phoneInput.text.toString()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                updateEmployeeProfile(employee.id, firstName, lastName, phone)
            } else {
                Toast.makeText(requireContext(), "Nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun updateEmployeeProfile(employeeId: String, firstName: String, lastName: String, phone: String) {
        lifecycleScope.launch {
            val userResult = authRepository.getUserById(employeeId)
            if (userResult is Result.Success) {
                val updatedUser = userResult.data.copy(firstName = firstName, lastName = lastName, phone = phone)
                when (val result = authRepository.updateUser(updatedUser)) {
                    is Result.Success -> {
                        Toast.makeText(requireContext(),
                            "Empleado actualizado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        Toast.makeText(requireContext(),
                            "Error al actualizar el empleado: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    private fun showDeleteEmployeeConfirmDialog(employee: User) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Eliminar Empleado")
        builder.setMessage("¿Estás seguro de que deseas eliminar a ${employee.firstName} ${employee.lastName}? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar") { _, _ ->
            deleteEmployee(employee.id)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun deleteEmployee(employeeId: String) {
        lifecycleScope.launch {
            when (val result = authRepository.deleteUser(employeeId)) {
                is Result.Success -> {
                    Toast.makeText(requireContext(),
                        "Empleado eliminado correctamente", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error al eliminar el empleado: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cambiar Contraseña")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

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
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            changePassword(newPassword)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun changePassword(newPassword: String) {
        val user = authManager.currentUser.value ?: return

        lifecycleScope.launch {
            when (val result = authRepository.resetPassword(user.email)) {
                is Result.Success -> {
                    Toast.makeText(requireContext(),
                        "Se ha enviado un correo para restablecer tu contraseña", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Eliminar Cuenta")
        builder.setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar Cuenta") { _, _ ->
            deleteAccount()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun deleteAccount() {
        val user = authManager.currentUser.value ?: return

        lifecycleScope.launch {
            when (val result = authRepository.deleteUser(user.id)) {
                is Result.Success -> {
                    authManager.signOut()
                    Toast.makeText(requireContext(),
                        "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.logoutUser()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error al eliminar cuenta: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cerrar Sesion")
        builder.setMessage("¿Estás seguro de que deseas cerrar sesion?")

        builder.setPositiveButton("Cerrar Sesion") { _, _ ->
            (activity as? MainActivity)?.logoutUser()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}
