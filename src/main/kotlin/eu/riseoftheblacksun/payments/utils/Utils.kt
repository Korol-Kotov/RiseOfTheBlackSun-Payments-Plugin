package eu.riseoftheblacksun.payments.utils

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object Utils {
    fun serializeItemStack(item: ItemStack): String {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            BukkitObjectOutputStream(byteArrayOutputStream).use { outputStream ->
                outputStream.writeObject(item)
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
        }
    }

    fun deserializeItemStack(data: String): ItemStack {
        val bytes = Base64.getDecoder().decode(data)
        ByteArrayInputStream(bytes).use { byteArrayInputStream ->
            BukkitObjectInputStream(byteArrayInputStream).use { inputStream ->
                return inputStream.readObject() as ItemStack
            }
        }
    }
}