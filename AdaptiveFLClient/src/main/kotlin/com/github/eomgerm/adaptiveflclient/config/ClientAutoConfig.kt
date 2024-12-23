package com.github.eomgerm.adaptiveflclient.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ClientProperties::class)
class ClientAutoConfig
