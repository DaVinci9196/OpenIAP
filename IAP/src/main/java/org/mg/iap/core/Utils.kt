package org.mg.iap.core

import com.google.protobuf.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mg.iap.LogUtils
import org.mg.iap.proto.BundleItem
import org.mg.iap.proto.CKDocument
import org.mg.iap.proto.ClientTokenKt
import org.mg.iap.proto.DocId
import org.mg.iap.proto.ResponseBundle
import org.mg.iap.proto.SkuParam
import org.mg.iap.proto.clientToken
import org.mg.iap.proto.skuParam
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.Locale
import org.mg.iap.proto.PurchaseItem

fun List<ByteArray>.toByteStringList(): List<ByteString> {
    return this.map { ByteString.copyFrom(it) }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun mapToSkuParamList(map: Map<String, Any>?): List<SkuParam> {
    val result = mutableListOf<SkuParam>()
    if (map == null)
        return result
    map.forEach { entry ->
        result.add(
            when (val value = entry.value) {
                is Boolean -> skuParam {
                    name = entry.key
                    bv = value
                }

                is Long -> skuParam {
                    name = entry.key
                    i64V = value
                }

                is Int -> skuParam {
                    name = entry.key
                    i64V = value.toLong()
                }

                is ArrayList<*> -> skuParam {
                    name = entry.key
                    svList.addAll(value.map { it as String })
                }

                is String -> skuParam {
                    name = entry.key
                    sv = value
                }

                else -> skuParam {
                    name = entry.key
                    sv = value.toString()
                }
            }
        )
    }

    return result
}

fun localeToString(locale: Locale): String {
    val result = StringBuilder()
    result.append(locale.language)
    locale.country?.let {
        if (it.isNotEmpty())
            result.append("-$it")
    }
    locale.variant?.let {
        if (it.isNotEmpty())
            result.append("-$it")
    }
    return result.toString()
}

fun createClientToken(deviceInfo: DeviceEnvInfo, authData: AuthData): String {
    val clientToken = clientToken {
        this.info1 = ClientTokenKt.info1 {
            this.locale = localeToString(deviceInfo.locale)
            this.unknown8 = 2
            this.gpVersionCode = deviceInfo.gpVersionCode
            this.deviceInfo = ClientTokenKt.deviceInfo {
                this.unknown3 = "33"
                this.device = deviceInfo.device
                deviceInfo.displayMetrics?.let {
                    this.widthPixels = it.widthPixels
                    this.heightPixels = it.heightPixels
                    this.xdpi = it.xdpi
                    this.ydpi = it.ydpi
                    this.densityDpi = it.densityDpi
                }
                this.gpPackage = deviceInfo.gpPkgName
                this.gpVersionCode = deviceInfo.gpVersionCode.toString()
                this.gpVersionName = deviceInfo.gpVersionName
                this.envInfo = ClientTokenKt.envInfo {
                    this.deviceData = ClientTokenKt.deviceData {
                        this.unknown1 = 0
                        deviceInfo.telephonyData?.let {
                            this.simOperatorName = it.simOperatorName
                            this.phoneDeviceId = it.phoneDeviceId
                            this.phoneDeviceId1 = it.phoneDeviceId
                        }
                        this.gsfId = authData.gsfId.toLong(16)
                        this.device = deviceInfo.device
                        this.product = deviceInfo.product
                        this.model = deviceInfo.model
                        this.manufacturer = deviceInfo.manufacturer
                        this.fingerprint = deviceInfo.fingerprint
                        this.release = deviceInfo.release
                        this.brand = deviceInfo.brand
                        this.serial = deviceInfo.serialNo
                        this.isEmulator = false
                    }
                    this.otherInfo = ClientTokenKt.otherInfo {
                        this.gpInfo.add(
                            ClientTokenKt.gPInfo {
                                this.package_ = deviceInfo.gpPkgName
                                this.versionCode = deviceInfo.gpVersionCode.toString()
                                this.lastUpdateTime = deviceInfo.gpLastUpdateTime
                                this.firstInstallTime = deviceInfo.gpFirstInstallTime
                                this.sourceDir = deviceInfo.gpSourceDir
                            })
                        this.batteryLevel = deviceInfo.batteryLevel
                        this.timeZoneOffset = deviceInfo.timeZoneOffset
                        this.location = ClientTokenKt.location {
                            deviceInfo.locationData?.let {
                                this.altitude = it.altitude
                                this.latitude = it.latitude
                                this.longitude = it.longitude
                                this.accuracy = it.accuracy
                                this.time = it.time
                            }
                            this.isMock = false
                        }
                        this.isAdbEnabled = deviceInfo.isAdbEnabled
                        this.installNonMarketApps = deviceInfo.installNonMarketApps
                        this.iso3Language = deviceInfo.locale.isO3Language
                        this.netAddress.addAll(
                            deviceInfo.networkData?.netAddressList ?: emptyList()
                        )
                        this.locale = deviceInfo.locale.toString()
                        deviceInfo.telephonyData?.let {
                            this.networkOperator = it.networkOperator
                            this.simOperator = it.simOperator
                            this.phoneType = it.phoneType
                        }
                        this.language = deviceInfo.locale.language
                        this.country = deviceInfo.locale.country
                        this.uptimeMillis = deviceInfo.uptimeMillis
                        this.timeZoneDisplayName = deviceInfo.timeZoneDisplayName
                        this.googleAccountCount = deviceInfo.googleAccounts.size
                    }
                }
                this.marketClientId = "am-google"
                this.unknown15 = 1
                this.unknown16 = 2
                this.unknown22 = 2
                deviceInfo.networkData?.let {
                    this.linkDownstreamBandwidth = it.linkDownstreamBandwidth
                    this.linkUpstreamBandwidth = it.linkUpstreamBandwidth
                    this.isActiveNetworkMetered = it.isActiveNetworkMetered
                }
                this.unknown34 = 2
                this.uptimeMillis = deviceInfo.uptimeMillis
                this.timeZoneDisplayName = deviceInfo.timeZoneDisplayName
                this.unknown40 = 1
            }
            this.unknown11 = "-5228872483831680725"
            this.googleAccounts.addAll(deviceInfo.googleAccounts)
        }
        this.info2 = ClientTokenKt.info2 {
            this.unknown1 =
                "https://play.app.goo.gl/?link=http%3A%2F%2Funused.google.com&apn=com.android.vending&al=google-orchestration%3A%2F%2Freturn"
            this.unknown3 = 1
            this.unknown4.add(2)
            this.unknown5 = 1
        }
    }
    return org.mg.iap.core.Base64.encodeToString(
        clientToken.toByteArray(),
        org.mg.iap.core.Base64.URL_SAFE or org.mg.iap.core.Base64.NO_WRAP
    )
}

fun getAcquireCacheKey(
    deviceInfo: DeviceEnvInfo,
    accountName: String,
    docList: List<CKDocument>,
    callingPackage: String,
    extras: Map<String, String>,
    authFrequency: Int
): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(accountName)
    for (item in docList) {
        stringBuilder.append("#")
        stringBuilder.append(org.mg.iap.core.Base64.encodeToString(item.toByteArray(), 2))
    }
    stringBuilder.append("#simId=${deviceInfo.deviceId}")
    stringBuilder.append("#clientTheme=2")
    stringBuilder.append("#fingerprintValid=false")
    stringBuilder.append("#desiredAuthMethod=0")
    stringBuilder.append("#authFrequency=$authFrequency")
    stringBuilder.append("#userHasFop=false")
    stringBuilder.append("#callingAppPackageName=$callingPackage")
    for (item in extras) {
        stringBuilder.append("#${item.key}=${item.value}")
    }
    return stringBuilder.toString()
}

