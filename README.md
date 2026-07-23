# Aura 🌤️ - Aplicación de Clima Inteligente y Adaptativa

Aura es una aplicación móvil de clima de última generación para dispositivos Android, diseñada con un enfoque minimalista, intuitivo y moderno. La interfaz está construida 100% sobre **Jetpack Compose** aplicando los lineamientos éticos y estéticos de **Material Design 3 (M3)**. 

La aplicación se destaca por su capacidad de autoubicación en tiempo real, sincronización horaria dinámica y su adaptabilidad visual intuitiva basada en ciclos día/noche. Todo el consumo se realiza directamente desde la API meteorológica de **Open-Meteo**, con un mecanismo local de persistencia basado en **Room Database**.

---

## 🎨 Características Destacadas

### 1. 🌙 Tema Oscuro Global Elegante (Versión 3.0)
* Adopción global de una paleta de noche profunda (Deep Night Slate `#0B1120`, `DarkSurface` `#151D30`, `DarkPrimary` `#38BDF8`), contraste elevado y gradientes atmosféricos dinámicos en Jetpack Compose.
* Experiencia visual fluida y consistente en todas las tarjetas de información, métricas horarias y pantallas de la aplicación.

### 2. 🔒 Endurecimiento de Seguridad y Privacidad (allowBackup="false")
* Desactivación de respaldos automáticos en el manifiesto de Android para proteger la privacidad local del usuario ante posibles extracciones no autorizadas en nubes de terceros o mediante depuración adb.

### 3. ⚡ Hilos Asíncronos (Dispatchers.IO) y Tiempos de Red
* Ejecución de tareas de geocodificación inversa en hilos de entrada/salida (`Dispatchers.IO`) para garantizar fluidez total de la interfaz de usuario sin bloqueos.
* Políticas de conexión seguras en Retrofit/OkHttp con timeouts de 15 segundos para máxima resistencia ante redes inestables.

### 4. 📍 Autoubicación mediante GPS de Alta Precisión
* Aura utiliza **Google Play Services Location API** para adquirir de forma automática las coordenadas del dispositivo en tiempo real.
* A través de un servicio integrado de **Reverse Geocoding (Geocoder de Android)**, traduce la latitud y longitud físicas a una ciudad legible con su correspondiente estado/provincia y país.
* Ofrece un botón flotante de acceso directo en la barra de búsqueda para refrescar la ubicación en cualquier momento.

### 2. 🕒 Visualización de la Hora Local Dinámica
* El dashboard principal muestra un reloj adaptado a la franja horaria local en tiempo real con un formato elegante e intuitivo: `Lunes, 25 de Mayo • 02:43`.
* El reloj se actualiza automáticamente respetando la configuración regional y el huso horario detectado.

### 3. 🌗 Consciencia de Ciclos Día/Noche
* Aura integra un sistema que descifra si es de **día** o de **noche** basándose en la hora local actual del punto seleccionado y el estado de la API solar.
* Dependiendo de la fase del día, la interfaz completa cambia su paleta de gradientes dinámicos de fondo (brillando en tonos diurnos de azul cielo o atenuándose a azules estelares profundos).
* Los elementos gráficos del clima cambian inteligentemente: el sol radiante de los iconos del clima pasa a ser una luna menguante detallada rodeada de estrellas titilantes durante las horas nocturnas correspondientes.

### 4. 📅 Pronóstico de las Próximas 24 Horas Sincronizado
* La sección horaria ya no empieza desde valores genéricos o arbitrarios o del inicio de los días calendarios del API. En Aura se calibra en tiempo real **empezando exactamente desde la hora actual del dispositivo**, mostrando las siguientes 24 horas continuas de pronóstico térmico con sus iconos de día/noche correspondientes.

### 5. 🔍 Búsqueda con Autocompletado e Historial
* Una potente barra de búsqueda que implementa llamadas asíncronas de geocodificación espacial. Al teclear unas letras, una lista superpuesta de resultados autocompletados te permitirá viajar a cualquier rincón meteorológico del planeta.

