package com.vardansoft.arduinoweather.controller

import com.vardansoft.arduinoweather.model.WeatherData
import com.vardansoft.arduinoweather.model.WeatherResponse
import com.vardansoft.arduinoweather.service.WeatherService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Controller
@RequestMapping
class WeatherController(private val weatherService: WeatherService) {

    private val logger = LoggerFactory.getLogger(WeatherController::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @GetMapping("/")
    fun indexPage(model: Model): String {
        return try {
            val latestReadings = weatherService.getLatestReadings()
            val recentAlerts = weatherService.getRecentAlerts()
            val summary = weatherService.getDataSummary()
            val currentTime = LocalDateTime.now()

            model.addAttribute("latestReadings", latestReadings)
            model.addAttribute("recentAlerts", recentAlerts)
            model.addAttribute("summary", summary)
            model.addAttribute("currentTime", currentTime)

            "index"
        } catch (e: Exception) {
            logger.error("Error generating index page: ${e.message}", e)
            model.addAttribute("error", e.message)
            "error"
        }
    }

    @PostMapping("/submit")
    @ResponseBody
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
