package org.mg.iap

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.util.TimeZone
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Base64
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.google.android.gms.droidguard.DroidGuardClient
import kotlinx.coroutines.runBlocking
import org.mg.iap.core.ClientInfo
import org.mg.iap.core.DeviceEnvInfo
import org.mg.iap.core.DisplayMetrics
import org.mg.iap.core.LocationData
import org.mg.iap.core.NetworkData
import org.mg.iap.core.TelephonyData
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Map<String, Any?>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray())

/**
 * Returns true if the receiving collection contains any of the specified elements.
 *
 * @param elements the elements to look for in the receiving collection.
 * @return true if any element in [elements] is found in the receiving collection.
 */
fun <T> Collection<T>.containsAny(vararg elements: T): Boolean {
    return containsAny(elements.toSet())
}

/**
 * Returns true if the receiving collection contains any of the elements in the specified collection.
 *
 * @param elements the elements to look for in the receiving collection.
 * @return true if any element in [elements] is found in the receiving collection.
 */
fun <T> Collection<T>.containsAny(elements: Collection<T>): Boolean {
    val set = if (elements is Set) elements else elements.toSet()
    return any(set::contains)
}

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun resultBundle(code: Int, msg: String?, data: Bundle = Bundle.EMPTY): Bundle {
    val res = bundleOf(
        "RESPONSE_CODE" to code,
        "DEBUG_MESSAGE" to msg
    )
    res.putAll(data)
    return res
}

fun hashBase64(data: ByteArray, alg: String, flag: Int): String {
    val messageDigest = try {
        MessageDigest.getInstance(alg)
    } catch (e: NoSuchAlgorithmException) {
        return ""
    }
    messageDigest.update(data)
    return Base64.encodeToString(messageDigest.digest(), flag)
}

@SuppressLint("MissingPermission")
fun getDeviceIdentifier(): String {
    var deviceId = try {
        (ContextProvider.context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.let {
            it.subscriberId ?: it.deviceId
        }
    } catch (e: Exception) {
        null
    }
    LogUtils.d("getDeviceIdentifier deviceId: $deviceId")
    return hashBase64((deviceId ?: DeviceIdentifier.meid).toByteArray(Charsets.UTF_8), "SHA-1", 11)
}

fun getGoogleAccount(name: String? = null): Account? {
    var accounts =
        AccountManager.get(ContextProvider.context).getAccountsByType(DEFAULT_ACCOUNT_TYPE).toList()
    name?.let { accounts = accounts.filter { it.name == name } }
    if (accounts.isEmpty())
        return null
    return accounts[0]
}

fun createClient(pkgName: String): ClientInfo? {
    return try {
        val packageInfo = ContextProvider.context.packageManager.getPackageInfo(pkgName, 0x40)
        ClientInfo(
            pkgName,
            hashBase64(packageInfo.signatures[0].toByteArray(), "MD5", 11),
            packageInfo.versionCode
        )
    } catch (e: Exception) {
        LogUtils.d("createClient", e)
        null
    }
}

@SuppressLint("MissingPermission")
fun getSerialNo(): String {
    return try {
        if (android.os.Build.VERSION.SDK_INT < 26) android.os.Build.SERIAL else android.os.Build.getSerial()
    } catch (e: SecurityException) {
        return DeviceIdentifier.serial
    }
}

fun bundleToMap(bundle: Bundle?): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    if (bundle == null)
        return result
    for (key in bundle.keySet()) {
        bundle.get(key)?.let {
            result[key] = it
        }
    }
    return result
}

fun getDisplayInfo(context: Context): DisplayMetrics? {
    return try {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (windowManager != null) {
            val displayMetrics = android.util.DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            return DisplayMetrics(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.xdpi,
                displayMetrics.ydpi,
                displayMetrics.densityDpi
            )
        }
        return DisplayMetrics(
            context.resources.displayMetrics.widthPixels,
            context.resources.displayMetrics.heightPixels,
            context.resources.displayMetrics.xdpi,
            context.resources.displayMetrics.ydpi,
            context.resources.displayMetrics.densityDpi
        )
    } catch (e: Exception) {
        null
    }
}

fun getBatteryLevel(context: Context): Int {
    var batteryLevel = -1;
    val intentFilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
    context.registerReceiver(null, intentFilter)?.let {
        val level = it.getIntExtra("level", -1)
        val scale = it.getIntExtra("scale", -1)
        if (scale > 0) {
            batteryLevel = level * 100 / scale
        }
    }
    if (batteryLevel == -1 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(null, intentFilter, Context.RECEIVER_EXPORTED)?.let {
            val level = it.getIntExtra("level", -1)
            val scale = it.getIntExtra("scale", -1)
            if (scale > 0) {
                batteryLevel = level * 100 / scale
            }
        }
    }
    return batteryLevel
}

fun getTelephonyData(context: Context): TelephonyData? {
    return try {
        context.getSystemService(Context.TELEPHONY_SERVICE)?.let {
            val telephonyManager = it as TelephonyManager
            return TelephonyData(
                telephonyManager.simOperatorName!!,
                DeviceIdentifier.meid,
                telephonyManager.networkOperator!!,
                telephonyManager.simOperator!!,
                telephonyManager.phoneType
            )
        }
    } catch (e: Exception) {
        LogUtils.d("getTelephonyData", e)
        null
    }
}

