import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.sprite
import korlibs.korge.view.SpriteAnimation
import korlibs.korge.view.anchor
import korlibs.math.geom.Anchor
import korlibs.math.geom.RectangleD

class ChunkGenerator(
    private val floorY: Double,
    private val batAnim: SpriteAnimation,
    private val chunkWidth: Double
) {


    fun createFirstChunk(root: Container, startX: Double): Chunk {
        val chunkContainer = Container()
        root.addChild(chunkContainer)

        val chunk = Chunk(
            startX = startX,
            width = chunkWidth,
            container = chunkContainer
        )

        fun addPlatform(x: Double, y: Double, w: Double, h: Double) {
            val worldX = startX + x
            chunkContainer.solidRect(w, h, Colors["#555555"]).position(worldX, y)
            chunk.platformRects += RectangleD(worldX, y, w, h)
        }

        fun addBatEnemy(x: Double, y: Double) {
            val worldX = startX + x

            // Igual que el jugador: sprite() + playAnimationLooped()
            val batView = chunkContainer.sprite(batAnim) {
                position(worldX, y)
                anchor(Anchor.CENTER)
                smoothing = false
                playAnimationLooped()
            }

            // Guardamos la vista en el chunk para la lógica de colisión
            chunk.enemyViews += batView
        }

        // --- Diseño del chunk ---
        addPlatform(x = 80.0,  y = floorY - 90.0,  w = 160.0, h = 20.0)
        addPlatform(x = 280.0, y = floorY - 170.0, w = 120.0, h = 20.0)
        addPlatform(x = 380.0, y = floorY - 250.0, w = 80.0,  h = 20.0)

        addBatEnemy(x = 120.0, y = floorY - 90.0 - 16.0)

        return chunk
    }

    fun getChunkWidth(): Double = chunkWidth
}
