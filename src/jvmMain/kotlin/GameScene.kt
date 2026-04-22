import korlibs.korge.scene.Scene
import korlibs.korge.view.*
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

class GameScene : Scene() {

    data class Enemy(val view: View, var vx: Double)

    override suspend fun SContainer.sceneMain() {
        val input = views.input
        val screenW = views.virtualWidth.toDouble()
        val screenH = views.virtualHeight.toDouble()

        // --- FONDO ---
        solidRect(screenW, screenH, Colors["#1e1e1e"])

        // --- LÍMITES VERTICALES ---
        val floorY = 650.0
        val ceilY  = 60.0

        solidRect(screenW, 20.0, Colors["#333333"]).position(0.0, floorY)
        solidRect(screenW, 20.0, Colors["#333333"]).position(0.0, ceilY - 20.0)

        // --- COLISIONES DE SUELO Y TECHO ---
        val platformRects = mutableListOf(
            RectangleD(0.0, floorY, screenW, 20.0),
            RectangleD(0.0, ceilY - 20.0, screenW, 20.0)
        )

        // --- SPRITES DEL JUGADOR ---
        val sheet = resourcesVfs["Assets/sprirtes/warpsara-nohelmet-anim-sheet-alpha.png"].readBitmap()
        val frameW = 48
        val frameH = 48

        val idleAnim = SpriteAnimation(
            spriteMap = sheet,
            spriteWidth = frameW, spriteHeight = frameH,
            marginLeft = 0, marginTop = 0,
            columns = 4, rows = 1
        )
        val runAnim = SpriteAnimation(
            spriteMap = sheet,
            spriteWidth = frameW, spriteHeight = frameH,
            marginLeft = 0, marginTop = frameH * 4,
            columns = 10, rows = 1
        )

        // --- SPRITE DEL MURCIÉLAGO ---
        val batSheet = resourcesVfs["Assets/sprirtes/trashmobz-alpha.png"].readBitmap()
        val batAnim = SpriteAnimation(
            spriteMap = batSheet,
            spriteWidth = 16, spriteHeight = 16,
            marginTop = 16 * 2, marginLeft = 0,
            columns = 4, rows = 1
        )

        // --- JUGADOR ---
        var currentAnim = "idle"
        val player = sprite(idleAnim) {
            scale = 1.0
            position(240.0, 300.0)
            anchor(Anchor.CENTER)
            smoothing = false
            playAnimationLooped()
        }

        // --- CHUNKS ---
        val chunkGenerator = ChunkGenerator(floorY, batAnim, screenW)
        val chunks = mutableListOf<Chunk>()
        val firstChunk = chunkGenerator.createFirstChunk(this, startX = 0.0)
        chunks += firstChunk
        platformRects.addAll(firstChunk.platformRects)

        // --- ENEMIGOS ---
        val enemies = mutableListOf<Enemy>()
        for (enemyView in firstChunk.enemyViews) {
            enemies += Enemy(enemyView, vx = 60.0)
        }

        // --- ESTADO DEL JUEGO ---
        var currentEndX    = firstChunk.endX
        var velocityY      = 0.0
        var gravityDirection = 1
        var running        = true
        var survivalSeconds = 0.0
        var chunksPassed   = 0
        var maxDifficulty  = 1
        var worldScrollSpeed = 0.0
        val baseScrollSpeed  = 150.0

        fun gameOver() {
            running = false
            val finalScore = (survivalSeconds * 10).toInt() +
                chunksPassed * 100 +
                maxDifficulty * 250
            ScoreRepository.insert(
                Score(playerName = "Player1", level = maxDifficulty, points = finalScore)
            )
            showGameOver(finalScore, survivalSeconds, chunksPassed, maxDifficulty)
        }

        // --- GAME LOOP ---
        addUpdater { dt ->
            if (!running) return@addUpdater
            val secs = dt.toDouble(DurationUnit.SECONDS)
            survivalSeconds += secs
            var moving = false

            // Entrada horizontal
            worldScrollSpeed = 0.0
            if (input.keys[Key.RIGHT]) { worldScrollSpeed =  baseScrollSpeed; moving = true; player.scaleX =  1.0 }
            if (input.keys[Key.LEFT])  { worldScrollSpeed = -baseScrollSpeed; moving = true; player.scaleX = -1.0 }

            // Cambio de gravedad
            if (input.keys.justPressed(Key.SPACE)) {
                gravityDirection *= -1
                if (gravityDirection > 0 && player.y - 16.0 <= ceilY + 2.0)  player.y = ceilY + 18.0
                if (gravityDirection < 0 && player.y + 16.0 >= floorY - 2.0) player.y = floorY - 18.0
            }
            player.scaleY = if (gravityDirection > 0) 1.0 else -1.0

            // Física vertical
            velocityY = (velocityY + 400 * gravityDirection * secs).coerceIn(-600.0, 600.0)
            player.y += velocityY * secs

            // Colisiones con plataformas
            val playerRect = Rectangle(player.x - 16.0, player.y - 16.0, 32.0, 32.0)
            for (c in chunks) {
                for (pr in c.platformRects) {
                    if (!playerRect.intersects(pr)) continue
                    val ox1 = pr.right  - playerRect.left
                    val ox2 = playerRect.right  - pr.left
                    val oy1 = pr.bottom - playerRect.top
                    val oy2 = playerRect.bottom - pr.top
                    if (minOf(ox1, ox2) < minOf(oy1, oy2)) {
                        player.x = if (ox1 < ox2) pr.right + 16.0 else pr.left - 16.0
                    } else {
                        if (oy1 < oy2) { player.y = pr.bottom + 16.0; if (velocityY < 0.0) velocityY = 0.0 }
                        else           { player.y = pr.top    - 16.0; if (velocityY > 0.0) velocityY = 0.0 }
                    }
                }
            }

            // Scroll del mundo
            val dx = worldScrollSpeed * secs
            if (dx != 0.0) {
                for (c in chunks) {
                    c.container.x -= dx
                    for (i in c.platformRects.indices) {
                        val r = c.platformRects[i]
                        c.platformRects[i] = RectangleD(r.x - dx, r.y, r.width, r.height)
                    }
                }
                currentEndX -= dx
            }

            // Lógica de enemigos
            for (e in enemies) {
                e.view.x += e.vx * secs
                if (e.view.x < 20.0)            { e.view.x = 20.0;            e.vx = -e.vx }
                if (e.view.x > screenW - 20.0)  { e.view.x = screenW - 20.0;  e.vx = -e.vx }
                if (e.view.globalBounds.intersects(player.globalBounds)) { gameOver(); return@addUpdater }
            }

            // Muerte por salir de pantalla
            if (player.y > screenH + 20.0 || player.y < -20.0) { gameOver(); return@addUpdater }

            // Generar nuevo chunk
            if (player.x > currentEndX - 200.0) {
                val nextChunk = chunkGenerator.createFirstChunk(this, startX = currentEndX)
                chunks += nextChunk
                platformRects.addAll(nextChunk.platformRects)
                for (ev in nextChunk.enemyViews) enemies += Enemy(ev, vx = 60.0 + maxDifficulty * 10)
                currentEndX = nextChunk.endX
                maxDifficulty++
            }

            // Contador de chunks superados
            if (chunks.size >= 2 && player.x > chunks[1].startX) chunksPassed = chunks.size - 1

            // Animaciones del jugador
            val idleFps = 4.0
            val runFps  = 12.0
            if (moving && currentAnim != "run") {
                currentAnim = "run"
                player.playAnimationLooped(runAnim, spriteDisplayTime = (1.0 / runFps).seconds)
            } else if (!moving && currentAnim != "idle") {
                currentAnim = "idle"
                player.playAnimationLooped(idleAnim, spriteDisplayTime = (1.0 / idleFps).seconds)
            }

            // Salir al menú
            if (input.keys.justPressed(Key.ESCAPE)) launchImmediately { sceneContainer.changeTo<MenuScene>() }
        }
    }

    private fun SContainer.showGameOver(
        finalScore: Int, survivalSeconds: Double, chunksPassed: Int, maxDifficulty: Int
    ) {
        text("GAME OVER",                               28.0) { centerOnStage(); y -= 60 }
        text("Score: $finalScore",                      22.0) { centerOnStage(); y -= 20 }
        text("Tiempo: ${"%.1f".format(survivalSeconds)}s", 18.0) { centerOnStage(); y += 20 }
        text("Chunks: $chunksPassed  |  Dif: $maxDifficulty", 16.0) { centerOnStage(); y += 50 }
        text("ENTER para menú",                         16.0) { centerOnStage(); y += 90 }
        addUpdater {
            if (views.input.keys.justPressed(Key.ENTER))
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
        }
    }
}
