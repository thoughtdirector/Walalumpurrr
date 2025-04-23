package com.example.notificacionesapp.firebase

import android.util.Log
import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.model.TransactionStats
import com.example.notificacionesapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Usuario actual
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // Autenticación
    fun registerUser(email: String, password: String, name: String, role: String, listener: AuthListener) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    // Crear objeto de usuario
                    val user = User(
                        uid = userId,
                        email = email,
                        name = name,
                        role = role
                    )
                    // Guardar en la base de datos
                    database.child("users").child(userId).setValue(user)
                        .addOnSuccessListener {
                            listener.onSuccess()
                        }
                        .addOnFailureListener { e ->
                            listener.onError(e.message ?: "Error al guardar usuario")
                        }
                } else {
                    listener.onError(task.exception?.message ?: "Error en registro")
                }
            }
    }

    fun loginUser(email: String, password: String, listener: AuthListener) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listener.onSuccess()
                } else {
                    listener.onError(task.exception?.message ?: "Error en inicio de sesión")
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    // Manejo de usuarios y roles
    fun getCurrentUserData(listener: UserDataListener) {
        val userId = currentUser?.uid ?: return

        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    listener.onUserDataLoaded(user)
                } else {
                    listener.onError("Usuario no encontrado")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onError(error.message)
            }
        })
    }

    fun getAdminData(adminId: String, listener: UserDataListener) {
        database.child("users").child(adminId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val admin = snapshot.getValue(User::class.java)
                if (admin != null) {
                    listener.onUserDataLoaded(admin)
                } else {
                    listener.onError("Administrador no encontrado")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onError(error.message)
            }
        })
    }

    fun updateUserFCMToken(token: String) {
        val userId = currentUser?.uid ?: return
        database.child("users").child(userId).child("fcmToken").setValue(token)
    }

    fun linkEmployeeToAdmin(employeeId: String, adminId: String, listener: OperationListener) {
        // Actualizar referencia en el empleado
        database.child("users").child(employeeId).child("adminId").setValue(adminId)

        // Actualizar lista de empleados del admin
        database.child("users").child(adminId).child("employees")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val employeesList = ArrayList<String>()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            child.getValue(String::class.java)?.let { employeesList.add(it) }
                        }
                    }

                    if (!employeesList.contains(employeeId)) {
                        employeesList.add(employeeId)
                    }

                    database.child("users").child(adminId).child("employees")
                        .setValue(employeesList)
                        .addOnSuccessListener { listener.onSuccess() }
                        .addOnFailureListener { e -> listener.onError(e.message ?: "Error al vincular") }
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    fun getAdminEmployees(adminId: String, listener: EmployeesListener) {
        database.child("users").child(adminId).child("employees")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val employeeIds = ArrayList<String>()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            child.getValue(String::class.java)?.let { employeeIds.add(it) }
                        }
                    }

                    if (employeeIds.isEmpty()) {
                        listener.onEmployeesLoaded(emptyList())
                        return
                    }

                    val employees = ArrayList<User>()
                    var loadedCount = 0

                    for (id in employeeIds) {
                        database.child("users").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val employee = snapshot.getValue(User::class.java)
                                    if (employee != null) {
                                        employees.add(employee)
                                    }

                                    loadedCount++
                                    if (loadedCount >= employeeIds.size) {
                                        listener.onEmployeesLoaded(employees)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    loadedCount++
                                    if (loadedCount >= employeeIds.size) {
                                        listener.onEmployeesLoaded(employees)
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    // Historial de notificaciones
    fun saveNotification(notification: NotificationItem) {
        val userId = currentUser?.uid ?: return
        val notificationId = database.child("notifications").child(userId).push().key ?: return

        database.child("notifications").child(userId).child(notificationId).setValue(notification)
            .addOnSuccessListener {
                // Actualizar estadísticas diarias
                updateDailyStats(notification)
            }
    }

    fun getNotificationHistory(listener: NotificationsListener) {
        val userId = currentUser?.uid ?: return

        database.child("notifications").child(userId)
            .orderByChild("timestamp")
            .limitToLast(100)  // Limitar a las últimas 100 notificaciones
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = ArrayList<NotificationItem>()

                    for (child in snapshot.children) {
                        val notification = child.getValue(NotificationItem::class.java)
                        if (notification != null) {
                            notifications.add(notification)
                        }
                    }

                    // Ordenar por timestamp descendente (más reciente primero)
                    notifications.sortByDescending { it.timestamp }
                    listener.onNotificationsLoaded(notifications)
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    fun getNotificationsByType(type: String, listener: NotificationsListener) {
        val userId = currentUser?.uid ?: return

        database.child("notifications").child(userId)
            .orderByChild("type")
            .equalTo(type)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = ArrayList<NotificationItem>()

                    for (child in snapshot.children) {
                        val notification = child.getValue(NotificationItem::class.java)
                        if (notification != null) {
                            notifications.add(notification)
                        }
                    }

                    notifications.sortByDescending { it.timestamp }
                    listener.onNotificationsLoaded(notifications)
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    // Estadísticas
    private fun updateDailyStats(notification: NotificationItem) {
        val userId = currentUser?.uid ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date(notification.timestamp))

        database.child("stats").child(userId).child("daily").child(today)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var stats = snapshot.getValue(TransactionStats::class.java)

                    if (stats == null) {
                        stats = TransactionStats(
                            date = today,
                            totalTransactions = 0,
                            totalAmount = 0.0,
                            bySource = HashMap(),
                            byAmount = HashMap()
                        )
                    }

                    // Actualizar estadísticas
                    val updatedStats = updateStats(stats, notification)

                    // Guardar estadísticas actualizadas
                    database.child("stats").child(userId).child("daily").child(today)
                        .setValue(updatedStats)
                        .addOnSuccessListener {
                            // Actualizar estadísticas semanales y mensuales
                            updateWeeklyAndMonthlyStats(notification)
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseManager", "Error al actualizar estadísticas: ${error.message}")
                }
            })
    }

    private fun updateStats(stats: TransactionStats, notification: NotificationItem): TransactionStats {
        val amount = notification.amount?.replace("[^0-9.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0

        // Actualizar contadores
        val totalTransactions = stats.totalTransactions + 1
        val totalAmount = stats.totalAmount + amount

        // Actualizar por fuente
        val source = notification.appName
        val bySource = stats.bySource.toMutableMap()
        bySource[source] = (bySource[source] ?: 0) + 1

        // Actualizar por rango de monto
        val amountRange = getAmountRange(amount)
        val byAmount = stats.byAmount.toMutableMap()
        byAmount[amountRange] = (byAmount[amountRange] ?: 0) + 1

        return stats.copy(
            totalTransactions = totalTransactions,
            totalAmount = totalAmount,
            bySource = bySource,
            byAmount = byAmount
        )
    }

    private fun getAmountRange(amount: Double): String {
        return when {
            amount <= 0 -> "0"
            amount < 10000 -> "0-10k"
            amount < 50000 -> "10k-50k"
            amount < 100000 -> "50k-100k"
            amount < 500000 -> "100k-500k"
            else -> "500k+"
        }
    }

    private fun updateWeeklyAndMonthlyStats(notification: NotificationItem) {
        val userId = currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = notification.timestamp

        // Determinar semana actual
        val weekFormat = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault())
        val currentWeek = weekFormat.format(calendar.time)

        // Determinar mes actual
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = monthFormat.format(calendar.time)

        // Actualizar estadísticas semanales
        updatePeriodStats(userId, "weekly", currentWeek, notification)

        // Actualizar estadísticas mensuales
        updatePeriodStats(userId, "monthly", currentMonth, notification)
    }

    private fun updatePeriodStats(userId: String, period: String, periodKey: String, notification: NotificationItem) {
        database.child("stats").child(userId).child(period).child(periodKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var stats = snapshot.getValue(TransactionStats::class.java)

                    if (stats == null) {
                        stats = TransactionStats(
                            date = periodKey,
                            totalTransactions = 0,
                            totalAmount = 0.0,
                            bySource = HashMap(),
                            byAmount = HashMap()
                        )
                    }

                    // Actualizar estadísticas
                    val updatedStats = updateStats(stats, notification)

                    // Guardar estadísticas actualizadas
                    database.child("stats").child(userId).child(period).child(periodKey)
                        .setValue(updatedStats)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseManager", "Error al actualizar estadísticas de periodo: ${error.message}")
                }
            })
    }

    fun getDailyStats(date: String, listener: StatsListener) {
        val userId = currentUser?.uid ?: return

        database.child("stats").child(userId).child("daily").child(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stats = snapshot.getValue(TransactionStats::class.java)
                    if (stats != null) {
                        listener.onStatsLoaded(stats)
                    } else {
                        listener.onError("No hay estadísticas para esta fecha")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    fun getWeeklyStats(week: String, listener: StatsListener) {
        val userId = currentUser?.uid ?: return

        database.child("stats").child(userId).child("weekly").child(week)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stats = snapshot.getValue(TransactionStats::class.java)
                    if (stats != null) {
                        listener.onStatsLoaded(stats)
                    } else {
                        listener.onError("No hay estadísticas para esta semana")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    fun getMonthlyStats(month: String, listener: StatsListener) {
        val userId = currentUser?.uid ?: return

        database.child("stats").child(userId).child("monthly").child(month)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stats = snapshot.getValue(TransactionStats::class.java)
                    if (stats != null) {
                        listener.onStatsLoaded(stats)
                    } else {
                        listener.onError("No hay estadísticas para este mes")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    listener.onError(error.message)
                }
            })
    }

    fun clearNotificationHistory(listener: OperationListener) {
        val userId = currentUser?.uid ?: return

        database.child("notifications").child(userId).removeValue()
            .addOnSuccessListener {
                listener.onSuccess()
            }
            .addOnFailureListener { e ->
                listener.onError(e.message ?: "Error al limpiar historial")
            }
    }

    // Share notification to employees
    fun shareNotificationWithEmployees(notification: NotificationItem) {
        val adminId = currentUser?.uid ?: return

        // Obtener empleados del admin
        getAdminEmployees(adminId, object : EmployeesListener {
            override fun onEmployeesLoaded(employees: List<User>) {
                for (employee in employees) {
                    // Guardar notificación en los nodos de los empleados
                    database.child("notifications").child(employee.uid).push().setValue(notification)
                }
            }

            override fun onError(message: String) {
                Log.e("FirebaseManager", "Error al compartir notificación: $message")
            }
        })
    }

    // Interfaces
    interface AuthListener {
        fun onSuccess()
        fun onError(message: String)
    }

    interface UserDataListener {
        fun onUserDataLoaded(user: User)
        fun onError(message: String)
    }

    interface EmployeesListener {
        fun onEmployeesLoaded(employees: List<User>)
        fun onError(message: String)
    }

    interface NotificationsListener {
        fun onNotificationsLoaded(notifications: List<NotificationItem>)
        fun onError(message: String)
    }

    interface StatsListener {
        fun onStatsLoaded(stats: TransactionStats)
        fun onError(message: String)
    }

    interface OperationListener {
        fun onSuccess()
        fun onError(message: String)
    }
}