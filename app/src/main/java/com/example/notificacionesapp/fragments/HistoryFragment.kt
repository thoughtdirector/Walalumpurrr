package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificacionesapp.adapter.NotificationAdapter
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.databinding.FragmentHistoryBinding
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.util.NotificationHistoryManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    @Inject lateinit var notificationRepository: NotificationRepository

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
        notificationHistoryManager = NotificationHistoryManager(requireContext())

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter()
        binding.historyRecyclerView.adapter = adapter

        setupCategorySpinner()

        binding.clearHistoryButton.setOnClickListener {
            lifecycleScope.launch {
                notificationRepository.clearAllNotifications()
            }
            notificationHistoryManager.clearHistory()
            updateNotificationsList()
        }

        loadSupabaseNotifications()
    }

    private fun loadSupabaseNotifications() {
        lifecycleScope.launch {
            when (val result = notificationRepository.getAllNotifications()) {
                is Result.Success -> {
                    val notifications = result.data
                    if (notifications.isNotEmpty()) {
                        val items = notifications.map { n ->
                            NotificationItem(
                                appName = n.appName,
                                title = n.title,
                                content = n.content,
                                timestamp = n.timestamp.time,
                                sender = n.sender,
                                amount = n.amount
                            )
                        }
                        showNotifications(items)
                    } else {
                        updateNotificationsList()
                    }
                }
                is Result.Error -> {
                    updateNotificationsList()
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun setupCategorySpinner() {
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterOrReload()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterOrReload() {
        val selectedCategory = binding.categorySpinner.selectedItem.toString()
        lifecycleScope.launch {
            when (val result = notificationRepository.getAllNotifications()) {
                is Result.Success -> {
                    val filtered = if (selectedCategory == "Todas") {
                        result.data
                    } else {
                        result.data.filter { it.appName.equals(selectedCategory, ignoreCase = true) }
                    }
                    if (filtered.isNotEmpty()) {
                        val items = filtered.map { n ->
                            NotificationItem(
                                appName = n.appName,
                                title = n.title,
                                content = n.content,
                                timestamp = n.timestamp.time,
                                sender = n.sender,
                                amount = n.amount
                            )
                        }
                        showNotifications(items)
                    } else {
                        updateNotificationsList()
                    }
                }
                is Result.Error -> {
                    updateNotificationsList()
                }
                is Result.Loading -> {}
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
            showNotifications(notificationItems)
        }
    }

    private fun showNotifications(items: List<NotificationItem>) {
        if (items.isEmpty()) {
            binding.emptyHistoryText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyHistoryText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            adapter.updateData(items)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSupabaseNotifications()
    }
}