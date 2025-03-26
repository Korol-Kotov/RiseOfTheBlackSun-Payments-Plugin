package eu.riseoftheblacksun.payments.messaging

import com.google.inject.Inject
import com.rabbitmq.client.*
import eu.riseoftheblacksun.payments.config.ConfigManager
import eu.riseoftheblacksun.payments.domain.RewardsManager
import eu.riseoftheblacksun.payments.logs.Logger
import eu.riseoftheblacksun.payments.utils.JsonUtil
import java.util.*
import kotlin.NoSuchElementException

class RabbitMQService @Inject constructor(
    private val configManager: ConfigManager,
    private val rewardsManager: RewardsManager,
    private val logger: Logger
) {
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val thread: Thread

    init {
        thread = Thread {
            try {
                val config = configManager.rabbitConfig
                val queueName = config["queue_name"] ?: ""
                val factory = ConnectionFactory().apply {
                    host = config["host"] ?: throw IllegalArgumentException("Value 'host' not found in rabbit-config")
                    port = config["port"]?.toIntOrNull() ?: 5672
                    virtualHost = config["virtual_host"] ?: "/"
                    username = config["username"] ?: throw IllegalArgumentException("Value 'username' not found in rabbit-config")
                    password = config["password"] ?: throw IllegalArgumentException("Value 'password' not found in rabbit-config")
                }

                connection = factory.newConnection()
                channel = connection?.createChannel()

                channel?.queueDeclare(queueName, true, false, false, null)

                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String?,
                        envelope: Envelope?,
                        properties: AMQP.BasicProperties?,
                        body: ByteArray?
                    ) {
                        try {
                            val message = body?.toString(Charsets.UTF_8)
                            logger.debug("Received message: $message")
                            if (message != null) handleEvent(message)
                        } catch (e: IllegalArgumentException) {
                            logger.error("Error while handling event: ${e.message}")
                        } catch (e: NoSuchElementException) {
                            logger.error("Error while handling event: ${e.message}")
                        }
                    }
                }

                channel?.basicConsume(queueName, true, consumer)
            } catch (e: Exception) {
                logger.error("Error in rabbit thread", e)
                return@Thread
            }
        }

        thread.start()
        logger.debug("RabbitMQ thread started")
    }

    fun close() {
        channel?.close()
        connection?.close()
        thread.join()
        logger.debug("RabbitMQ connection closed")
    }

    private fun handleEvent(value: String) {
        val json: Map<String, String> = JsonUtil.fromJson(value)
        val serverId = json["server_id"] ?: return
        if (configManager.config.getString("server-id") != serverId) return

        val playerName = json["playerName"] ?: throw NoSuchElementException("Required parameter 'playerName' is missing in JSON")
        val body = json["body"] ?: return
        val decodedBody = String(Base64.getDecoder().decode(body), Charsets.UTF_8)
        val bodyJson: Map<String, List<String>> = JsonUtil.fromJson(decodedBody)

        val commands = bodyJson.getOrDefault("commands", emptyList())
        val items = bodyJson.getOrDefault("items", emptyList())

        rewardsManager.giveRewards(playerName, commands, items)
    }
}