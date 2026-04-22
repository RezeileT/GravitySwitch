# GravitySwitch

GravitySwitch es un videojuego de puzles 2D desarrollado en Kotlin usando el motor KorGE.  
El jugador se desplaza por un escenario formado por chunks encadenados y debe avanzar cambiando la gravedad para evitar obstáculos y caídas, mientras acumula puntos que se guardan en una base de datos local.

---

## Objetivo del proyecto

Este proyecto forma parte de un Proyecto Fin de Ciclo (PFC) y tiene como objetivo:

- Crear un juego de puzles sencillo para PC, pensado para partidas cortas mientras el usuario espera otras tareas.
- Explorar una mecánica básica pero diferenciadora: el cambio de gravedad del personaje.
- Implementar un sistema simple de puntuaciones persistentes usando una base de datos local (SQLite).
- Aplicar buenas prácticas de organización del código, escenas y recursos en un proyecto con KorGE.

---

## Características principales

- Vista 2D lateral con escenario generado por chunks.
- Mecánica de cambio de gravedad del jugador (pasar de suelo a techo y viceversa).
- Obstáculos en el escenario que obligan al jugador a usar la gravedad de forma estratégica.
- Sistema de puntuación que aumenta conforme el jugador avanza.
- Persistencia de puntuaciones en base de datos local (`scores.db`).
- Menú inicial, escena de juego y escena de puntuaciones separadas.

---

## Estructura del proyecto

El código principal se encuentra en `src/jvmMain/kotlin`:

- `main.kt`  
  Punto de entrada de la aplicación. Configura KorGE y lanza la escena inicial.

- `MenuScene.kt`  
  Escena de menú principal. Desde aquí se puede iniciar una partida o consultar las puntuaciones.

- `GameScene.kt`  
  Escena de juego. Contiene la lógica de:
  - movimiento del jugador,
  - cambio de gravedad,
  - gestión de colisiones con el escenario y obstáculos,
  - actualización de la puntuación y fin de partida.

- `ScoreScene.kt`  
  Escena de visualización de puntuaciones guardadas.

- `Score.kt`  
  Modelo sencillo para representar una puntuación (por ejemplo, nombre y puntos).

- `ScoreRepository.kt`  
  Capa de acceso a datos para trabajar con la base de datos SQLite (`scores.db`): inserción y lectura de puntuaciones.

- `Dbtest.kt`  
  Código de apoyo/pruebas para validar la conexión y operaciones básicas sobre la base de datos.

- `Chunk.kt`  
  Representación de un chunk del escenario, con la información necesaria para posicionar el tramo y sus elementos.

- `ChunkGenerator.kt`  
  Lógica de generación de chunks y composición del escenario, incluyendo el cálculo de posiciones y ancho de cada tramo.

Otros elementos relevantes:

- `scores.db`  
  Base de datos SQLite utilizada para guardar las puntuaciones.

- `build.gradle.kts`, `settings.gradle.kts`, `deps.kproject.yml`  
  Configuración del proyecto Gradle y dependencias de KorGE.

---

## Dependencias y tecnología

- **Lenguaje:** Kotlin
- **Motor de juego:** [KorGE](https://github.com/korlibs/korge)
- **Build system:** Gradle Kotlin DSL
- **Base de datos local:** SQLite (a través de JDBC en JVM)
- **IDE recomendado:** IntelliJ IDEA

---

## Requisitos previos

- JDK 21 (o la versión indicada en `gradle.properties` / configuración del proyecto).
- IntelliJ IDEA u otro IDE compatible con Kotlin/Gradle.
- Conexión a Internet la primera vez para que Gradle descargue dependencias.

---

## Cómo ejecutar el juego

Desde la línea de comandos:

```bash
# En la raíz del proyecto
./gradlew runJvm
```

En Windows:

```bash
gradlew.bat runJvm
```

También se puede ejecutar desde IntelliJ IDEA:

1. Importar el proyecto como proyecto Gradle.
2. Esperar a que se resuelvan las dependencias.
3. Ejecutar la configuración `runJvm` (o la que genere KorGE por defecto).

---

## Flujo básico de juego

1. El usuario abre el juego y llega al **menú principal** (`MenuScene`).
2. Desde el menú puede:
   - Empezar una **nueva partida** (carga `GameScene`).
   - Ir a la **pantalla de puntuaciones** (carga `ScoreScene`).
3. En `GameScene`, el jugador:
   - Se mueve por un escenario formado por **chunks** generados por `ChunkGenerator`.
   - Cambia la gravedad para esquivar obstáculos y continuar avanzando.
   - Va acumulando puntos según la distancia o los eventos definidos.
4. Al terminar la partida, se guarda la puntuación en la base de datos local mediante `ScoreRepository`.
5. En `ScoreScene` se consultan y muestran las puntuaciones almacenadas en `scores.db`.

---

## Control de versiones (Git)

El desarrollo se ha gestionado con Git, utilizando commits asociados a iteraciones de la Fase 3 del PFC, por ejemplo:

- `Initial commit (bones)` – creación de la estructura base del proyecto.
- `F3-I2-persistencia-scores` – implementación de la base de datos SQLite y dependencias con Java 21 y Gradle.
- `F3-I3-navegacion- Implementación de navegación entre escenas` – separación y conexión de `MenuScene`, `GameScene` y `ScoreScene`.
- `F3-I4-Animación bit map - Implementación de animaciones bit` – trabajo sobre las animaciones del juego.
- `F3-I5- Cambio en la modalidad de juego` – ajustes posteriores en la forma de jugar (por ejemplo, cambios en la mecánica o en el flujo de escenas).

Este historial permite rastrear la evolución del proyecto y relacionar cada commit con las iteraciones del MVP.

---

## Estado actual y posibles mejoras

Estado actual aproximado:

- Juego funcional con cambio de gravedad y puntuación.
- Escenario basado en chunks y obstáculos.
- Sistema de persistencia de puntuaciones operativo.
- Menú y escenas separadas para juego y puntuaciones.

Líneas de mejora futuras:

- Ajustar dificultad y ritmo de aparición de obstáculos.
- Pulir físicas y tiempos de cambio de gravedad.
- Añadir efectos visuales y de sonido.
- Mejorar la gestión de puntuaciones (por ejemplo, top N, filtros, nombres de jugador).
- Internacionalización básica (es/en) si se requiere.

---

## Licencia

Este proyecto se distribuye bajo la licencia incluida en el archivo `LICENSE` del repositorio.
