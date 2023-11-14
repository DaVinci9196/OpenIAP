package org.mg.iap.core

import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpResponse(
    var data: ByteArray = byteArrayOf(),
    var errorString: String = "No Error",
    var isSuccessful: Boolean = false,
    var code: Int = 0
)

object HttpClient {
    private val httpClient = OkHttpClient().newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun processRequest(request: Request): HttpResponse {
        httpClient.newCall(request).execute().use { response ->
            return HttpResponse().apply {
                isSuccessful = response.isSuccessful
                code = response.code
                response.body?.let {
                    data = it.bytes()
                }
                if (!isSuccessful) {
                    errorString = response.message
                }
            }
        }
    }

    @Throws(IOException::class)
    fun get(
        baseUrl: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): HttpResponse {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()
        params.forEach {
            urlBuilder.addQueryParameter(it.key, it.value)
        }
        val request = Request.Builder()
            .url(urlBuilder.build())
            .headers(headers.toHeaders())
            .get()
            .build()
        return processRequest(request)
    }

    @Throws(IOException::class)
    fun post(
        url: String,
        headers: Map<String, String>,
        body: ByteArray,
        contentType: String = "application/x-protobuf"
    ): HttpResponse {
        val request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .post(body.toRequestBody(contentType.toMediaType()))
            .build()
        return processRequest(request)
    }
}