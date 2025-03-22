package eu.riseoftheblacksun.payments.listeners

import eu.riseoftheblacksun.payments.menu.Menu
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MenuListener : Listener {
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val view = event.view
        if (view.topInventory.holder is Menu) {
            event.isCancelled = true

            val holder = event.clickedInventory?.holder
            if (holder != null && holder is Menu) {
                holder.onClick(event.whoClicked as Player, event.slot, event)
            }
        }
    }
}