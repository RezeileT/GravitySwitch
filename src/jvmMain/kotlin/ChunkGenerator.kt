import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.math.geom.RectangleD


class ChunkGenerator(
    // Altura del suelo para colocar las plataformas a partir de ahí
    private val floorY: Double
) {
    // Ancho fijo que tendrá cada chunk en el eje X
    private val chunkWidth = 512.0

    // Crea un chunk empezando en startX y lo añade al contenedor root
    fun createFirstChunk(root: Container, startX: Double): Chunk {
        // Contenedor propio del chunk para agrupar sus vistas
        val chunkContainer = Container()
        root.addChild(chunkContainer)

        // Instancia del chunk con su posición inicial y su ancho
        val chunk = Chunk(
            startX = startX,
            width = chunkWidth,
            container = chunkContainer
        )

        // Función auxiliar para crear una plataforma en el chunk
        fun addPlatform(x: Double, y: Double, w: Double, h: Double) {
            // Convierte la X relativa al chunk en X absoluta del mundo
            val worldX = startX + x
            // Dibuja el rectángulo de la plataforma en pantalla
            val platformView = chunkContainer
                .solidRect(w, h, Colors["#555555"])
                .position(worldX, y)
            // Añade el rectángulo de colisión de la plataforma al chunk
            chunk.platformRects += RectangleD(worldX, y, w, h)
        }

        // Función auxiliar para crear un enemigo en el chunk
        fun addEnemy(x: Double, y: Double) {
            // Convierte la X relativa al chunk en X absoluta del mundo
            val worldX = startX + x
            // Dibuja un cuadrado rojo como enemigo
            val enemyView = chunkContainer
                .solidRect(24.0, 24.0, Colors["#ff4444"])
                .position(worldX, y)
            // Guarda la vista del enemigo en el chunk
            chunk.enemyViews += enemyView
        }

        // --- Diseño fijo del primer chunk ---

        // Plataforma baja larga apoyada sobre el suelo, un poco elevada
        addPlatform(x = 80.0,  y = floorY - 90.0,  w = 160.0, h = 20.0)

        // Plataforma intermedia más alta
        addPlatform(x = 280.0, y = floorY - 170.0, w = 120.0, h = 20.0)

        // Plataforma alta pequeña aún más arriba
        addPlatform(x = 380.0, y = floorY - 250.0, w = 80.0,  h = 20.0)

        // Enemigo colocado encima de la plataforma baja (ajustando su altura)
        addEnemy(
            x = 120.0,
            y = floorY - 90.0 - 24.0
        )

        // Devuelve el chunk ya construido con sus plataformas y enemigos
        return chunk
    }

    // Devuelve el ancho estándar de un chunk (por si lo necesitas desde fuera)
    fun getChunkWidth(): Double = chunkWidth
}