fun hasPermissions(context: Context, permissions: List<String>): Boolean {
    for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(
                context, permission
            ) != PackageManager.PERMISSION_GRANTED
        )
            return false
    }
    return true
}

@SuppressLint("MissingPermission")
fun getLocationData(context: Context): LocationData? {
    return try {
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?)?.let { locationManager ->
            if (hasPermissions(
                    context,
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            ) {
                locationManager.getLastKnownLocation("network")?.let { location ->
                    return LocationData(
                        location.altitude,
                        location.latitude,
                        location.longitude,
                        location.accuracy,
                        location.time.toDouble()
                    )
                }
            } else {
                null
            }
        }
    } catch (e: Exception) {
        LogUtils.d("getLocationData", e)
        null
    }
}

fun getNetworkData(context: Context): NetworkData {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    var linkDownstreamBandwidth: Long = 0
    var linkUpstreamBandwidth: Long = 0
    if (hasPermissions(
            context,
            listOf(Manifest.permission.ACCESS_NETWORK_STATE)
        ) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
    ) {
        connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
            linkDownstreamBandwidth = (it.linkDownstreamBandwidthKbps * 1000 / 8).toLong()
            linkUpstreamBandwidth = (it.linkUpstreamBandwidthKbps * 1000 / 8).toLong()
        }
    }
    var isActiveNetworkMetered = connectivityManager?.isActiveNetworkMetered ?: false
    val netAddressList = mutableListOf<String>()
    try {
        NetworkInterface.getNetworkInterfaces()?.let { enumeration ->
            while (true) {
                if (!enumeration.hasMoreElements()) {
                    break
                }
                val enumeration1 = enumeration.nextElement().inetAddresses
                while (enumeration1.hasMoreElements()) {
                    val inetAddress = enumeration1.nextElement() as InetAddress
                    if (inetAddress.isLoopbackAddress) {
                        continue
                    }
                    netAddressList.add(inetAddress.hostAddress)
                }
            }
        }
    } catch (socketException: NullPointerException) {
    }
    return NetworkData(
        linkDownstreamBandwidth,
        linkUpstreamBandwidth,
        isActiveNetworkMetered,
        netAddressList
    )
}

fun getAndroidId(): String {
    return Settings.Secure.getString(
        ContextProvider.context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: ""
}

fun createDeviceEnvInfo(): DeviceEnvInfo? {
    try {
        val context = ContextProvider.context
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return DeviceEnvInfo(
            gpVersionCode = GP_VERSION_CODE,
            gpVersionName = GP_VERSION_NAME,
            gpPkgName = GP_PACKAGE_NAME,
            androidId = getAndroidId(),
            biometricSupport = true,
            biometricSupportCDD = true,
            deviceId = getDeviceIdentifier(),
            serialNo = Build.SERIAL,
            locale = Locale.getDefault(),
            userAgent = "Android-Finsky/${
                URLEncoder.encode(
                    GP_VERSION_NAME,
                    "UTF-8"
                ).replace("+", "%20")
            } (api=3,versionCode=$GP_VERSION_CODE,sdk=${Build.VERSION.SDK_INT},device=${Build.DEVICE},hardware=${Build.HARDWARE},product=${Build.PRODUCT},platformVersionRelease=${Build.VERSION.RELEASE},model=${
                URLEncoder.encode(
                    Build.MODEL,
                    "utf-8"
                ).replace("+", "%20")
            },buildId=${Build.ID},isWideScreen=0,supportedAbis=${Build.SUPPORTED_ABIS.joinToString(";")})",
            gpLastUpdateTime = packageInfo.lastUpdateTime,
            gpFirstInstallTime = packageInfo.firstInstallTime,
            gpSourceDir = packageInfo.applicationInfo.sourceDir!!,
            device = Build.DEVICE!!,
            displayMetrics = getDisplayInfo(context),
            telephonyData = getTelephonyData(context),
            product = Build.PRODUCT!!,
            model = Build.MODEL!!,
            manufacturer = Build.MANUFACTURER!!,
            fingerprint = Build.FINGERPRINT!!,
            release = Build.VERSION.RELEASE!!,
            brand = Build.BRAND!!,
            batteryLevel = getBatteryLevel(context),
            timeZoneOffset = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                TimeZone.getDefault().rawOffset.toLong()
            } else {
                0
            },
            locationData = getLocationData(context),
            isAdbEnabled = Settings.Global.getInt(context.contentResolver, "adb_enabled", 0) == 1,
            installNonMarketApps = Settings.Secure.getInt(
                context.contentResolver,
                "install_non_market_apps",
                0
            ) == 1,
            networkData = getNetworkData(context),
            uptimeMillis = SystemClock.uptimeMillis(),
            timeZoneDisplayName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                TimeZone.getDefault().displayName!!
            } else {
                ""
            },
            googleAccounts = AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE)
                .map { it.name }
        )
    } catch (e: Exception) {
        LogUtils.d("createDeviceInfo", e)
        return null
    }
}

fun getDeviceIdentManager(): DeviceIdentifier {
    return DeviceIdentifier
}

fun getLocale(): Locale? {
    return Locale.getDefault() // TODO
}

fun getDroidGuardResult(context: Context, flow: String, bindingMap: Map<String, String>): String {
    return runBlocking {
        suspendCoroutine { continuation ->
            DroidGuardClient.getResults(context, flow, bindingMap)
                .addOnCompleteListener {
                    continuation.resume(it.result)
                }.addOnFailureListener {
                    continuation.resume("")
                }
        }
    }
}