# 🛡️ anti-spam - Bloqueador Nativo para Android

[![Descargar APK v1.1.0](https://img.shields.io/badge/Descargar%20APK-v1.1.0-brightgreen?logo=android&style=for-the-badge)](https://github.com/Pablo-Millones/AntiSpam-600-80/releases/download/v1.1.0/anti-spam-v1.1.0.apk)
[![Última Versión](https://img.shields.io/badge/Versi%C3%B3n-1.1.0-blue?style=for-the-badge)](https://github.com/Pablo-Millones/AntiSpam-600-80/releases/tag/v1.1.0)
[![Licencia](https://img.shields.io/badge/Licencia-MIT-green?style=for-the-badge)](LICENSE)

> 📲 **Enlace directo para descargar y compartir el APK**:  
> 👉 [**Descargar anti-spam v1.1.0 APK (Clic aquí)**](https://github.com/Pablo-Millones/AntiSpam-600-80/releases/download/v1.1.0/anti-spam-v1.1.0.apk)  
> 📦 Ver todas las versiones en [GitHub Releases](https://github.com/Pablo-Millones/AntiSpam-600-80/releases).

Aplicación Android nativa desarrollada en **Kotlin** con **Jetpack Compose** y la API oficial **`CallScreeningService`**, diseñada específicamente para interceptar y colgar llamadas no deseadas de publicidad y telemarketing cuyo número comience con **600** o **80** (incluye 800, 801, etc.).

---

## 🚀 Características Principales

1. **Rechazo en Tiempo Real (`CallScreeningService`)**:
   - La API oficial de Android intercepta la llamada entrante a nivel de sistema telefónico.
   - Corta o rechaza la llamada **antes** de que el teléfono suene o vibre.
2. **Normalización Inteligente de Números**:
   - Detecta números con prefijos internacionales (como `+56` en Chile u otros países).
   - Ignora espacios, guiones y paréntesis para que ningún número de spam evada el filtro.
   - Coincide números locales (`600XXXXXXX`, `80XXXXXXX`) e internacionales (`+56600...`, `+5680...`).
3. **Prefijos Personalizables**:
   - Viene configurado por defecto para **600** y **80**.
   - Permite agregar fácilmente otros prefijos molestos (ej: `44`, `800`, `22`, etc.) o eliminarlos con un toque.
4. **Simulador y Probador Integrado**:
   - Incluye un buscador en pantalla donde puedes escribir cualquier número (ej: `+56 600 300 4000`) para ver de inmediato si sería bloqueado y cuál regla lo interceptaría.
5. **Historial de Bloqueos y Estadísticas**:
   - Registra fecha, hora y número de las llamadas interceptadas.
   - Opción para mantener o no el registro en el historial general del teléfono.

---

## 📁 Estructura del Proyecto

```text
app_bloqueo nro 600 -  80/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/antispam/blocker/
│       │   ├── MainActivity.kt                # Pantalla principal y gestión de permisos
│       │   ├── service/
│       │   │   └── SpamCallScreeningService.kt# Servicio nativo que cuelga las llamadas
│       │   ├── data/
│       │   │   ├── BlockRuleManager.kt        # Gestor de reglas y prefijos
│       │   │   ├── BlockedCall.kt             # Modelo de llamada bloqueada
│       │   │   └── BlockedCallsRepository.kt  # Almacenamiento local del historial
│       │   ├── util/
│       │   │   └── PhoneNumberHelper.kt       # Algoritmo de normalización y matching
│       │   └── ui/
│       │       ├── screens/MainScreen.kt      # Interfaz gráfica moderna en Compose
│       │       └── theme/                     # Paleta de colores, tipografía y tema
│       └── res/                               # Iconos vectoriales y recursos
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🛠️ Cómo Abrir y Compilar la Aplicación

### Paso 1: Abrir en Android Studio
1. Abre **Android Studio** (versión Hedgehog, Iguana, Jellyfish o posterior).
2. Selecciona **Open** (Abrir) y navega a esta carpeta:
   `c:\Users\Pablo\Desktop\app_bloqueo nro 600 -  80`
3. Espera unos segundos a que Gradle sincronice las dependencias automáticamente.

### Paso 2: Generar el APK o Instalar en tu Teléfono
- **Instalación directa**: Conecta tu teléfono Android por cable USB (con Depuración USB activada) y presiona el botón verde de **Run ▶** en Android Studio.
- **Generar archivo APK instalable**: En el menú superior de Android Studio ve a:
  `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`.
  El archivo `.apk` generado lo puedes enviar a tu celular por WhatsApp, Telegram o cable e instalarlo directamente.

---

## 📱 Paso Crucial: Activación en Android

Android exige por seguridad que tú autorices a la aplicación a gestionar las llamadas entrantes:

1. Abre la aplicación en tu celular.
2. Toca el botón rojo **"Activar Filtro en el Sistema"**.
3. Android te mostrará una ventana emergente preguntando:
   > *"¿Deseas establecer AntiSpam 600 y 80 como tu app de Identificación de llamadas y spam?"*
4. Selecciona **AntiSpam 600 y 80** y presiona **Establecer como predeterminada**.
5. ¡Listo! A partir de ese segundo, toda llamada que empiece por **600** o **80** será cortada automáticamente.

*(Si alguna vez necesitas configurarlo a mano: entra en `Ajustes del teléfono` > `Aplicaciones` > `Aplicaciones predeterminadas` > `Identificador de llamadas y spam`).*

---

## 💡 Alternativa Rápida Complementaria (Sin Programar)

Si necesitas frenar llamadas hoy mismo mientras compilas la app:
1. **Google Teléfono**: Si usas la app oficial de Teléfono de Google, ve a `Ajustes` > `Identificador de llamada y spam` y activa **Filtrar llamadas de spam**.
2. **Registro No Molestar (Chile / SERNAC)**: Puedes ingresar a [sernac.cl/nomolestar](https://www.sernac.cl/portal/618/w3-propertyvalue-63234.html) e inscribir tu número para exigir legalmente que las empresas bloqueen tus datos de sus listas de telemarketing.
