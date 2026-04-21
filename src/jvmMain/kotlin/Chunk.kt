import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.math.geom.RectangleD

class Chunk(
    val startX: Double,          // posición X donde empieza el chunk
    val width: Double,           // ancho total del chunk
    val container: Container     // contenedor donde se han dibujado sus vistas
) {
    // Lista de rectángulos de colisión de las plataformas del chunk
    val platformRects = mutableListOf<RectangleD>()

    // Lista de vistas de enemigos pertenecientes a este chunk
    val enemyViews = mutableListOf<View>()

    // Coordenada X donde termina el chunk (inicio + ancho)
    val endX: Double
        get() = startX + width
}
