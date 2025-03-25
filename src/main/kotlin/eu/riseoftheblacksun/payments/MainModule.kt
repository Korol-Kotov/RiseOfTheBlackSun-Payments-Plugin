package eu.riseoftheblacksun.payments

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import eu.riseoftheblacksun.payments.commands.PurchasesCommand
import eu.riseoftheblacksun.payments.config.ConfigManager
import eu.riseoftheblacksun.payments.data.DatabaseManager
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.domain.MessageService
import eu.riseoftheblacksun.payments.domain.RewardsManager
import eu.riseoftheblacksun.payments.listeners.PlayerListener
import eu.riseoftheblacksun.payments.logs.Logger
import eu.riseoftheblacksun.payments.messaging.RabbitMQService
import eu.riseoftheblacksun.payments.providers.*
import org.bukkit.plugin.Plugin

class MainModule(
    private val plugin: Plugin,
    private val configManager: ConfigManager
) : AbstractModule() {
    override fun configure() {
        bind(Plugin::class.java).toInstance(plugin)
        bind(ConfigManager::class.java).toInstance(configManager)

        bind(Logger::class.java).toInstance(
            Logger(configManager, plugin, plugin.dataFolder)
        )
        bind(MessageService::class.java).toInstance(
            MessageService(configManager, plugin.dataFolder)
        )
        bind(DatabaseManager::class.java).toInstance(
            DatabaseManager(configManager, plugin.dataFolder)
        )
        bind(CacheManager::class.java).toInstance(
            CacheManager(plugin)
        )

        bind(PlayerListener::class.java).toProvider(PlayerListenerProvider::class.java)
        bind(PurchasesCommand::class.java).toProvider(PurchaseCommandProvider::class.java)

        bind(RewardsManager::class.java).toProvider(RewardsManagerProvider::class.java).`in`(Singleton::class.java)
        bind(RabbitMQService::class.java).toProvider(RabbitMQServiceProvider::class.java).`in`(Singleton::class.java)
    }
}