package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentInfoLiteBinding

class InfoFragmentLite : BaseFragment<FragmentInfoLiteBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInfoLiteBinding {
        return FragmentInfoLiteBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        val instructions = """
✦ NOTIBANK LITE — GUÍA DE USO ✦

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1️⃣  ACTIVAR EL SERVICIO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Pulsa el botón circular de encendido (🔴) en la pantalla de Inicio.
• El servicio leerá tus notificaciones bancarias en voz alta, incluso con la app cerrada.
• La sesión dura 5 minutos (modo prueba). Al acabar, el servicio se detiene automáticamente y oirás:
  "Servicio de notificaciones desactivado, vuelve a conectarte".
• Puedes reactivarlo todas las veces que quieras.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2️⃣  PERMISOS NECESARIOS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Para que la app funcione correctamente debes conceder:

🔹 Acceso a notificaciones
   Ajustes → Apps → NotiBank Lite → Permisos → "Acceso a notificaciones" → ACTIVADO.
   Sin este permiso la app no puede leer las notificaciones entrantes.

🔹 Alarmas exactas (Android 12+)
   Permite que la app programe alarmas para detectar el fin de la sesión en segundo plano.

🔹 Notificaciones (Android 13+)
   Activa las notificaciones de la app para recibir el aviso de voz del sistema.

Si faltan permisos, la app te pedirá activarlos al iniciar.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3️⃣  VOLUMEN DE NOTIFICACIÓN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Usa el deslizador "Volumen de notificación" en la pantalla de Inicio.
• La prueba de voz ("Probar volumen") y las notificaciones reales comparten el mismo motor de voz,
  por lo que suenan al mismo nivel.
• El volumen real depende del nivel de "Notificaciones" que tengas ajustado en los Ajustes del sistema.
  La app no modifica silenciosamente el volumen del dispositivo.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4️⃣  CONFIGURACIÓN (AJUSTES)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
En la pestaña Ajustes puedes:

🔹 Seleccionar los bancos activos: Nequi y / o DaviPlata.
🔹 Activar o desactivar el filtro por monto.
🔹 Establecer el límite de monto (pesos): solo se leerán notificaciones menores a ese valor.
🔹 Pulsa "Guardar" para aplicar los cambios.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5️⃣  ESTADO DEL BOTÓN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• 🟢 Verde = servicio activo y escuchando.
• 🔴 Rojo = servicio apagado.
• 🟡 Amarillo = sesión expirada.

Si sales de la pantalla y vuelves, el botón y el contador se actualizan solos.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

¿Necesitas ayuda? Reinicia la app y vuelve a activar el servicio desde Inicio.
        """.trimIndent()

        binding.instructionsText.text = instructions
    }

    companion object {
        fun newInstance(): InfoFragmentLite {
            return InfoFragmentLite()
        }
    }
}
