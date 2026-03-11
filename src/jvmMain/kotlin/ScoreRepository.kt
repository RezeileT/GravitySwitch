import java.sql.DriverManager

object ScoreRepository {
    private const val DB_URL = "jdbc:sqlite:scores.db"

    init {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS scores(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player TEXT,
                        level INTEGER,
                        points INTEGER
                    )
                    """.trimIndent()
                )
            }
        }
    }

    fun insert(score: Score) {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.prepareStatement(
                "INSERT INTO scores(player, level, points) VALUES (?, ?, ?)"
            ).use { ps ->
                ps.setString(1, score.playerName)
                ps.setInt(2, score.level)
                ps.setInt(3, score.points)
                ps.executeUpdate()
            }
        }
    }

    fun getTop(limit: Int = 10): List<Score> {
        val result = mutableListOf<Score>()

        DriverManager.getConnection(DB_URL).use { conn ->
            conn.prepareStatement(
                "SELECT player, level, points FROM scores ORDER BY points DESC LIMIT ?"
            ).use { ps ->
                ps.setInt(1, limit)
                val rs = ps.executeQuery()

                while (rs.next()) {
                    result += Score(
                        playerName = rs.getString("player"),
                        level = rs.getInt("level"),
                        points = rs.getInt("points")
                    )
                }
            }
        }

        return result
    }
}
