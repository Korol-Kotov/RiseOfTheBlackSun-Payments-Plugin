package eu.riseoftheblacksun.payments.commands

import com.google.inject.Inject
import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.domain.MessageService
import eu.riseoftheblacksun.payments.menu.Menu
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class PurchasesCommand @Inject constructor(
    private val cacheManager: CacheManager,
    private val messageService: MessageService
) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            if (sender !is Player) {
                messageService.sendMessage(sender, "only-for-player")
                return true
            }

            if (!sender.hasPermission("payments.purchases")) {
                messageService.sendMessage(sender, "not-enough-perms")
                return true
            }

            val menu = Menu(cacheManager, 1, sender.uniqueId)
            menu.open(sender)
        } else {
            if (!sender.hasPermission("payments.purchases.others")) {
                messageService.sendMessage(sender, "not-enough-perms")
                return true
            }

            val player = Bukkit.getPlayerExact(args[0])
            if (player == null) {
                messageService.sendMessage(sender, "player-not-found")
                return true
            }

            val menu = Menu(cacheManager, 1, player.uniqueId)
            menu.open(player)

            messageService.sendMessage(sender, "purchases-opened", mapOf("%player%" to player.name))
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String> {
        if (args.size == 1 && sender.hasPermission("payments.purchases.others")) {
            return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        }

        return mutableListOf()
    }
}