# Aura 🌤️ - Aplicación de Clima Inteligente y Adaptativa

Aura es una aplicación móvil de clima de última generación para dispositivos Android, diseñada con un enfoque minimalista, intuitivo y moderno. La interfaz está construida 100% sobre **Jetpack Compose** aplicando los lineamientos éticos y estéticos de **Material Design 3 (M3)**. 

La aplicación se destaca por su capacidad de autoubicación en tiempo real, sincronización horaria dinámica y su adaptabilidad visual intuitiva basada en ciclos día/noche. Todo el consumo se realiza directamente desde la API meteorológica de **Open-Meteo**, con un mecanismo local de persistencia basado en **Room Database**.

---

## 🎨 Características Destacadas

### 1. 🌐 Soporte Bilingüe Integral & Modal de Ajustes (Versión 3.4)
* **Soporte Bilingüe Nativo (Español / English):** Cambio dinámico de idioma en tiempo real a lo largo de toda la aplicación sin reiniciar ni interrumpir la ubicación activa.
* **Diálogo de Ajustes (SettingsDialog):** Botón de acceso directo con icono de engranaje (⚙️) en la barra superior para seleccionar fácilmente el idioma (🇪🇸 Español / 🇬🇧 English) y las unidades de temperatura (°C Celsius / °F Fahrenheit).
* **Persistencia Global de Preferencias (`SharedPreferences`):** Guardado automático de los ajustes elegidos (`aura_settings_prefs`), aplicándose al dashboard principal, gráfica de tendencia de 24h, tarjetas de métricas, notificaciones del WorkManager y Widget de la pantalla de inicio.
* **Internacionalización de Datos:** Descripciones del clima traducidas (ej: *Clear sky / Cielo despejado*), días de la semana y fecha local adaptados al Locale activo, y rosa de los vientos cardinal ajustada (ej. `SW` / `NW` en inglés vs `SO` / `NO` en español).

### 2. 📊 Gráfica Interactiva de Tendencia Térmica 24h en Compose (Versión 3.3)
* **Gráfica Vectorial Interactiva (`TemperatureTrendChart`):** Curva suave de Bezier dibujada directamente sobre un lienzo de `Canvas` en Jetpack Compose con degradado de color cian neón.
* **Gestos Táctiles y Deslizamiento:** Soporte para gestos de toque y arrastre (`pointerInput`) sobre la gráfica, permitiendo al usuario explorar la temperatura hora por hora.
* **Tarjeta Flotante de Detalle (`Tooltip`):** Al presionar o arrastrar el dedo por la gráfica, se despliega una tarjeta dinámica con el icono animado del estado del clima, la probabilidad de lluvia (💧), el horario exacto y la descripción traducida.

### 2. 🔮 Probabilidad de Ocurrencia del Pronóstico de 7 Días (Versión 3.3)
* **Motor Dinámico de Certidumbre:** Algoritmo que calcula dinámicamente el porcentaje de probabilidad de ocurrencia (entre 80% y 99%) para cada día del pronóstico de 7 días según los datos específicos de la ciudad.
* **Variables Meteorológicas Integradas:** Pondera la degradación natural por horizonte temporal, códigos meteorológicos WMO (cielos estables vs. tormentas), probabilidad de precipitación, velocidad del viento y amplitud térmica diaria.
* **Insignia y Lista Informativa:** Muestra el rango global de ocurrencia en la cabecera de la tarjeta (ej. `80% - 99% prob. ocurrencia`) y la probabilidad calculada por día individual.

### 3. 🌧️ Métricas Meteorológicas Ampliadas & Dirección del Viento (Versión 3.3)
* **Nuevas Tarjetas de Métricas:** Incorporación de métricas de **Nubosidad** (% cobertura de nubes) y **Precipitación** (acumulada por hora en mm).
* **Rosa de los Vientos Cardinal:** Dirección del viento enriquecida con grados sexagesimales e indicación cardinal dinámica (ej. `180° (S)`, `45° (NE)`).

