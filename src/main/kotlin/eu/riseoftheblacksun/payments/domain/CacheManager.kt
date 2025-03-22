package eu.riseoftheblacksun.payments.domain

import com.google.inject.Inject
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.MemoryUnit
import java.io.File
import java.util.*

class CacheManager @Inject constructor(
    plugin: Plugin
) {
    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerBuilder.persistence(File(plugin.dataFolder, "ehcache")))
        .build(true)

    private val playerItemsCache = createCache<UUID, String>(cacheManager, "playerItems", 50000)

    fun setItems(uniqueId: UUID, items: List<String>) {
        playerItemsCache.put(uniqueId, items.joinToString(";"))
    }

    fun addItems(uniqueId: UUID, items: List<String>) {
        val playerItems = getItems(uniqueId).toMutableList()
        playerItems.addAll(items)
        setItems(uniqueId, playerItems)
    }

    fun removeItem(uniqueId: UUID, index: Int) {
        val items = getItems(uniqueId).toMutableList()
        if (items.isEmpty()) return
        items.removeAt(index)
        if (items.isEmpty()) clearItems(uniqueId)
        else setItems(uniqueId, items)
    }

    fun getItems(uniqueId: UUID): List<String> {
        return playerItemsCache.get(uniqueId)?.split(';') ?: emptyList()
    }

    fun getOnlinePlayerItems(): Map<UUID, String> {
        val result = mutableMapOf<UUID, String>()
        Bukkit.getOnlinePlayers().forEach { player ->
            result[player.uniqueId] = playerItemsCache.get(player.uniqueId) ?: ""
        }
        return result
    }

    fun clearItems(uniqueId: UUID) {
        playerItemsCache.remove(uniqueId)
    }

    private inline fun <reified K, reified V> createCache(
        cacheManager: CacheManager,
        cacheName: String,
        heapSize: Long
    ): Cache<K, V> {
        return cacheManager.createCache(
            cacheName,
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                K::class.java,
                V::class.java,
                ResourcePoolsBuilder.heap(heapSize).disk(5, MemoryUnit.GB, true)
            )
        )
    }
}