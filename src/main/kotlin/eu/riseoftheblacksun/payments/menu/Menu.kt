package eu.riseoftheblacksun.payments.menu

import eu.riseoftheblacksun.payments.domain.CacheManager
import eu.riseoftheblacksun.payments.utils.Utils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.max

class Menu(
    private val cacheManager: CacheManager,
    private val page: Int,
    private val uniqueId: UUID
) : InventoryHolder {
    companion object {
        private fun getNextPage(): ItemStack {
            val item = ItemStack(Material.ARROW)
            val meta = item.itemMeta ?: Bukkit.getItemFactory().getItemMeta(Material.ARROW)
            meta.displayName(Component.text("Next page"))
            meta.addItemFlags(*ItemFlag.entries.toTypedArray())
            item.setItemMeta(meta)
            return item
        }

        private fun getBackPage(): ItemStack {
            val item = ItemStack(Material.ARROW)
            val meta = item.itemMeta ?: Bukkit.getItemFactory().getItemMeta(Material.ARROW)
            meta.displayName(Component.text("Back page"))
            meta.addItemFlags(*ItemFlag.entries.toTypedArray())
            item.setItemMeta(meta)
            return item
        }
    }

    private val inventory = Bukkit.createInventory(this, 6 * 9, "Purchased items (page $page)")

    init {
        val items = cacheManager.getItems(uniqueId).map { Utils.deserializeItemStack(it) }
        if (items.isNotEmpty()) {
            if (items.size > (inventory.size - 9) * page)
                inventory.setItem(inventory.size - 1, getNextPage())
            if (page > 1)
                inventory.setItem(inventory.size - 9, getBackPage())

            for (i in 0..<45) {
                val item = items.getOrNull((page - 1) * (inventory.size - 9) + i) ?: break
                inventory.setItem(i, item)
            }
        }
    }

    fun open(player: Player) {
        player.openInventory(this.inventory)
    }

    fun onClick(player: Player, slot: Int, event: InventoryClickEvent) {
        when (slot) {
            45 -> {
                if (page > 1 && this.inventory.getItem(45) != null) {
                    val menu = Menu(cacheManager, page - 1, player.uniqueId)
                    menu.open(player)
                }
            }
            53 -> {
                if (this.inventory.getItem(53) != null) {
                    val menu = Menu(cacheManager, page + 1, player.uniqueId)
                    menu.open(player)
                }
            }
            in 0..<45 -> {
                if (player.inventory.firstEmpty() == -1) {
                    player.sendMessage("Release the inventory")
                    return
                }

                val item = this.inventory.getItem(slot)
                if (item == null || item.type.isAir) return

                cacheManager.removeItem(player.uniqueId, (inventory.size - 9) * (this.page - 1) + slot)
                player.inventory.addItem(item)

                val page = if (cacheManager.getItems(this.uniqueId).size <= (inventory.size - 9) * (this.page - 1)) max(1, this.page - 1) else this.page
                val menu = Menu(cacheManager, page, player.uniqueId)
                menu.open(player)
            }
        }
    }

    override fun getInventory(): Inventory = this.inventory
}