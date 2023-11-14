package org.mg.iap

import android.util.Log

/**
 * 强制开启日志方法：adb shell setprop log.tag.MGLog99 D
 */
object LogUtils {
    private val FORCE_DEBUG_ENABLED = Log.isLoggable("MGLog99", Log.DEBUG)
    private val isDebug = true//FORCE_DEBUG_ENABLED || BuildConfig.DEBUG
    private const val DEFAULT_TAG = "billing"

    fun d(msg: String) {
        val maxLength = 4000 // The maximum length for Logcat messages
        var i = 0
        while (i < msg.length) {
            val end = (i + maxLength).coerceAtMost(msg.length)
            val chunk: String = msg.substring(i, end)
            d(DEFAULT_TAG, chunk)
            i += maxLength
        }
    }

    fun d(msg: String, tr: Throwable) {
        d(DEFAULT_TAG, msg, tr)
    }

    fun d(tag: String, msg: String) {
        if (isDebug) Log.d(tag, msg)
    }

    fun d(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.d(tag, msg, tr)
    }

    fun i(msg: String) {
        i(DEFAULT_TAG, msg)
    }

    fun i(msg: String, tr: Throwable) {
        i(DEFAULT_TAG, msg, tr)
    }

    fun i(tag: String, msg: String) {
        if (isDebug) Log.i(tag, msg)
    }

    fun i(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.i(tag, msg, tr)
    }

    fun w(msg: String) {
        w(DEFAULT_TAG, msg)
    }

    fun w(msg: String, tr: Throwable) {
        w(DEFAULT_TAG, msg, tr)
    }

    fun w(tag: String, msg: String) {
        if (isDebug) Log.w(tag, msg)
    }

    fun w(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.w(tag, msg, tr)
    }

    fun e(msg: String) {
        e(DEFAULT_TAG, msg)
    }

    fun e(msg: String, tr: Throwable) {
        e(DEFAULT_TAG, msg, tr)
    }

    fun e(tag: String, msg: String) {
        if (isDebug) Log.e(tag, msg)
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.e(tag, msg, tr)
    }
}