# NotiBank Lite — Documentación técnica

> **Nota:** Este documento describe exclusivamente la versión **Lite** de NotiBank. No sobrescribe ni reemplaza la documentación general del proyecto principal.

---

## 1. Descripción general

**NotiBank Lite** es una variante ligera de NotificacionesApp cuya función principal es leer en voz alta las notificaciones bancarias entrantes (Nequi, DaviPlata, etc.) mediante un servicio de segundo plano y síntesis de voz (TTS). Incluye una pantalla de inicio con botón de activación, contador de sesión, control de volumen y una pantalla de ajustes para filtrar bancos y montos.

---

## 2. Arquitectura real

La versión Lite reutiliza la infraestructura de la versión principal (Hilt, Supabase, NotificationListenerService, procesadores de notificaciones) y añade sus propias capas:

| Componente | Responsabilidad |
|---|---|
| `MainActivityLite` | Actividad principal de Lite. Alberga el `TextToSpeech`, gestiona los fragmentos (Home / Info / Ajustes) y expone `toggleNotificationService()` / `testTTS()`. |
| `HomeFragmentLite` | Pantalla principal. Botón de encendido, contador de sesión, slider de volumen y ticker de UI (1 s) mientras es visible. |
| `InfoFragmentLite` | Guía de uso con permisos, volumen, configuración y estados del botón. |
| `SettingsFragmentLite` | Bancos activos, filtro por monto y umbral (usa `app_settings`). |
| `NotificationService` | `NotificationListenerService` compartido. Escucha notificaciones, las procesa y las lee por TTS mediante broadcast `TTS_SPEAK`. Mantiene `isServiceActive` (estático) y polling de respaldo cada 8 s. |
| `SessionExpirationReceiver` | `BroadcastReceiver` despierto por `AlarmManager` al expirar la sesión. Detiene el servicio incluso si la app está en segundo plano o cerrada. |
| `SessionManager` | Sesión de usuario (login/rol/id) — compartida con la versión principal. |

**Flujo de activación:**
1. Usuario pulsa el botón de encendido en Home.
2. Se valida el permiso de acceso a notificaciones.
3. Se guarda `activation_timestamp` en `SharedPreferences` (`lite_session_prefs`).
4. Se inicia `NotificationService` en primer plano (`startForegroundService` + notificación persistente).
5. Se programa una alarma exacta (`AlarmManager.setExactAndAllowWhileIdle`) para `activation_timestamp + duración_sesión`.
6. Mientras el servicio esté activo, las notificaciones bancarias se leen por TTS.

**Flujo de expiración:**
1. La alarma dispara `SessionExpirationReceiver`.
2. El receiver envía `ACTION_STOP_SERVICE` al `NotificationService`.
3. El servicio detiene el foreground y llama `stopSelf()` para morir completamente.
4. El receiver deja un flag (`session_expired_notified`) en `SharedPreferences`.
5. Al volver el usuario a Home, `HomeFragmentLite` lee el flag, anuncia por TTS *"Servicio de notificaciones desactivado, vuelve a conectarte"*, limpia la sesión y muestra el botón apagado.

---

## 3. Funcionalidades

- **Activación/desactivación del servicio** desde el botón circular de Inicio.
- **Lectura de notificaciones bancarias en voz alta** (Nequi, DaviPlata y otras apps configuradas), incluso con la app cerrada.
- **Notificación persistente** que indica que el servicio está activo (primer plano).
- **Contador de sesión** con tiempo restante, mostrado en pantalla de Inicio.
- **Expiración automática** de la sesión (duración configurable en código, actualmente 2 minutos para pruebas).
- **Aviso por voz al expirar** la sesión.
- **Control de volumen de notificación** (slider que guarda preferencia; no toca el volumen del sistema).
- **Prueba de voz** manual ("Probar volumen").
- **Ajustes:** selección de bancos activos (Nequi / DaviPlata) y filtro por monto con umbral.
- **Sincronización de estado** al volver de cualquier pantalla (Home ↔ Info ↔ Ajustes).

---

