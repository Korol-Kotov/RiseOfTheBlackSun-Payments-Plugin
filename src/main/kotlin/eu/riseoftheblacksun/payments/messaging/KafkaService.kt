package eu.riseoftheblacksun.payments.messaging

import com.google.inject.Inject
import eu.riseoftheblacksun.payments.config.ConfigManager
import eu.riseoftheblacksun.payments.domain.RewardsManager
import eu.riseoftheblacksun.payments.logs.Logger
import eu.riseoftheblacksun.payments.utils.JsonUtil
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaService @Inject constructor(
    configManager: ConfigManager,
    private val rewardsManager: RewardsManager,
    private val logger: Logger
) {
    companion object {
        const val TOPIC = "purchase-event"
    }

    private var consumer: KafkaConsumer<String, String>?
    private val thread: Thread?
    private val running = AtomicBoolean(true)

    private val isEnabled = configManager.config.getBoolean("kafka.enabled", true)

    init {
        if (isEnabled) {
            val props = configManager.kafkaConfig.apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "${this["ip"]}:${this["port"]}")
            }

            val classLoader = Thread.currentThread().contextClassLoader
            try {
                Thread.currentThread().contextClassLoader = null
                consumer = KafkaConsumer(props)
            } finally {
                Thread.currentThread().contextClassLoader = classLoader
            }

            consumer?.subscribe(listOf(TOPIC))

            thread = Thread {
                try {
                    while (running.get()) {
                        val records = consumer?.poll(Duration.ofMillis(1000)) ?: ConsumerRecords.empty()
                        for (record in records) {
                            logger.debug("Received message: topic=${record.topic()}, key=${record.key()}, value=${record.value()}")
                            if (record.topic() != TOPIC || record.value() == null) continue
                            runCatching { handleEvent(record.value()) }
                                .onFailure { e ->
                                    logger.error("Error with kafka message processing:", e)
                                }
                        }
                        Thread.sleep(100)
                    }
                } catch (e: InterruptedException) {
                    logger.debug("Kafka consumer thread interrupted")
                } catch (e: WakeupException) {
                } catch (e: Exception) {
                    logger.error("Error in Kafka polling loop.", e)
                } finally {
                    consumer?.close()
                }
            }

            thread.start()
            logger.debug("Kafka thread started")
        } else {
            consumer = null
            thread = null
            logger.warn("Kafka is disabled!")
        }
    }

    private fun handleEvent(value: String) {
        val json: Map<String, String> = JsonUtil.fromJson(value)
        val playerName = json["playerName"] ?: throw NoSuchElementException("Required parameter 'playerName' is missing in JSON")
        val body = json["body"] ?: return
        val decodedBody = String(Base64.getDecoder().decode(body), Charsets.UTF_8)
        val bodyJson: Map<String, List<String>> = JsonUtil.fromJson(decodedBody)

        val commands = bodyJson.getOrDefault("commands", emptyList())
        val items = bodyJson.getOrDefault("items", emptyList())

        rewardsManager.giveRewards(playerName, commands, items)
    }

    fun close() {
        running.set(false)
        consumer?.wakeup()
        thread?.join()
        logger.debug("Kafka connection closed")
    }
}