### 4. 🧹 Optimización Visual y Eliminación de Redundancias (Versión 3.3)
* Limpieza del flujo visual en la cabecera eliminando lecturas térmicas duplicadas tras la fecha/hora, garantizando que el protagonismo recaiga en la gráfica interactiva de 24h y el pronóstico de 7 días.

### 5. 🎯 Sincronización Térmica Exacta de Estado Actual y Pronóstico (Versión 3.3)
* Corrección y alineación estricta del estado térmico: La temperatura actual en tiempo real y el código de condición meteorológica coinciden exactamente con el primer elemento (**"Ahora"**) del carrusel horaria de 24 horas y de la gráfica de tendencia térmica.

### 6. 🛡️ Manejo Inteligente de Errores & Botón de Rescate (Versión 3.3)
* **Traducción Amigable de Errores de Red:** Captura de excepciones HTTP/IO (`UnknownHostException`, `SocketTimeoutException`, `HttpException`) con interpretación automática en español para informar problemas de conectividad Wi-Fi/datos móviles o caídas del servidor.
* **Tarjeta de Error & Botón de Rescate:** Tarjeta rediseñada en la interfaz principal que incluye botón de **Reintentar** y botón de rescate directo a **Madrid** para restablecer la aplicación en caso de falla de GPS o red.
* **Notificaciones Snackbar:** Avisos interactivos flotantes al agregar/eliminar ubicaciones de favoritos y al sincronizar con el GPS.
* **Manejo Seguro en Base de Datos:** Aislamiento de excepciones en operaciones con Room SQLite para evitar cierres inesperados.

### 4. ⏰ Tareas Programadas (WorkManager) & Hora Local por Ubicación (Versión 3.2)
* **Resumen Matutino Automático (`DailyWeatherWorker`):** Trabajo periódico en segundo plano que envía cada mañana (07:30 AM) una notificación enriquecida con el pronóstico del día y alertas de la ubicación actual.
* **Hora y Fecha Local Internacional:** Detección de zona horaria y cálculo dinámico de la hora local exacta según la ubicación buscada (ej: Tokio, París, Buenos Aires).

### 4. 🌙 Tema Oscuro Global Elegante (Versión 3.0)
* Adopción global de una paleta de noche profunda (Deep Night Slate `#0B1120`, `DarkSurface` `#151D30`, `DarkPrimary` `#38BDF8`), contraste elevado y gradientes atmosféricos dinámicos en Jetpack Compose.
* Experiencia visual fluida y consistente en todas las tarjetas de información, métricas horarias y pantallas de la aplicación.

### 5. 🔒 Endurecimiento de Seguridad y Privacidad (allowBackup="false")
* Desactivación de respaldos automáticos en el manifiesto de Android para proteger la privacidad local del usuario ante posibles extracciones no autorizadas en nubes de terceros o mediante depuración adb.

### 6. ⚡ Hilos Asíncronos (Dispatchers.IO) y Tiempos de Red
* Ejecución de tareas de geocodificación inversa en hilos de entrada/salida (`Dispatchers.IO`) para garantizar fluidez total de la interfaz de usuario sin bloqueos.
* Políticas de conexión seguras en Retrofit/OkHttp con timeouts de 15 segundos para máxima resistencia ante redes inestables.

### 7. 📍 Autoubicación mediante GPS de Alta Precisión
* Aura utiliza **Google Play Services Location API** para adquirir de forma automática las coordenadas del dispositivo en tiempo real.
* A través de un servicio integrado de **Reverse Geocoding (Geocoder de Android)**, traduce la latitud y longitud físicas a una ciudad legible con su correspondiente estado/provincia y país.
* Ofrece un botón flotante de acceso directo en la barra de búsqueda para refrescar la ubicación en cualquier momento.

### 8. 🕒 Visualización de la Hora Local Dinámica
* El dashboard principal muestra un reloj adaptado a la franja horaria local en tiempo real con un formato elegante e intuitivo: `Lunes, 25 de Mayo • 02:43`.
* El reloj se actualiza automáticamente respetando la configuración regional y el huso horario detectado.

