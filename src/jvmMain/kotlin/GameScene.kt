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
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.seconds

class GameScene : Scene() {

    data class Enemy(val view: View, var vx: Double)

    override suspend fun SContainer.sceneMain() {
        val input   = views.input
        val screenW = views.virtualWidth.toDouble()
        val screenH = views.virtualHeight.toDouble()

        // ── Fondo ─────────────────────────────────────────────────────────────
        val bgBitmap = resourcesVfs["Assets/backgrounds/fondo_nivel.png"].readBitmap()
        image(bgBitmap) {
            position(0.0, 0.0)
            smoothing    = false
            scaledWidth  = screenW
            scaledHeight = screenH
        }

        // ── Límites verticales ────────────────────────────────────────────────
        val floorY = 650.0
        val ceilY  = 60.0

        solidRect(screenW, 20.0, Colors["#333333"]).position(0.0, floorY)
        solidRect(screenW, 20.0, Colors["#333333"]).position(0.0, ceilY - 20.0)

        // ── Sprites del jugador ───────────────────────────────────────────────
        val sheet  = resourcesVfs["Assets/sprirtes/warpsara-nohelmet-anim-sheet-alpha.png"].readBitmap()
        val frameW = 48
        val frameH = 48

        val idleAnim = SpriteAnimation(
            spriteMap = sheet, spriteWidth = frameW, spriteHeight = frameH,
            marginLeft = 0, marginTop = 0, columns = 4, rows = 1
        )
        val runAnim = SpriteAnimation(
            spriteMap = sheet, spriteWidth = frameW, spriteHeight = frameH,
            marginLeft = 0, marginTop = frameH * 4, columns = 10, rows = 1
        )

        // ── Sprite del murciélago ─────────────────────────────────────────────
        val batSheet = resourcesVfs["Assets/sprirtes/trashmobz-alpha.png"].readBitmap()
        val batAnim  = SpriteAnimation(
            spriteMap = batSheet, spriteWidth = 16, spriteHeight = 16,
            marginTop = 16 * 2, marginLeft = 0, columns = 4, rows = 1
        )

        // ── Jugador ───────────────────────────────────────────────────────────
        var currentAnim = "idle"
        val player = sprite(idleAnim) {
            scale = 1.0
            position(240.0, floorY - 100.0)
            anchor(Anchor.CENTER)
            smoothing = false
            playAnimationLooped()
        }

        // ── Generador de chunks ───────────────────────────────────────────────
        val chunkGen = ChunkGenerator(
            floorY       = floorY,
            ceilY        = ceilY,
            screenHeight = screenH,
            batAnim      = batAnim,
            chunkWidth   = 1000.0
        )

        val chunks     = mutableListOf<Chunk>()
        val firstChunk = chunkGen.createFirstChunk(this, 0.0)
        chunks += firstChunk

        // ── Enemigos ──────────────────────────────────────────────────────────
        val enemies = mutableListOf<Enemy>()
        for (ev in firstChunk.enemyViews) {
            enemies += Enemy(ev, vx = (if (Random.nextBoolean()) 1 else -1) * 80.0)
        }

        // ── Estado ────────────────────────────────────────────────────────────
        var currentEndX      = firstChunk.startX + firstChunk.width
        var velocityY        = 0.0
        var gravityDirection = 1
        var running          = true
        var survivalSeconds  = 0.0
        var chunksPassed     = 0
        var difficulty       = 0
        var chunksDone       = 0
        val baseScrollSpeed  = 150.0
        var worldScrollSpeed = 0.0

        // ── Spawn de chunk ────────────────────────────────────────────────────
        fun spawnNextChunk() {
            val nextX    = chunks.last().startX + chunks.last().width
            val newChunk = chunkGen.createChunk(this, nextX, difficulty)
            chunks += newChunk
            for (ev in newChunk.enemyViews) {
                val speed = 80.0 + difficulty * 20.0
                enemies += Enemy(ev, vx = (if (Random.nextBoolean()) 1 else -1) * speed)
            }
            currentEndX = newChunk.startX + newChunk.width
            chunksDone++
            if (chunksDone % 3 == 0) difficulty++
        }

        // Pre-spawn para que el mundo no empiece vacío
        repeat(3) { spawnNextChunk() }

        // ── Game Over ─────────────────────────────────────────────────────────
        fun gameOver() {
            running = false
            val finalScore = (survivalSeconds * 10).toInt() +
                chunksPassed * 100 +
                difficulty * 250

            ScoreRepository.insert(
                Score(
                    playerName   = "Player1",
                    points       = finalScore,
                    level        = difficulty,
                    survivalTime = survivalSeconds,
                    chunksPassed = chunksPassed
                )
            )
            showGameOver(finalScore, survivalSeconds, chunksPassed, difficulty)
        }

        // ── Game Loop ─────────────────────────────────────────────────────────
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
            velocityY = (velocityY + 400.0 * gravityDirection * secs).coerceIn(-600.0, 600.0)
            player.y += velocityY * secs

            // Colisiones con plataformas
            val playerRect = Rectangle(player.x - 16.0, player.y - 16.0, 32.0, 32.0)
            for (c in chunks) {
                for (pr in c.platformRects) {
                    if (!playerRect.intersects(pr)) continue
                    val ox1 = pr.right        - playerRect.left
                    val ox2 = playerRect.right - pr.left
                    val oy1 = pr.bottom        - playerRect.top
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
                    // Mueve el container de plataformas
                    c.container.x -= dx

                    // Actualiza los RectangleD de colisión
                    for (i in c.platformRects.indices) {
                        val r = c.platformRects[i]
                        c.platformRects[i] = RectangleD(r.x - dx, r.y, r.width, r.height)
                    }

                    // ✅ Mueve también los murciélagos (están en root, no en el container)
                    for (ev in c.enemyViews) {
                        ev.x -= dx
                    }
                }
                currentEndX -= dx
            }

            // Enemigos
            for (e in enemies) {
                e.view.x += e.vx * secs

                // ✅ Rebote basado en posición absoluta (los murciélagos ya están en root)
                if (e.view.x < 0.0)           { e.view.x = 0.0;      e.vx = -e.vx }
                if (e.view.x > screenW)       { e.view.x = screenW;  e.vx = -e.vx }

                if (e.view.globalBounds.intersects(player.globalBounds)) {
                    gameOver(); return@addUpdater
                }
            }
            // Muerte por salir de pantalla
            if (player.y > screenH + 20.0 || player.y < -20.0) { gameOver(); return@addUpdater }

            // Spawn anticipado
            if (currentEndX - player.x < 1000.0) spawnNextChunk()

            // Limpieza de chunks viejos
            val toRemove = chunks.filter { it.startX + it.width < player.x - screenW }
            for (old in toRemove) {
                old.container.removeFromParent()
                // ✅ Elimina también los murciélagos de esta chunk del root y de la lista
                for (ev in old.enemyViews) {
                    ev.removeFromParent()
                    enemies.removeAll { it.view == ev }
                }
                chunks.remove(old)
            }

            // Contador chunks
            chunksPassed = (chunks.size - 1).coerceAtLeast(0)

            // Animaciones
            if (moving && currentAnim != "run") {
                currentAnim = "run"
                player.playAnimationLooped(runAnim, spriteDisplayTime = (1.0 / 12.0).seconds)
            } else if (!moving && currentAnim != "idle") {
                currentAnim = "idle"
                player.playAnimationLooped(idleAnim, spriteDisplayTime = (1.0 / 4.0).seconds)
            }

            if (input.keys.justPressed(Key.ESCAPE))
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
        }
    }

    private fun SContainer.showGameOver(
        finalScore: Int, survivalSeconds: Double, chunksPassed: Int, difficulty: Int
    ) {
        text("GAME OVER",                                   28.0) { centerOnStage(); y -= 60 }
        text("Score: $finalScore",                          22.0) { centerOnStage(); y -= 20 }
        text("Tiempo: ${"%.1f".format(survivalSeconds)}s",  18.0) { centerOnStage(); y += 20 }
        text("Chunks: $chunksPassed  |  Dif: $difficulty",  16.0) { centerOnStage(); y += 50 }
        text("ENTER para menú",                             16.0) { centerOnStage(); y += 90 }
        addUpdater {
            if (views.input.keys.justPressed(Key.ENTER))
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
        }
    }
}
