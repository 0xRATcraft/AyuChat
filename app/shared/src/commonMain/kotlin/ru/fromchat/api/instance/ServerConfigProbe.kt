package ru.fromchat.api.instance

import kotlinx.coroutines.withTimeout
import ru.fromchat.api.ApiClient
import ru.fromchat.api.local.WebSocketManager
import ru.fromchat.api.local.cache.CacheContext
import ru.fromchat.api.local.db.store.InstanceRegistryStore
import ru.fromchat.config.ServerConfig
import ru.fromchat.config.ServerConfigData
import ru.fromchat.legal.DocumentRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

sealed interface ServerProbeResult {
    data class Supported(
        val instanceId: String,
        val callsOk: Boolean,
        val pingMs: Int,
    ) : ServerProbeResult

    data object Unsupported : ServerProbeResult
    data object Timeout : ServerProbeResult
    data object Unreachable : ServerProbeResult
}

sealed interface ApplyServerResult {
    data object Applied : ApplyServerResult
    data object ServerUnreachable : ApplyServerResult
}

private const val CALLS_PROBE_MS = 1_500L

/** True when [error] looks like a TLS/SSL handshake failure (safe to fall back to HTTP). */
fun Throwable.isSslProtocolError(): Boolean {
    var cur: Throwable? = this
    while (cur != null) {
        val name = cur::class.simpleName.orEmpty()
        val msg = cur.message.orEmpty()
        if (
            name.contains("SSL", ignoreCase = true) ||
            name.contains("TLS", ignoreCase = true) ||
            name.contains("Cert", ignoreCase = true) ||
            msg.contains("SSL", ignoreCase = true) ||
            msg.contains("TLS", ignoreCase = true) ||
            msg.contains("certificate", ignoreCase = true) ||
            msg.contains("handshake", ignoreCase = true)
        ) {
            return true
        }
        cur = cur.cause
    }
    return false
}

suspend fun probeCallsReachable(config: ServerConfigData) = runCatching {
    withTimeout(CALLS_PROBE_MS.milliseconds) {
        ApiClient.probeHttpGet(
            buildString {
                append(if (config.httpsEnabled) "https" else "http")
                append("://")
                append(
                    config.serverIp.trim().let {
                        it.removePrefix("[").removeSuffix("]").ifEmpty { it }
                    }
                )
                append(":")
                append(config.apiPort)
                append("/livekit-health")
            }
        )
    }
}.getOrDefault(false)

suspend fun probeServer(config: ServerConfigData) =
    probeServerEndpoint(
        host = config.serverIp,
        port = config.apiPort,
        httpsPreferred = config.httpsEnabled,
        schemeExplicit = true,
    ).second

/**
 * Builds configs to try: explicit scheme only, or HTTPS then HTTP on SSL errors.
 * Returns the working config (httpsEnabled set to what succeeded) and the probe result.
 */
suspend fun probeServerEndpoint(
    host: String,
    port: Int,
    httpsPreferred: Boolean,
    schemeExplicit: Boolean,
): Pair<ServerConfigData, ServerProbeResult> {
    var lastConfig = ServerConfigData(
        serverIp = host,
        apiPort = port,
        callsPort = port,
        httpsEnabled = httpsPreferred,
    )

    var lastResult: ServerProbeResult = ServerProbeResult.Unreachable

    when {
        schemeExplicit -> listOf(httpsPreferred)
        else -> listOf(true, false)
    }.apply {
        forEachIndexed { index, https ->
            val config = ServerConfigData(
                serverIp = host,
                apiPort = port,
                callsPort = port,
                httpsEnabled = https,
            )

            lastConfig = config
            val (result, failure) = probeServerOnce(config)
            lastResult = result

            when (result) {
                is ServerProbeResult.Supported,
                ServerProbeResult.Unsupported -> return config to result
                ServerProbeResult.Timeout,
                ServerProbeResult.Unreachable -> {
                    if (!(!schemeExplicit && https && index < lastIndex)) return config to result

                    // Browser-like: only fall back to HTTP after a TLS/protocol failure.
                    if (failure?.isSslProtocolError() == true)
                        return@forEachIndexed
                    if (result is ServerProbeResult.Timeout)
                        return config to result

                    // Non-SSL Unreachable (e.g. LAN cleartext / connection refused on 443): try HTTP.
                    return@forEachIndexed
                }
            }
        }
    }

    return lastConfig to lastResult
}

