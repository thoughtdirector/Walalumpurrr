package com.example.notificacionesapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificacionesapp.adapter.NotificationAdapter
import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.util.NotificationHistoryManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var categorySpinner: Spinner
    private lateinit var clearButton: FloatingActionButton
    private lateinit var adapter: NotificationAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Inicializar el gestor de historial
        notificationHistoryManager = NotificationHistoryManager(this)

        // Inicializar vistas
        recyclerView = findViewById(R.id.historyRecyclerView)
        emptyText = findViewById(R.id.emptyHistoryText)
        categorySpinner = findViewById(R.id.categorySpinner)
        clearButton = findViewById(R.id.clearHistoryButton)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter()
        recyclerView.adapter = adapter

        // Configurar el spinner
        setupCategorySpinner()

        // Configurar botón de limpieza
        clearButton.setOnClickListener {
            notificationHistoryManager.clearHistory()
            updateNotificationsList()
        }

        // Cargar el historial
        updateNotificationsList()
    }

    private fun setupCategorySpinner() {
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateNotificationsList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No es necesario hacer nada
            }
        }
    }

    private fun updateNotificationsList() {
        val selectedCategory = categorySpinner.selectedItem.toString()

        val notifications = when (selectedCategory) {
            "Todas" -> notificationHistoryManager.getNotifications()
            "Nequi" -> notificationHistoryManager.getNotificationsByType("NEQUI")
            "DaviPlata" -> notificationHistoryManager.getNotificationsByType("DAVIPLATA")
            "Bancolombia" -> notificationHistoryManager.getNotificationsByType("BANCOLOMBIA")
            "WhatsApp" -> notificationHistoryManager.getNotificationsByType("WHATSAPP")
            else -> notificationHistoryManager.getNotifications()
        }

        if (notifications.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

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
        // Actualizar la lista cada vez que la actividad se retoma
        updateNotificationsList()
    }
}