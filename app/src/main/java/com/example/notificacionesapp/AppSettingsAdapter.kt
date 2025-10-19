package com.example.notificacionesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSettingsAdapter(
    private val appSettings: Map<String, Boolean>,
    private val onAppToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppSettingsAdapter.AppViewHolder>() {

    private val appNames = appSettings.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_checked, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appName = appNames[position]
        val isEnabled = appSettings[appName] ?: false
        
        holder.bind(appName, isEnabled)
    }

    override fun getItemCount(): Int = appNames.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        private val checkBox: CheckBox = itemView.findViewById(android.R.id.checkbox)

        fun bind(appName: String, isEnabled: Boolean) {
            textView.text = appName
            checkBox.isChecked = isEnabled
            
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onAppToggle(appName, isChecked)
            }
        }
    }
}
