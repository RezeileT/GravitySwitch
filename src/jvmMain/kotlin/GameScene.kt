import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.korge.view.sprite
import korlibs.korge.view.animation.*
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.event.Key
import korlibs.math.geom.Anchor
import korlibs.math.geom.Rectangle
import korlibs.math.geom.RectangleD
import kotlin.time.DurationUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GameScene(private val level: Int) : Scene() {
    override suspend fun SContainer.sceneMain() {
        val input = views.input

        // Fondo
        solidRect(512, 512, Colors["#1e1e1e"])

        // Suelo y techo
        val floorY = 450.0
        val ceilY = 60.0
        solidRect(512, 20, Colors["#333333"]).position(0.0, floorY)
        solidRect(512, 20, Colors["#333333"]).position(0.0, ceilY - 20)

        // Cargar spritesheet UNA sola vez
        val sheet = resourcesVfs["Assets/sprirtes/warpsara-nohelmet-anim-sheet-alpha.png"].readBitmap()

        // Tamaño de cada frame (ajusta si hace falta)
        val frameW = 48
        val frameH = 48

        // Animación idle: primera fila, 4 frames
        val idleAnim = SpriteAnimation(
            spriteMap = sheet,
            spriteWidth = frameW,
            spriteHeight = frameH,
            marginLeft = 0,
            marginTop = 0,
            columns = 4,
            rows = 1
        )

        // Animación run: fila siguiente, 10 frames
        val runStartRow = 4
        val runAnim = SpriteAnimation(
            spriteMap = sheet,
            spriteWidth = frameW,
            spriteHeight = frameH,
            marginLeft = 0,
            marginTop = frameH * runStartRow,  // saltar 3 filas
            columns = 10,
            rows = 1
        )

        var currentAnim = "idle"
        var flipped = false

        val player = sprite(idleAnim) {
            scale = 1.0
            position(240.0, 300.0)
            anchor(Anchor.CENTER)
            smoothing = false
            playAnimationLooped()
        }
        val goalX = if (level == 1) 440.0 else 420.0
        val goalY = if (level == 1) 300.0 else 150.0

        val goal = solidRect(32, 32, Colors["#ffcc00"]) {
            position(goalX, goalY)
            anchor(Anchor.CENTER)
        }

        data class Platform(val view: View, val rect: RectangleD)

        val platforms = mutableListOf<Platform>()

        fun addPlatform(x: Double, y: Double, w: Double, h: Double) {
            val v = solidRect(w, h, Colors["#555555"]).position(x, y)
            platforms += Platform(v, RectangleD(x, y, w, h))
        }

        // Nivel 1
        if (level == 1) {
            addPlatform(150.0, 360.0, 120.0, 20.0)
            addPlatform(320.0, 280.0, 100.0, 20.0)
        } else {
            // Nivel 2
            addPlatform(80.0, 340.0, 140.0, 20.0)
            addPlatform(260.0, 260.0, 140.0, 20.0)
        }

        data class Enemy(val view: View, var vx: Double)

        val enemies = mutableListOf<Enemy>()

        fun addEnemy(x: Double, y: Double): Enemy {
            val e = solidRect(24.0, 24.0, Colors["#ff4444"]).position(x, y)
            val enemy = Enemy(e, vx = 60.0)
            enemies += enemy
            return enemy
        }

        if (level == 1) {
            addEnemy(200.0, floorY - 24.0)
        } else {
            addEnemy(300.0, floorY - 24.0)
        }


        /*val goal = solidRect(32, 32, Colors["#ffcc00"]) {
            position(440.0, 300.0)
            anchor(Anchor.CENTER)
        }*/

        var velocityY = 0.0
        var gravityDirection = 1
        var running = true
        var elapsedSeconds = 0.0

        addUpdater { dt ->
            if (!running) return@addUpdater
            val seconds = dt.toDouble(DurationUnit.SECONDS)

            elapsedSeconds += seconds
            var moving = false

            // Movimiento horizontal
            if (input.keys[Key.LEFT]) {
                player.x -= 150 * seconds
                moving = true
                player.scaleX = -1.0
            }
            if (input.keys[Key.RIGHT]) {
                player.x += 150 * seconds
                moving = true
                player.scaleX = 1.0
            }

            val playerRect = Rectangle(
                player.x - 16.0,
                player.y - 16.0,
                32.0,
                32.0
            )

            for (p in platforms) {
                val pr = p.rect

                if (playerRect.intersects(pr)) {
                    val overlapX1 = pr.right - playerRect.left
                    val overlapX2 = playerRect.right - pr.left
                    val overlapY1 = pr.bottom - playerRect.top
                    val overlapY2 = playerRect.bottom - pr.top

                    val minOverlapX = minOf(overlapX1, overlapX2)
                    val minOverlapY = minOf(overlapY1, overlapY2)

                    if (minOverlapX < minOverlapY) {
                        // Colisión lateral
                        if (overlapX1 < overlapX2) {
                            // chocando desde la izquierda
                            player.x = pr.right + 16.0
                        } else {
                            // chocando desde la derecha
                            player.x = pr.left - 16.0
                        }
                    } else {
                        // Colisión vertical
                        if (overlapY1 < overlapY2) {
                            // chocando desde arriba (pisas plataforma)
                            player.y = pr.bottom + 16.0
                            velocityY = 0.0
                        } else {
                            // chocando desde abajo (cabeza)
                            player.y = pr.top - 16.0
                            velocityY = 0.0
                        }
                    }
                }
            }

            for (e in enemies) {
                val v = e.view
                v.x += e.vx * seconds

                // Rebotar en bordes de pantalla
                if (v.x < 20.0) {
                    v.x = 20.0
                    e.vx = -e.vx
                }
                if (v.x > 512.0 - 20.0) {
                    v.x = 512.0 - 20.0
                    e.vx = -e.vx
                }

                // Colisión con jugador → muerte / reinicio
                if (v.globalBounds.intersects(player.globalBounds)) {
                    // por ahora: resetear nivel
                    running = false
                    launchImmediately { sceneContainer.changeTo { GameScene(level) } }
                    return@addUpdater
                }
            }




            if (input.keys.justPressed(Key.SPACE)) {
                flipped = !flipped
                player.scaleY = if (flipped) -player.scaleY else player.scaleY
            }



            val idleFps = 4.0

            val runFps = 12.0

            // Cambiar animación según movimiento
            if (moving && currentAnim != "run") {
                currentAnim = "run"
                player.playAnimationLooped(
                    runAnim,
                    spriteDisplayTime = (1.0 / runFps).seconds
                )
            } else if (!moving && currentAnim != "idle") {
                currentAnim = "idle"
                player.playAnimationLooped(
                    idleAnim,
                    spriteDisplayTime = (1.0 / idleFps).seconds
                )
            }

            // Cambio de gravedad
            if (input.keys.justPressed(Key.SPACE)) gravityDirection *= -1

            // Física vertical
            velocityY += 400 * gravityDirection * seconds
            player.y += velocityY * seconds

            if (player.y + 16 > floorY) {
                player.y = floorY - 16
                velocityY = 0.0
            }
            if (player.y - 16 < ceilY) {
                player.y = ceilY + 16
                velocityY = 0.0
            }

            // Victoria
            if (player.globalBounds.intersects(goal.globalBounds)) {
                running = false
                val points = (1000 - (elapsedSeconds * 100)).toInt().coerceAtLeast(0)
                ScoreRepository.insert(Score("Player1", 1, points))
                showWinMessage(points, elapsedSeconds)
            }

            // ESC → menú
            if (input.keys.justPressed(Key.ESCAPE)) {
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
            }
        }
    }

    private fun SContainer.showWinMessage(points: Int, seconds: Double) {
        text("¡VICTORIA!", 28.0) { centerOnStage(); y -= 50 }
        text("Score: $points", 22.0) { centerOnStage() }
        text("Tiempo: ${"%.1f".format(seconds)}s", 18.0) { centerOnStage(); y += 30 }
        text("ENTER para menú", 16.0) { centerOnStage(); y += 80 }

        addUpdater {
            if (views.input.keys.justPressed(Key.ENTER)) {
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
            }
        }
    }
}