### 6. 💖 Gestión de Favoritos Persistente (Room)
* Soporte nativo para almacenar tus ciudades preferidas en una base de datos interna **SQLite** a través de **Room**.
* Puedes marcar con un solo botón de estrella tus ciudades de consulta frecuente para cargarlas de forma instantánea de forma offline o en futuros arranques.

### 7. 🌡️ Conversión de Unidades al Instante
* Conversión termodinámica instantánea entre grados **Celsius (°C)** y **Fahrenheit (°F)** presionando un solo botón en los detalles térmicos, propagándose a todos los paneles y tarjetas horarias de la pantalla.

### 8. 🌌 Widget Climatológico Translúcido (Glassmorphism)
* El fondo del widget de Aura para la pantalla de inicio de Android ahora adopta un diseño moderno de **vidrio esmerilado translúcido** con un borde cian neón interactivo, integrándose estéticamente con cualquier fondo de pantalla de tu dispositivo.

### 9. 🎬 Carrusel Animado (ViewFlipper Nativo)
* Transiciones suaves animadas mediante desvanecimiento que se ejecutan automáticamente cada 3 segundos, rotando de forma fluida entre estadísticas del clima real (temperatura actual, lluvia) y recomendaciones contextuales inteligentes según el estado climatológico.

### 10. 🚨 Alertas Climáticas Proactivas en el Widget
* Monitoreo prioritario nativo de peligros ambientales sutiles (tales como olas de calor/frío extremas, radiación UV alta, fuertes de viento o tormentas). El widget se viste opcionalmente de rojo de alerta extrema o naranja de advertencia crítica para alertar al usuario proactivamente.

### 11. 📈 Gráfica de Tendencia Térmica NATIVA de 24 Horas
* El widget dibuja en tiempo real una **gráfica de líneas fluida con un lienzo vectorizado (`Canvas` nativo)** mostrando la progresión térmica continua de las próximas 24 horas. Cuenta con un relleno de degradado cian que se difumina hacia las bases y señalizaciones legibles de las temperaturas máximas y mínimas calculadas.

---

## 🏗️ Arquitectura y Estructura del Código

La aplicación sigue fielmente el patrón de diseño arquitectónico **MVVM (Model - View - ViewModel)** combinado con los principios de desarrollo limpio (**Clean Architecture**):

```text
/app/src/main/java/com/example
│
├── MainActivity.kt                 # Punto de entrada de la app, gestiona insets edge-to-edge
│
├── data
│   ├── api
│   │   ├── GeocodingResponse.kt    # Data Transfer Objects (DTO) para geocodificación
│   │   ├── WeatherApiService.kt    # Definición de endpoints de Retrofit para Open-Meteo
│   │   └── WeatherResponse.kt      # Modelado exhaustivo de respuestas horarias e históricas
│   │
│   ├── db
│   │   ├── ClimaDatabase.kt        # Entidad gestora de Room (SQLite database)
│   │   ├── FavoriteLocation.kt     # Definición de tabla de ubicaciones favoritas
│   │   └── LocationDao.kt          # Objeto de acceso a datos (DAO) para queries de CRUD
│   │
│   └── repository
│       └── WeatherRepository.kt    # Fuente única de verdad, sincroniza datos de red y locales
│
└── ui
    ├── theme
    │   ├── Color.kt                # Paleta de colores M3
    │   ├── Theme.kt                # Configuración global del tema y sistema de gradientes
    │   └── Type.kt                 # Fórmulas de escala tipográfica adaptativas
    │
    └── weather
        ├── WeatherDashboardScreen.kt  # Pantalla principal (Compose UI: widgets, forecast, Search)
        └── WeatherViewModel.kt        # Gestor de estados de UI usando StateFlow y Corutinas
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
