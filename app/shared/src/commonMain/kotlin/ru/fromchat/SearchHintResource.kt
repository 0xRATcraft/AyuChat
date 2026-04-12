package ru.fromchat

import kotlin.collections.HashMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.InternalResourceApi

@OptIn(InternalResourceApi::class)
private fun collectCommonMainStringResources(): HashMap<String, StringResource> {
    val map = HashMap<String, StringResource>()
    _collectCommonMainString0Resources(map)
    _collectCommonMainString1Resources(map)
    return map
}

internal fun resolveSearchHintResource(): StringResource {
    val map = collectCommonMainStringResources()
    return map["search_hint"]
        ?: map["app_name"]
        ?: error("Search hint resource is missing")
}

internal fun resolveSearchNotFoundTitleResource(): StringResource {
    val map = collectCommonMainStringResources()
    return map["search_not_found"]
        ?: map["search_not_found_message"]
        ?: map["app_name"]
        ?: error("Search not-found title resource is missing")
}

internal fun resolveSearchNotFoundMessageResource(): StringResource {
    val map = collectCommonMainStringResources()
    return map["search_not_found_message"]
        ?: map["search_not_found"]
        ?: map["app_name"]
        ?: error("Search not-found message resource is missing")
}

internal fun resolveSearchTitleResource(): StringResource {
    val map = collectCommonMainStringResources()
    return map["search_title"]
        ?: map["search_hint"]
        ?: map["app_name"]
        ?: error("Search title resource is missing")
}
