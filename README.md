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

## 🔑 Licencia / activación (para alquilar la app)

AutoReply no funciona hasta que se activa con un **código** generado por vos
(el dueño de la app). No hace falta servidor ni internet: el código lleva su
propia firma y fecha de vencimiento, y queda **atado a ese teléfono**.

### Flujo con el cliente

1. El cliente instala la app y abre la pantalla **Inicio**. Ahí ve la tarjeta
   **Licencia** con el **ID de este teléfono** (un código corto, con botón
   para copiarlo).
2. Te pasa ese ID (WhatsApp, lo que sea).
3. Vos generás el código de activación en tu computadora:

   ```bash
   # Demo de 1 día
   python3 tools/generate_license.py --device-id <ID_DEL_CLIENTE> --demo

   # Alquiler (30 días por mes)
   python3 tools/generate_license.py --device-id <ID_DEL_CLIENTE> --rental-months 1
   python3 tools/generate_license.py --device-id <ID_DEL_CLIENTE> --rental-months 3
   ```

4. Le mandás el código que imprime el script (algo como
   `9P2KS-NAGSG-0HRDP-KRW2G`). Lo pega en el campo **Código de activación** y
   toca **Activar**.
5. Mientras la licencia esté vigente, la app funciona normalmente. Al vencer,
   el interruptor maestro se bloquea y deja de responder hasta que cargue un
   código nuevo (podés vender una renovación así, sin tocar el teléfono).

### Cómo funciona por dentro

- El código codifica: un hash corto del ID del teléfono, la fecha de
  vencimiento y el tipo de plan (demo/alquiler), firmados con HMAC-SHA256.
- La app verifica la firma con la misma clave y revisa que el hash del
  teléfono coincida y que no haya vencido — todo local, sin red.
- Un código generado para un teléfono **no sirve en otro** (el hash no
  coincide) y no se puede alargar atrasando la fecha del sistema (la app
  recuerda el último día visto).
- La clave secreta vive en `LicenseSecret.kt` (app) y en
  `tools/generate_license.py` (tu computadora) — **deben coincidir**. Guardá
  ese script en un lugar privado: quien lo tenga puede generar códigos
  válidos para cualquier teléfono. Si el código fuente de la app deja de ser
  privado, generá una clave nueva (ver comentario en `LicenseSecret.kt`) y
  recompilá.

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

## 🔄 Actualizaciones automáticas (sin compilar tú)

Este repo tiene **GitHub Actions** configurado. En cada cambio en `main`:

1. Se compila el APK en la nube automáticamente.
2. Se publica en **Releases** con la versión leída de `app/build.gradle.kts`.

La app incluye un **buscador de actualizaciones** (pantalla *Inicio*):
- Comprueba GitHub Releases al abrirse.
- Si hay versión nueva, ofrece **descargar e instalar** con un toque.

Para publicar una versión nueva basta con **subir el número de versión**
(`versionCode` y `versionName`) en `app/build.gradle.kts` y hacer push a `main`.

> La primera vez, en el teléfono debes permitir a AutoReply
> "instalar apps desconocidas" (la app te llevará a ese ajuste).

## 🗺️ Ideas para el futuro

- Variables en las respuestas (ej. nombre del contacto).
- Reglas por app (responder distinto en WhatsApp vs Telegram).
- Respuestas con retraso aleatorio para parecer más natural.
- Exportar/importar reglas.
- Integración opcional con IA para respuestas inteligentes.
