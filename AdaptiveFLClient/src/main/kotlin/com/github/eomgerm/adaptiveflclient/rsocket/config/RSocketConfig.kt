package com.github.eomgerm.adaptiveflclient.rsocket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils

@Configuration
class RSocketConfig {
    @Bean
    fun rSocketRequester(rSocketStrategies: RSocketStrategies): RSocketRequester =
        RSocketRequester
            .builder()
            .rsocketConnector {
                it.fragment(16_777_215)
            }.rsocketStrategies(rSocketStrategies)
            .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
            .tcp("localhost", 7002)
}
