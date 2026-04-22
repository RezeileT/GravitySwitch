import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.time.*
import kotlin.time.Duration

class Enemy(
    private val frames: List<BmpSlice>,
    startX: Double,
    startY: Double
) {
    // Animación manual con frames
    private var frameIndex = 0
    private var frameTimer = 0.0
    private val frameInterval = 0.15  // segundos por frame

    // Vista: usamos Image en lugar de Sprite
    val view: Image = Image(frames[0])

    var x: Double = startX
    var y: Double = startY
    private val speed = 60.0
    private var direction = -1.0
    var patrolRange = 150.0
    private val originX = startX

    fun update(dt: Duration) {
        // Mover
        x += speed * direction * dt.seconds
        if (x < originX - patrolRange || x > originX + patrolRange) {
            direction *= -1.0
            view.scaleX = direction
        }

        // Animar
        frameTimer += dt.seconds
        if (frameTimer >= frameInterval) {
            frameTimer = 0.0
            frameIndex = (frameIndex + 1) % frames.size
            view.bitmap = frames[frameIndex]
        }

        view.x = x
        view.y = y
    }
}