### 9. 🌗 Consciencia de Ciclos Día/Noche
* Aura integra un sistema que descifra si es de **día** o de **noche** basándose en la hora local actual del punto seleccionado y el estado de la API solar.
* Dependiendo de la fase del día, la interfaz completa cambia su paleta de gradientes dinámicos de fondo (brillando en tonos diurnos de azul cielo o atenuándose a azules estelares profundos).
* Los elementos gráficos del clima cambian inteligentemente: el sol radiante de los iconos del clima pasa a ser una luna menguante detallada rodeada de estrellas titilantes durante las horas nocturnas correspondientes.

### 10. 🔍 Búsqueda con Autocompletado e Historial
* Una potente barra de búsqueda que implementa llamadas asíncronas de geocodificación espacial. Al teclear unas letras, una lista superpuesta de resultados autocompletados te permitirá viajar a cualquier rincón meteorológico del planeta.

### 11. 💖 Gestión de Favoritos Persistente (Room)
* Soporte nativo para almacenar tus ciudades preferidas en una base de datos interna **SQLite** a través de **Room**.
* Puedes marcar con un solo botón de estrella tus ciudades de consulta frecuente para cargarlas de forma instantánea de forma offline o en futuros arranques.

### 12. 🌡️ Conversión de Unidades al Instante
* Conversión termodinámica instantánea entre grados **Celsius (°C)** y **Fahrenheit (°F)** presionando un solo botón en los detalles térmicos, propagándose a todos los paneles y tarjetas horarias de la pantalla.

### 13. 🌌 Widget Climatológico Translúcido (Glassmorphism)
* El fondo del widget de Aura para la pantalla de inicio de Android ahora adopts un diseño moderno de **vidrio esmerilado translúcido** con un borde cian neón interactivo, integrándose estéticamente con cualquier fondo de pantalla de tu dispositivo.

---

## 📱 Guía Detallada de Interfaz de Usuario e Iconografía

Aura implementa componentes interactivos desarrollados sobre Jetpack Compose y gráficos dibujados sobre lienzo `Canvas`. A continuación se detalla cada icono y su función correspondiente:

### 🔍 Controles de Búsqueda y Navegación Superior
* **🔍 Lupa (`Icons.Default.Search`):** Campo de entrada de texto. Al escribir 3 caracteres o más, despliega una lista flotante de sugerencias con geocodificación de ciudades en tiempo real.
* **❌ Cruz (`Icons.Default.Close`):** Botón dinámico que borra instantáneamente el texto ingresado en la barra de búsqueda.
* **📍 GPS / Ubicación (`Icons.Default.LocationOn`):** Solicita permisos dinámicos en Android y actualiza la ubicación y datos del tiempo utilizando el GPS del dispositivo.
* **⭐ Estrella Dorada (`Icons.Default.Star`):** 
  * En la cabecera: Agrega o elimina la ubicación actual de la base de datos local SQLite con Room.
  * En la barra de favoritos: Representa un chip con acceso directo para cambiar a esa ubicación con un solo toque.

### 📅 Cabecera de Hora Local e Indicador de Unidades
* **📅 Calendario / Hora (`Icons.Default.DateRange`):** Muestra la fecha y la hora local exacta de la ciudad buscada, calculada respetando su zona horaria internacional.
* **°C / °F Conmutador:** Botón selector para cambiar instantáneamente todas las lecturas térmicas de la aplicación entre Celsius y Fahrenheit.

### 🎨 Catálogo de Gráficos y Símbolos Meteorológicos Vectoriales (`Canvas`)
En lugar de recursos estáticos, la función `WeatherConditionGraphic` dibuja cada condición climática dinámicamente mediante vectores de Jetpack Compose Canvas:
* **☀️ Sol Radiante (Día - Códigos 0, 1):** Círculo dorado central con rayos vectoriales expansivos para días despejados.
* **🌙 Luna y Estrellas (Noche - Códigos 0, 1):** Luna creciente plateada rodeada de destellos estelares para noches despejadas.
* **☁️ Nubes Acumulativas (Códigos 2, 3):** Estructura fluida de óvalos superpuestos para estados parcialmente nublados o cubiertos.
* **🌫️ Capas de Niebla (Códigos 45, 48):** Franjas horizontales en degradado con bordes esmerilados para niebla y escarcha.
* **🌧️ Nube con Lluvia (Códigos 51-67, 80-82):** Nube plomo acompañada de líneas de lluvia inclinadas en tono turquesa neón (`#80DEEA`).
* **❄️ Copos de Nieve (Códigos 71-77, 85-86):** Nube blanca suave con copos circulares flotantes en la base.
* **⚡ Tormenta Eléctrica (Códigos 95, 96, 99):** Nube de tormenta oscura atravesada por un rayo vectorial en zigzag amarillo brillante.

