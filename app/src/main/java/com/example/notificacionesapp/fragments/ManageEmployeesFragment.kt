package com.example.notificacionesapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentManageEmployeesBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ManageEmployeesFragment : BaseFragment<FragmentManageEmployeesBinding>() {

    private lateinit var auth: FirebaseAuth
    private val database = Firebase.firestore
    private lateinit var employeesAdapter: EmployeesAdapter
    private val employeesList = ArrayList<EmployeeModel>()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentManageEmployeesBinding {
        return FragmentManageEmployeesBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun setupUI() {
        // Configurar RecyclerView
        employeesAdapter = EmployeesAdapter(employeesList,
            object : EmployeeClickListener {
                override fun onEditClick(employee: EmployeeModel) {
                    showEditEmployeeDialog(employee)
                }

                override fun onDeleteClick(employee: EmployeeModel) {
                    showDeleteEmployeeConfirmDialog(employee)
                }
            }
        )

        binding.employeesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = employeesAdapter
        }

        // Botón de crear empleado
        binding.createEmployeeButton.setOnClickListener {
            showCreateEmployeeDialog()
        }

        // Cargar lista de empleados
        loadEmployees()
    }

    // Replace the loadEmployees method
    private fun loadEmployees() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.progressBar.visibility = View.VISIBLE
            binding.noEmployeesText.visibility = View.GONE

            // Find employees from Firestore
            Firebase.firestore.collection("users")
                .whereEqualTo("adminId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    employeesList.clear()

                    for (document in documents) {
                        val employee = EmployeeModel(
                            uid = document.id,
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            email = document.getString("email") ?: "",
                            phone = document.getString("phone") ?: "",
                            birthDate = document.getString("birthDate") ?: ""
                        )
                        employeesList.add(employee)
                    }

                    employeesAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE

                    // Show message if no employees
                    if (employeesList.isEmpty()) {
                        binding.noEmployeesText.visibility = View.VISIBLE
                    } else {
                        binding.noEmployeesText.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.noEmployeesText.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Error al cargar empleados: ${e.message}",
                        Toast.LENGTH_SHORT).show()
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

                if (email.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()
                    && phone.isNotEmpty() && birthDate.isNotEmpty()) {
                    mainActivity.createEmployeeAccount(email, firstName, lastName, phone, birthDate)
                    // Recargar la lista después de un breve retraso
                    binding.root.postDelayed({ loadEmployees() }, 1500)
                } else {
                    Toast.makeText(requireContext(), "Todos los campos son requeridos",
                        Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }
    }

    private fun showEditEmployeeDialog(employee: EmployeeModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar Empleado")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        // Campos a editar
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
                updateEmployeeProfile(employee.uid, firstName, lastName, phone)
            } else {
                Toast.makeText(requireContext(), "Nombre y apellido son obligatorios",
                    Toast.LENGTH_SHORT).show()
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

        Firebase.firestore.collection("users").document(employeeId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Empleado actualizado correctamente",
                    Toast.LENGTH_SHORT).show()
                loadEmployees() // Reload data
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al actualizar el empleado: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
    private fun showDeleteEmployeeConfirmDialog(employee: EmployeeModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Eliminar Empleado")
        builder.setMessage("¿Estás seguro de que deseas eliminar a ${employee.firstName} ${employee.lastName}? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar") { _, _ ->
            deleteEmployee(employee.uid)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun deleteEmployee(employeeId: String) {
        // Delete from Firestore
        Firebase.firestore.collection("users").document(employeeId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Empleado eliminado correctamente",
                    Toast.LENGTH_SHORT).show()
                loadEmployees() // Reload list
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al eliminar el empleado: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
    // Modelo de datos para empleado
    data class EmployeeModel(
        val uid: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val phone: String,
        val birthDate: String
    )

    // Interfaz para manejar clics en empleados
    interface EmployeeClickListener {
        fun onEditClick(employee: EmployeeModel)
        fun onDeleteClick(employee: EmployeeModel)
    }

    // Adaptador para RecyclerView
    inner class EmployeesAdapter(
        private val employees: List<EmployeeModel>,
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
            private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteEmployeeButton)

            fun bind(employee: EmployeeModel) {
                nameText.text = "${employee.firstName} ${employee.lastName}"
                emailText.text = employee.email
                phoneText.text = employee.phone

                editButton.setOnClickListener {
                    listener.onEditClick(employee)
                }

                deleteButton.setOnClickListener {
                    listener.onDeleteClick(employee)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ManageEmployeesFragment"
    }
}