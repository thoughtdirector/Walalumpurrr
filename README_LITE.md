# NotiBank Lite — Documentación técnica

> **Nota:** Este documento describe exclusivamente la versión **Lite** de NotiBank. No sobrescribe ni reemplaza la documentación general del proyecto principal.

---

## 1. Descripción general

**NotiBank Lite** es una variante ligera de NotificacionesApp cuya función principal es leer en voz alta las notificaciones bancarias entrantes (Nequi, DaviPlata, etc.) mediante síntesis de voz (TTS). Incluye una pantalla de inicio con botón de activación, contador de sesión, control de volumen y una pantalla de ajustes para filtrar bancos y montos.

**Diferencia clave con la versión principal:** Lite **no usa Supabase ni guarda historial en la nube**. Todo el procesamiento es local en el dispositivo.

---

## 2. Arquitectura real

| Componente | Responsabilidad |
|---|---|
| `MainActivityLite` | Actividad principal de Lite. Alberga el `TextToSpeech`, gestiona los fragmentos (Home / Info / Ajustes) y expone `toggleNotificationService()` / `testTTS()`. |
| `HomeFragmentLite` | Pantalla principal. Botón de encendido, contador de sesión, prueba de voz y estado del servicio. |
| `InfoFragmentLite` | Guía de uso con permisos, volumen, configuración y estados del botón. |
| `SettingsFragmentLite` | Bancos activos (con logos), filtro por monto y umbral (usa `app_settings`). |
| `NotificationServiceLite` | `NotificationListenerService` ligero **sin Supabase**. Escucha notificaciones nativas de Android, las procesa con regex y las anuncia por TTS. Mantiene `isServiceActive` (estático). |
| `SessionAlarmManager` | Gestiona la alarma de expiración de sesión (patrón `ScheduleManager` de la versión principal). |
| `SessionExpirationReceiver` | `BroadcastReceiver` que muestra notificación push al expirar la sesión. |
| `SessionManager` | Sesión de usuario (compartida con la versión principal, no usada en Lite v1). |

**Flujo de activación (Lite):**
1. Usuario pulsa el botón de encendido en Home.
2. Se valida el permiso de acceso a notificaciones.
3. Se guarda `activation_timestamp` en `SharedPreferences` (`lite_session_prefs`).
4. Se inicia `NotificationServiceLite` en primer plano (`startForegroundService` + notificación persistente).
5. Se programa una alarma exacta (`AlarmManager.setExactAndAllowWhileIdle`) para `activation_timestamp + 4 horas`.
6. Mientras el servicio esté activo, las notificaciones bancarias se leen por TTS.

**Flujo de expiración (Lite):**
1. La alarma dispara `SessionExpirationReceiver`.
2. El receiver detiene `NotificationServiceLite` y muestra una **notificación push** visible.
3. Al volver el usuario a Home, se anuncia por TTS si corresponde y se limpia la sesión.

---

## 3. Funcionalidades

- **Activación/desactivación del servicio** desde el botón circular de Inicio.
- **Lectura de notificaciones bancarias en voz alta** (Nequi, DaviPlata y otras apps configuradas), incluso con la app cerrada.
- **Notificación persistente** que indica que el servicio está activo (primer plano).
- **Contador de sesión** con tiempo restante, mostrado en pantalla de Inicio.
- **Expiración automática** de la sesión (4 horas en producción).
- **Aviso por push al expirar** la sesión (confiable en segundo plano).
- **Aviso por voz al volver** a la app si la sesión expiró mientras estaba cerrada.
- **Prueba de voz** manual ("Probar volumen").
- **Ajustes:** selección de bancos activos (Nequi / DaviPlata con logos) y filtro por monto con umbral.
- **Sincronización de estado** al volver de cualquier pantalla (Home ↔ Info ↔ Ajustes).

---

## 4. Servicio en segundo plano

- Implementado como `NotificationListenerService` (`NotificationServiceLite`) con `startForeground` y notificación persistente (ID 1002, canal `notification_service_lite_channel`, importancia baja).
- **No usa Supabase ni polling de nube.** Solo depende del listener nativo de Android (`onNotificationPosted`).
- **Detención al expirar:** `SessionExpirationReceiver` dispara por alarma exacta (`RTC_WAKEUP`) y detiene el servicio mediante `stopService` → `stopSelf()`.
- **Permisos requeridos:** `SCHEDULE_EXACT_ALARM` (Android 12+), `POST_NOTIFICATIONS` (Android 13+), `BIND_NOTIFICATION_LISTENER_SERVICE`.

---

## 5. Contador de tiempo

- `SESSION_DURATION_MS` = **4 horas** (producción).
- El `activation_timestamp` se persiste en `SharedPreferences` (`lite_session_prefs`, clave `activation_timestamp`).
- **Actualización de UI:** un `Handler` hace *tick* cada 1 s **solo mientras el fragmento Home es visible** (`onResume` / `onStop`). No hay temporizadores en segundo plano.
- Al volver a la pantalla, el contador se recalcula desde `System.currentTimeMillis() - activation_timestamp` (no se reinicia).

---

## 6. Reproducción TTS

