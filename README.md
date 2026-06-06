# AutoReply

App de Android (tipo WhatAuto) que responde **automáticamente** a los mensajes
entrantes de cualquier app de mensajería (WhatsApp, Telegram, Messenger, etc.),
usando reglas, horarios, mensaje de ausencia y control anti-spam.

No requiere root. Funciona leyendo las notificaciones y usando la acción de
**respuesta directa** que las apps de mensajería incluyen en sus notificaciones.

---

## ✨ Funciones

- **Interruptor maestro** para activar/desactivar todas las respuestas.
- **Reglas** "palabra clave → respuesta" con 4 tipos de coincidencia:
  *Contiene*, *Exacto*, *Empieza con* y *Cualquier mensaje* (comodín).
- **Mensaje de ausencia**: respuesta por defecto cuando ninguna regla coincide.
- **Horario de actividad**: responde solo en un rango de horas y días elegidos
  (soporta rangos que cruzan la medianoche, p. ej. 22:00 → 06:00).
- **Chats de grupo**: opción para responder o ignorar grupos.
- **Anti-spam**: tiempo mínimo configurable entre respuestas al mismo contacto.
- **Registro** de todas las respuestas enviadas.
- Responde a **todas** las apps de mensajería que admitan respuesta directa.

---

## 🏗️ Arquitectura (MVVM por capas)

```
ui/        -> Jetpack Compose + ViewModels (Inicio, Reglas, Ajustes, Registro)
domain/    -> Modelos + ReplyEngine (lógica pura de coincidencia y horario)
data/      -> Room (reglas y registro) + DataStore (ajustes) + repositorios
service/   -> NotificationListenerService (lee y responde notificaciones)
di/        -> AppContainer (inyección de dependencias manual y ligera)
```

**Stack:** Kotlin · Jetpack Compose (Material 3) · Room · DataStore ·
Coroutines/Flow · Navigation Compose.

---

## 🔧 Compilar e instalar

### Requisitos
- Android Studio (versión reciente, ej. Ladybug o superior).
- Un teléfono Android **8.0 (API 26) o superior**.

### Pasos
1. Abre **Android Studio** → *Open* → selecciona la carpeta `AutoReply`.
2. Espera a que Gradle sincronice. Si te pide instalar el **Android SDK 35**
   o crear el *Gradle wrapper*, acepta.
3. Conecta tu teléfono por USB con la **depuración USB** activada
   (o crea un emulador).
4. Pulsa **Run ▶** para compilar e instalar la app.

> Para generar un APK instalable manualmente:
> `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
> El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📱 Configuración en el teléfono (¡importante!)

1. Abre la app **AutoReply**.
2. En la pantalla **Inicio**, pulsa **Activar** y concede el permiso de
   **"Acceso a las notificaciones"** a AutoReply.
3. Vuelve a la app y enciende el **interruptor maestro**.
4. Ve a **Reglas** y crea tus respuestas (o usa solo el mensaje de ausencia).
5. (Opcional) Ajusta horario, grupos y anti-spam en **Ajustes**.

A partir de ahí, cuando llegue un mensaje, AutoReply responderá según tus reglas.

---

## ⚠️ Notas y limitaciones

- Solo puede responder en apps cuya notificación incluye **respuesta directa**
  (la mayoría: WhatsApp, Telegram, Messenger, Signal, etc.). Si una app no la
  ofrece, no se puede responder desde la notificación.
- Algunos fabricantes (Xiaomi, Huawei, Oppo…) son agresivos cerrando servicios
  en segundo plano. Si deja de responder, **desactiva la optimización de batería**
  para AutoReply y permite su ejecución en segundo plano.
- El servicio puede tardar unos segundos en reconectarse tras reiniciar el
  teléfono; abre la app una vez para asegurarte de que está activo.
- Esta app es para uso personal. Úsala de forma responsable: avisa a tus
  contactos de que reciben respuestas automáticas.

---

## 🗺️ Ideas para el futuro

- Variables en las respuestas (ej. nombre del contacto).
- Reglas por app (responder distinto en WhatsApp vs Telegram).
- Respuestas con retraso aleatorio para parecer más natural.
- Exportar/importar reglas.
- Integración opcional con IA para respuestas inteligentes.
