import korlibs.event.Key
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.input.*
import korlibs.korge.text.*
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.ui.*
import Score
import korlibs.image.color.Colors
import korlibs.io.async.launchImmediately

class ScoreScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        uiVerticalStack(width = views.virtualWidth.toDouble(), padding = 20.0, false) {

            text("TOP SCORES", textSize = 32.0, color = Colors.BLACK) { }

            // ← CARGAR TOP 10
            val topScores = ScoreRepository.getTop(10)

            if (topScores.isEmpty()) {
                text("No hay scores aún. ¡Juega y sé el primero!", textSize = 18.0)
            } else {
                // ← LISTA TOP SCORES
                topScores.forEachIndexed { index, score ->
                    text(
                        "#${index+1} ${score.playerName.padEnd(10)} | Nivel ${score.level} | ${score.points} pts",
                        textSize = 20.0,
                        color = if (index < 3) Colors.GOLD else Colors.WHITE
                    )
                }
            }

            text("ENTER → Menú | SPACE → Jugar", textSize = 16.0, color = Colors.LIGHTGRAY) { }

            // ← NAVEGACIÓN
            addUpdater {
                if (views.input.keys.justPressed(Key.ENTER)) {
                    launchImmediately { sceneContainer.changeTo { MenuScene() } }
                }
                if (views.input.keys.justPressed(Key.SPACE)) {
                    launchImmediately { sceneContainer.changeTo { GameScene() } }
                }
            }
        }
    }
}
