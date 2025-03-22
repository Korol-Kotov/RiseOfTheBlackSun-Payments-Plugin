package eu.riseoftheblacksun.payments

import com.google.inject.Guice
import com.google.inject.Injector
import eu.riseoftheblacksun.payments.commands.PurchaseCommand
import eu.riseoftheblacksun.payments.config.ConfigManager
import eu.riseoftheblacksun.payments.data.DatabaseManager
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.listeners.MenuListener
import eu.riseoftheblacksun.payments.listeners.PlayerListener
import eu.riseoftheblacksun.payments.messaging.KafkaService
import org.bukkit.plugin.java.JavaPlugin

class Payments : JavaPlugin() {

    private lateinit var injector: Injector

    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdir()
        val configManager = ConfigManager(dataFolder)

        injector = Guice.createInjector(MainModule(this, configManager))

        injector.getInstance(KafkaService::class.java)

        server.pluginManager.registerEvents(injector.getInstance(PlayerListener::class.java), this)
        server.pluginManager.registerEvents(MenuListener(), this)

        getCommand("purchase")?.setExecutor(injector.getInstance(PurchaseCommand::class.java))

        logger.info("Plugin $name enabled!")
    }

    override fun onDisable() {
        injector.getInstance(KafkaService::class.java).close()
        val databaseManager = injector.getInstance(DatabaseManager::class.java)
        databaseManager.saveAllPlayersInfo(injector.getInstance(CacheManager::class.java).getOnlinePlayerItems())
        databaseManager.close()

        logger.info("Plugin $name disabled!")
    }
}
