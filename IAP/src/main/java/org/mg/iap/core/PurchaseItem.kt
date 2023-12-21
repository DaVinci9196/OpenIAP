package org.mg.iap.core

data class PurchaseItem(
    val type: String,
    val sku: String,
    val pkgName: String,
    val purchaseToken: String,
    val purchaseState: Int,
    val jsonData: String,
    val signature: String
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PurchaseItem -> {
                this.purchaseToken == other.purchaseToken
            }

            else -> false
        }
    }
}
