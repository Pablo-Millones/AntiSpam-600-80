# Memoria del Proyecto - anti-spam

Documento de memoria persistente para el desarrollo y contexto histórico del proyecto **anti-spam**.

---

## 📌 Datos Clave del Proyecto
- **Nombre**: anti-spam (anteriormente AntiSpam 600 y 80)
- **Repositorio**: `https://github.com/Pablo-Millones/AntiSpam-600-80`
- **Última Versión Estable**: `v1.1.0` (versionCode: `2`, versionName: `"1.1.0"`)
- **Enlace de Descarga Directa**: [anti-spam-v1.1.0.apk](https://github.com/Pablo-Millones/AntiSpam-600-80/releases/download/v1.1.0/anti-spam-v1.1.0.apk)
- **Tecnologías**: Android SDK 34, Kotlin 1.9+, Jetpack Compose BOM 2024.06.00, Gradle 8.4, Java 17.
- **Dispositivo de Pruebas Físico**: OnePlus Ace 5 (ColorOS / Android 14/15).

---

## 📜 Historial de Decisiones y Evolución

### v1.0.0 (03-10-2026) - Lanzamiento Inicial
- Creación de la base de la app nativa en Kotlin con Jetpack Compose.
- Implementación de `CallScreeningService` como mecanismo oficial para interceptar llamadas.
- Interfaz gráfica con selector de temas (Oscuro/Claro), simulador de coincidencia de números telefónicos y visualización de llamadas bloqueadas.
- Publicación de la primera versión en GitHub Releases (`AntiSpam_600_80.apk`).

### v1.1.0 (05-10-2026) - Detección Instantánea Dual y Mejoras
- **Problema Detectado en OnePlus Ace 5 (ColorOS)**: Las capas agresivas de gestión de batería e integración telefónica del fabricante a veces retrasaban la ejecución del `CallScreeningService` estándar, permitiendo que sonara 1 tono antes de evaluar el número.
- **Solución Implementada**:
  - Se implementó un **mecanismo de doble protección**:
    1. `CallScreeningService` a nivel de Telecom oficial.
    2. `IncomingCallReceiver` (`BroadcastReceiver` de `PHONE_STATE`) que detecta `EXTRA_STATE_RINGING` y cuelga de inmediato usando `TelecomManager.endCall()`.
  - Se agregaron permisos en tiempo de ejecución: `ANSWER_PHONE_CALLS`, `READ_PHONE_STATE`, `READ_CALL_LOG` y `POST_NOTIFICATIONS`.
- **Mejoras en la UI**:
  - Reactividad instantánea con `SharedPreferences.OnSharedPreferenceChangeListener` y `ON_RESUME`: el contador e historial se actualizan al regresar a la app o al colgar una llamada en segundo plano sin reiniciar la pantalla.
  - Botón para simular una llamada de prueba (+1 en vivo).
  - Opción de "Vaciar (0)" para limpiar tanto el historial como reiniciar el contador acumulado.
  - Botón de compartir en el `TopAppBar` que invoca el diálogo nativo de Android con el enlace directo al APK.
- **Simplificación de Marca**:
  - Nombre oficial simplificado a `anti-spam`.
- **Publicación**:
  - Release `v1.1.0` en GitHub Releases con binario `anti-spam-v1.1.0.apk`.

---

## 💡 Notas Técnicas y Lecciones Aprendidas

1. **Gestión de Permisos en Android 10+ a 14+**:
   - `TelecomManager.endCall()` requiere el permiso en tiempo de ejecución `android.permission.ANSWER_PHONE_CALLS`.
   - `CallScreeningService` requiere que el usuario acepte el diálogo del sistema mediante `RoleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)`.
2. **Normalización Telefónica Chilena**:
   - Muchos números de spam de servicios 600 y 80 entran con formato internacional `+56 600...` o `+56 80...`.
   - `PhoneNumberHelper` limpia cualquier prefijo `56` cuando la longitud restante coincide con un número válido, permitiendo interceptar tanto el formato local como el internacional.
3. **Control de Archivos y Repositorio**:
   - Los archivos `.apk` y las capturas temporales (`/captura*.png`) pesan decenas de megabytes; deben mantenerse en `.gitignore` para no ralentizar el repositorio git.
   - Las versiones compiladas se publican como Assets en GitHub Releases usando `gh release create` / `gh release upload`.
