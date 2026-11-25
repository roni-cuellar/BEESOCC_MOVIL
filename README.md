# Beesocc Mobile (Kotlin)

Aplicación nativa Kotlin que replica los flujos principales del frontend web de BEESOCC.
Usa XML + ViewBinding, un `MainActivity` con fragments y HTTP directo contra la API Laravel.

## Requisitos

- Android Studio Ladybug o superior (SDK 35 instalado).
- JDK 17 o 21.
- Backend Laravel ejecutándose con `php artisan serve --host=0.0.0.0 --port=8000`.

## Compilar

```bash
cd mobile
./gradlew assembleDebug
```

El APK queda en `mobile/app/build/outputs/apk/debug/app-debug.apk`.

## URL base de la API

Hay dos niveles:

1. **Valor por defecto (compilación):** `mobile/gradle.properties`
   ```properties
   apiBaseUrl=http://10.0.2.2:8000
   ```
   - Emulador Android: `http://10.0.2.2:8000`
   - Celular en la misma Wi‑Fi: `http://IP_DE_TU_PC:8000` (por ejemplo `http://192.168.1.10:8000`)

2. **Override en la app:** abre **Ajustes** desde la barra inferior, escribe la URL, pulsa **Guardar** y luego **Probar conexión**. El valor se guarda en `SharedPreferences` y tiene prioridad sobre el de `gradle.properties`. **Restablecer** vuelve al valor compilado.

Para imágenes `/storage/...`, la app resuelve automáticamente la URL base.

## Autenticación

El backend usa sesión stateful de Laravel Sanctum (cookies + CSRF), no Bearer:

1. `GET /sanctum/csrf-cookie`
2. Cookie `XSRF-TOKEN` → cabecera `X-XSRF-TOKEN`
3. `Origin: http://localhost`

No es necesario cambiar nada en el backend para usar la app nativa. Si se quiere autenticar con Bearer tokens en el futuro, habría que añadir esos endpoints al backend.

## Estructura

```
mobile/app/src/main/java/com/beesocc/mobile/
├── core/          HTTP, sesión, configuración, estados, imágenes
├── model/         mappers JSON → ItemLista
├── repository/    acceso a endpoints reales por dominio
└── ui/
    ├── adapter/   lista reutilizable
    ├── formulario/ formulario genérico + resultados
    └── fragments/ pantallas
```

## Pantallas principales

- Inicio / acceso / registro / selección de rol.
- Mis ligas: CRUD de campeonatos, configuración, requisitos, ciclo y publicación.
- Dashboard: módulos de equipos, plantillas, jugadores, árbitros, partidos, calendario, tabla, estadísticas, grupos, eliminación y auditoría.
- Acta de partido: panel arbitral, reloj, eventos y correcciones.
- Consulta pública: campeonatos, equipos, plantillas, ficha de jugador, partidos, tabla, deporte, estadísticas e identidades.
- Mis arbitrajes: campeonatos asignados y sus partidos.
- Ajustes: URL base, prueba de conexión y cierre de sesión.

Todas las pantallas muestran estados de **carga**, **vacío** y **error** con reintento.
