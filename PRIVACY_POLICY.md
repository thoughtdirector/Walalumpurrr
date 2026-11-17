# Política de Privacidad — NotiBank Lite

**Fecha de entrada en vigor:** 17 de noviembre de 2026  
**Responsable del tratamiento:** NotiBank Lite / NotificacionesApp  
**Contacto de privacidad:** privacidad@notibank.co (ejemplo; actualizar con el canal real)

---

## 1. Alcance y aceptación

La presente Política de Privacidad describe cómo NotiBank Lite (en adelante, **la App**) recopila, usa, almacena, protege y transfiere los datos personales de sus usuarios, en cumplimiento de la **Ley 1581 de 2012** de Colombia y su decreto reglamentario **1377 de 2013**, así como los lineamientos de seguridad de la información de la norma **ISO/IEC 27001** cuando resulten aplicables.

Al instalar, acceder o usar NotiBank Lite, usted acepta expresamente el tratamiento de sus datos personales conforme a esta política.

---

## 2. Responsable del tratamiento

**Nombre:** NotificacionesApp / NotiBank Lite  
**Canal de atención para derechos de los titulares:** privacidad@notibank.co  
**Domicilio:** [Insertar dirección legal en Colombia]

El responsable es quien decide sobre la base de datos, el propósito del tratamiento y las medidas de seguridad aplicables.

---

## 3. Datos personales recopilados

NotiBank Lite es una aplicación de lectura de notificaciones bancarias por voz (TTS). Los datos se clasifican en:

### 3.1 Datos personales (no sensibles)
- Correo electrónico (si se crea cuenta de usuario en versiones futuras).
- Nombre de usuario o alias (si aplica).
- Identificador del dispositivo (Android ID, ID de publicidad de Google) para funcionamiento del servicio en segundo plano y alarmas de expiración de sesión.
- Preferencias de la app: bancos activos (Nequi, DaviPlata), filtro por monto, umbral de monto, volumen de notificación, hora de activación de sesión.

### 3.2 Datos personales sensibles (solo si el usuario los expone en notificaciones)
La App lee el contenido de notificaciones de otras aplicaciones (por ejemplo, Nequi, DaviPlata) mediante el permiso **NotificationListenerService** de Android. Ese contenido puede incluir:
- Nombres de remitentes o beneficiarios.
- Montos de transacciones.
- Fechas y horas de movimientos bancarios.

**Importante:** La App **no almacena** el contenido de las notificaciones en servidores externos en la versión Lite. Todo el procesamiento es local en el dispositivo. Los datos permanecen en la memoria del dispositivo y se borran al detener el servicio o al expirar la sesión.

---

## 4. Finalidad del tratamiento

Los datos personales se tratan únicamente para:

1. Leer en voz alta las notificaciones bancarias entrantes mediante síntesis de voz (TTS).
2. Gestionar la activación y expiración de la sesión del servicio (contador de 4 horas).
3. Permitir al usuario configurar qué bancos leer y aplicar filtros por monto.
4. Mostrar notificaciones push del sistema (ej. aviso de expiración de sesión).
5. Mejorar el funcionamiento técnico de la App y atender solicitudes de soporte.

No se usan los datos para publicidad, perfilado comercial, toma de decisiones automatizadas ni venta a terceros.

---

## 5. Base legal del tratamiento

El tratamiento se basa en:
- **Autorización previa, expresa e informada** del titular (Ley 1581 de 2012, Art. 7 y 8).
- **Ejecutoria de una relación contractual o precontractual** entre el usuario y la App (condiciones de uso).
- **Interés legítimo** del responsable para garantizar el funcionamiento técnico del servicio.

---

## 6. Derechos de los titulares (Ley 1581 de 2012, Art. 8)

Como titular de datos personales, usted tiene derecho a:

a) **Conocer, actualizar y rectificar** sus datos personales frente al responsable.  
b) **Solicitar prueba de la autorización** otorgada.  
c) **Ser informado** sobre el uso que se le ha dado a sus datos personales.  
d) **Presentar quejas ante la Superintendencia de Industria y Comercio** por infracciones a las normas de protección de datos.  
e) **Revocar la autorización** y/o **suprimir** sus datos cuando el tratamiento no respete los principios legales, salvo que exista un deber legal o contractual de conservarlos.  
f) **Acceder en forma gratuita** a sus datos personales al menos una vez al mes.

---

