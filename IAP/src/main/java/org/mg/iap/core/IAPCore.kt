package org.mg.iap.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mg.iap.proto.AcquireRequest
import org.mg.iap.proto.DocId
import org.mg.iap.proto.ResponseWrapper
import org.mg.iap.proto.acknowledgePurchaseRequest
import org.mg.iap.proto.acquireRequest
import org.mg.iap.proto.cKDocument
import org.mg.iap.proto.clientInfo
import org.mg.iap.proto.copy
import org.mg.iap.proto.deviceAuthInfo
import org.mg.iap.proto.docId
import org.mg.iap.proto.documentInfo
import org.mg.iap.proto.iABX
import org.mg.iap.proto.itemColor
import org.mg.iap.proto.multiOfferSkuDetail
import org.mg.iap.proto.skuDetailsExtra
import org.mg.iap.proto.skuDetailsRequest
import org.mg.iap.proto.timestamp
import org.mg.iap.proto.unkMessage1
import org.mg.iap.proto.unkMessage2
import org.mg.iap.proto.unkMessage5
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val skuDetailsCache = IAPCacheManager(2048)
class IAPCore(
    private val deviceInfo: DeviceEnvInfo,
    private val clientInfo: ClientInfo,
    private val authData: AuthData
) {
    fun requestAuthProofToken(password: String): Pair<Int?, String?> {
        return try {
            val response = HttpClient.post(
                GooglePlayApi.URL_AUTH_PROOF_TOKENS,
                HeaderProvider.getBaseHeaders(authData, deviceInfo),
                "{\"credentialType\": \"password\", \"credential\": \"$password\"}".toByteArray(),
                "application/json; charset=utf-8"
            )
            if (response.isSuccessful) {
                response.code to Json.parseToJsonElement(response.data.decodeToString()).jsonObject["encodedRapt"]?.jsonPrimitive?.content
            } else
                response.code to null
        } catch (e: IOException) {
            null to null
        }
    }

    fun getSkuDetails(params: GetSkuDetailsParams): GetSkuDetailsResult {
        val skuDetailsRequest = skuDetailsRequest {
            this.apiVersion = params.apiVersion
            this.type = params.skuType
            this.package_ = clientInfo.pkgName
            this.isWifi = true
            this.skuPackage = params.skuPkgName
            this.skuId.addAll(params.skuIdList)
            this.skuDetailsExtra = skuDetailsExtra {
                this.version = params.sdkVersion
            }
            params.multiOfferSkuDetail.forEach {
                this.multiOfferSkuDetail.add(
                    when (val value = it.value) {
                        is Boolean -> multiOfferSkuDetail {
                            this.key = it.key
                            this.bv = value
                        }

                        is Long -> multiOfferSkuDetail {
                            this.key = it.key
                            this.iv = value
                        }

                        is Int -> multiOfferSkuDetail {
                            this.key = it.key
                            this.iv = value.toLong()
                        }

                        else -> multiOfferSkuDetail {
                            this.key = it.key
                            this.sv = value.toString()
                        }
                    }
                )
            }
        }

        return try {
            val requestBody = skuDetailsRequest.toByteArray()
            val cacheEntry = skuDetailsCache.get(requestBody)
            if (cacheEntry != null) {
                return GetSkuDetailsResult.parseFrom(ResponseWrapper.parseFrom(cacheEntry).payload.skuDetailsResponse)
            }
            val response = HttpClient.post(
                GooglePlayApi.URL_SKU_DETAILS,
                HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                skuDetailsRequest.toByteArray()
            )
            if (response.isSuccessful) {
                skuDetailsCache.put(requestBody, response.data)
                GetSkuDetailsResult.parseFrom(ResponseWrapper.parseFrom(response.data).payload.skuDetailsResponse)
            } else {
                throw RuntimeException("Request failed. code=${response.code}")
            }
        } catch (e: IOException) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    private fun createAcquireRequest(params: AcquireParams): AcquireRequest {
        val theme = 2

        val skuPackageName = params.buyFlowParams.skuParams["skuPackageName"] ?: clientInfo.pkgName
        val docId = if (params.buyFlowParams.skuSerializedDockIdList?.isNotEmpty() == true) {
            val sDocIdBytes = Base64.decode(params.buyFlowParams.skuSerializedDockIdList[0], 10)
            DocId.parseFrom(sDocIdBytes)
        } else {
            docId {
                this.backendDocId =
                    "${params.buyFlowParams.skuType}:$skuPackageName:${params.buyFlowParams.sku}"
                this.type = getSkuType(params.buyFlowParams.skuType)
                this.backend = 3
            }
        }

        val documentInfo = documentInfo {
            this.docId = docId
            this.unknown2 = 1
            if (params.buyFlowParams.skuOfferIdTokenList?.isNotEmpty() == true) {
                if (params.buyFlowParams.skuOfferIdTokenList[0].isNotBlank())
                    this.token14 = params.buyFlowParams.skuOfferIdTokenList[0]
            }
        }

        val authFrequency = if (params.buyFlowParams.needAuth) 0 else 3
        return acquireRequest {
            this.documentInfo = documentInfo
            this.clientInfo = clientInfo {
                this.apiVersion = params.buyFlowParams.apiVersion
                this.package_ = this@IAPCore.clientInfo.pkgName
                this.versionCode = this@IAPCore.clientInfo.versionCode
                this.signatureMD5 = this@IAPCore.clientInfo.signatureMD5
                this.skuParamList.addAll(mapToSkuParamList(params.buyFlowParams.skuParams))
                this.unknown8 = 1
                this.installerPackage = deviceInfo.gpPkgName
                this.unknown10 = false
                this.unknown11 = false
                this.unknown15 = unkMessage1 {
                    this.unknown1 = unkMessage2 {
                        this.unknown1 = 1
                    }
                }
                this.versionCode1 = this@IAPCore.clientInfo.versionCode
                if (params.buyFlowParams.oldSkuPurchaseToken?.isNotBlank() == true)
                    this.oldSkuPurchaseToken = params.buyFlowParams.oldSkuPurchaseToken
                if (params.buyFlowParams.oldSkuPurchaseId?.isNotBlank() == true)
                    this.oldSkuPurchaseId = params.buyFlowParams.oldSkuPurchaseId
            }
            this.clientTokenB64 =
                createClientToken(this@IAPCore.deviceInfo, this@IAPCore.authData)
            this.deviceAuthInfo = deviceAuthInfo {
                this.canAuthenticate = true
                this.unknown5 = 1
                this.unknown9 = true
                this.authFrequency = authFrequency
                this.itemColor = itemColor {
                    this.androidAppsColor = -16735885
                    this.booksColor = -11488012
                    this.musicColor = -45771
                    this.moviesColor = -52375
                    this.newsStandColor = -7686920
                }
            }
            this.unknown12 = unkMessage5 {
                this.unknown1 = 9
            }
            this.deviceIDBase64 = deviceInfo.deviceId
            this.newAcquireCacheKey = getAcquireCacheKey(
                this@IAPCore.deviceInfo,
                this@IAPCore.authData.email,
                listOf(
                    cKDocument {
                        this.docId = docId
                        this.token3 = documentInfo.token3
                        this.token14 = documentInfo.token14
                        this.unknown3 = 1
                    }
                ),
                this@IAPCore.clientInfo.pkgName,
                mapOf(
                    "enablePendingPurchases" to (params.buyFlowParams.skuParams["enablePendingPurchases"]
                        ?: false).toString()
                ),
                authFrequency
            )
            this.nonce = createNonce()
            this.theme = theme
            this.ts = timestamp {
                val ts = System.currentTimeMillis()
                this.seconds = TimeUnit.MILLISECONDS.toSeconds(ts)
                this.nanos = ((ts + TimeUnit.HOURS.toMillis(1L)) % 1000L * 1000000L).toInt()
            }
        }
    }

    fun doAcquireRequest(params: AcquireParams): AcquireResult {
        val acquireRequest =
            if (params.lastAcquireResult == null) {
                createAcquireRequest(params)
            } else {
                params.lastAcquireResult!!.acquireRequest.copy {
                    this.serverContextToken =
                        params.lastAcquireResult!!.acquireResponse.serverContextToken
                    this.actionContext.addAll(params.actionContext.toByteStringList())
                    this.deviceAuthInfo =
                        params.lastAcquireResult!!.acquireRequest.deviceAuthInfo.copy {
                            if (params.droidGuardResult?.isNotBlank() == true) {
                                this.droidGuardPayload = params.droidGuardResult
                            }
                        }
                    params.authToken?.let {
                        this.authTokens.put("rpt", it)
                    }
                    this.ts = timestamp {
                        val ts = System.currentTimeMillis()
                        this.seconds = TimeUnit.MILLISECONDS.toSeconds(ts)
                        this.nanos = ((ts + TimeUnit.HOURS.toMillis(1L)) % 1000L * 1000000L).toInt()
                    }
                }
            }
        return try {
            val response = HttpClient.post(
                "${GooglePlayApi.URL_EES_ACQUIRE}?theme=${acquireRequest.theme}",
                HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                acquireRequest.toByteArray()
            )
            if (response.isSuccessful) {
                AcquireResult.parseFrom(
                    params,
                    acquireRequest,
                    ResponseWrapper.parseFrom(response.data).payload.acquireResponse
                )
            } else {
                throw RuntimeException("Request failed. code=${response.code}")
            }
        } catch (e: IOException) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    fun consumePurchase(params: ConsumePurchaseParams): ConsumePurchaseResult {
        val iabx = iABX {
            this.skuParam.addAll(mapToSkuParamList(params.extraParams))
        }
        val requestBody = "pt=${URLEncoder.encode(params.purchaseToken, "UTF-8")}&ot=1&shpn=${
            URLEncoder.encode(clientInfo.pkgName, "UTF-8")
        }&iabx=${URLEncoder.encode(org.mg.iap.core.Base64.encodeToString(iabx.toByteArray(), 10), "UTF-8")}"

        return try {
            val response = HttpClient.post(
                GooglePlayApi.URL_CONSUME_PURCHASE,
                HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                requestBody.toByteArray(),
                "application/x-www-form-urlencoded; charset=UTF-8"
            )
            if (response.isSuccessful) {
                ConsumePurchaseResult.parseFrom(ResponseWrapper.parseFrom(response.data).payload.consumePurchaseResponse)
            } else {
                throw RuntimeException("Request failed. code=${response.code}")
            }
        } catch (e: IOException) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    fun acknowledgePurchase(params: org.mg.iap.core.AcknowledgePurchaseParams): AcknowledgePurchaseResult {
        val acknowledgePurchaseRequest = acknowledgePurchaseRequest {
            this.purchaseToken = params.purchaseToken
            params.extraParams["developerPayload"]?.let {
                this.developerPayload = it as String
            }
        }

        return try {
            val response = HttpClient.post(
                GooglePlayApi.URL_ACKNOWLEDGE_PURCHASE,
                HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                acknowledgePurchaseRequest.toByteArray()
            )
            if (response.isSuccessful) {
                AcknowledgePurchaseResult.parseFrom(ResponseWrapper.parseFrom(response.data).payload.acknowledgePurchaseResponse)
            } else {
                throw RuntimeException("Request failed. code=${response.code}")
            }
        } catch (e: IOException) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }

    fun getPurchaseHistory(params: GetPurchaseHistoryParams): GetPurchaseHistoryResult {
        var reqParams = mutableMapOf(
            "bav" to params.apiVersion.toString(),
            "shpn" to clientInfo.pkgName,
            "iabt" to params.type
        )
        if (!params.continuationToken.isNullOrEmpty()) {
            reqParams["ctntkn"] = params.continuationToken
        }
        if (params.extraParams.isNotEmpty()) {
            val iabx = iABX {
                this.skuParam.addAll(mapToSkuParamList(params.extraParams))
            }
            reqParams["iabx"] = org.mg.iap.core.Base64.encodeToString(iabx.toByteArray(), 10)
        }

        return try {
            val response = HttpClient.get(
                GooglePlayApi.URL_GET_PURCHASE_HISTORY,
                HeaderProvider.getDefaultHeaders(authData, deviceInfo),
                reqParams
            )
            if (response.isSuccessful) {
                GetPurchaseHistoryResult.parseFrom(ResponseWrapper.parseFrom(response.data).payload.purchaseHistoryResponse)
            } else {
                throw RuntimeException("Request failed. code=${response.code}")
            }
        } catch (e: IOException) {
            throw RuntimeException("Network request failed. message=${e.message}")
        }
    }
}