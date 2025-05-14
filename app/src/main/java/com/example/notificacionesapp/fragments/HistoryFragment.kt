package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificacionesapp.adapter.NotificationAdapter
import com.example.notificacionesapp.databinding.FragmentHistoryBinding
import com.example.notificacionesapp.firebase.NotificationSyncService
import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.util.NotificationHistoryManager
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
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
        // Mostrar indicador de carga
        binding.progressBar.visibility = View.VISIBLE

        // Para categorías específicas, usar el filtro por tipo
        if (currentCategory != "Todas") {
            val type = when (currentCategory) {
                "Nequi" -> "NEQUI"
                "DaviPlata" -> "DAVIPLATA"
                "Bancolombia" -> "BANCOLOMBIA"
                "WhatsApp" -> "WHATSAPP"
                else -> ""
            }

            if (type.isNotEmpty()) {
                notificationSyncService.getNotificationsByType(type) { notifications ->
                    handleNotifications(notifications)
                }
                return
            }
        }

        // Para "Todas", obtener todas las notificaciones
        notificationSyncService.startListeningForNotifications { notifications ->
            handleNotifications(notifications)
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