import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.SpriteAnimation
import korlibs.korge.view.anchor
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.sprite
import korlibs.math.geom.Anchor
import korlibs.math.geom.RectangleD
import kotlin.random.Random

class ChunkGenerator(
    private val floorY: Double,
    private val ceilY: Double,
    private val screenHeight: Double,
    private val batAnim: SpriteAnimation,
    private val chunkWidth: Double
) {
    companion object {
        const val PLATFORM_H      = 20.0
        const val MIN_GAP         = 30.0
        const val BASE_MAX_GAP    = 70.0
        const val GAP_PER_DIFF    = 5.0
        const val BASE_PLAT_W     = 140.0
        const val MIN_PLAT_W      = 70.0
        const val PLAT_W_PER_DIFF = 6.0
        const val SAFE_MARGIN_V   = 60.0
        const val ENEMY_CHANCE    = 0.45
    }

    private enum class Pattern {
        EASY_FLAT, DUAL_RAIL, ALTERNATING, STAIRS_DOWN, STAIRS_UP, CHAOS
    }

    // ── Primer chunk ──────────────────────────────────────────────────────────
    fun createFirstChunk(root: Container, startX: Double): Chunk {
        val container = Container().also { root.addChild(it) }
        val chunk = Chunk(startX, chunkWidth, container)

        fun plat(x: Double, y: Double, w: Double) {
            val wx = startX + x
            container.solidRect(w, PLATFORM_H, Colors["#555555"]).position(wx, y)
            chunk.platformRects += RectangleD(wx, y, w, PLATFORM_H)
        }

        // Carril inferior
        plat(30.0,  floorY - 80.0, 150.0)
        plat(220.0, floorY - 80.0, 150.0)
        plat(410.0, floorY - 80.0, 150.0)
        plat(600.0, floorY - 80.0, 150.0)
        plat(790.0, floorY - 80.0, 150.0)

        // Carril superior
        plat(120.0, ceilY + 60.0, 150.0)
        plat(310.0, ceilY + 60.0, 150.0)
        plat(500.0, ceilY + 60.0, 150.0)
        plat(690.0, ceilY + 60.0, 150.0)
        plat(880.0, ceilY + 60.0, 150.0)

        // Sin enemigos en el primer chunk para dar margen al jugador
        return chunk
    }

    // ── Chunk procedural ──────────────────────────────────────────────────────
    // IMPORTANTE: root se pasa para colocar los murciélagos FUERA del container del chunk
    fun createChunk(root: Container, startX: Double, difficulty: Int): Chunk {
        val container = Container().also { root.addChild(it) }
        val chunk = Chunk(startX, chunkWidth, container)

        val maxGapBase = BASE_MAX_GAP + difficulty * GAP_PER_DIFF
        val minPlatW   = (BASE_PLAT_W - difficulty * PLAT_W_PER_DIFF).coerceAtLeast(MIN_PLAT_W)
        val maxPlatW   = minPlatW + 50.0
        val pattern    = choosePattern(difficulty)

        val minY  = ceilY + SAFE_MARGIN_V
        val maxY  = floorY - SAFE_MARGIN_V - PLATFORM_H
        val midY  = (minY + maxY) / 2.0

        when (pattern) {

            Pattern.DUAL_RAIL -> {
                var cursorX = 20.0
                while (cursorX < chunkWidth - 80.0) {
                    val platWidth = Random.nextDouble(minPlatW, maxPlatW)
                    val gap       = Random.nextDouble(MIN_GAP, maxGapBase)
                    val wx        = startX + cursorX

                    val yBottom = floorY - SAFE_MARGIN_V - Random.nextDouble(0.0, 40.0)
                    addPlat(root, container, chunk, wx, yBottom, platWidth, difficulty)

                    val yTop   = ceilY + SAFE_MARGIN_V + Random.nextDouble(0.0, 40.0)
                    val wxTop  = (wx + Random.nextDouble(-30.0, 30.0)).coerceIn(startX, startX + chunkWidth - platWidth)
                    addPlat(root, container, chunk, wxTop, yTop, platWidth, difficulty)

                    cursorX += platWidth + gap
                }
            }

            Pattern.ALTERNATING -> {
                var cursorX  = 20.0
                var isBottom = true
                while (cursorX < chunkWidth - 80.0) {
                    val platWidth = Random.nextDouble(minPlatW, maxPlatW)
                    val gap       = Random.nextDouble(MIN_GAP, maxGapBase)
                    val wx        = startX + cursorX
                    val y = if (isBottom)
                        floorY - SAFE_MARGIN_V - Random.nextDouble(0.0, 35.0)
                    else
                        ceilY + SAFE_MARGIN_V + Random.nextDouble(0.0, 35.0)

                    addPlat(root, container, chunk, wx, y, platWidth, difficulty)
                    isBottom = !isBottom
                    cursorX += platWidth + gap
                }
            }

            Pattern.STAIRS_DOWN -> {
                var cursorX = 20.0
                var step    = 0
                val total   = 6
                while (cursorX < chunkWidth - 80.0) {
                    val platWidth = Random.nextDouble(minPlatW, maxPlatW)
                    val gap       = Random.nextDouble(MIN_GAP, maxGapBase)
                    val wx        = startX + cursorX
                    val t         = (step % total).toDouble() / total
                    val y         = (minY + t * (maxY - minY)).coerceIn(minY, maxY)
                    addPlat(root, container, chunk, wx, y, platWidth, difficulty)
                    step++
                    cursorX += platWidth + gap
                }
            }

            Pattern.STAIRS_UP -> {
                var cursorX = 20.0
                var step    = 0
                val total   = 6
                while (cursorX < chunkWidth - 80.0) {
                    val platWidth = Random.nextDouble(minPlatW, maxPlatW)
                    val gap       = Random.nextDouble(MIN_GAP, maxGapBase)
                    val wx        = startX + cursorX
                    val t         = (step % total).toDouble() / total
                    val y         = (maxY - t * (maxY - minY)).coerceIn(minY, maxY)
                    addPlat(root, container, chunk, wx, y, platWidth, difficulty)
                    step++
                    cursorX += platWidth + gap
                }
            }

            Pattern.CHAOS -> {
                var cursorX = 20.0
                while (cursorX < chunkWidth - 80.0) {
                    val platWidth = Random.nextDouble(minPlatW, maxPlatW)
                    val gap       = Random.nextDouble(MIN_GAP, maxGapBase)
                    val wx        = startX + cursorX
                    val y = if (Random.nextBoolean())
                        Random.nextDouble(minY, midY - 30.0)
                    else
                        Random.nextDouble(midY + 30.0, maxY)
                    addPlat(root, container, chunk, wx, y, platWidth, difficulty)
                    cursorX += platWidth + gap
                }
            }

            Pattern.EASY_FLAT -> {
                var cursorX = 20.0
                while (cursorX < chunkWidth - 80.0) {
                    val platWidth = Random.nextDouble(minPlatW, maxPlatW)
                    val gap       = Random.nextDouble(MIN_GAP, maxGapBase)
                    val wx        = startX + cursorX
                    val y         = floorY - SAFE_MARGIN_V - Random.nextDouble(0.0, 30.0)
                    addPlat(root, container, chunk, wx, y, platWidth, difficulty)
                    cursorX += platWidth + gap
                }
            }
        }

        return chunk
    }

    // ── Añade plataforma + posiblemente murciélago ────────────────────────────
    private fun addPlat(
        root: Container,
        container: Container,
        chunk: Chunk,
        wx: Double, y: Double, w: Double,
        difficulty: Int
    ) {
        // Plataforma dentro del container del chunk (se scrollea con él)
        container.solidRect(w, PLATFORM_H, Colors["#555555"]).position(wx, y)
        chunk.platformRects += RectangleD(wx, y, w, PLATFORM_H)

        // Murciélago directamente en root (NO dentro del container)
        if (Random.nextDouble() < ENEMY_CHANCE + difficulty * 0.03) {
            val midWorldY = (floorY + ceilY) / 2.0
            val offsetX   = Random.nextDouble(-w * 0.25, w * 0.25)
            // Si plataforma está arriba → enemigo justo debajo | si está abajo → justo encima
            val enemyY = if (y < midWorldY) y + PLATFORM_H + 8.0 else y - 10.0

            val bat = root.sprite(batAnim) {
                position(wx + w / 2.0 + offsetX, enemyY)
                anchor(Anchor.CENTER)
                smoothing = false
                playAnimationLooped()
            }
            chunk.enemyViews += bat   // guardamos referencia para scroll y limpieza
        }
    }

    private fun choosePattern(difficulty: Int): Pattern = when {
        difficulty == 0 -> Pattern.EASY_FLAT
        difficulty == 1 -> if (Random.nextBoolean()) Pattern.DUAL_RAIL else Pattern.ALTERNATING
        difficulty == 2 -> listOf(Pattern.DUAL_RAIL, Pattern.ALTERNATING, Pattern.STAIRS_DOWN).random()
        difficulty == 3 -> listOf(Pattern.ALTERNATING, Pattern.STAIRS_DOWN, Pattern.STAIRS_UP, Pattern.DUAL_RAIL).random()
        else            -> Pattern.values().toList().filter { it != Pattern.EASY_FLAT }.random()
    }

    fun getChunkWidth(): Double = chunkWidth
}
