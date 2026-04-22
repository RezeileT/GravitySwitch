import java.sql.Connection
import java.sql.DriverManager

object ScoreRepository {

    private const val DB_URL = "jdbc:sqlite:gravityswitch.db"

    private fun connect(): Connection {
        Class.forName("org.sqlite.JDBC")
        return DriverManager.getConnection(DB_URL)
    }

    // ── Crea la tabla si no existe ────────────────────────────────────────────
    fun init() {
        connect().use { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS SCORE (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    playerName    TEXT    NOT NULL,
                    points        INTEGER NOT NULL,
                    level         INTEGER NOT NULL,
                    survivalTime  REAL    NOT NULL,
                    chunksPassed  INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // ── Inserta una nueva puntuación ──────────────────────────────────────────
    fun insert(score: Score) {
        connect().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO SCORE (playerName, points, level, survivalTime, chunksPassed) VALUES (?, ?, ?, ?, ?)"
            )
            stmt.setString(1, score.playerName)
            stmt.setInt(2, score.points)
            stmt.setInt(3, score.level)
            stmt.setDouble(4, score.survivalTime)
            stmt.setInt(5, score.chunksPassed)
            stmt.executeUpdate()
        }
    }

    // ── Top 10 puntuaciones globales ordenadas por puntos DESC ────────────────
    fun getTopScores(limit: Int = 10): List<Score> {
        val results = mutableListOf<Score>()
        connect().use { conn ->
            val rs = conn.createStatement().executeQuery(
                "SELECT * FROM SCORE ORDER BY points DESC LIMIT $limit"
            )
            while (rs.next()) {
                results += Score(
                    id           = rs.getInt("id"),
                    playerName   = rs.getString("playerName"),
                    points       = rs.getInt("points"),
                    level        = rs.getInt("level"),
                    survivalTime = rs.getDouble("survivalTime"),
                    chunksPassed = rs.getInt("chunksPassed")
                )
            }
        }
        return results
    }

    // ── Top por dificultad ────────────────────────────────────────────────────
    fun getTopByLevel(level: Int, limit: Int = 5): List<Score> {
        val results = mutableListOf<Score>()
        connect().use { conn ->
            val stmt = conn.prepareStatement(
                "SELECT * FROM SCORE WHERE level = ? ORDER BY points DESC LIMIT ?"
            )
            stmt.setInt(1, level)
            stmt.setInt(2, limit)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                results += Score(
                    id           = rs.getInt("id"),
                    playerName   = rs.getString("playerName"),
                    points       = rs.getInt("points"),
                    level        = rs.getInt("level"),
                    survivalTime = rs.getDouble("survivalTime"),
                    chunksPassed = rs.getInt("chunksPassed")
                )
            }
        }
        return results
    }
}