### 📊 Tarjetas de Métricas Detalladas
* **🌡️ Sensación Térmica:** Calcula la temperatura aparente experimentada por el cuerpo humano.
* **💨 Viento:** Indica la velocidad del viento en km/h y su dirección exacta en grados sexagesimales.
* **💧 Humedad Relativa:** Porcentaje de humedad contenida en la atmósfera.
* **☔ Probabilidad de Lluvia:** Porcentaje de probabilidad máxima de precipitación proyectada.
* **☀️ Índice UV:** Clasificación de intensidad de radiación solar ultravioleta (Bajo, Moderado, Alto, Extremo).
* **🌅 Ciclo Solar:** Muestra la hora exacta del amanecer (orto) y atardecer (ocaso).
* **🚨 Avisos de Emergencia (`Icons.Default.Warning`):** Banner rojo de alerta emergente para condiciones extremas (calor/frío severo, tormentas).
* **🔔 Resumen Matutino (`Icons.Default.Notifications`):** Indica la programación periódica del WorkManager para las 07:30 AM. Incluye el botón **Send (🚀)** para simular y probar la notificación en segundo plano de manera inmediata.
* **🏠 Botón de Rescate (`Icons.Default.Home`):** Restablece la app a la ubicación base de Madrid en caso de error de conexión.

---

## 🏗️ Arquitectura y Estructura del Código

La aplicación sigue fielmente el patrón de diseño arquitectónico **MVVM (Model - View - ViewModel)** combinado con los principios de desarrollo limpio (**Clean Architecture**):

```text
/app/src/main/java/com/aura
│
├── MainActivity.kt                 # Punto de entrada de la app, gestiona insets edge-to-edge
├── AuraApplication.kt              # Clase de aplicación para inicialización global y WorkManager
│
├── data
│   ├── api
│   │   ├── GeocodingResult.kt      # Data Transfer Objects (DTO) para geocodificación
│   │   ├── WeatherApiService.kt    # Definición de endpoints de Retrofit para Open-Meteo
│   │   └── WeatherModels.kt        # Modelado exhaustivo de respuestas horarias e históricas
│   │
│   ├── db
│   │   ├── ClimaDatabase.kt        # Entidad gestora de Room (SQLite database)
│   │   ├── FavoriteLocation.kt     # Definición de tabla de ubicaciones favoritas
│   │   └── FavoriteDao.kt          # Objeto de acceso a datos (DAO) para queries de CRUD
│   │
│   └── repository
│       └── WeatherRepositoryImpl.kt # Fuente única de verdad, sincroniza datos de red y locales
│
├── domain
│   ├── model                       # Modelos de dominio limpios desacoplados de DTOs
│   ├── repository                  # Interfaz IWeatherRepository
│   └── usecase                     # Casos de uso de negocio (Search, GetWeather, Favorites, GPS)
│
├── worker
│   └── DailyWeatherWorker.kt       # WorkManager para notificaciones matutinas periódicas
│
└── ui
    ├── theme
    │   ├── Color.kt                # Paleta de colores M3
    │   ├── Theme.kt                # Configuración global del tema y sistema de gradientes
    │   └── Type.kt                 # Fórmulas de escala tipográfica adaptativas
    │
    └── weather
        ├── WeatherDashboardScreen.kt # Pantalla principal (Compose UI)
        ├── TemperatureTrendChart.kt  # Gráfica interactiva de tendencia térmica 24h
        ├── WeatherAppWidgetProvider.kt # Widget climatológico para pantalla de inicio
        └── WeatherViewModel.kt       # Gestor de estados de UI usando StateFlow y Corutinas
```

