import java.sql.DriverManager

fun main() {
    val url = "jdbc:sqlite:scores.db"

    DriverManager.getConnection(url).use { conn ->
        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS scores(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player TEXT,
                    level INTEGER,
                    score INTEGER
                )
                """.trimIndent()
            )

            st.executeUpdate(
                "INSERT INTO scores(player, level, score) VALUES ('TestPlayer', 1, 1000)"
            )

            val rs = st.executeQuery("SELECT player, level, score FROM scores")
            println("== SCORES ==")
            while (rs.next()) {
                println("${rs.getString("player")} - L${rs.getInt("level")}: ${rs.getInt("score")}")
            }
        }
    }

    println("✅ SQLite OK (scores.db creado/actualizado)")
}
