package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
            notificationHistoryManager.clearHistory()
            updateNotificationsList()
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

        val notifications = when (selectedCategory) {
            "Todas" -> notificationHistoryManager.getNotifications()
            "Nequi" -> notificationHistoryManager.getNotificationsByType("NEQUI")
            "DaviPlata" -> notificationHistoryManager.getNotificationsByType("DAVIPLATA")
            "Bancolombia" -> notificationHistoryManager.getNotificationsByType("BANCOLOMBIA")
            "WhatsApp" -> notificationHistoryManager.getNotificationsByType("WHATSAPP")
            else -> notificationHistoryManager.getNotifications()
        }

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
                    amount = notification["amount"]
                )
            }

            // Actualizar el adaptador
            adapter.updateData(notificationItems)
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar la lista cada vez que el fragmento se retoma
        updateNotificationsList()
    }
}