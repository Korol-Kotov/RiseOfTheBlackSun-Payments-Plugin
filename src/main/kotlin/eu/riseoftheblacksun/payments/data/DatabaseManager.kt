package eu.riseoftheblacksun.payments.data

import com.google.inject.Inject
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.riseoftheblacksun.payments.config.ConfigManager
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class DatabaseManager @Inject constructor(
    configManager: ConfigManager,
    private val dataFolder: File
) {

    private val databaseType = configManager.config.getString("database.type", "sqlite")!!

    private var dataSource: HikariDataSource? = null
    private var sqliteConnection: Connection? = null

    init {
        when (databaseType.lowercase()) {
            "mysql" -> initializeMySQL(configManager.config)
            "sqlite" -> initializeSQLite(configManager.config)
            else -> throw IllegalArgumentException("Unsupported database type: $databaseType")
        }

        initializeSchema()
    }

    private fun initializeMySQL(config: YamlConfiguration) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${config.getString("database.mysql.host")}:${config.getString("database.mysql.port")}/${config.getString("database.mysql.database")}?useSSL=false"
            username = config.getString("database.mysql.user")
            password = config.getString("database.mysql.password")
            maximumPoolSize = config.getInt("database.mysql.pool-size", 10)
            minimumIdle = config.getInt("database.mysql.minimum-idle", 5)
            idleTimeout = config.getLong("database.mysql.idle-timeout", 600000)
            connectionTimeout = config.getLong("database.mysql.connection-timeout", 30000)
            maxLifetime = config.getLong("database.mysql.max-lifetime", 1800000)
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        dataSource = HikariDataSource(hikariConfig)
    }

    private fun initializeSQLite(config: YamlConfiguration) {
        val databaseFile = File(dataFolder, config.getString("database.sqlite.file", "database.sqlite")!!)
        if (!databaseFile.exists()) {
            databaseFile.parentFile.mkdirs()
            databaseFile.createNewFile()
        }
        sqliteConnection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
    }

    fun getConnection(): Connection {
        return when (databaseType.lowercase()) {
            "mysql" -> dataSource!!.connection
            "sqlite" -> sqliteConnection!!
            else -> throw IllegalStateException("Unsupported database type: $databaseType")
        }
    }

    fun registerPlayer(uniqueId: UUID, playerName: String) {
        getConnection().use { conn ->
            val query = "INSERT INTO players (uuid, name) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM players WHERE uuid = ? OR name = ?)"
            conn.prepareStatement(query).use { stmt ->
                stmt.setString(1, uniqueId.toString())
                stmt.setString(2, playerName)
                stmt.setString(3, uniqueId.toString())
                stmt.setString(4, playerName)
                stmt.executeUpdate()
            }
        }
    }

    fun getPlayerData(uniqueIds: List<UUID>): Map<UUID, Pair<String, String>> {
        val result = mutableMapOf<UUID, Pair<String, String>>()
        if (uniqueIds.isEmpty()) return emptyMap()
        getConnection().use { conn ->
            val query = "SELECT player_uuid, commands, items FROM player_purchases WHERE player_uuid IN (${uniqueIds.joinToString { "?" }})"
            conn.prepareStatement(query).use { stmt ->
                uniqueIds.forEachIndexed { index, uuid -> stmt.setString(index + 1, uuid.toString()) }
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("player_uuid"))
                    val commands = rs.getString("commands")
                    val items = rs.getString("items")
                    result[uuid] = commands to items
                }
            }
        }
        return result
    }

    fun getPlayerInfo(uniqueId: UUID): Pair<String, String>? {
        getConnection().use { conn ->
            val query = "SELECT commands, items FROM player_purchases WHERE player_uuid = ?"
            conn.prepareStatement(query).use { stmt ->
                stmt.setString(1, uniqueId.toString())
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return rs.getString("commands") to rs.getString("items")
                }
            }
        }
        return null
    }

    fun setPlayerInfo(uniqueId: UUID, commands: String, items: String) {
        getConnection().use { conn ->
            val query = """
                INSERT INTO player_purchases (player_uuid, commands, items) 
                VALUES (?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                    commands = VALUES(commands), 
                    items = VALUES(items)
            """.trimIndent()

            conn.prepareStatement(query).use { stmt ->
                stmt.setString(1, uniqueId.toString())
                stmt.setString(2, commands)
                stmt.setString(3, items)
                stmt.executeUpdate()
            }
        }
    }

    fun addPlayerInfo(uniqueId: UUID, commands: String, items: String) {
        getConnection().use { conn ->
            val query = """
                INSERT INTO player_purchases (player_uuid, commands, items) 
                VALUES (?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                    commands = CONCAT(commands, ';', VALUES(commands)), 
                    items = CONCAT(items, ';', VALUES(items))
            """.trimIndent()

            conn.prepareStatement(query).use { stmt ->
                stmt.setString(1, uniqueId.toString())
                stmt.setString(2, commands)
                stmt.setString(3, items)
                stmt.executeUpdate()
            }
        }
    }

    fun saveAllPlayersInfo(data: Map<UUID, String>) {
        getConnection().use { conn ->
            val deleteQuery = """
                DELETE FROM player_purchases 
                WHERE player_uuid = ? 
                AND (commands = '')
            """.trimIndent()

            val insertOrUpdateQuery = """
                INSERT INTO player_purchases (player_uuid, commands, items) 
                VALUES (?, '', ?) 
                ON DUPLICATE KEY UPDATE 
                    items = VALUES(items)
            """.trimIndent()

            conn.prepareStatement(deleteQuery).use { deleteStmt ->
                conn.prepareStatement(insertOrUpdateQuery).use { insertStmt ->
                    for ((uniqueId, items) in data) {
                        if (items.isEmpty()) {
                            deleteStmt.setString(1, uniqueId.toString())
                            deleteStmt.addBatch()
                        } else {
                            insertStmt.setString(1, uniqueId.toString())
                            insertStmt.setString(2, items)
                            insertStmt.addBatch()
                        }
                    }
                    deleteStmt.executeBatch()
                    insertStmt.executeBatch()
                }
            }
        }
    }


    fun close() {
        dataSource?.close()
        sqliteConnection?.close()
    }

    private fun initializeSchema() {
        getConnection().use { conn ->
            val schemaStatements = listOf(
            """
            CREATE TABLE IF NOT EXISTS players (
                uuid CHAR(36) PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_name ON players(name);
            """,
            """
            CREATE TABLE IF NOT EXISTS player_purchases (
                player_uuid CHAR(36) NOT NULL,
                commands LONGTEXT NOT NULL,
                items LONGTEXT NOT NULL,
                PRIMARY KEY (player_uuid),
                FOREIGN KEY (player_uuid) REFERENCES players (uuid) ON DELETE CASCADE
            );
            """
            )
            conn.createStatement().use { stmt ->
                schemaStatements.forEach { stmt.addBatch(it) }
                stmt.executeBatch()
            }
        }
    }
}