package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificacionesapp.SessionManager
import com.example.notificacionesapp.adapter.NotificationAdapter
import com.example.notificacionesapp.databinding.FragmentHistoryBinding
import com.example.notificacionesapp.firebase.NotificationSyncService
import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.util.NotificationHistoryManager
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import com.example.notificacionesapp.model.FirestoreNotification
import com.example.notificacionesapp.firebase.FirestoreService
import java.util.Date
import java.util.Locale

class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private lateinit var notificationSyncService: NotificationSyncService
    private lateinit var adapter: NotificationAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var currentCategory = "Todas"
    private var firestoreService: FirestoreService? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHistoryBinding {
        return FragmentHistoryBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Verificar que el fragmento esté agregado antes de continuar
        if (!isAdded) return

        try {
            // Inicializar los gestores
            notificationHistoryManager = NotificationHistoryManager(requireContext())
            notificationSyncService = NotificationSyncService(requireContext())
            firestoreService = FirestoreService()

            // Configurar RecyclerView
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            adapter = NotificationAdapter(object : NotificationAdapter.NotificationClickListener {
                override fun onNotificationClick(notificationId: String) {
                    // Verificar que el fragmento esté activo antes de proceder
                    if (!isAdded || _binding == null) return

                    // Marcar notificación como leída en Firebase
                    notificationSyncService.markNotificationAsRead(notificationId)

                    // Actualizar UI
                    startListeningForNotifications()
                }
            })
            binding.historyRecyclerView.adapter = adapter

            // Configurar el spinner
            setupCategorySpinner()

            // Configurar botón de limpieza
            binding.clearHistoryButton.setOnClickListener {
                if (!isAdded || _binding == null) return@setOnClickListener
                notificationHistoryManager.clearHistory()
                startListeningForNotifications()
            }

            // Configurar botón de sincronización
            binding.syncButton.setOnClickListener {
                if (!isAdded || _binding == null) return@setOnClickListener

                // Mostrar progreso
                val snackbar = Snackbar.make(binding.root, "Sincronizando notificaciones...", Snackbar.LENGTH_SHORT)
                snackbar.show()

                // Forzar sincronización
                startListeningForNotifications()
            }

            // Iniciar listener para notificaciones de Firebase
            startListeningForNotifications()
        } catch (e: Exception) {
            android.util.Log.e("HistoryFragment", "Error in setupUI: ${e.message}", e)
            if (isAdded) {
                Toast.makeText(requireContext(), "Error al cargar el historial", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategorySpinner() {
        if (!isAdded || _binding == null) return

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isAdded || _binding == null) return

                currentCategory = parent?.getItemAtPosition(position).toString()
                startListeningForNotifications()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No es necesario hacer nada
            }
        }
    }

    private fun startListeningForNotifications() {
        if (!isAdded || _binding == null) return

        try {
            // Show progress indicator
            binding.progressBar.visibility = View.VISIBLE

            val sessionManager = SessionManager(requireContext())
            val userId = sessionManager.getUserId()
            val role = sessionManager.getUserRole() ?: "user"

            if (userId == null) {
                binding.progressBar.visibility = View.GONE
                showEmptyState()
                return
            }

            // Determine adminId
            val adminId = if (role == "admin") {
                userId
            } else {
                sessionManager.getUserDetails()["adminId"]
            }

            if (adminId == null) {
                binding.progressBar.visibility = View.GONE
                showEmptyState()
                return
            }

            // For specific categories, use type filter
            if (currentCategory != "Todas") {
                val type = when (currentCategory) {
                    "Nequi" -> "NEQUI"
                    "DaviPlata" -> "DAVIPLATA"
                    "Bancolombia" -> "BANCOLOMBIA"
                    "WhatsApp" -> "WHATSAPP"
                    else -> ""
                }

                if (type.isNotEmpty()) {
                    firestoreService?.getNotificationsByType(
                        adminId = adminId,
                        type = type,
                        onSuccess = { notifications ->
                            if (isAdded && _binding != null) {
                                handleFirestoreNotifications(notifications)
                            }
                        },
                        onError = { e ->
                            if (isAdded && _binding != null) {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    return
                }
            }

            // For "All", use listener for real-time updates
            firestoreService?.addNotificationsListener(
                adminId = adminId,
                onUpdate = { notifications ->
                    if (isAdded && _binding != null) {
                        handleFirestoreNotifications(notifications)
                    }
                },
                onError = { e ->
                    if (isAdded && _binding != null) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("HistoryFragment", "Error in startListeningForNotifications: ${e.message}", e)
            if (isAdded && _binding != null) {
                binding.progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun handleFirestoreNotifications(firestoreNotifications: List<FirestoreNotification>) {
        if (!isAdded || _binding == null) return

        try {
            binding.progressBar.visibility = View.GONE

            if (firestoreNotifications.isEmpty()) {
                showEmptyState()
            } else {
                binding.emptyHistoryText.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE

                // Convert to NotificationItem objects
                val notificationItems = firestoreNotifications.mapNotNull { notification ->
                    try {
                        NotificationItem(
                            id = notification.id,
                            appName = notification.appName,
                            title = notification.title,
                            content = notification.content,
                            timestamp = notification.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
                            sender = notification.sender,
                            amount = notification.amount,
                            isRead = notification.read
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("HistoryFragment", "Error converting notification: ${e.message}")
                        null
                    }
                }

                // Update adapter
                adapter.updateData(notificationItems)
            }
        } catch (e: Exception) {
            android.util.Log.e("HistoryFragment", "Error in handleFirestoreNotifications: ${e.message}", e)
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        if (!isAdded || _binding == null) return

        binding.emptyHistoryText.visibility = View.VISIBLE
        binding.historyRecyclerView.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Solo reiniciar si el fragmento está correctamente inicializado
        if (isAdded && _binding != null) {
            startListeningForNotifications()
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener escucha cuando el fragmento se pausa
        try {
            notificationSyncService.stopListeningForNotifications()
        } catch (e: Exception) {
            android.util.Log.e("HistoryFragment", "Error stopping notifications listener: ${e.message}")
        }
    }

    override fun onDestroyView() {
        try {
            notificationSyncService.stopListeningForNotifications()
            firestoreService = null
        } catch (e: Exception) {
            android.util.Log.e("HistoryFragment", "Error in onDestroyView: ${e.message}")
        }
        super.onDestroyView()
    }
}