# Guía de Desarrollo para Agentes - anti-spam

Este documento contiene las directrices, arquitectura y flujos de trabajo esenciales para cualquier agente de IA o desarrollador que trabaje en el repositorio **anti-spam**.

---

## 📌 Visión General del Proyecto

- **Nombre de la App**: `anti-spam`
- **Repositorio**: `Pablo-Millones/AntiSpam-600-80`
- **Plataforma**: Android Nativo (Kotlin)
- **UI**: Jetpack Compose + Material 3 (Tema Claro y Oscuro)
- **Objetivo**: Bloquear y colgar de forma automática llamadas entrantes de publicidad, cobranza y telemarketing que comiencen con prefijos no deseados (por defecto **600** y **80**, incluyendo 800, 801, etc. en Chile y otros países).

---

## 🏗️ Arquitectura Técnica

### 1. Capa de Interceptación Dual (Crítico)
Para garantizar la máxima confiabilidad en todas las marcas y capas de Android (especialmente ColorOS, OxygenOS, MIUI/HyperOS, OneUI):
- **Interceptor Principal**: [`SpamCallScreeningService`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/service/SpamCallScreeningService.kt)
  - Utiliza la API oficial `android.telecom.CallScreeningService`.
  - Requiere que la app tenga el rol del sistema `RoleManager.ROLE_CALL_SCREENING`.
  - Construye un `CallResponse` con `setDisallowCall(true)`, `setRejectCall(true)` y `setSkipNotification(true)`.
- **Interceptor de Respaldo Instantáneo**: [`IncomingCallReceiver`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/service/IncomingCallReceiver.kt)
  - `BroadcastReceiver` registrado para `android.intent.action.PHONE_STATE`.
  - Se activa inmediatamente al detectar el estado `EXTRA_STATE_RINGING`.
  - Corta la llamada de inmediato utilizando `TelecomManager.endCall()`.
  - Requiere los permisos `ANSWER_PHONE_CALLS`, `READ_PHONE_STATE` y `READ_CALL_LOG`.

### 2. Normalización de Números
- [`PhoneNumberHelper`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/util/PhoneNumberHelper.kt):
  - Limpia caracteres no numéricos (espacios, guiones, paréntesis).
  - Remueve el prefijo de país `56` cuando aplica.
  - Verifica coincidencias directas y con código internacional para los prefijos configurados.

### 3. Persistencia y Reactividad
- [`BlockRuleManager`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/data/BlockRuleManager.kt): Gestiona los prefijos bloqueados y configuraciones en `SharedPreferences`.
- [`BlockedCallsRepository`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/data/BlockedCallsRepository.kt): Almacena el historial en formato JSON y el contador total acumulado. Expone `registerListener` / `unregisterListener` para notificar cambios en tiempo real a la interfaz.

### 4. Interfaz de Usuario (Compose)
- [`MainActivity`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/MainActivity.kt): Comprueba el rol del sistema y solicita permisos múltiples en runtime.
- [`MainScreen`](file:///c:/Users/Pablo/Desktop/app_bloqueo%20nro%20600%20-%20%2080/app/src/main/java/com/antispam/blocker/ui/screens/MainScreen.kt):
  - Estado del filtro con switch de encendido/apagado.
  - Contador total de llamadas bloqueadas sincronizado en vivo.
  - Gestión visual de chips de prefijos (agregar/eliminar).
  - Probador/simulador de números en tiempo real.
  - Botón de prueba simulada (+1 al contador).
  - Historial de llamadas bloqueadas con opción de vaciado y reseteo a 0.
  - Botón de compartir APK directo vía Android Share Sheet (`Intent.ACTION_SEND`).

---

## 🛠️ Comandos de Compilación y Verificación

Ejecutar siempre en PowerShell dentro del directorio raíz:

```powershell
# 1. Comprobación rápida de sintaxis y compilación Kotlin
.\gradlew.bat compileDebugKotlin

# 2. Generar el APK instalable de desarrollo
.\gradlew.bat assembleDebug
# Archivo de salida: app\build\outputs\apk\debug\app-debug.apk

# 3. Subir o actualizar versión en GitHub Releases
gh release create vX.Y.Z anti-spam-vX.Y.Z.apk --title "anti-spam vX.Y.Z" --notes "Notas..."
gh release upload vX.Y.Z anti-spam-vX.Y.Z.apk --clobber
```

---

## ⚠️ Reglas Obligatorias para Agentes

1. **Gestión de Binarios**: **NUNCA** commitear archivos binarios pesados (`.apk`, `.aab`, `/captura*.png`) directamente al árbol de Git. Deben estar ignorados en `.gitignore` y distribuirse vía GitHub Releases.
2. **Permisos Sincronizados**: Si se agrega un permiso nuevo a `AndroidManifest.xml`, debe incluirse en la solicitud en tiempo de ejecución en `MainActivity.requestNecessaryPermissions()`.
3. **Verificación Previa**: Antes de confirmar o realizar `git push`, validar que `.\gradlew.bat compileDebugKotlin` termine con código 0.
4. **Manejo de Versiones**: Al liberar una nueva versión, incrementar `versionCode` y `versionName` en `app/build.gradle.kts` y actualizar las notas correspondientes.
