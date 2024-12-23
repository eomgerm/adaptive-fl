package com.github.eomgerm.adaptiveflserver

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Model {
    companion object {
        const val MODEL_SIZE = 5_000_000L

        fun getParameters(): List<Double> =
            mutableListOf<Double>().apply {
                for (i in 0 until MODEL_SIZE) {
                    add(Random.nextDouble())
                }
            }

        fun fit(parameters: List<Double>): List<Double> {
            runBlocking {
                delay(10.seconds)
            }

            return getParameters()
        }
    }
}