---

## 🛠️ Stack Tecnológico

Aura ha sido estructurada utilizando las herramientas oficiales más modernas y sofisticadas del ecosistema de desarrollo de Android:

* **Jetpack Compose**: Kit de herramientas declarativo moderno para la creación de interfaces de usuario nativas de alta fluidez.
* **Corutinas de Kotlin & Flow**: Gestión asíncrona de subprocesos, concurrencia web y reactividad nativa mediante flujos de datos fríos y estados compartidos (`StateFlow`, `collectAsStateWithLifecycle`).
* **Retrofit 2 & OkHttp 3**: Conexiones de red seguras, intercepción de logs de red, timeouts refinados y descodificación ágil en segundo plano.
* **Moshi / Moshi Converter**: Manejo seguro y eficiente de la des-serialización de datos JSON provenientes de la API de Open-Meteo a objetos de Kotlin tipados.
* **Room Database**: Capa de abstracción fluida sobre SQLite que permite aprovechar la máxima robustez de almacenamiento local con compatibilidad de Corutinas mediante flujos transaccionales.
* **Play Services Location**: Suite oficial de Google para geolocalización ágil y precisa con uso optimizado de la batería bajo políticas modernas de runtime permissions.

---

## 🚀 Instalación y Puesta en Marcha

Para compilar e instalar localmente Aura en tu entorno de desarrollo, sigue estos sencillos pasos:

### Prerrequisitos
* Android Studio (Ladybug o posterior recomendado)
* Android SDK 34 o posterior
* Gradle JDK Versión 17 o posterior

### Paso 1: Clonar el Repositorio
```bash
git clone https://github.com/tu-usuario/aura-clima.git
cd aura-clima
```

### Paso 2: Configurar las Variables de Entorno (Opcional)
La aplicación cuenta con el plugin de Gradle para Secrets. Si deseas añadir variables o llaves privadas que requieran persistencia, puedes declararlas directamente en un archivo de entorno `.env` en la raíz del proyecto para evitar ser expuestas en versionamientos:
```text
# Crear archivo .env en la raíz para variables opcionales
MY_API_KEY="tu_llave_aqui"
```

### Paso 3: Compilar y Ejecutar
Inicia Android Studio, importa el proyecto seleccionando la carpeta raíz y deja que Gradle descargue las dependencias necesarias. Una vez sincronizado, haz clic en **Run 'app'** con un simulador Android o dispositivo físico conectado mediante depuración USB.

---

## 🧪 Pruebas y Control de Calidad (Testing)

Aura cuenta con un pipeline de pruebas locales para asegurar la lógica del negocio de forma instantánea sin requerir emuladores pesados:

* **Robolectric**: Pruebas unitarias de calidad industrial para probar los componentes internos (ViewModels, persistencia transaccional y flujos reactivos) corriendo al unísono en un entorno de sandbox nativa que emula el framework de Android directamente en la JVM.
* **Roborazzi**: Verificación de regresión visual interactiva mediante la captura y comprobación de capturas de pantalla de la UI de Jetpack Compose en escala de píxeles automáticos.

### Ejecución de Pruebas Unitarias
Ejecuta la suite completa de pruebas unitarias directamente desde la terminal del sistema:
```bash
gradle :app:testDebugUnitTest
```

### Registro de Nuevas Capturas de Pantalla (Roborazzi)
Si introduces cambios estéticos y deseas actualizar las referencias de capturas para pruebas automáticas de regresión visual:
```bash
gradle :app:recordRoborazziDebug
```

### Validación de Capturas de Pantalla
Para comparar los cambios recientes contra las referencias registradas automáticamente:
```bash
gradle :app:verifyRoborazziDebug
```

---

Desarrollado con pasión, buscando siempre la fusión idónea entre **ingeniería limpia** y **diseño elegante**. ¡Disfruta el cielo despejado o las noches estrelladas con **Aura**! 🌤️🌌
