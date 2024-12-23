package com.github.eomgerm.adaptiveflclient

import kotlin.random.Random

class Model {
    companion object {
        const val MODEL_SIZE = 5_000_000L

        fun getParameters(): List<Double> {
            val parameters =
                mutableListOf<Double>().apply {
                    for (i in 0..MODEL_SIZE) {
                        add(Random.nextDouble())
                    }
                }

            return parameters
        }

        fun fit(parameters: List<Double>) = Pair(getParameters(), Random.nextLong(10_000, 1_000_000))

        fun evaluate(parameters: List<Double>) =
            Pair(
                Random.nextDouble(),
                Random.nextLong(
                    10_000,
                    1_000_000,
                ),
            )
    }
}
