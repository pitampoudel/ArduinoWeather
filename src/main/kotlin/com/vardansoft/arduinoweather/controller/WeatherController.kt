package com.vardansoft.arduinoweather.controller

import com.vardansoft.arduinoweather.model.WeatherData
import com.vardansoft.arduinoweather.model.WeatherResponse
import com.vardansoft.arduinoweather.service.WeatherService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class WeatherController(private val weatherService: WeatherService) {

    private val logger = LoggerFactory.getLogger(WeatherController::class.java)

    @PostMapping("/submit")
    fun submitWeatherData(@Valid @RequestBody weatherData: WeatherData): ResponseEntity<WeatherResponse> {
        return try {
            logger.info("Received weather data from Arduino: context=${weatherData.context}, temp=${weatherData.temperature}°C, humidity=${weatherData.humidity}%")

            val result = weatherService.processWeatherData(weatherData)

            if (result) {
                logger.info("Weather data processed successfully")
                ResponseEntity.ok(
                    WeatherResponse(
                        success = true,
                        message = "Weather data received and processed successfully"
                    )
                )
            } else {
                logger.error("Failed to process weather data")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        WeatherResponse(
                            success = false,
                            message = "Failed to process weather data"
                        )
                    )
            }
        } catch (e: Exception) {
            logger.error("Error processing weather data: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    WeatherResponse(
                        success = false,
                        message = "Internal server error: ${e.message}"
                    )
                )
        }
    }
}