package org.mg.iap.core

import java.io.IOException
import java.security.MessageDigest

class LRUCache<K, V>(private val capacity: Int) : LinkedHashMap<K, V>(capacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }
}

class CacheEntry(
    val data: ByteArray,
    val expiredAt: Long
)

class IAPCacheManager(maxSize: Int = 1024, private val expireMs: Int = 7200000) {
    private val lruCache = LRUCache<String, CacheEntry>(maxSize)

    @Synchronized
    fun get(requestBody: ByteArray): ByteArray? {
        val entry = lruCache[calculateHash(requestBody)]
        if (entry == null || entry.expiredAt < System.currentTimeMillis())
            return null
        return entry.data
    }

    @Synchronized
    fun put(requestBody: ByteArray, responseData: ByteArray) {
        val key = calculateHash(requestBody)
        lruCache[key] = CacheEntry(responseData, System.currentTimeMillis() + expireMs)
    }

    @Throws(IOException::class)
    private fun calculateHash(body: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(body)
        return digest.digest().toHex()
    }
}