# NotiBank Lite

Lectura de notificaciones bancarias en voz alta — versión ligera.

---

## ¿Qué es?

**NotiBank Lite** es una aplicación Android que lee en voz alta las notificaciones que recibes de apps bancarias (Nequi, DaviPlata, Bancolombia y otras configurables), incluso cuando la app está cerrada.

Está pensada para usuarios que quieren escuchar sus movimientos bancarios sin tener que mirar la pantalla del celular.

---

## ¿Para qué sirve?

- 🗣️ **Leer en voz alta** notificaciones entrantes de bancos y otras apps.
- 🔊 **Anunciar por voz** incluso con la aplicación en segundo plano.
- ⏱️ **Controlar el tiempo de sesión** (4 horas por activación).
- 🏦 **Seleccionar qué bancos escuchar** (Nequi, DaviPlata, etc.).
- 📊 **Filtrar por monto**: solo leer notificaciones menores al límite que tú defines.
- 🔇 **Probar el volumen** de voz desde la pantalla principal.

---

## ¿Qué incluye esta versión?

| Función | Descripción |
|---------|-------------|
| **Pantalla de Inicio** | Botón de encendido, contador de sesión y botón "Probar volumen". |
| **Servicio en segundo plano** | `NotificationServiceLite` escucha notificaciones y las anuncia por TTS. |
| **Notificación persistente** | Indica que el servicio está activo (Android primero plano). |
| **Alarma de expiración** | Al terminar la sesión, aparece una notificación push avisando que el servicio se desactivó. |
| **Pantalla de Ajustes** | Activa/desactiva bancos (con logos), filtro por monto y umbral en pesos. |
| **Pantalla de Información** | Guía rápida de uso con 5 pasos numerados. |
| **Sin base de datos en la nube** | Todo el procesamiento es local en el dispositivo (no usa Supabase). |

---

## ¿Qué NO hace esta versión?

- ❌ No guarda historial de notificaciones en servidores externos.
- ❌ No requiere cuenta de usuario ni login (versión 1).
- ❌ No envía datos a la nube ni a terceros.
- ❌ No es la app completa (esa se distribuye por separado).

---

## Cómo usarla

1. Abre la app y pulsa el **botón de encendido** en Inicio.
2. Concede el permiso de **Acceso a notificaciones** cuando te lo pida.
3. El servicio se activa y leerá tus notificaciones bancarias en voz alta.
4. En **Ajustes**, elige qué bancos leer y si quieres filtrar por monto.
5. Al terminar la sesión (4 horas), recibirás una notificación push avisando que el servicio se detuvo.

---

## Tecnología

- **Lenguaje:** Kotlin
- **Platforma:** Android
- **Servicio:** `NotificationListenerService` (escucha nativa de notificaciones)
- **Voz:** `TextToSpeech` (TTS) con `AudioAttributes` de notificación
- **Alarmas:** `AlarmManager` (expiración de sesión en segundo plano)
- **Almacenamiento:** `SharedPreferences` local (sin nube)

---

## Estado del proyecto

Esta es la rama **`feature/notibank-lite`**. Está lista para probar en emulador o dispositivo físico.

- ✅ Compilación exitosa (`./gradlew :app:assembleDebug`)
- ✅ Notificación push de expiración funcional en segundo plano
- ✅ Sin dependencia de Supabase ni base de datos externa
- ✅ Documentación técnica en `README_LITE.md`
- ✅ Política de privacidad en `politica de privacidad/PRIVACY_POLICY.md`

---

## Futuras mejoras (hoja de ruta)

| Área | Mejora sugerida | ¿Lite o Premium? |
|------|-----------------|------------------|
| **Duración de sesión** | Configurable por el usuario (no solo 4 horas fijas) | Lite |
| **Historial local** | Guardar notificaciones leídas en SQLite/Room para revisar después | Lite |
| **Más bancos** | Soporte para Bancolombia, BBVA, DaviPlata, etc. mediante regex | Lite |
| **Cuentas de usuario** | Login y sincronización de preferencias entre dispositivos | Premium |
| **Historial en la nube** | Respaldar notificaciones leídas en Supabase/base de datos remota | Premium |
| **Programación horaria** | Activar/desactivar servicio según horario fijo (mañana/tarde/noche) | Premium |
| **Temas y accesibilidad** | Modo oscuro, tamaños de fuente, lectura de pantalla mejorada | Lite |

---

## Documentación relacionada

- [`README_LITE.md`](README_LITE.md) — Arquitectura técnica detallada, flujo TTS, servicio en segundo plano, contador, correcciones y limitaciones.
- [`politica de privacidad/PRIVACY_POLICY.md`](politica%20de%20privacidad/PRIVACY_POLICY.md) — Tratamiento de datos personales bajo la Ley 1581 de 2012 (Colombia).

---

¿Tienes dudas o sugerencias? Abre un issue en este repositorio.
