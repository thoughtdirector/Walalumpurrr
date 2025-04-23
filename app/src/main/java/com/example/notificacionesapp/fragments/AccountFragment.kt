// app/src/main/java/com/example/notificacionesapp/fragments/AccountFragment.kt
package com.example.notificacionesapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificacionesapp.R
import com.example.notificacionesapp.adapter.EmployeeAdapter
import com.example.notificacionesapp.databinding.FragmentAccountBinding
import com.example.notificacionesapp.firebase.FirebaseManager
import com.example.notificacionesapp.model.User

class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private lateinit var firebaseManager: FirebaseManager
    private lateinit var employeeAdapter: EmployeeAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccountBinding {
        return FragmentAccountBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseManager = FirebaseManager()
    }

    override fun setupUI() {
        // Comprobar si hay usuario logueado
        if (firebaseManager.currentUser == null) {
            showLoginScreen()
            return
        }

        // Configurar Vista de Perfil
        setupProfileView()

        // Configurar botón de cerrar sesión
        binding.logoutButton.setOnClickListener {
            firebaseManager.logout()
            showLoginScreen()
        }

        // Configurar RecyclerView para empleados (solo visible para administradores)
        setupEmployeesList()

        // Cargar datos del usuario
        loadUserData()
    }

    private fun showLoginScreen() {
        binding.profileContent.visibility = View.GONE
        binding.loginPrompt.visibility = View.VISIBLE

        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_loginFragment)
        }
    }

    private fun setupProfileView() {
        binding.profileContent.visibility = View.VISIBLE
        binding.loginPrompt.visibility = View.GONE

        binding.userEmail.text = firebaseManager.currentUser?.email ?: ""
    }

    private fun setupEmployeesList() {
        employeeAdapter = EmployeeAdapter()
        binding.employeesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.employeesRecyclerView.adapter = employeeAdapter

        binding.addEmployeeButton.setOnClickListener {
            showAddEmployeeDialog()
        }
    }

    private fun loadUserData() {
        binding.progressBar.visibility = View.VISIBLE

        firebaseManager.getCurrentUserData(object : FirebaseManager.UserDataListener {
            override fun onUserDataLoaded(user: User) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    updateUI(user)
                }
            }

            override fun onError(message: String) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateUI(user: User) {
        binding.userName.text = user.name
        binding.userRole.text = if (user.role == "admin") "Administrador" else "Empleado"

        // Mostrar/ocultar sección de empleados según el rol
        if (user.role == "admin") {
            binding.employeesSection.visibility = View.VISIBLE
            loadEmployees(user.uid)
        } else {
            binding.employeesSection.visibility = View.GONE
            // Si es empleado, mostrar información del administrador asociado
            if (user.adminId.isNotEmpty()) {
                loadAdminInfo(user.adminId)
            }
        }
    }

    private fun loadEmployees(adminId: String) {
        binding.progressBar.visibility = View.VISIBLE

        firebaseManager.getAdminEmployees(adminId, object : FirebaseManager.EmployeesListener {
            override fun onEmployeesLoaded(employees: List<User>) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE

                    if (employees.isEmpty()) {
                        binding.noEmployeesText.visibility = View.VISIBLE
                        binding.employeesRecyclerView.visibility = View.GONE
                    } else {
                        binding.noEmployeesText.visibility = View.GONE
                        binding.employeesRecyclerView.visibility = View.VISIBLE
                        employeeAdapter.updateData(employees)
                    }
                }
            }

            override fun onError(message: String) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.noEmployeesText.visibility = View.VISIBLE
                    binding.employeesRecyclerView.visibility = View.GONE
                    binding.noEmployeesText.text = "Error al cargar empleados: $message"
                }
            }
        })
    }

    private fun loadAdminInfo(adminId: String) {
        binding.adminInfoSection.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        firebaseManager.getAdminData(adminId, object : FirebaseManager.UserDataListener {
            override fun onUserDataLoaded(admin: User) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.adminName.text = admin.name
                    binding.adminEmail.text = admin.email
                }
            }

            override fun onError(message: String) {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.adminName.text = "No disponible"
                    binding.adminEmail.text = "Error: $message"
                }
            }
        })
    }

    private fun showAddEmployeeDialog() {
        // Esta función podría implementarse para mostrar un diálogo
        // para agregar un nuevo empleado ingresando su ID o email
        Toast.makeText(requireContext(), "Función de agregar empleado en desarrollo", Toast.LENGTH_SHORT).show()
    }
}