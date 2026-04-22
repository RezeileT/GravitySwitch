import korlibs.audio.sound.readSound
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.view.align.centerOnStage
import korlibs.event.Key
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.input.onClick
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiVerticalStack
import korlibs.audio.sound.PlaybackTimes
import korlibs.image.format.readBitmap

class MenuScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val input = views.input
        val screenW = views.virtualWidth.toDouble()
        val screenH = views.virtualHeight.toDouble()

        // Fondo base
        solidRect(Size(screenW, screenH), Colors["#101820"])

        val bg = resourcesVfs["Assets/backgrounds/fondo_menu.png"].readBitmap()
        image(bg) { position(0, 0) }

        // Cargar y reproducir la música en loop
        val music = resourcesVfs["Assets/audio/menu_theme.mp3"].readSound()
        val channel = music.play(PlaybackTimes.INFINITE)
        channel.volume = 0.6


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



        uiVerticalStack(width = 220.0, padding = 14.0) {
            centerOnStage()
            y = 240.0

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
