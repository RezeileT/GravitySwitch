import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.image.color.*
import korlibs.korge.view.align.centerOnStage
import korlibs.korge.view.text
import korlibs.math.geom.*

suspend fun main() = Korge(
    windowSize = Size(512, 512),
    backgroundColor = Colors["#2b2b2b"]
) {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { MenuScene() }
}
