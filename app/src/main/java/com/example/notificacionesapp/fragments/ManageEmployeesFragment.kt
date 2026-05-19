package com.example.notificacionesapp.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.example.notificacionesapp.databinding.FragmentManageEmployeesBinding
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManageEmployeesFragment : BaseFragment<FragmentManageEmployeesBinding>() {

    @Inject lateinit var authRepository: AuthRepository

    private lateinit var employeesAdapter: EmployeesAdapter
    private val employeesList = ArrayList<User>()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentManageEmployeesBinding {
        return FragmentManageEmployeesBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        employeesAdapter = EmployeesAdapter(employeesList,
            object : EmployeeClickListener {
                override fun onEditClick(employee: User) {
                    showEditEmployeeDialog(employee)
                }
                override fun onDeleteClick(employee: User) {
                    showDeleteEmployeeConfirmDialog(employee)
                }
                override fun onResetPasswordClick(employee: User) {
                    showResetPasswordDialog(employee)
                }
            }
        )

        binding.employeesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = employeesAdapter
        }

        binding.createEmployeeButton.setOnClickListener { showCreateEmployeeDialog() }

        loadEmployees()
    }

    private fun loadEmployees() {
        val mainActivity = activity as? MainActivity ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.noEmployeesText.visibility = View.GONE

        lifecycleScope.launch {
            val adminId = mainActivity.sessionManager.getUserId() ?: return@launch
            binding.progressBar.visibility = View.GONE

            when (val result = authRepository.getUsersByAdminId(adminId)) {
                is Result.Success -> {
                    employeesList.clear()
                    employeesList.addAll(result.data)
                    employeesAdapter.notifyDataSetChanged()

                    if (employeesList.isEmpty()) {
                        binding.noEmployeesText.visibility = View.VISIBLE
                    } else {
                        binding.noEmployeesText.visibility = View.GONE
                    }
                }
                is Result.Error -> {
                    binding.noEmployeesText.visibility = View.VISIBLE
                    Toast.makeText(requireContext(),
                        "Error al cargar empleados: ${result.exception.message}", Toast.LENGTH_SHORT).show()
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
                binding.root.postDelayed({ loadEmployees() }, 1500)
            } else {
                Toast.makeText(requireContext(), "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
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
        phoneInput.hint = "Teléfono"
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
                        Toast.makeText(requireContext(), "Empleado actualizado correctamente", Toast.LENGTH_SHORT).show()
                        loadEmployees()
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
                    Toast.makeText(requireContext(), "Empleado eliminado correctamente", Toast.LENGTH_SHORT).show()
                    loadEmployees()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(),
                        "Error al eliminar el empleado: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun showResetPasswordDialog(employee: User) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Resetear Contraseña")
        builder.setMessage("¿Estás seguro de que deseas resetear la contraseña de ${employee.firstName} ${employee.lastName}?")

        builder.setPositiveButton("Resetear") { dialog, _ ->
            lifecycleScope.launch {
                when (val result = authRepository.resetPassword(employee.email)) {
                    is Result.Success -> {
                        Toast.makeText(requireContext(),
                            "Correo de restablecimiento enviado a ${employee.email}", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        Toast.makeText(requireContext(),
                            "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Loading -> {}
                }
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    interface EmployeeClickListener {
        fun onEditClick(employee: User)
        fun onDeleteClick(employee: User)
        fun onResetPasswordClick(employee: User)
    }

    inner class EmployeesAdapter(
        private val employees: List<User>,
        private val listener: EmployeeClickListener
    ) : RecyclerView.Adapter<EmployeesAdapter.EmployeeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_employee, parent, false)
            return EmployeeViewHolder(view)
        }

        override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
            holder.bind(employees[position])
        }

        override fun getItemCount() = employees.size

        inner class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.employeeNameText)
            private val emailText: TextView = itemView.findViewById(R.id.employeeEmailText)
            private val phoneText: TextView = itemView.findViewById(R.id.employeePhoneText)
            private val editButton: MaterialButton = itemView.findViewById(R.id.editEmployeeButton)
            private val resetPasswordButton: MaterialButton = itemView.findViewById(R.id.resetPasswordButton)
            private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteEmployeeButton)

            fun bind(employee: User) {
                nameText.text = "${employee.firstName} ${employee.lastName}"
                emailText.text = employee.email
                phoneText.text = employee.phone

                editButton.setOnClickListener { listener.onEditClick(employee) }
                resetPasswordButton.setOnClickListener { listener.onResetPasswordClick(employee) }
                deleteButton.setOnClickListener { listener.onDeleteClick(employee) }
            }
        }
    }

    companion object {
        private const val TAG = "ManageEmployeesFragment"
    }
}
