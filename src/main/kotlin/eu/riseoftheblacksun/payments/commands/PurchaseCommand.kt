package eu.riseoftheblacksun.payments.commands

import com.google.inject.Inject
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.domain.MessageService
import eu.riseoftheblacksun.payments.menu.Menu
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class PurchaseCommand @Inject constructor(
    private val cacheManager: CacheManager,
    private val messageService: MessageService
) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            messageService.sendMessage(sender, "only-for-player")
            return true
        }

        if (!sender.hasPermission("payments.command")) {
            messageService.sendMessage(sender, "not-enough-perms")
            return true
        }

        val menu = Menu(cacheManager, 1, sender.uniqueId)
        menu.open(sender)

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String> {
        return mutableListOf()
    }
}