package eu.riseoftheblacksun.payments.listeners

import com.google.inject.Inject
import eu.riseoftheblacksun.payments.data.DatabaseManager
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.domain.RewardsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener @Inject constructor(
    private val rewardsManager: RewardsManager,
    private val databaseManager: DatabaseManager,
    private val cacheManager: CacheManager
) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("player joined: ${event.player.name}")
        val pair = databaseManager.getPlayerInfo(event.player.uniqueId) ?: return
        val commandsString = pair.first
        val items = pair.second

        if (commandsString.isNotEmpty()) {
            val commands = commandsString.split(';')
            rewardsManager.runCommands(event.player, commands)
        }
        if (items.isNotEmpty()) {
            val itemsToSet = items.split(';')
            cacheManager.setItems(event.player.uniqueId, itemsToSet)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val items = cacheManager.getItems(event.player.uniqueId)
        databaseManager.setPlayerInfo(event.player.uniqueId, "", items.joinToString(";"))
        cacheManager.clearItems(event.player.uniqueId)
    }
}