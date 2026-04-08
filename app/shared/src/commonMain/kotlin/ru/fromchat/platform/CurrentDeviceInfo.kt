package ru.fromchat.platform

data class CurrentDeviceInfo(
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceType: String? = null,
    val deviceName: String? = null,
    val brand: String? = null,
    val model: String? = null
)

expect fun currentDeviceInfo(): CurrentDeviceInfo
