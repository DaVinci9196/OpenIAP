package org.mg.iap.core

open class IAPResult(val resultMap: Map<String, Any>) {
    fun getCode(): Int {
        return resultMap["RESPONSE_CODE"] as Int
    }

    fun getMessage(): String {
        return resultMap["RESPONSE_MESSAGE"] as String
    }
}