fun createNonce(): String {
    val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        ?: throw RuntimeException("Uninitialized SecureRandom.")
    val result = ByteArray(0x100)
    secureRandom.nextBytes(result)
    return "nonce=" + org.mg.iap.core.Base64.encodeToString(result, 11)
}

fun responseBundleToMap(responseBundle: ResponseBundle): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    for (bundleItem in responseBundle.bundleItemList) {
        when (bundleItem.valueCase) {
            BundleItem.ValueCase.BV -> result[bundleItem.key] = bundleItem.bv
            BundleItem.ValueCase.I32V -> result[bundleItem.key] = bundleItem.i32V
            BundleItem.ValueCase.I64V -> result[bundleItem.key] = bundleItem.i64V
            BundleItem.ValueCase.SV -> result[bundleItem.key] = bundleItem.sv
            BundleItem.ValueCase.SLIST -> result[bundleItem.key] =
                ArrayList(bundleItem.sList.valueList)

            else -> {}
        }
    }
    return result
}

fun getSkuType(skuType: String): Int {
    return when (skuType) {
        "subs" -> 15
        "inapp" -> 11
        "first_party" -> 15
        else -> throw InvalidParameterException("unknown skuType: $skuType")
    }
}

fun splitDocId(docId: DocId): List<String> {
    return docId.backendDocId.split(":")
}

fun parsePurchaseItem(purchaseItem: PurchaseItem): List<org.mg.iap.core.PurchaseItem> {
    val result = mutableListOf<org.mg.iap.core.PurchaseItem>()
    for (it in purchaseItem.purchaseItemDataList) {
        if (it == null)
            continue
        val spr = splitDocId(it.docId)
        if (spr.size < 3)
            continue
        val (type, _, sku) = spr
        val (jsonData, signature) = when (type) {
            "inapp" -> {
                if (!it.hasInAppPurchase())
                    continue
                it.inAppPurchase.jsonData to it.inAppPurchase.signature
            }

            "subs" -> {
                if (!it.hasSubsPurchase())
                    continue
                it.subsPurchase.jsonData to it.subsPurchase.signature
            }

            else -> {
                LogUtils.e("unknown sku type $type")
                continue
            }
        }
        val jdo = Json.parseToJsonElement(jsonData).jsonObject
        val pkgName = jdo["packageName"]?.jsonPrimitive?.content ?: continue
        val purchaseToken = jdo["purchaseToken"]?.jsonPrimitive?.content ?: continue
        val purchaseState = jdo["purchaseState"]?.jsonPrimitive?.int ?: continue
        result.add(
            PurchaseItem(
                type,
                sku,
                pkgName,
                purchaseToken,
                purchaseState,
                jsonData,
                signature
            )
        )
    }
    return result
}