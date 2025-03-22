package eu.riseoftheblacksun.payments.providers

import com.google.inject.Inject
import com.google.inject.Provider
import eu.riseoftheblacksun.payments.commands.PurchaseCommand
import eu.riseoftheblacksun.payments.config.ConfigManager
import eu.riseoftheblacksun.payments.data.DatabaseManager
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.domain.MessageService
import eu.riseoftheblacksun.payments.domain.RewardsManager
import eu.riseoftheblacksun.payments.listeners.PlayerListener
import eu.riseoftheblacksun.payments.logs.Logger
import eu.riseoftheblacksun.payments.messaging.KafkaService
import org.bukkit.plugin.Plugin

class KafkaServiceProvider @Inject constructor(
    private val configManager: ConfigManager,
    private val rewardsManager: RewardsManager,
    private val logger: Logger
) : Provider<KafkaService> {
    override fun get(): KafkaService {
        return KafkaService(configManager, rewardsManager, logger)
    }
}

class RewardsManagerProvider @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val cacheManager: CacheManager,
    private val logger: Logger,
    private val plugin: Plugin
) : Provider<RewardsManager> {
    override fun get(): RewardsManager {
        return RewardsManager(databaseManager, cacheManager, logger, plugin)
    }
}

class PlayerListenerProvider @Inject constructor(
    private val rewardsManager: RewardsManager,
    private val databaseManager: DatabaseManager,
    private val cacheManager: CacheManager,
) : Provider<PlayerListener> {
    override fun get(): PlayerListener {
        return PlayerListener(rewardsManager, databaseManager, cacheManager)
    }
}

class PurchaseCommandProvider @Inject constructor(
    private val cacheManager: CacheManager,
    private val messageService: MessageService
) : Provider<PurchaseCommand> {
    override fun get(): PurchaseCommand {
        return PurchaseCommand(cacheManager, messageService)
    }
}