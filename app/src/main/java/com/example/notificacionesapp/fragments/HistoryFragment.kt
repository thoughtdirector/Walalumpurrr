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

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHistoryBinding {
        return FragmentHistoryBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Inicializar los gestores
        notificationHistoryManager = NotificationHistoryManager(requireContext())
        notificationSyncService = NotificationSyncService(requireContext())

        // Configurar RecyclerView
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(object : NotificationAdapter.NotificationClickListener {
            override fun onNotificationClick(notificationId: String) {
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
            notificationHistoryManager.clearHistory()
            startListeningForNotifications()
        }

        // Configurar botón de sincronización
        binding.syncButton.setOnClickListener {
            // Mostrar progreso
            val snackbar = Snackbar.make(binding.root, "Sincronizando notificaciones...", Snackbar.LENGTH_SHORT)
            snackbar.show()

            // Forzar sincronización
            startListeningForNotifications()
        }

        // Iniciar listener para notificaciones de Firebase
        startListeningForNotifications()
    }

    private fun setupCategorySpinner() {
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = parent?.getItemAtPosition(position).toString()
                startListeningForNotifications()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No es necesario hacer nada
            }
        }
    }

    private fun startListeningForNotifications() {
        // Show progress indicator
        binding.progressBar.visibility = View.VISIBLE

        val sessionManager = SessionManager(requireContext())
        val userId = sessionManager.getUserId() ?: return
        val role = sessionManager.getUserRole() ?: "user"

        // Determine adminId
        val adminId = if (role == "admin") {
            userId
        } else {
            sessionManager.getUserDetails()["adminId"] ?: return
        }

        val firestoreService = FirestoreService()

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
                firestoreService.getNotificationsByType(
                    adminId = adminId,
                    type = type,
                    onSuccess = { notifications ->
                        handleFirestoreNotifications(notifications)
                    },
                    onError = { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
                return
            }
        }

        // For "All", use listener for real-time updates
        firestoreService.addNotificationsListener(
            adminId = adminId,
            onUpdate = { notifications ->
                handleFirestoreNotifications(notifications)
            },
            onError = { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add this helper method
    private fun handleFirestoreNotifications(firestoreNotifications: List<FirestoreNotification>) {
        binding.progressBar.visibility = View.GONE

        if (firestoreNotifications.isEmpty()) {
            binding.emptyHistoryText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyHistoryText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE

            // Convert to NotificationItem objects
            val notificationItems = firestoreNotifications.map { notification ->
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
            }

            // Update adapter
            adapter.updateData(notificationItems)
        }
    }
    private fun handleNotifications(notifications: List<NotificationItem>) {
        // Ocultar indicador de carga
        binding.progressBar.visibility = View.GONE

        if (notifications.isEmpty()) {
            binding.emptyHistoryText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyHistoryText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE

            // Actualizar el adaptador
            adapter.updateData(notifications)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reiniciar escucha cuando el fragmento se reanuda
        startListeningForNotifications()
    }

    override fun onPause() {
        super.onPause()
        // Detener escucha cuando el fragmento se pausa
        notificationSyncService.stopListeningForNotifications()
    }
}