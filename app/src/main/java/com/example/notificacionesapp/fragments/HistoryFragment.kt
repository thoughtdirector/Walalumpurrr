package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificacionesapp.adapter.NotificationAdapter
import com.example.notificacionesapp.databinding.FragmentHistoryBinding
import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.util.NotificationHistoryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private lateinit var adapter: NotificationAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHistoryBinding {
        return FragmentHistoryBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Inicializar el gestor de historial
        notificationHistoryManager = NotificationHistoryManager(requireContext())

        // Configurar RecyclerView
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter()
        binding.historyRecyclerView.adapter = adapter

        // Configurar el spinner
        setupCategorySpinner()

        // Configurar botón de limpieza
        binding.clearHistoryButton.setOnClickListener {
            showClearConfirmationDialog()
        }

        // Cargar el historial
        updateNotificationsList()
    }

    private fun setupCategorySpinner() {
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateNotificationsList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No es necesario hacer nada
            }
        }
    }

    private fun updateNotificationsList() {
        val selectedCategory = binding.categorySpinner.selectedItem.toString()

        // Mostrar progreso
        binding.emptyHistoryText.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        if (selectedCategory == "Todas") {
            notificationHistoryManager.getNotifications { notifications ->
                handleNotificationsResult(notifications)
            }
        } else {
            val type = when (selectedCategory) {
                "Nequi" -> "NEQUI"
                "DaviPlata" -> "DAVIPLATA"
                "Bancolombia" -> "BANCOLOMBIA"
                "WhatsApp" -> "WHATSAPP"
                else -> "OTRO"
            }

            notificationHistoryManager.getNotificationsByType(type) { notifications ->
                handleNotificationsResult(notifications)
            }
        }
    }

    private fun handleNotificationsResult(notifications: List<Map<String, String>>) {
        requireActivity().runOnUiThread {
            binding.progressBar.visibility = View.GONE

            if (notifications.isEmpty()) {
                binding.emptyHistoryText.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyHistoryText.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE

                // Convertir Map<String, String> a NotificationItem
                val notificationItems = notifications.map { notification ->
                    NotificationItem(
                        appName = notification["appName"] ?: "Desconocido",
                        title = notification["title"] ?: "",
                        content = notification["content"] ?: "",
                        timestamp = try {
                            dateFormat.parse(notification["timestamp"] ?: "")?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        },
                        sender = notification["sender"],
                        amount = notification["amount"])
                }

                // Actualizar el adaptador
                adapter.updateData(notificationItems)
            }
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Limpiar historial")
            .setMessage("¿Estás seguro de querer eliminar todo el historial de notificaciones?")
            .setPositiveButton("Eliminar") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearHistory() {
        binding.progressBar.visibility = View.VISIBLE
        notificationHistoryManager.clearHistory { success ->
            requireActivity().runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (success) {
                    updateNotificationsList()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar la lista cada vez que el fragmento se retoma
        updateNotificationsList()
    }
}