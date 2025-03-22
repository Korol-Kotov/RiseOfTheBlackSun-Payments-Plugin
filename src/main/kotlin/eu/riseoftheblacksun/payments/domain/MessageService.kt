package eu.riseoftheblacksun.payments.domain

import eu.riseoftheblacksun.payments.config.ConfigManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MessageService(
    configManager: ConfigManager,
    private val pluginFolder: File
) {
    private val miniMessage = MiniMessage.miniMessage()

    private val language: String = configManager.config.getString("plugin.language", "en")!!

    private var messages: YamlConfiguration

    init {
        messages = loadLanguageFile(language)
    }

    private fun loadLanguageFile(language: String): YamlConfiguration {
        val langFile = File(pluginFolder, "messages/$language.yml")
        if (!langFile.exists()) {
            pluginFolder.resolve("messages").mkdirs()
            this::class.java.getResourceAsStream("/messages/$language.yml")?.use {
                langFile.outputStream().use { out -> it.copyTo(out) }
            }
        }
        return YamlConfiguration.loadConfiguration(langFile)
    }

    fun getMessage(key: String, replacements: Map<String, String> = emptyMap()): Component {
        val rawMessage = messages.getString("messages.$key")
            ?: return Component.text("Message not found: $key")

        return format(rawMessage, replacements)
    }

    fun getList(key: String, replacements: Map<String, String> = emptyMap()): List<Component> {
        return format(messages.getStringList(key), replacements)
    }

    fun format(text: String, replacements: Map<String, String> = emptyMap()): Component {
        val formattedMessage = replacements.entries.fold(text) { message, (placeholder, value) ->
            message.replace(placeholder, value)
        }

        return miniMessage.deserialize(formattedMessage)
    }

    fun format(list: List<String>, replacements: Map<String, String> = emptyMap()): List<Component> {
        return list.map { format(it, replacements) }
    }

    fun sendMessage(sender: CommandSender, key: String, replacements: Map<String, String> = emptyMap(), isList: Boolean = false) {
        if (!isList) {
            val message = getMessage(key, replacements)
            sender.sendMessage(message)
        } else {
            val messages = getList(key, replacements)
            messages.forEach(sender::sendMessage)
        }
    }

    fun reloadMessages() {
        messages = loadLanguageFile(language)
    }
}