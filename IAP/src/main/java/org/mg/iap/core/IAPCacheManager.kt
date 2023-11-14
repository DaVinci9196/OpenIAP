package org.mg.iap.core

import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.security.MessageDigest

class LRUCache<K, V>(private val capacity: Int) : LinkedHashMap<K, V>(capacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }
}

class IAPCacheManager(maxSize: Int = 1024) {
    private val lruCache = LRUCache<String, ByteArray>(maxSize)

    fun get(request: Request): ByteArray? {
        val key = "${request.url}_${calculateHash(request.body!!)}"
        return lruCache[key]
    }

    fun put(request: Request, responseData: ByteArray) {
        val key = "${request.url}_${calculateHash(request.body!!)}"
        lruCache[key] = responseData
        println("IAPCacheManager.put(key=$key, data=${responseData.toHex()})")
    }

    @Throws(IOException::class)
    private fun calculateHash(body: RequestBody): String {
        val buffer = Buffer()
        body.writeTo(buffer)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(buffer.readByteArray())
        return digest.digest().toHex()
    }
}