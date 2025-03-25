package eu.riseoftheblacksun.payments

import com.google.inject.Guice
import com.google.inject.Injector
import eu.riseoftheblacksun.payments.commands.PurchasesCommand
import eu.riseoftheblacksun.payments.config.ConfigManager
import eu.riseoftheblacksun.payments.data.DatabaseManager
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.listeners.MenuListener
import eu.riseoftheblacksun.payments.listeners.PlayerListener
import eu.riseoftheblacksun.payments.logs.Logger
import eu.riseoftheblacksun.payments.messaging.RabbitMQService
import org.bukkit.plugin.java.JavaPlugin

class Payments : JavaPlugin() {

    private lateinit var injector: Injector

    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdir()
        val configManager = ConfigManager(dataFolder)

        injector = Guice.createInjector(MainModule(this, configManager))

        injector.getInstance(RabbitMQService::class.java)

        server.pluginManager.registerEvents(injector.getInstance(PlayerListener::class.java), this)
        server.pluginManager.registerEvents(MenuListener(), this)

        getCommand("purchases")?.setExecutor(injector.getInstance(PurchasesCommand::class.java))

        logger.info("Plugin $name enabled!")
    }

    override fun onDisable() {
        injector.getInstance(RabbitMQService::class.java).close()
        val databaseManager = injector.getInstance(DatabaseManager::class.java)
        databaseManager.saveAllPlayersInfo(injector.getInstance(CacheManager::class.java).getOnlinePlayerItems())
        databaseManager.close()
        injector.getInstance(Logger::class.java).onDisable()

        logger.info("Plugin $name disabled!")
    }
}
