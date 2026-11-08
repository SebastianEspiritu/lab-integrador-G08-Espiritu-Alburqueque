# PROMPTS.md — Uso de IA en el laboratorio

Grupo G08 · Pokémon (tipo `fire`) · Rama `mejora-ia`

> Cada integrante registra sus propias consultas. Las entradas de Kiara están abajo; Sebastián agrega las suyas al final.

---

## Kiara Alburqueque

### Consulta 1
- **Fecha:** 2026-11-08
- **Herramienta:** Claude (claude.ai)
- **Prompt:** «Ayúdame a hacer este laboratorio. Mi pareja ya avanzó lo suyo; a mí me toca Firebase Authentication y login, roles ADMIN/USUARIO desde Firestore, mantener y cerrar sesión, pantalla de favoritos, panel de administración, notificaciones locales y pruebas. Los commits deben salir con mi cuenta de GitHub.» (Se adjuntó el PDF del laboratorio y el enlace del repositorio).
- **Qué respondió (resumen):** Revisó el código de mi pareja y armó mi parte respetando los contratos del PDF: `FirebaseAuthRepository` que lee el rol de `usuarios/{uid}`, `LoginViewModel` con validación antes de llamar a Firebase, `SesionViewModel` para mantener y cerrar sesión, `FavoritosViewModel`, `AdminViewModel`, `NotificadorAndroid` detrás de la interface `Notificador` y 18 pruebas unitarias con repositorios falsos. Lo entregó en 9 commits, uno por hito.
- **Qué usamos / qué cambiamos y por qué:** El docente autorizó usar IA también en la Fase 1 para poder presentar el laboratorio a tiempo. Usamos el código tal cual después de compilarlo (`BUILD SUCCESSFUL`, 62 pruebas sin fallos) y probarlo en el celular. Yo configuré Firebase en la consola (proyecto, usuarios, colección `usuarios` y reglas).
- **Qué aprendimos:** *(completar con tus palabras; por ejemplo: por qué el rol se guarda en Firestore y no en la app, por qué el ViewModel recibe la interface `Notificador` en vez de un `Context`, y cómo las pruebas usan repositorios falsos).*

### Consulta 2
- **Fecha:** 2026-11-08
- **Herramienta:** Claude (claude.ai)
- **Prompt:** «Una mejora es que cuando volteo el celular y quiero ver los Pokémon se ve súper chico, no puedo verlos; ni siquiera puedo ver los datos del Pokémon aunque suba.» (Se adjuntaron capturas del celular en horizontal).
- **Qué respondió (resumen):** Explicó que en horizontal la barra superior, la barra inferior, el título, el buscador y los filtros ocupaban casi toda la altura. Propuso: (1) mover el menú al costado con `NavigationRail` y reducir la barra superior; (2) que el encabezado de la lista se desplace junto con los Pokémon, usando `LazyVerticalGrid` con columnas adaptables (una en vertical, dos en horizontal); (3) mostrar el detalle en dos columnas (imagen a la izquierda, datos a la derecha) con barras para las estadísticas.
- **Qué usamos / qué cambiamos y por qué:** Usamos las tres mejoras, una por commit. Detectar `ORIENTATION_LANDSCAPE` con `LocalConfiguration` está en la interfaz (composables), no en el ViewModel, así se respeta MVVM.
- **Qué aprendimos:** *(completar con tus palabras; por ejemplo: probar en horizontal es parte de probar la app, y el mismo estado del ViewModel puede dibujarse distinto según el tamaño de pantalla sin tocar la lógica).*

---

## Sebastián Espíritu

*(agregar aquí tus consultas con el mismo formato)*
