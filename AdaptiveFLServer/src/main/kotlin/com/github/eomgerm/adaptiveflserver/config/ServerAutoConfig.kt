package com.github.eomgerm.adaptiveflserver.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ServerProperties::class)
class ServerAutoConfig
