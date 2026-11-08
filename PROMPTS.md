# PROMPTS.md — Uso de IA en el laboratorio

Grupo G08 · Pokémon (tipo `fire`) · Rama `mejora-ia`

> Cada integrante registra sus propias consultas.

---

## Kiara Alburqueque

### Consulta 1
- **Fecha:** 2026-11-08
- **Herramienta:** Claude (claude.ai)
- **Prompt:** Implementar en el proyecto `com.tecsup.pokemon` el módulo de autenticación con Firebase Authentication (login con correo y contraseña y validación de campos), la lectura del rol desde Firestore en `usuarios/{uid}`, la persistencia y el cierre de sesión, la pantalla de favoritos, el panel de administración y las notificaciones locales, respetando los contratos `AuthRepository`, `ItemRepository` y `Notificador` de la sección 5 del laboratorio, con pruebas unitarias de los ViewModel usando repositorios falsos. Uso autorizado por el docente para la Fase 1.
- **Qué respondió (resumen):** Generó `FirebaseAuthRepository`, `LoginViewModel`, `SesionViewModel`, `FavoritosViewModel`, `AdminViewModel`, `NotificadorAndroid` y sus pantallas en Compose, además de 18 pruebas unitarias, organizados en 9 commits por hito.
- **Qué usamos / qué cambiamos y por qué:** Usamos el código después de compilarlo (`BUILD SUCCESSFUL`, 62 pruebas sin fallos) y probarlo en el celular con los dos roles. La configuración de Firebase (proyecto, usuarios, colección `usuarios` y reglas de Firestore) se hizo manualmente en la consola.
- **Qué aprendimos:** Que el rol debe venir de Firestore y no decidirse en la app, que el ViewModel no recibe `Context` sino la interface `Notificador`, y que con repositorios falsos se pueden probar los ViewModel sin Firebase ni Room.

### Consulta 2
- **Fecha:** 2026-11-08
- **Herramienta:** Claude (claude.ai)
- **Prompt:** Analizar por qué en orientación horizontal la lista de Pokémon queda reducida a una franja mínima y la pantalla de detalle no permite ver los datos, y proponer cambios en la interfaz para que la lista y el detalle se adapten a la orientación horizontal sin modificar los ViewModel. (Se adjuntaron capturas del celular en horizontal).
- **Qué respondió (resumen):** Identificó que la barra superior, la barra inferior, el título, el buscador y los filtros ocupaban casi toda la altura disponible. Propuso: (1) mover el menú al costado con `NavigationRail` y reducir la altura de la barra superior; (2) que el encabezado de la lista se desplace junto con los Pokémon usando `LazyVerticalGrid` con columnas adaptables (una en vertical, dos en horizontal); (3) mostrar el detalle en dos columnas, con la imagen a la izquierda y los datos a la derecha, y barras de progreso para las estadísticas.
- **Qué usamos / qué cambiamos y por qué:** Usamos las tres mejoras, una por commit. La orientación se detecta con `LocalConfiguration` dentro de los composables y no en el ViewModel, así se respeta MVVM. Se compiló (`BUILD SUCCESSFUL`) y se probó girando el celular.
- **Qué aprendimos:** Que probar la app en horizontal también es parte de probarla, y que la misma información del ViewModel puede mostrarse de forma distinta según el tamaño de la pantalla sin cambiar la lógica.

---

## Sebastián Espíritu

*(agregar aquí tus consultas con el mismo formato)*
