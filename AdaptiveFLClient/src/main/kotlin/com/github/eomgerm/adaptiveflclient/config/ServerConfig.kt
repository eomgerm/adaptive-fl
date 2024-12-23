package com.github.eomgerm.adaptiveflclient.config

import org.springframework.stereotype.Component

@Component
data class ServerConfig(
    var parametersThreshold: Long = 2_000_000,
    var chunkSize: Int = 1_000,
)
