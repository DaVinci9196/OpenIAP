package org.mg.iap.core

data class ClientInfo(
    val pkgName: String,
    val signatureMD5: String,
    val versionCode: Int
)