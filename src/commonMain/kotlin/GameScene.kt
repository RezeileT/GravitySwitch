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

        // Bucle principal del juego (60fps aprox)
        addUpdater { dt ->
            if (!running) return@addUpdater

            val seconds = dt.toDouble(DurationUnit.SECONDS)  // Delta time → segundos

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
                showWinMessage()
            }

            // Volver al menú (escape)
            if (input.keys.justPressed(Key.ESCAPE)) {
                launchImmediately { sceneContainer.changeTo { MenuScene() } }
            }
        }
    }

    // Pantalla de victoria (overlay)
    private fun SContainer.showWinMessage() {
        text("¡Nivel completado!", textSize = 24.0) {
            centerOnStage()
            y -= 40.0
        }
        text("Pulsa ENTER para volver al menú", textSize = 16.0) {
            centerOnStage()
            y += 20.0
        }

        // Segundo updater para la pantalla de victoria
        addUpdater {
            val input = views.input
            if (input.keys.justPressed(Key.ENTER)) {
                launchImmediately { sceneContainer.changeTo { MenuScene() } }
            }
        }
    }
}
