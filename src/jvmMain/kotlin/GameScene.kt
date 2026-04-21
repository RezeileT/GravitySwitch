import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.animation.ImageAnimationView
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.event.Key
import korlibs.korge.view.align.centerOnStage
import korlibs.math.geom.Anchor
import korlibs.math.geom.Rectangle
import korlibs.math.geom.RectangleD
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.seconds

// Escena principal de juego
class GameScene : Scene() {

    // Datos básicos de un enemigo: la vista y su velocidad horizontal
    data class Enemy(val view: View, var vx: Double)

    override suspend fun SContainer.sceneMain() {
        // Referencia al sistema de entrada de KorGE
        val input = views.input

        // Dibuja un fondo simple de color oscuro
        solidRect(512, 512, Colors["#1e1e1e"])

        // Coordenadas verticales del suelo y del techo
        val floorY = 450.0
        val ceilY = 60.0

        // Dibuja el rectángulo que representa el suelo en pantalla
        solidRect(512, 20, Colors["#333333"]).position(0.0, floorY)
        // Dibuja el rectángulo que representa el techo en pantalla
        solidRect(512, 20, Colors["#333333"]).position(0.0, ceilY - 20)

        // Lista con todos los rectángulos sólidos de colisión (suelo, techo y plataformas)
        val platformRects = mutableListOf<RectangleD>().apply {
            // Rectángulo de colisión del suelo
            add(RectangleD(0.0, floorY, 512.0, 20.0))
            // Rectángulo de colisión del techo
            add(RectangleD(0.0, ceilY - 20.0, 512.0, 20.0))
        }

        // Velocidad a la que se desplaza el mundo hacia la izquierda cuando el jugador avanza
        var worldScrollSpeed = 0.0
        val baseScrollSpeed = 150.0

        // Carga la hoja de sprites del jugador desde recursos
        val sheet = resourcesVfs["Assets/sprirtes/warpsara-nohelmet-anim-sheet-alpha.png"].readBitmap()

        // Tamaño de cada frame en la hoja de sprites
        val frameW = 48
        val frameH = 48

        // Animación de idle del jugador (quieto)
        val idleAnim = SpriteAnimation(
            spriteMap = sheet,
            spriteWidth = frameW,
            spriteHeight = frameH,
            marginLeft = 0,
            marginTop = 0,
            columns = 4,
            rows = 1
        )

        // Fila de inicio para la animación de correr
        val runStartRow = 4

        // Animación de correr del jugador
        val runAnim = SpriteAnimation(
            spriteMap = sheet,
            spriteWidth = frameW,
            spriteHeight = frameH,
            marginLeft = 0,
            marginTop = frameH * runStartRow,
            columns = 10,
            rows = 1
        )

        // Estado actual de la animación del jugador
        var currentAnim = "idle"

        // Crea el sprite del jugador en la escena, con la animación idle por defecto
        val player = sprite(idleAnim) {
            // Escala normal
            scale = 1.0
            // Posición inicial del jugador en pantalla
            position(240.0, 300.0)
            // Ancla el sprite en el centro para facilitar giros y escalados
            anchor(Anchor.CENTER)
            // Desactiva suavizado de la imagen para estética pixel-art
            smoothing = false
            // Reproduce la animación de forma continua
            playAnimationLooped()
        }

        // ---------- CONFIGURACIÓN DE CHUNKS (TRAMOS DEL ESCENARIO) ----------

        // Generador de chunks que sabe crear tramos de escenario a partir del suelo
        val chunkGenerator = ChunkGenerator(floorY)

        // Lista con todos los chunks actualmente en el juego
        val chunks = mutableListOf<Chunk>()

        // Crea el primer chunk comenzando en la posición X = 0
        val firstChunk = chunkGenerator.createFirstChunk(this, startX = 0.0)

        // Añade el primer chunk a la lista de chunks cargados
        chunks += firstChunk

        // Añade las plataformas del primer chunk a la lista global de colisiones
        platformRects.addAll(firstChunk.platformRects)

        // Lista de enemigos activos en el juego
        val enemies = mutableListOf<Enemy>().apply {
            // Crea un Enemy por cada vista de enemigo del primer chunk
            for (enemyView in firstChunk.enemyViews) {
                add(Enemy(enemyView, vx = 60.0))
            }
        }

        // Marca la coordenada X donde termina el último chunk generado
        var currentEndX = firstChunk.endX

        // Velocidad vertical actual del jugador (para la física de gravedad)
        var velocityY = 0.0

        // Dirección de la gravedad: 1 hacia el suelo, -1 hacia el techo
        var gravityDirection = 1

        // Indica si el juego sigue en marcha o se ha terminado
        var running = true

        // Tiempo total que el jugador ha sobrevivido en la partida
        var survivalSeconds = 0.0

        // Número de chunks que el jugador ha superado
        var chunksPassed = 0

        // Nivel de dificultad máximo alcanzado durante la partida
        var maxDifficulty = 1

        // Función que se ejecuta cuando la partida termina
        fun gameOver() {
            // Detiene la lógica de actualización del juego
            running = false

            // Calcula los componentes de la puntuación final
            val timeScore = (survivalSeconds * 10).toInt()
            val chunkScore = chunksPassed * 100
            val difficultyScore = maxDifficulty * 250

            // Suma todos los componentes en una puntuación final
            val finalScore = timeScore + chunkScore + difficultyScore

            // Guarda la puntuación final en la base de datos local
            ScoreRepository.insert(
                Score(
                    playerName = "Player1",
                    level = maxDifficulty,
                    points = finalScore
                )
            )

            // Muestra la pantalla de Game Over con el resumen de la partida
            showGameOver(finalScore, survivalSeconds, chunksPassed, maxDifficulty)
        }

        // Actualizador que se ejecuta en cada frame del juego
        addUpdater { dt ->
            // Si el juego no está en marcha, no actualiza nada más
            if (!running) return@addUpdater

            // Convierte el delta de tiempo a segundos como Double
            val seconds = dt.toDouble(DurationUnit.SECONDS)

            // Acumula el tiempo total sobrevivido
            survivalSeconds += seconds

            // Indica si el jugador se está moviendo horizontalmente en este frame
            var moving = false

            // ---------- ENTRADA HORIZONTAL DEL JUGADOR ----------

            // Reinicia velocidad del mundo cada frame
            worldScrollSpeed = 0.0

            // Si pulsas derecha, quieres avanzar: el mundo se mueve a la izquierda
            if (input.keys[Key.RIGHT]) {
                worldScrollSpeed = baseScrollSpeed
                moving = true
                player.scaleX = 1.0
            }

            // Si pulsas izquierda, retrocedes: el mundo se mueve a la derecha (opcional)
            if (input.keys[Key.LEFT]) {
                worldScrollSpeed = -baseScrollSpeed
                moving = true
                player.scaleX = -1.0
            }

            // ---------- CAMBIO DE GRAVEDAD ----------

            // Si se pulsa espacio una vez, invierte la dirección de la gravedad
            if (input.keys.justPressed(Key.SPACE)) {
                // Cambia la gravedad de 1 a -1 o de -1 a 1
                gravityDirection *= -1

                // Ajusta ligeramente la posición para evitar quedarse incrustado en techo o suelo
                if (gravityDirection > 0 && player.y - 16.0 <= ceilY + 2.0) {
                    // Coloca al jugador un poco por debajo del techo si la gravedad vuelve hacia el suelo
                    player.y = ceilY + 18.0
                } else if (gravityDirection < 0 && player.y + 16.0 >= floorY - 2.0) {
                    // Coloca al jugador un poco por encima del suelo si la gravedad va hacia el techo
                    player.y = floorY - 18.0
                }
                // No se toca la velocidad vertical para mantener el momento
            }

            // Refleja visualmente el cambio de gravedad invirtiendo la escala vertical
            player.scaleY = if (gravityDirection > 0) 1.0 else -1.0

            // ---------- FÍSICA VERTICAL DEL JUGADOR ----------

            // Aplica la aceleración de la gravedad según su dirección
            velocityY += 400 * gravityDirection * seconds

            // Limita la velocidad vertical máxima en ambas direcciones
            val maxVel = 600.0
            if (velocityY > maxVel) velocityY = maxVel
            if (velocityY < -maxVel) velocityY = -maxVel

            // Actualiza la posición vertical del jugador según su velocidad
            player.y += velocityY * seconds

            // ---------- COLISIÓN DEL JUGADOR CON PLATAFORMAS ----------

            // Rectángulo de colisión del jugador basado en su posición y tamaño
            val playerRect = Rectangle(
                player.x - 16.0,
                player.y - 16.0,
                32.0,
                32.0
            )

            // Recorre todas las plataformas sólidas del juego
            for (pr in platformRects) {
                // Comprueba si el rectángulo del jugador intersecta con la plataforma
                if (playerRect.intersects(pr)) {
                    // Calcula el solapamiento en cada dirección
                    val overlapX1 = pr.right - playerRect.left
                    val overlapX2 = playerRect.right - pr.left
                    val overlapY1 = pr.bottom - playerRect.top
                    val overlapY2 = playerRect.bottom - pr.top

                    // Obtiene el solapamiento mínimo en horizontal y vertical
                    val minOverlapX = minOf(overlapX1, overlapX2)
                    val minOverlapY = minOf(overlapY1, overlapY2)

                    // Decide si corrige primero en eje X (lateral) o eje Y (vertical)
                    if (minOverlapX < minOverlapY) {
                        // Resolución lateral de la colisión
                        if (overlapX1 < overlapX2) {
                            // Coloca al jugador a la derecha de la plataforma
                            player.x = pr.right + 16.0
                        } else {
                            // Coloca al jugador a la izquierda de la plataforma
                            player.x = pr.left - 16.0
                        }
                    } else {
                        // Resolución vertical de la colisión
                        if (overlapY1 < overlapY2) {
                            // Coloca al jugador por debajo de la plataforma
                            player.y = pr.bottom + 16.0
                            // Detiene la velocidad hacia arriba
                            if (velocityY < 0.0) velocityY = 0.0
                        } else {
                            // Coloca al jugador por encima de la plataforma
                            player.y = pr.top - 16.0
                            // Detiene la velocidad hacia abajo
                            if (velocityY > 0.0) velocityY = 0.0
                        }
                    }
                }
            }
            // ---------- DESPLAZAR EL MUNDO SEGÚN worldScrollSpeed ----------

            val dx = worldScrollSpeed * seconds

            if (dx != 0.0) {
                // Mueve todas las plataformas del mundo (rectángulo de colisión y vista)
                for (c in chunks) {
                    // mover el contenedor entero del chunk
                    c.container.x -= dx

                    // actualizar las coordenadas de colisión de cada plataforma de ese chunk
                    for (i in c.platformRects.indices) {
                        val r = c.platformRects[i]
                        c.platformRects[i] = RectangleD(r.x - dx, r.y, r.width, r.height)
                    }
                }

                // También deberías mover suelo y techo si quieres que el fondo “viaje”
                // pero como ya son globales, mejor que se queden fijos para que parezca referencia.

                // Actualiza currentEndX porque el mundo se ha desplazado
                currentEndX -= dx
            }

            // ---------- LÓGICA DE ENEMIGOS ----------

            // Recorre todos los enemigos activos
            for (e in enemies) {
                val v = e.view

                // Mueve al enemigo en horizontal según su velocidad
                v.x += e.vx * seconds

                // Invierte la dirección del enemigo si llega a la zona izquierda
                if (v.x < 20.0) {
                    v.x = 20.0
                    e.vx = -e.vx
                }

                // Invierte la dirección del enemigo si llega a la zona derecha
                if (v.x > 512.0 - 20.0) {
                    v.x = 512.0 - 20.0
                    e.vx = -e.vx
                }

                // Comprueba si el enemigo choca con el jugador
                if (v.globalBounds.intersects(player.globalBounds)) {
                    // Termina la partida si hay colisión con un enemigo
                    gameOver()
                    return@addUpdater
                }
            }

            // ---------- CONDICIÓN DE MUERTE POR SALIR DE PANTALLA ----------

            // Si el jugador sale por arriba o por abajo de la pantalla, termina la partida
            if (player.y > 520.0 || player.y < 0.0) {
                gameOver()
                return@addUpdater
            }

            // ---------- GENERACIÓN DE NUEVOS CHUNKS (ENDLESS) ----------

            // Distancia de margen para generar el siguiente chunk antes de llegar al final
            val spawnMargin = 200.0

            // Si el jugador se acerca al final del último chunk, se crea uno nuevo
            if (player.x > currentEndX - spawnMargin) {
                // Punto de inicio del nuevo chunk justo después del último
                val nextChunkStart = currentEndX

                // Crea un nuevo chunk comenzando en nextChunkStart
                val nextChunk = chunkGenerator.createFirstChunk(this, startX = nextChunkStart)

                // Añade el nuevo chunk a la lista de chunks activos
                chunks += nextChunk

                // Añade las plataformas del nuevo chunk a la lista global de colisiones
                platformRects.addAll(nextChunk.platformRects)

                // Añade los enemigos del nuevo chunk, con velocidad algo mayor por dificultad
                for (enemyView in nextChunk.enemyViews) {
                    enemies += Enemy(enemyView, vx = 60.0 + maxDifficulty * 10)
                }

                // Actualiza la X final del mundo al final del nuevo chunk
                currentEndX = nextChunk.endX

                // Aumenta el nivel máximo de dificultad al generar un nuevo chunk
                maxDifficulty++
            }

            // Actualiza el número de chunks superados en función de la posición del jugador
            if (chunks.size >= 2) {
                val secondChunk = chunks[1]
                // Si el jugador ha sobrepasado el inicio del segundo chunk, cuenta al menos uno pasado
                if (player.x > secondChunk.startX) {
                    chunksPassed = 1
                }
                // Si más adelante reciclas chunks, aquí podrías incrementar chunksPassed
            }

            // ---------- GESTIÓN DE ANIMACIONES DEL JUGADOR ----------

            // Fotogramas por segundo de la animación idle
            val idleFps = 4.0

            // Fotogramas por segundo de la animación de correr
            val runFps = 12.0

            // Cambia a animación de correr si el jugador se está moviendo
            if (moving && currentAnim != "run") {
                currentAnim = "run"
                player.playAnimationLooped(
                    runAnim,
                    spriteDisplayTime = (1.0 / runFps).seconds
                )
            }
            // Cambia a animación idle si el jugador está quieto
            else if (!moving && currentAnim != "idle") {
                currentAnim = "idle"
                player.playAnimationLooped(
                    idleAnim,
                    spriteDisplayTime = (1.0 / idleFps).seconds
                )
            }

            // ---------- SALIR AL MENÚ PRINCIPAL ----------

            // Si se pulsa ESC, cambia a la escena de menú
            if (input.keys.justPressed(Key.ESCAPE)) {
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
            }
        }
    }

    // Muestra la pantalla de Game Over con la información de la partida
    private fun SContainer.showGameOver(
        finalScore: Int,
        survivalSeconds: Double,
        chunksPassed: Int,
        maxDifficulty: Int
    ) {
        // Texto principal de Game Over
        text("GAME OVER", 28.0) {
            centerOnStage()
            y -= 60
        }

        // Muestra la puntuación final
        text("Score: $finalScore", 22.0) {
            centerOnStage()
            y -= 20
        }

        // Muestra el tiempo sobrevivido con un decimal
        text("Tiempo: ${"%.1f".format(survivalSeconds)}s", 18.0) {
            centerOnStage()
            y += 20
        }

        // Muestra chunks superados y dificultad máxima alcanzada
        text("Chunks: $chunksPassed  |  Dif: $maxDifficulty", 16.0) {
            centerOnStage()
            y += 50
        }

        // Indica cómo volver al menú principal
        text("ENTER para menú", 16.0) {
            centerOnStage()
            y += 90
        }

        // Escucha la tecla ENTER para volver al menú
        addUpdater {
            if (views.input.keys.justPressed(Key.ENTER)) {
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
            }
        }
    }
}
