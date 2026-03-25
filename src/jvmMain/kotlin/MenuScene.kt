import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.view.align.centerOnStage
import korlibs.event.Key
import korlibs.io.async.launchImmediately
import korlibs.korge.input.onClick
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiVerticalStack

class MenuScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(Size(512, 512), Colors["#2b2b2b"])

        text("MENU SCENE", textSize = 32.0) {
            centerOnStage()
            y -= 40.0
        }

        text("Pulsa ENTER para jugar", textSize = 20.0) {
            centerOnStage()
            y += 20.0
        }

        val input = views.input

        uiVerticalStack(width = 300.0, padding = 20.0) {
            centerOnStage()

            uiButton("Jugar") {
                onClick {
                    launchImmediately {
                        sceneContainer.changeTo { GameScene() }
                    }
                }
            }

            uiButton("Top Scores") {
                onClick {
                    launchImmediately {
                        sceneContainer.changeTo{ ScoreScene() }
                    }
                }
            }

            uiButton("Salir") {
                onClick { gameWindow.close() }
            }
        }

        addUpdater {
            if (input.keys.justPressed(Key.ENTER)) {
                launchImmediately {
                    sceneContainer.changeTo<GameScene>()
                }
            }
        }
    }
}
