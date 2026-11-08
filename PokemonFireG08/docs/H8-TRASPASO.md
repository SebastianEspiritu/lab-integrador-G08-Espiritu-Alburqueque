# H8 — Revisión técnica y traspaso

Fecha: 2026-11-08. Base revisada: `63cb9d8`, rama `main`, paquete `com.tecsup.pokemon`.
Referencia: PDF GLAB-S11-JLEONS-2026-1, requisitos R01–R13, arquitectura de páginas 5–6 y aceptación PA-01–PA-14.
Los hitos de este reparto agrupan los del PDF: H6 incluye detalle y H7 incluye Room/offline.

## Arquitectura auditada

- Compose observa `StateFlow` con `collectAsStateWithLifecycle()` y delega acciones en ViewModels. No hay acceso directo a Retrofit, Room ni Firebase en las pantallas.
- ListaViewModel y DetalleViewModel dependen de `ItemRepository`, no de su implementación. Exponen estado de solo lectura y no reciben Context ni clases de vista.
- Las fábricas reciben AppContainer. PokemonApplication conserva el contenedor y la base con applicationContext.
- API, DTO y errores HTTP quedan en data; los mappers separan DTO, Entity e Item. Room publica los flujos de lista y favoritos.
- Sincronizar hace una petición de referencias, sin descargar todas las fichas. Guardar referencias conserva favoritos y detalles en transacción. Los detalles se descargan a demanda y se reutilizan desde Room.
- `Theme.kt` usa LocalContext para el tema del sistema; no es un ViewModel ni contiene acceso a datos.

## Defecto corregido

Una sincronización lenta retenía el Mutex y bloqueaba también la lectura de una ficha ya descargada. La prueba `detalleLocalNoEsperaUnaSincronizacionRemotaLenta` falló antes de la corrección. Ahora se consulta primero Room fuera del bloqueo; si falta el detalle, se mantiene el bloqueo y se vuelve a consultar antes de descargar, evitando peticiones duplicadas.
No se modificaron contratos, dependencias, esquema de Room ni diseño visual.

## Verificación de H8

Comando ejecutado:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

Resultado: `BUILD SUCCESSFUL in 9s`, 76 tareas (17 ejecutadas y 59 al día).

- 26 pruebas unitarias: 5 mappers, 14 integración Repository/ViewModel con API/DAO falsos, 6 ViewModels con ItemRepository falso y 1 prueba base.
- 6 pruebas instrumentadas: 5 Room/DAO/persistencia y 1 prueba base. Sin fallos ni errores.
- Se cubren carga suspendida, éxito, error, vacío, IO, HTTP 403/404/503, reintento, cancelación, deduplicación, concurrencia, limpieza y lectura local mientras la red está ocupada.
- Room real verifica persistencia tras cerrar/reabrir, favoritos, contadores, referencias repetidas/vacías y detalles incompletos sin pérdida de información.
- En emulator-5554: APK instalado, lista real, ficha de Arcanine con seis estadísticas, botón Volver, regreso del sistema y rotación a horizontal conservando la ficha. Se restauró la orientación original. XML y captura de comprobación están en app/build, ignorado por Git.

## Requisitos y trabajo siguiente

| Requisito | Estado al entregar H8 |
|---|---|
| R03, R04 | Lista remota y detalle con estados, vacío, error y reintento; dos endpoints del grupo G08. |
| R05 | Arquitectura MVVM de las funcionalidades actuales verificada. Debe mantenerse al agregar pantallas. |
| R06, R08 | Room y fallback local con aviso; sin datos locales se presenta error/reintento. |
| R07 | Persistencia y operaciones de favoritos listas y probadas; falta interacción y pantalla Favoritos, a cargo de Kiara. |
| R01, R02, R11 | Pendientes para Kiara: Firebase Auth, rol leído de Firestore, sesión y cierre. |
| R09, R10 | Pendientes para Kiara: notificación/permisos y panel Admin. Contadores y limpieza ya existen en el contrato. |
| R12 | Hay cuatro pruebas de lista con ItemRepository falso y más cobertura de detalle; no se inició Fase 2. |
| R13 | Pendiente de la fase conjunta mejora-ia; PROMPTS.md permanece local. |

Aceptación: PA-06 y navegación comprobadas en emulador; PA-13 comprobada respecto a conservación del detalle al rotar. PA-08/PA-09 se cubren por pruebas offline; la verificación manual sin red se realizó en H7. PA-07 y PA-10 tienen respaldo de persistencia, pero su flujo completo requiere la UI de favoritos. PA-05 queda parcial hasta añadir notificación. PA-11 tiene backend probado, pero falta el panel. PA-01–PA-04, PA-12 y PA-14 requieren las funcionalidades pendientes.

## Puntos de integración para Kiara

- Favoritos: observar `itemRepository.favoritos` y llamar `alternarFavorito(id: String)` desde su ViewModel. No introducir llamadas DAO en UI.
- Firebase: implementar `data/auth/FirebaseAuthRepository.kt` con el contrato AuthRepository existente; usar Sesion y Rol, y proporcionar la dependencia desde AppContainer.
- Admin: usar `cantidadGuardada`, `cantidadFavoritos`, `refrescar` y `limpiarCache`. La limpieza elimina la caché local y sus favoritos según el contrato actual; no afecta autenticación.
- Notificaciones: implementar NotificadorAndroid y pasar la interface Notificador al ViewModel; no pasar Context al ViewModel.
- Navegación: extender AppNav conservando el identificador String y las fábricas por destino.
- Mantener `google-services.json` y archivos locales excluidos; no agregar PROMPTS.md en main. Commits y push requieren autorización.

## Límites actuales de la caché

Room conserva URLs; Coil administra los archivos de imagen y puede expulsarlos de su caché. Solo las fichas ya descargadas tienen tipos completos y estadísticas offline. Los detalles se reutilizan hasta limpiar la caché; no hay vencimiento automático. Sincronizar actualiza/agrega referencias, sin borrar filas antiguas por una respuesta remota vacía. No se implementaron funciones nuevas de Kiara.
