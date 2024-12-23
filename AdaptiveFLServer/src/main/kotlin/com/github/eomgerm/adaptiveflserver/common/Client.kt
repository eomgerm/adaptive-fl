package com.github.eomgerm.adaptiveflserver.common

import com.github.eomgerm.adaptivefl.proto.Transport
import io.grpc.ManagedChannel
import org.springframework.messaging.rsocket.RSocketRequester
import java.net.Inet4Address
import java.net.Inet6Address

class Client(
    val clientId: String,
    val backPressureRequired: Boolean,
    val transports: List<Transport>,
    val limitRate: Int? = null,
    val ipv4Address: Inet4Address? = null,
    val ipv6Address: Inet6Address? = null,
    var grpcChannel: ManagedChannel? = null,
    var rSocketRequester: RSocketRequester? = null,
    host: String,
    port: Int,
)
