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
        val input = views.input

        // Fondo base
        solidRect(Size(512, 512), Colors["#101820"])

        // Panel decorativo grande
        roundRect(
            Size(380.0, 300.0),          // size: Size2D
            RectCorners(16.0, 16.0),     // radius: RectCorners
            fill = Colors["#1b263b"]     // fill: Paint
        ) {
            centerOnStage()
            y += 10.0
            alpha = 0.95
        }


        // Título
        text("Gravity Switch", textSize = 34.0, color = Colors.WHITE) {
            centerOnStage()
            y = 95.0
        }



        uiVerticalStack(width = 220.0, padding = 14.0) {
            centerOnStage()
            y = 220.0

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
                        sceneContainer.changeTo { ScoreScene() }
                    }
                }
            }

            uiButton("Pantalla completa") {
                onClick {
                    gameWindow.fullscreen = !gameWindow.fullscreen
                }
            }

            uiButton("Nivel 1") {
                onClick {
                    launchImmediately {
                        sceneContainer.changeTo { GameScene() }
                    }
                }
            }

            uiButton("Nivel 2") {
                onClick {
                    launchImmediately {
                        sceneContainer.changeTo { GameScene() }
                    }
                }
            }

            uiButton("Salir") {
                onClick { gameWindow.close() }
            }


        }

        text("flechas para moverse  |   SPACE cambiar de gravedad", textSize = 14.0, color = Colors["#94a3b8"]) {
            centerOnStage()
            y = 490.0
        }



        addUpdater {
            if (input.keys.justPressed(Key.ENTER)) {
                launchImmediately {
                    sceneContainer.changeTo { GameScene() }
                }
            }
        }
    }
}
