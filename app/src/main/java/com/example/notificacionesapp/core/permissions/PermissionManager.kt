package com.example.notificacionesapp.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling app permissions
 */
@Singleton
class PermissionManager @Inject constructor() {
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 123
    }
    
    /**
     * Check and request necessary permissions
     */
    fun checkAndRequestPermissions(activity: Activity) {
        val permissions = getRequiredPermissions()
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Get list of required permissions based on Android version
     */
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        
        // Permisos necesarios para Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        
        // Permisos necesarios para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Audio permissions for TTS
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        
        return permissions
    }
    
    /**
     * Check if notification service is enabled
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }
    
    /**
     * Open notification listener settings
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of denied permissions
     */
    fun getDeniedPermissions(context: Context): List<String> {
        val permissions = getRequiredPermissions()
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}
