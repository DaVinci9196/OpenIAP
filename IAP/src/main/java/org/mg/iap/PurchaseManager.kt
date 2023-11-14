package org.mg.iap

import android.accounts.Account
import org.mg.iap.core.PurchaseItem

object PurchaseManager {
    private val purchaseListMgrMap = mutableMapOf<String, PurchaseListMgr>()

    fun getPurchaseListMgr(account: Account, pkgName: String): PurchaseListMgr {
        val key = "${account.name}:$pkgName"
        if (purchaseListMgrMap.containsKey(key))
            return purchaseListMgrMap[key]!!
        val newItem = PurchaseListMgr(account, pkgName)
        purchaseListMgrMap[key] = newItem
        return newItem
    }

    class PurchaseListMgr(private val account: Account, private val pkgName: String) {
        private val purchaseSet = mutableSetOf<PurchaseItem>()

        private fun findItem(purchaseToken: String): PurchaseItem? {
            return purchaseSet.find {
                it.purchaseToken == purchaseToken
            }
        }

        fun getPurchaseByType(type: String): List<PurchaseItem> {
            return purchaseSet.filter { type == it.type }
        }

        fun addItem(purchaseItem: PurchaseItem): Boolean {
            return purchaseSet.add(purchaseItem)
        }

        fun updateItem(newItem: PurchaseItem): Boolean {
            return findItem(newItem.purchaseToken)?.let {
                return purchaseSet.remove(it) && purchaseSet.add(newItem)
            } ?: false
        }

        fun removeItem(purchaseToken: String): Boolean {
            return findItem(purchaseToken)?.let {
                return purchaseSet.remove(it)
            } ?: false
        }
    }
}