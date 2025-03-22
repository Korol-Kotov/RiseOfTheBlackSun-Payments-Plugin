package eu.riseoftheblacksun.payments.domain

import com.google.inject.Inject
import eu.riseoftheblacksun.payments.data.DatabaseManager
import eu.riseoftheblacksun.payments.logs.Logger
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class RewardsManager @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val cacheManager: CacheManager,
    private val logger: Logger,
    private val plugin: Plugin
) {
    init {
        val data = databaseManager.getPlayerData(Bukkit.getOnlinePlayers().map { it.uniqueId })
        data.forEach { (uniqueId, pair) ->
            val commandsString = pair.first
            val items = pair.second

            val player = Bukkit.getPlayer(uniqueId)
            if (player != null && player.isOnline) {
                if (commandsString.isNotEmpty()) {
                    val commands = commandsString.split(';')
                    runCommands(player, commands)
                }
                if (items.isNotEmpty()) {
                    cacheManager.setItems(uniqueId, items.split(';'))
                }
            }
        }
    }

    fun giveRewards(playerName: String, commands: List<String>, items: List<String>) {
        val player = Bukkit.getPlayerExact(playerName)

        val playerId = player?.uniqueId ?: Bukkit.getOfflinePlayerIfCached(playerName)?.uniqueId ?: Bukkit.getPlayerUniqueId(playerName)
        if (playerId == null) {
            logger.warn("No unique identifier found for $playerName")
            return
        }

        databaseManager.registerPlayer(playerId, playerName)

        if (player != null) {
            runCommands(player, commands)
            cacheManager.addItems(playerId, items)
        } else {
            databaseManager.addPlayerInfo(playerId, commands.joinToString(";"), items.joinToString(";"))
        }
    }

    fun runCommands(player: Player, commands: List<String>) {
        for (command in commands) {
            val args = command.split(' ', limit = 2)
            val sender = args.getOrNull(0) ?: continue
            val cmd = args.getOrNull(1) ?: continue

            sync {
                when (sender) {
                    "[player]" -> Bukkit.dispatchCommand(player, cmd)
                    "[console]" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                    "[op]" -> {
                        val isOp = player.isOp
                        player.isOp = true
                        player.chat("/$cmd")
                        player.isOp = isOp
                    }
                }
            }
        }
    }

    private fun sync(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}