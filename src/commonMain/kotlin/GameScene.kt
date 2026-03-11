import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.view.align.centerOnStage
import korlibs.event.Key
import korlibs.io.async.launchImmediately
import kotlin.time.DurationUnit

class GameScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val input = views.input

        solidRect(Size(512, 512), Colors["#1e1e1e"])

        val floorY = 450.0
        val ceilY = 60.0
        solidRect(Size(512, 20), Colors["#333333"]).position(0.0, floorY)
        solidRect(Size(512, 20), Colors["#333333"]).position(0.0, ceilY - 20)

        val playerSize = Size(32, 32)
        val player = solidRect(playerSize, Colors["#00ff99"]).position(240.0, 300.0)

        val goal = solidRect(Size(32, 32), Colors["#ffcc00"]).position(440.0, 300.0)

        var velocityY = 0.0
        val gravity = 400.0
        var gravityDirection = 1
        var running = true

        addUpdater { dt ->
            if (!running) return@addUpdater

            val seconds = dt.toDouble(DurationUnit.SECONDS)  // dt -> segundos[web:277]

            if (input.keys[Key.LEFT])  player.x -= 150 * seconds
            if (input.keys[Key.RIGHT]) player.x += 150 * seconds

            if (input.keys.justPressed(Key.SPACE)) {
                gravityDirection *= -1
            }

            velocityY += gravity * gravityDirection * seconds
            player.y += velocityY * seconds

            if (player.y + player.height > floorY) {
                player.y = floorY - player.height
                velocityY = 0.0
            }
            if (player.y < ceilY) {
                player.y = ceilY
                velocityY = 0.0
            }

            if (player.globalBounds.intersects(goal.globalBounds)) {
                running = false
                showWinMessage()
            }

            if (input.keys.justPressed(Key.ESCAPE)) {
                launchImmediately { sceneContainer.changeTo { MenuScene() } }
            }
        }
    }

    private fun SContainer.showWinMessage() {
        text("¡Nivel completado!", textSize = 24.0) {
            centerOnStage()
            y -= 40.0
        }
        text("Pulsa ENTER para volver al menú", textSize = 16.0) {
            centerOnStage()
            y += 20.0
        }

        addUpdater {
            val input = views.input
            if (input.keys.justPressed(Key.ENTER)) {
                launchImmediately { sceneContainer.changeTo { MenuScene() } }
            }
        }
    }
}
