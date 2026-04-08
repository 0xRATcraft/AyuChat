package ru.fromchat.platform

import android.os.Build

private fun String?.trimIfNotBlank(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }

actual fun currentDeviceInfo(): CurrentDeviceInfo {
    val osName = "Android"
    val osVersion = Build.VERSION.RELEASE.trimIfNotBlank() ?: Build.VERSION.SDK_INT.toString()
    val brand = Build.BRAND.trimIfNotBlank() ?: Build.MANUFACTURER.trimIfNotBlank()
    val model = Build.MODEL.trimIfNotBlank()

    return CurrentDeviceInfo(
        osName = osName,
        osVersion = osVersion,
        deviceType = "mobile",
        deviceName = model?.let { if (it.contains(brand.orEmpty(), ignoreCase = true)) it else listOfNotNull(brand, it).joinToString(" ") },
        brand = brand,
        model = model
    )
}