## 7. Cómo ejercer sus derechos

Para ejercer cualquiera de los derechos anteriores, envíe una solicitud escrita al canal de privacidad:

**Correo:** privacidad@notibank.co  
**Asunto:** «Derechos de titular — NotiBank Lite»  
**Contenido mínimo:** nombre completo, documento de identidad, derecho que solicita, datos de contacto y firma.

El responsable responderá dentro de los términos establecidos por la Ley 1581 de 2012 (máximo 15 días hábiles para conocer y 8 días hábiles para suprimir, salvo casos complejos debidamente motivados).

---

## 8. Almacenamiento y ubicación de los datos

- **Versión Lite:** Los datos de configuración (bancos activos, umbral de monto, timestamp de sesión) se almacenan en **SharedPreferences** del dispositivo Android, de forma local. No hay base de datos en la nube en esta versión.
- **Notificaciones leídas:** No se guardan en servidores externos. Se procesan en memoria y se descartan al expirar la sesión o detener el servicio.
- **Versiones futuras:** Si se integra almacenamiento en la nube (por ejemplo, Supabase), se notificará al usuario mediante actualización de esta política y se obtendrá autorización expresa para transferencia internacional de datos, si aplica.

---

## 9. Transferencia y transmisión de datos

NotiBank Lite **no transfiere** datos personales a terceros ni realiza transferencias internacionales en su versión actual.

Si en versiones futuras se usa un servicio en la nube ubicado fuera de Colombia (por ejemplo, proveedores de infraestructura en EE. UU. o Europa), se informará al usuario, se obtendrá su autorización previa y se celebrarán las cláusulas contractuales tipo o mecanismos equivalentes exigidos por la Ley 1581 de 2012.

---

## 10. Medidas de seguridad

El responsable ha adoptado medidas técnicas, administrativas y físicas para proteger los datos personales contra el acceso no autorizado, la alteración, la pérdida o la divulgación indebida, alineadas con los principios de la **ISO/IEC 27001** (Sistema de Gestión de Seguridad de la Información):

- Almacenamiento local en SharedPreferences de Android (acceso restringido a la app).
- No se almacenan credenciales bancarias ni contraseñas de los usuarios.
- El servicio de escucha de notificaciones requiere el permiso del sistema `BIND_NOTIFICATION_LISTENER_SERVICE`, otorgado explícitamente por el usuario en los ajustes de Android.
- Las alarmas de expiración de sesión usan `AlarmManager` con permiso `SCHEDULE_EXACT_ALARM`, gestionado exclusivamente por la App.

Ninguna medida de seguridad es absoluta; el responsable notificará al titular y a las autoridades competentes en caso de incidente de seguridad que afecte sus datos personales, conforme a la normativa aplicable.

---

## 11. Menores de edad

NotiBank Lite no está dirigida a menores de 18 años y no recopila datos personales de niños, niñas o adolescentes de manera intencional. Si usted es menor de edad, no debe usar la App sin la supervisión y autorización de un representante legal.

---

## 12. Vigencia y cambios a la política

Esta política es vigente a partir de su publicación. El responsable podrá modificarla en cualquier momento. Los cambios se comunicarán a los usuarios mediante la App o por correo electrónico, y entrarán en vigor tras su publicación. El uso continuado de la App tras la notificación de cambios implica aceptación de los mismos.

---

## 13. Contacto y consultas

Para cualquier duda, queja o solicitud relacionada con esta política o con el tratamiento de sus datos personales:

**Responsable:** NotiBank Lite / NotificacionesApp  
**Correo de privacidad:** privacidad@notibank.co  
**Teléfono:** [Insertar número de contacto]  
**Dirección:** [Insertar dirección física en Colombia]

---

### Referencias normativas

- Ley 1581 de 2012 (Colombia) — Protección de Datos Personales.  
- Decreto 1377 de 2013 (Colombia) — Reglamento parcialmente de la Ley 1581 de 2012.  
- Circular Única de la Superintendencia de Industria y Comercio en materia de protección de datos personales.  
- ISO/IEC 27001 — Sistemas de gestión de seguridad de la información (referencia de buenas prácticas de seguridad).

---

*Este documento es una plantilla base. Antes de publicarlo en Play Store o usarlo en producción, revíselo con un abogado especializado en protección de datos personales en Colombia para ajustar los datos del responsable, canales de contacto y cualquier requisito sectorial adicional.*