## 4. Servicio en segundo plano

- Implementado como `NotificationListenerService` (`NotificationService`) con `startForeground` y notificación persistente (ID 1001, canal `notification_service_channel`, importancia baja).
- **Polling de respaldo:** consulta Supabase cada ~8 s (hasta 15 s tras polls sin novedad) para detectar notificaciones si falla el canal Realtime.
- **Detención al expirar:** `SessionExpirationReceiver` dispara por alarma exacta (`RTC_WAKEUP`) y detiene el servicio mediante `stopService` → `stopSelf()`.
- **Permiso requerido:** `SCHEDULE_EXACT_ALARM` (Android 12+). Si no se concede, la alarma de fondo no funcionará y el servicio solo se detendrá al volver a abrir la app (fallback en `onResume`).

---

## 5. Contador de tiempo

- `SESSION_DURATION_MS` define la duración de la sesión. Actualmente **2 minutos** (modo prueba). Antes fue 4 horas y 5 minutos.
- El `activation_timestamp` se persiste en `SharedPreferences` (`lite_session_prefs`, clave `activation_timestamp`).
- **Actualización de UI:** un `Handler` hace *tick* cada 1 s **solo mientras el fragmento Home es visible** (`onResume` / `onStop`). No hay temporizadores en segundo plano; la expiración real se maneja con `AlarmManager`.
- Al volver a la pantalla, el contador se recalcula desde `System.currentTimeMillis() - activation_timestamp` (no se reinicia).

---

## 6. Reproducción TTS

- Una única instancia de `TextToSpeech` vive en `MainActivityLite`.
- `MainActivityLite.onInit()` configura `AudioAttributes` con `USAGE_NOTIFICATION_EVENT` + `CONTENT_TYPE_SPEECH` e idioma `es-ES`.
- **Ruta de prueba:** `testTTS()` → `tts.speak(..., "test_id")`.
- **Ruta de notificación:** `NotificationService.speakOut()` → broadcast `TTS_SPEAK` → `ttsReceiver` en `MainActivityLite` → `tts.speak(..., "svc_tts")`.
- Ambas rutas comparten la misma instancia y los mismos `AudioAttributes`, por lo que el volumen es consistente.
- El slider de volumen **no modifica el volumen del sistema** (`setStreamVolume` eliminado). Solo guarda la preferencia y muestra el porcentaje. El volumen real depende del nivel de "Notificaciones" del sistema.

---

## 7. Correcciones aplicadas (bugs resueltos)

### Bug 1 — Botón parece apagado al volver de Información
**Causa:** `HomeFragmentLite.updateStatus()` decidía el estado por la etiqueta de la vista (`btnPower.tag`) y sobrescribía `NotificationService.isServiceActive` desde ella. Al navegar Home → Info → Home, la vista se destruía/recreaba y el `tag` volvía a `null`, apagando el botón visual y corrompiendo el estado estático.

**Solución:** La fuente de verdad es ahora `NotificationService.isServiceActive`. La UI solo sincroniza su etiqueta desde ese estado; nunca lo sobrescribe. Al volver de Info, si el servicio sigue activo, el botón se pinta activo sin reiniciar el servicio ni el contador.

### Bug 2 — Contador no se actualiza al volver
**Causa:** `updateSessionInfo()` solo se invocaba al crear la vista y al pulsar el botón. No había refresco en `onResume` ni mecanismo periódico.

**Solución:** `onResume` ejecuta `checkExpirationAndSync()` + `startSessionTicker()` (tick de 1 s mientras es visible). `onStop` / `onDestroyView` detienen el ticker. Se añadió `SessionExpirationReceiver` + `AlarmManager` para detener el servicio en segundo plano al expirar. Al volver, si expiró en segundo plano, se anuncia por TTS y se limpia la sesión.

### Bug 3 — TTS: prueba manual fuerte, notificaciones más bajas
**Causa:** Ambas rutas usaban el mismo `tts.speak()` con los mismos `AudioAttributes`; no había diferenciación de código. Además, el slider usaba `setStreamVolume(STREAM_NOTIFICATION)`, tocando silenciosamente el volumen del sistema.

