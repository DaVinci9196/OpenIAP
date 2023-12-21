package org.mg.iap

import android.annotation.TargetApi
import android.os.Build

object Build {
    @JvmField
    var BOARD: String = android.os.Build.BOARD

    @JvmField
    var BOOTLOADER: String = android.os.Build.BOOTLOADER

    @JvmField
    var BRAND: String = android.os.Build.BRAND

    @JvmField
    var CPU_ABI: String = android.os.Build.CPU_ABI

    @JvmField
    var CPU_ABI2: String = android.os.Build.CPU_ABI2

    @JvmField
    @TargetApi(21)
    var SUPPORTED_ABIS: Array<String> = android.os.Build.SUPPORTED_ABIS

    @JvmField
    var DEVICE: String = android.os.Build.DEVICE

    @JvmField
    var DISPLAY: String = android.os.Build.DISPLAY

    @JvmField
    var FINGERPRINT: String = android.os.Build.FINGERPRINT

    @JvmField
    var HARDWARE: String = android.os.Build.HARDWARE

    @JvmField
    var HOST: String = android.os.Build.HOST

    @JvmField
    var ID: String = android.os.Build.ID

    @JvmField
    var MANUFACTURER: String = android.os.Build.MANUFACTURER

    @JvmField
    var MODEL: String = android.os.Build.MODEL

    @JvmField
    var PRODUCT: String = android.os.Build.PRODUCT

    @JvmField
    var RADIO: String = android.os.Build.RADIO

    @JvmField
    var SERIAL: String = getSerialNo()

    @JvmField
    var TAGS: String = android.os.Build.TAGS

    @JvmField
    var TIME: Long = android.os.Build.TIME

    @JvmField
    var TYPE: String = android.os.Build.TYPE

    @JvmField
    var USER: String = android.os.Build.USER

    object VERSION {
        @JvmField
        var CODENAME: String = android.os.Build.VERSION.CODENAME

        @JvmField
        var INCREMENTAL: String = android.os.Build.VERSION.INCREMENTAL

        @JvmField
        var RELEASE: String = android.os.Build.VERSION.RELEASE

        @JvmField
        var SDK: String = android.os.Build.VERSION.SDK

        @JvmField
        var SDK_INT: Int = android.os.Build.VERSION.SDK_INT

        @JvmField
        var SECURITY_PATCH: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.Build.VERSION.SECURITY_PATCH
        } else {
            null
        }

        @JvmField
        var DEVICE_INITIAL_SDK_INT: Int = 0
    }

    fun generateWebViewUserAgentString(original: String): String {
        if (!original.startsWith("Mozilla/5.0 (")) return original
        val closeParen: Int = original.indexOf(')')

        return "Mozilla/5.0 (Linux; Android ${VERSION.RELEASE}; $MODEL Build/$ID; wv)${
            original.substring(
                closeParen + 1
            )
        }"
    }
}