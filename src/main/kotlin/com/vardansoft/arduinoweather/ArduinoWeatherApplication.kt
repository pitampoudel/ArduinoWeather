package com.vardansoft.arduinoweather

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ArduinoWeatherApplication

fun main(args: Array<String>) {
    runApplication<ArduinoWeatherApplication>(*args)
}
