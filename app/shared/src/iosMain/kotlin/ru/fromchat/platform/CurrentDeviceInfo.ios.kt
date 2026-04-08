package ru.fromchat.platform

import platform.UIKit.UIDevice

private fun String?.trimIfNotBlank(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }

actual fun currentDeviceInfo(): CurrentDeviceInfo {
    val current = UIDevice.currentDevice

    return CurrentDeviceInfo(
        osName = current.systemName.trimIfNotBlank(),
        osVersion = current.systemVersion.trimIfNotBlank(),
        deviceType = "mobile",
        deviceName = current.name.trimIfNotBlank(),
        brand = "Apple",
        model = current.model.trimIfNotBlank()
    )
}
