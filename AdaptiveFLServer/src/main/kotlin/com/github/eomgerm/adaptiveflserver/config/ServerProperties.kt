package com.github.eomgerm.adaptiveflserver.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties

private val logger = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "adaptivefl.server")
data class ServerProperties(
    val parametersThreshold: Long = 2_000_000,
    val chunkSize: Int = 10_000,
) {
    init {

        logger.info {
            "\n" +
                "ServerConfig:\n " +
                "parametersThreshold=$parametersThreshold\n" +
                "chunkSize=$chunkSize"
        }
    }
}
