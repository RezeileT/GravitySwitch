import java.sql.DriverManager

object ScoreRepository{
    private const val DB_URL = "jdbc:sqlite:scores.db"

    // Inicialización lazy: crea tabla en primer acceso
    init {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS scores(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player TEXT NOT NULL,
                        level INTEGER NOT NULL,
                        score INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }

    // INSERT preparado (protege contra SQL injection)
    fun insert(score: Score) {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.prepareStatement(
                "INSERT INTO scores(player, level, score) VALUES (?, ?, ?)"
            ).use { ps ->
                ps.setString(1, score.playerName)
                ps.setInt(2, score.level)
                ps.setInt(3, score.points)
                ps.executeUpdate()
            }
        }
    }

    // SELECT TOP N ordenados por puntos (DESC)
    fun getTop(limit: Int): List<Score> {
        val result = mutableListOf<Score>()

        DriverManager.getConnection(DB_URL).use { conn ->
            conn.prepareStatement(
                "SELECT player, level, score FROM scores ORDER BY score DESC LIMIT ?"
            ).use { ps ->
                ps.setInt(1, limit)
                val rs = ps.executeQuery()

                // Iterar resultados y mapear a Score
                while (rs.next()) {
                    result += Score(
                        playerName = rs.getString("player"),
                        level = rs.getInt("level"),
                        points = rs.getInt("score")
                    )
                }
            }
        }

        return result
    }
}