- Una única instancia de `TextToSpeech` vive en `MainActivityLite`.
- `MainActivityLite.onInit()` configura `AudioAttributes` con `USAGE_NOTIFICATION_EVENT` + `CONTENT_TYPE_SPEECH` e idioma `es-ES`.
- **Ruta de prueba:** `testTTS()` → `tts.speak(..., "test_id")`.
- **Ruta de notificación:** `NotificationServiceLite.speakOut()` → broadcast `TTS_SPEAK` (`com.example.notibanklite.TTS_SPEAK`) → `ttsReceiver` en `MainActivityLite` → `tts.speak(..., "svc_tts")`.
- Ambas rutas comparten la misma instancia de `tts` y los mismos `AudioAttributes`, por lo que el volumen es consistente.
- **El slider de volumen fue eliminado** (no afectaba al TTS). Queda solo el botón "Probar volumen".

---

## 7. Correcciones aplicadas (bugs resueltos)

### Bug 1 — Botón parece apagado al volver de Información
**Causa:** `HomeFragmentLite` decidía el estado por la etiqueta de la vista (`btnPower.tag`) y sobrescribía `isServiceActive` desde ella. Al navegar Home → Info → Home, la vista se destruía/recreaba y el `tag` volvía a `null`.

**Solución:** La fuente de verdad es `NotificationServiceLite.isServiceActive`. La UI solo sincroniza su etiqueta desde ese estado; nunca lo sobrescribe.

### Bug 2 — Contador no se actualiza al volver
**Causa:** `updateSessionInfo()` solo se invocaba al crear la vista y al pulsar el botón.

**Solución:** `onResume` ejecuta `checkExpirationAndSync()` + `startSessionTicker()` (tick de 1 s mientras es visible). `onStop` detiene el ticker.

### Bug 3 — TTS: prueba manual fuerte, notificaciones más bajas
**Causa:** Ambas rutas usaban el mismo `tts.speak()` con los mismos `AudioAttributes`; el slider usaba `setStreamVolume(STREAM_NOTIFICATION)`, tocando silenciosamente el volumen del sistema.

**Solución:** Se eliminó `applyNotificationVolume` y el uso de `setStreamVolume`. El slider fue eliminado. El volumen es consistente entre prueba y notificaciones.

---

## 8. Compilación

- Proyecto Android con Gradle (Kotlin DSL).
- Tarea de compilación: `./gradlew :app:compileDebugKotlin`
- Tarea de build completo: `./gradlew :app:assembleDebug`
- SDK mínimo: consultar `app/build.gradle.kts`. Requiere `SCHEDULE_EXACT_ALARM` (API 31+) para alarmas exactas en segundo plano.

---

## 9. Pruebas

**Pruebas automatizadas:** existen en `app/src/test/` (JUnit). Ejecutar con `./gradlew :app:testDebugUnitTest`.

**Pruebas manuales:**
1. Activar servicio → entrar a Info → regresar: el botón debe seguir activo.
2. Activar servicio → poner app en segundo plano → esperar 4 horas: debe aparecer una notificación push de expiración.
3. Volver a la app: el botón debe verse apagado y el contador en "sin iniciar".
4. Salir y volver a Inicio: el contador debe actualizarse cada segundo.
5. Probar volumen (botón "Probar volumen") y recibir una notificación real: deben sonar al mismo nivel.

---

## 10. Limitaciones

- **Expiración en segundo plano:** depende del permiso `SCHEDULE_EXACT_ALARM` y de que el sistema no restrinja alarmas (optimización de batería / Doze).
- **Muerte del proceso:** si el sistema mata la app, `isServiceActive` se resetea. El `activation_timestamp` persiste, pero el anuncio de expiración solo ocurre al volver a abrir la app.
- **Volumen:** la app no puede forzar el volumen del sistema ni superar los límites físicos del dispositivo.
- **TTS:** requiere motor de TTS instalado y lenguaje español disponible.
- **Sin base de datos en la nube:** Lite no guarda historial de notificaciones en Supabase. Todo es local y efímero.

---

## 11. Ícono del launcher y retorno a la versión normal

Por defecto en esta rama Lite, el ícono del launcher abre directamente `MainActivityLite`. Esto se configura moviendo el filtro `android.intent.action.MAIN` + `android.intent.category.LAUNCHER` de `MainActivity` a `MainActivityLite` en `AndroidManifest.xml`.

### Cómo volver a la versión normal (`MainActivity`):

1. Abre `app/src/main/AndroidManifest.xml`.
2. Quita el `<intent-filter>` de `MAIN`/`LAUNCHER` de `MainActivityLite`.
3. Vuelve a agregar el `<intent-filter>` de `MAIN`/`LAUNCHER` a `MainActivity`.
4. Sincroniza Gradle y vuelve a compilar.

---

## 12. Funcionalidades futuras (sugerencias, no implementadas)

- Gestionar duración de sesión desde configuración remota.
- `WorkManager` más robusto para anuncios con app cerrada.
- Extraer lógica de sesión a un `ViewModel`/repositorio.
- Diferenciar claramente funcionalidades Lite vs Premium.
- Historial local de notificaciones (Room/SQLite) sin depender de Supabase.
