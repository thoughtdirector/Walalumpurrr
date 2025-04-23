package com.example.notificacionesapp.model

data class TransactionStats(
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "",
    val totalTransactions: Int = 0,
    val totalAmount: Double = 0.0,
    val bySource: Map<String, Int> = emptyMap(),  // Por ejemplo: "Nequi": 5, "DaviPlata": 3
    val byAmount: Map<String, Int> = emptyMap()   // Rangos, ej: "0-1000": 2, "1001-5000": 3
)