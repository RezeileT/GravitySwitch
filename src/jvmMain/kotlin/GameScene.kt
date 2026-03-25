import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.view.align.*
import korlibs.event.Key
import korlibs.io.async.*
import kotlin.time.DurationUnit


class GameScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val input = views.input  // Estado de teclado por frame

        // Fondo oscuro
        solidRect(Size(512, 512), Colors["#1e1e1e"])

        // Suelo y techo (límites visuales)
        val floorY = 450.0
        val ceilY = 60.0
        solidRect(Size(512, 20), Colors["#333333"]).position(0.0, floorY)
        solidRect(Size(512, 20), Colors["#333333"]).position(0.0, ceilY - 20)

        // Jugador: cuadrado verde (32x32)
        val playerSize = Size(32, 32)
        val player = solidRect(playerSize, Colors["#00ff99"]).position(240.0, 300.0)

        // Objetivo: bloque amarillo
        val goal = solidRect(Size(32, 32), Colors["#ffcc00"]).position(440.0, 300.0)

        // Física básica
        var velocityY = 0.0        // Velocidad vertical
        val gravity = 400.0        // Fuerza gravedad (px/s²)
        var gravityDirection = 1   // 1=abajo, -1=arriba
        var running = true         // Estado del juego
        var elapsedSeconds = 0.0   // Tiempo total partida

        // Bucle principal del juego (60fps aprox)
        addUpdater { dt ->
            if (!running) return@addUpdater

            val seconds = dt.toDouble(DurationUnit.SECONDS)  // Delta time → segundos
            elapsedSeconds += seconds

            // Movimiento horizontal (150 px/s)
            if (input.keys[Key.LEFT])  player.x -= 150 * seconds
            if (input.keys[Key.RIGHT]) player.x += 150 * seconds

            // Cambiar dirección gravedad (mecánica principal)
            if (input.keys.justPressed(Key.SPACE)) {
                gravityDirection *= -1
            }

            // Aplicar gravedad + movimiento vertical
            velocityY += gravity * gravityDirection * seconds
            player.y += velocityY * seconds

            // Colisiones suelo/techo (sin rebote)
            if (player.y + player.height > floorY) {
                player.y = floorY - player.height
                velocityY = 0.0
            }
            if (player.y < ceilY) {
                player.y = ceilY
                velocityY = 0.0
            }

            // Condición victoria (intersección con goal)
            if (player.globalBounds.intersects(goal.globalBounds)) {
                running = false

                //Calcular score
                val points = (1000 - (elapsedSeconds * 100)).toInt().coerceAtLeast(0)
                val score = Score("Player1", 1, points)

                // ← GUARDAR en SQLite
                ScoreRepository.insert(score)

                showWinMessage(points, elapsedSeconds)
            }

            // Volver al menú (escape)
            if (input.keys.justPressed(Key.ESCAPE)) {
                launchImmediately { sceneContainer.changeTo { MenuScene() } }
            }
        }
    }

    // Pantalla de victoria (overlay)
    private fun SContainer.showWinMessage(points: Int, seconds: Double) {
        text("¡Nivel completado!", textSize = 24.0) { centerOnStage(); y -= 60 }
        text("Puntos: $points", textSize = 20.0) { centerOnStage(); y -= 20 }
        text("Tiempo: ${"%.1f".format(seconds)}s", textSize = 18.0) { centerOnStage(); y += 10 }
        text("Guardado en scores.db", textSize = 14.0, color = Colors["#00ff00"]) { centerOnStage(); y += 40 }
        text("ENTER → Menú", textSize = 16.0) { centerOnStage(); y += 80 }

        // Segundo updater para la pantalla de victoria
        addUpdater {
            val input = views.input
            if (input.keys.justPressed(Key.ENTER)) {
                launchImmediately { sceneContainer.changeTo { MenuScene() } }
            }
        }
    }
}
