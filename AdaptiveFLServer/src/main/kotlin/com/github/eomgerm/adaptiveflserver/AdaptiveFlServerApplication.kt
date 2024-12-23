package com.github.eomgerm.adaptiveflserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AdaptiveFlServerApplication

fun main(args: Array<String>) {
    runApplication<AdaptiveFlServerApplication>(*args)
}
