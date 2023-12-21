package org.mg.iap

import android.net.Uri

object DeviceProfileMgr {
    private val CONTENT_URI: Uri = Uri.parse("content://com.google.android.gms.microg.profile")

    private fun getProfileData(): Map<String, String> {
        val data = mutableMapOf<String, String>()
        val cursor =
            ContextProvider.context.contentResolver.query(CONTENT_URI, null, null, null, null)
        cursor?.use {
            while (cursor.moveToNext()) {
                data[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return data
    }

    private fun applyProfileData(profileData: Map<String, String>) {
        fun applyStringField(key: String, valueSetter: (String) -> Unit) =
            profileData[key]?.let { valueSetter(it) }

        fun applyIntField(key: String, valueSetter: (Int) -> Unit) =
            profileData[key]?.toIntOrNull()?.let { valueSetter(it) }

        fun applyLongField(key: String, valueSetter: (Long) -> Unit) =
            profileData[key]?.toLongOrNull()?.let { valueSetter(it) }

        applyStringField("Build.BOARD") { Build.BOARD = it }
        applyStringField("Build.BOOTLOADER") { Build.BOOTLOADER = it }
        applyStringField("Build.BRAND") { Build.BRAND = it }
        applyStringField("Build.CPU_ABI") { Build.CPU_ABI = it }
        applyStringField("Build.CPU_ABI2") { Build.CPU_ABI2 = it }
        applyStringField("Build.DEVICE") { Build.DEVICE = it }
        applyStringField("Build.DISPLAY") { Build.DISPLAY = it }
        applyStringField("Build.FINGERPRINT") { Build.FINGERPRINT = it }
        applyStringField("Build.HARDWARE") { Build.HARDWARE = it }
        applyStringField("Build.HOST") { Build.HOST = it }
        applyStringField("Build.ID") { Build.ID = it }
        applyStringField("Build.MANUFACTURER") { Build.MANUFACTURER = it }
        applyStringField("Build.MODEL") { Build.MODEL = it }
        applyStringField("Build.PRODUCT") { Build.PRODUCT = it }
        applyStringField("Build.RADIO") { Build.RADIO = it }
        applyStringField("Build.SERIAL") { Build.SERIAL = it }
        applyStringField("Build.TAGS") { Build.TAGS = it }
        applyLongField("Build.TIME") { Build.TIME = it }
        applyStringField("Build.TYPE") { Build.TYPE = it }
        applyStringField("Build.USER") { Build.USER = it }
        applyStringField("Build.VERSION.CODENAME") { Build.VERSION.CODENAME = it }
        applyStringField("Build.VERSION.INCREMENTAL") { Build.VERSION.INCREMENTAL = it }
        applyStringField("Build.VERSION.RELEASE") { Build.VERSION.RELEASE = it }
        applyStringField("Build.VERSION.SDK") { Build.VERSION.SDK = it }
        applyIntField("Build.VERSION.SDK_INT") { Build.VERSION.SDK_INT = it }
        applyIntField("Build.VERSION.DEVICE_INITIAL_SDK_INT") {
            Build.VERSION.DEVICE_INITIAL_SDK_INT = it
        }
        Build.SUPPORTED_ABIS =
            profileData["Build.SUPPORTED_ABIS"]?.split(",")?.toTypedArray() ?: emptyArray()
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            Build.VERSION.SECURITY_PATCH = profileData["Build.VERSION.SECURITY_PATCH"]
        } else {
            Build.VERSION.SECURITY_PATCH = null
        }
    }

    fun applyProfile() {
        val profileData = getProfileData()
//        if (profileData.isEmpty()) {
//            LogUtils.d("getProfileData is empty")
//        }
//        for ((key, value) in profileData) {
//            LogUtils.d("<data key=\"$key\" value=\"$value\" />")
//        }

        applyProfileData(profileData)
    }
}