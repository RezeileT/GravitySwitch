import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.image.color.Colors
import korlibs.io.async.launchImmediately
import korlibs.event.Key
import korlibs.korge.view.align.centerOnStage

class ScoresScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val screenW = views.virtualWidth.toDouble()
        val screenH = views.virtualHeight.toDouble()

        // ── Fondo ─────────────────────────────────────────────────────────────
        solidRect(screenW, screenH, Colors["#0d0d0d"])

        // ── Título ────────────────────────────────────────────────────────────
        text("🏆  RANKING", 30.0, Colors["#f0d060"]) {
            centerOnStage()
            y = 40.0
        }

        // ── Cabecera de columnas ──────────────────────────────────────────────
        val colName   = 80.0
        val colPts    = 320.0
        val colLvl    = 470.0
        val colTime   = 580.0
        val colChunks = 700.0
        val headerY   = 100.0

        text("#",       16.0, Colors["#888888"]).position(40.0,     headerY)
        text("JUGADOR", 16.0, Colors["#888888"]).position(colName,  headerY)
        text("PUNTOS",  16.0, Colors["#888888"]).position(colPts,   headerY)
        text("DIF",     16.0, Colors["#888888"]).position(colLvl,   headerY)
        text("TIEMPO",  16.0, Colors["#888888"]).position(colTime,  headerY)
        text("CHUNKS",  16.0, Colors["#888888"]).position(colChunks, headerY)

        // Línea separadora
        solidRect(screenW - 80.0, 1.0, Colors["#333333"]).position(40.0, headerY + 26.0)

        // ── Filas de puntuaciones ─────────────────────────────────────────────
        val scores = ScoreRepository.getTopScores(10)

        if (scores.isEmpty()) {
            text("Todavía no hay puntuaciones guardadas.", 20.0, Colors["#666666"]) {
                centerOnStage()
                y = screenH / 2.0 - 20.0
            }
        } else {
            scores.forEachIndexed { i, score ->
                val rowY     = headerY + 46.0 + i * 44.0
                val rowColor = when (i) {
                    0    -> Colors["#f0d060"]   // oro
                    1    -> Colors["#cccccc"]   // plata
                    2    -> Colors["#cd7f32"]   // bronce
                    else -> Colors["#aaaaaa"]
                }

                // Fondo alternado para legibilidad
                if (i % 2 == 0) {
                    solidRect(screenW - 80.0, 38.0, Colors["#1a1a1a"]).position(40.0, rowY - 6.0)
                }

                text("${i + 1}",                          18.0, rowColor).position(40.0,      rowY)
                text(score.playerName.take(18),           18.0, rowColor).position(colName,   rowY)
                text("${score.points}",                   18.0, rowColor).position(colPts,    rowY)
                text("${score.level}",                    18.0, rowColor).position(colLvl,    rowY)
                text("${"%.1f".format(score.survivalTime)}s", 18.0, rowColor).position(colTime,   rowY)
                text("${score.chunksPassed}",             18.0, rowColor).position(colChunks, rowY)
            }
        }

        // ── Instrucciones de navegación ───────────────────────────────────────
        text("ENTER / ESC  →  Volver al menú", 16.0, Colors["#555555"]) {
            centerOnStage()
            y = screenH - 40.0
        }

        // ── Input ─────────────────────────────────────────────────────────────
        addUpdater {
            val keys = views.input.keys
            if (keys.justPressed(Key.ENTER) || keys.justPressed(Key.ESCAPE)) {
                launchImmediately { sceneContainer.changeTo<MenuScene>() }
            }
        }
    }
}