**Solución:** Se eliminó `applyNotificationVolume` y el uso de `setStreamVolume`. El slider solo guarda/muestra el porcentaje sin tocar el sistema. El volumen TTS es consistente entre prueba y notificaciones.

---

## 7. Ícono del launcher y retorno a la versión normal

Por defecto en esta rama Lite, el ícono del launcher del emulador/dispositivo abre directamente **MainActivityLite** (versión Lite). Esto se configura moviendo el filtro `android.intent.action.MAIN` + `android.intent.category.LAUNCHER` de `MainActivity` a `MainActivityLite` en `AndroidManifest.xml`.

### Cómo volver a la versión normal (MainActivity)

Cuando se quiera restaurar la versión principal como punto de entrada:

1. Abre `app/src/main/AndroidManifest.xml`.
2. Quita el `<intent-filter>` de `MAIN`/`LAUNCHER` de `MainActivityLite`.
3. Vuelve a agregar el `<intent-filter>` de `MAIN`/`LAUNCHER` a `MainActivity`.
4. Sincroniza Gradle y vuelve a compilar.

De esta forma el ícono del launcher volverá a abrir la app normal (`MainActivity`).

---

## 8. Compilación

- Proyecto Android con Gradle (Kotlin DSL).
- Tarea de compilación: `./gradlew :app:compileDebugKotlin`
- Tarea de build completo: `./gradlew :app:assembleDebug`
- SDK mínimo: consultar `app/build.gradle.kts`. Requiere `SCHEDULE_EXACT_ALARM` (API 31+) para alarmas exactas en segundo plano.
- Firmado: para pruebas se usa el build `debug`.

---

## 9. Pruebas

**Pruebas automatizadas:** existen en `app/src/test/` (JUnit) para `AmountSettings`, `SessionManager`, `ScheduleManager`, procesadores de notificaciones y `MainActivity`. Ejecutar con `./gradlew :app:testDebugUnitTest`.

**Pruebas manuales (pendiente de confirmación del usuario):**
1. Activar servicio → entrar a Info → regresar: el botón debe seguir activo (verde/halo).
2. Activar servicio → poner app en segundo plano → esperar 2 minutos: la notificación persistente debe desaparecer; al volver a la app, el botón debe verse apagado y sonar el aviso de voz de desconexión.
3. Activar servicio → salir y volver a Inicio: el contador debe actualizarse cada segundo (no congelado).
4. Probar volumen (botón "Probar volumen") y recibir una notificación real: deben sonar al mismo nivel.
5. Slider de volumen: no debe cambiar silenciosamente el volumen de notificaciones del sistema.

---

## 10. Limitaciones

- **Expiración en segundo plano:** depende del permiso `SCHEDULE_EXACT_ALARM` y de que el sistema no restrinja alarmas (optimización de batería / Doze). Si falla, el servicio solo se detendrá al reabrir la app.
- **Muerte del proceso:** si el sistema mata la app, `NotificationService.isServiceActive` (estático) se resetea. El `activation_timestamp` persiste, pero el anuncio de expiración solo ocurre al volver a abrir la app.
- **Volumen:** la app no puede forzar el volumen del sistema ni superar los límites físicos del dispositivo. El volumen real depende del ajuste de "Notificaciones" del usuario.
- **TTS:** requiere motor de TTS instalado y lenguaje español disponible.
- **Duración de sesión:** está hardcodeada en `SESSION_DURATION_MS` (2 min para prueba). Para producción se recomienda moverla a configuración remota o `SharedPreferences`.

---

## 11. Funcionalidades futuras (sugerencias, no implementadas)

Véase la sección de recomendaciones senior en el informe de corrección. Entre ellas: gestionar duración de sesión desde configuración, `AlarmManager`/`WorkManager` más robusto para anuncios con app cerrada, extraer lógica de sesión a un `ViewModel`/repositorio, migrar el temporizador manual a `LifecycleObserver`, y diferenciar claramente funcionalidades Lite vs Premium.
