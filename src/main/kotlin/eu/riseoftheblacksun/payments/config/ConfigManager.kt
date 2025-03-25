package eu.riseoftheblacksun.payments.config

import eu.riseoftheblacksun.payments.utils.JsonUtil
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val dataFolder: File) {
    val config: YamlConfiguration

    val rabbitConfig: Map<String, String>
        get() = getJSONConfig("rabbit-config.json")

    init {
        config = loadOrCreate("config.yml")
    }

    private fun getJSONConfig(fileName: String): Map<String, String> {
        val file = ensureFileExists(fileName)
        val configMap: Map<String, String> = JsonUtil.fromJson(file.readText())
        return configMap
    }

    private fun loadOrCreate(fileName: String): YamlConfiguration {
        val file = ensureFileExists(fileName)
        return YamlConfiguration.loadConfiguration(file)
    }

    private fun ensureFileExists(fileName: String): File {
        val file = File(dataFolder, fileName)

        if (!file.exists()) {
            file.parentFile.mkdirs()

            val resource = this::class.java.classLoader.getResource(fileName)
                ?: throw IllegalArgumentException("Resource $fileName not found")

            resource.openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file
    }

    fun reload() {
        config.load(File(dataFolder, "config.yml"))
    }
}