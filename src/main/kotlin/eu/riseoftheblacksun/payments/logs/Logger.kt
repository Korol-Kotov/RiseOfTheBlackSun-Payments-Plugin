package eu.riseoftheblacksun.payments.logs

import com.google.inject.Inject
import eu.riseoftheblacksun.payments.config.ConfigManager
import org.bukkit.plugin.Plugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Logger @Inject constructor(
    configManager: ConfigManager,
    plugin: Plugin,
    private val dataFolder: File
) {
    private val pluginLogger = plugin.logger

    private val logs = mutableListOf<Log>()

    private val isDebug = configManager.config.getBoolean("plugin.debug", false)

    private var logSaverThread: Thread

    init {
        logSaverThread = createThread()
        logSaverThread.start()
    }

    private fun createThread(): Thread =
        Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    saveLogs()

                    Thread.sleep(10 * 1000)
                }
            } catch (e: InterruptedException) {
                return@Thread
            }
        }

    fun log(level: Level, text: String, throwable: Throwable? = null) {
        when (level) {
            Level.DEBUG -> debug(text, throwable)
            Level.INFO -> info(text, throwable)
            Level.WARN -> warn(text, throwable)
            Level.ERROR -> error(text, throwable)
        }
    }

    fun debug(text: String, throwable: Throwable? = null) {
        if (isDebug) {
            pluginLogger.info(text)
            throwable?.printStackTrace()
        }

        logs.add(Log.create(text, Level.DEBUG))
    }

    fun debug(list: List<String>, throwable: Throwable? = null) {
        list.forEach { text -> debug(text, throwable) }
    }

    fun info(text: String, throwable: Throwable? = null) {
        pluginLogger.info(text)
        throwable?.printStackTrace()

        logs.add(Log.create(text, Level.INFO))
    }

    fun info(list: List<String>, throwable: Throwable? = null) {
        list.forEach { text -> info(text, throwable) }
    }

    fun warn(text: String, throwable: Throwable? = null) {
        pluginLogger.warning(text)
        throwable?.printStackTrace()

        logs.add(Log.create(text, Level.WARN))
    }

    fun warn(list: List<String>, throwable: Throwable? = null) {
        list.forEach { text -> warn(text, throwable) }
    }

    fun error(text: String, throwable: Throwable? = null) {
        pluginLogger.severe(text)
        throwable?.printStackTrace()

        logs.add(Log.create(text, Level.ERROR))
    }

    fun error(list: List<String>, throwable: Throwable? = null) {
        list.forEach { text -> error(text, throwable) }
    }

    fun reload() {
        onDisable()

        logSaverThread = createThread()
        logSaverThread.start()
    }

    fun onDisable() {
        logSaverThread.interrupt()
        saveLogs()
    }

    private fun saveLogs() {
        logs.forEach { log ->
            val date = Date(log.timestamp * 1000)
            val stringDate = dateToString("dd-MM-yyyy", date = date)
            val stringTime = dateToString("HH:mm:ss", date = date)
            val file = File(dataFolder, "logs" + File.separator + "log-$stringDate.log")

            val text = "[$stringTime] ${log.level.prefix}: ${log.text}"

            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.writeText(text)
            } else {
                file.appendText("\n$text")
            }
        }

        logs.clear()
    }

    private fun dateToString(format: String, date: Date = Calendar.getInstance().time, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(date)
    }

    private data class Log(
        val timestamp: Long,
        val text: String,
        val level: Level
    ) {
        companion object {
            fun create(text: String, level: Level): Log {
                val timestamp = Calendar.getInstance().timeInMillis / 1000
                return Log(timestamp, text, level)
            }
        }
    }
}