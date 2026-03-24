import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.view.align.centerOnStage
import korlibs.event.Key
import korlibs.io.async.launchImmediately

class MenuScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        // Fondo oscuro
        solidRect(Size(512, 512), Colors["#2b2b2b"])

        // Título principal
        text("MENU SCENE", textSize = 32.0) {
            centerOnStage()
            y -= 40.0
        }

        // Instrucción de control
        text("Pulsa ENTER para jugar", textSize = 20.0) {
            centerOnStage()
            y += 20.0
        }

        // Acceso al input de teclado
        val input = views.input

        // Bucle de actualización (60fps aprox)
        addUpdater {
            // Detectar pulsación única de ENTER
            if (input.keys.justPressed(Key.ENTER)) {
                // Cambio de escena en corrutina
                launchImmediately {
                    sceneContainer.changeTo { GameScene() }
                }
            }
        }
    }
}
