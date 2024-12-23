package com.github.eomgerm.adaptiveflclient.config

import com.github.eomgerm.adaptivefl.proto.Transport
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

private val logger = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "adaptivefl.client")
data class ClientProperties(
    val id: String,
    @NestedConfigurationProperty
    val backPressure: BackPressureProperties = BackPressureProperties(),
    val transports: List<Transport>,
    @NestedConfigurationProperty
    val reconnection: ReconnectionProperties = ReconnectionProperties(),
    @NestedConfigurationProperty
    val grpc: GrpcProperties = GrpcProperties(),
) {
    init {
        logger.info {
            "\n" +
                "ClientConfig:\n" +
                "id=$id\n" +
                "bpRequired=${backPressure.required}, bpLimitRate=${backPressure.limitRate}\n" +
                "transports=$transports"
        }
    }

    data class BackPressureProperties(
        val required: Boolean = false,
        val limitRate: Int = 10,
    )

    data class ReconnectionProperties(
        val enabled: Boolean = true,
        val reconnectionDelay: Long = 1000,
        val reconnectionAttempts: Int? = null,
        val reconnectionDelayMax: Long = 5000,
    )

    data class GrpcProperties(
        val host: String = "localhost",
        val port: Int = 9090,
    )
}