private suspend fun probeServerOnce(
    config: ServerConfigData,
): Pair<ServerProbeResult, Throwable?> {
    val apiBase = apiBaseUrlFor(config)
    val time = TimeSource.Monotonic.markNow()
    InstanceIdGuard.probeConfig = config

    try {
        val pingMs = time.elapsedNow().inWholeMilliseconds.toInt().coerceAtLeast(0)

        return ServerProbeResult.Supported(
            when (
                val resolve = resolveInstanceId(
                    config = config,
                    apiBaseUrl = apiBase,
                    forceNetwork = true,
                    allowCachedOnFailure = false,
                )
            ) {
                is InstanceIdResolveResult.Cached -> resolve.instanceId
                is InstanceIdResolveResult.Fetched -> resolve.instanceId
                is InstanceIdResolveResult.InstanceIdChanged -> resolve.newId
                InstanceIdResolveResult.Unsupported ->
                    return ServerProbeResult.Unsupported to null
                InstanceIdResolveResult.Timeout ->
                    return ServerProbeResult.Timeout to null
                InstanceIdResolveResult.Unreachable -> {
                    val failure = runCatching {
                        ApiClient.fetchServerInstanceId(apiBase)
                    }.exceptionOrNull()
                    return ServerProbeResult.Unreachable to failure
                }
            },
            probeCallsReachable(config),
            pingMs
        ) to null
    } finally {
        InstanceIdGuard.probeConfig = null
    }
}

suspend fun applyServerConfig(
    config: ServerConfigData,
    instanceId: String,
    callsOk: Boolean,
) {
    val tentative = config.copy(callsEnabled = callsOk, callsPort = config.apiPort)
    ServerConfig.updateServerConfig(tentative)
    DocumentRepository.invalidate()
    val userId = ApiClient.user?.id
    InstanceRegistryStore.rebindServerInstance(tentative, instanceId)
    CacheContext.setActiveInstance(instanceId, userId)
}

suspend fun applyServerAndNavigate(
    probe: ServerProbeResult.Supported,
    config: ServerConfigData,
    bearer: String,
    onNavigateLogin: suspend () -> Unit,
    onNavigateChat: suspend () -> Unit,
    onLogoutOldHost: suspend () -> Unit,
): ApplyServerResult {
    val apiBase = apiBaseUrlFor(config)
    val token = bearer.trim()

    if (token.isEmpty()) {
        applyServerConfig(config, probe.instanceId, probe.callsOk)
        WebSocketManager.disconnect()
        onNavigateLogin()
        return ApplyServerResult.Applied
    }

    when (ApiClient.checkAuthAt(apiBase, token)) {
        ApiClient.CheckAuthResult.Authenticated -> {
            applyServerConfig(config, probe.instanceId, probe.callsOk)
            WebSocketManager.disconnect()
            WebSocketManager.connect(forceRestart = true)
            onNavigateChat()
            return ApplyServerResult.Applied
        }

        ApiClient.CheckAuthResult.Unreachable -> {
            return ApplyServerResult.ServerUnreachable
        }

        ApiClient.CheckAuthResult.NotAuthenticated -> {
            onLogoutOldHost()
            applyServerConfig(config, probe.instanceId, probe.callsOk)
            WebSocketManager.disconnect()
            onNavigateLogin()
            return ApplyServerResult.Applied
        }
    }
}
