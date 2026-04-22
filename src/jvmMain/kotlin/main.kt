import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.image.color.*
import korlibs.math.geom.*

suspend fun main() = Korge(
    windowSize = Size(1280, 720),
    virtualSize = Size(1280, 720),
    backgroundColor = Colors["#2b2b2b"]
) {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { MenuScene() }
}
