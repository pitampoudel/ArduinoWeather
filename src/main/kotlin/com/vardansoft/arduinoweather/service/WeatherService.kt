package com.vardansoft.arduinoweather.service

import com.vardansoft.arduinoweather.model.WeatherData
import com.vardansoft.arduinoweather.repository.WeatherDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WeatherService(private val weatherDataRepository: WeatherDataRepository) {

    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    fun processWeatherData(weatherData: WeatherData): Boolean {
        return try {

            // Log detailed weather information
            logWeatherData(weatherData)

            // Check for alerts
            if (weatherData.alert.isNotEmpty()) {
                logger.warn("ALERT: ${weatherData.alert} - Level: ${weatherData.level}")
            }

            // Save weather data to MongoDB
            val savedData = weatherDataRepository.save(weatherData)
            logger.info("Weather data saved to database with ID: ${savedData.id}")

            true
        } catch (e: Exception) {
            logger.error("Error processing weather data: ${e.message}", e)
            false
        }
    }

    private fun logWeatherData(data: WeatherData) {
        logger.info("""
            Weather Data Received:
            Context: ${data.context}
            Temperature: ${data.temperature}°C
            Humidity: ${data.humidity}%
            CO Level: ${data.coLevel}
            CO2 Level: ${data.co2Level}
            Air Quality (Smoke): ${data.airQuality}
            Light Level: ${data.lightLevel}
            Pressure: ${data.pressure} hPa
            Signal Strength: ${data.signalStrength} dBm
            Alert: ${data.alert}
            Level: ${data.level}
            Timestamp: ${data.timestamp}
        """.trimIndent())

        // Log environmental conditions assessment
        assessEnvironmentalConditions(data)
    }

    private fun assessEnvironmentalConditions(data: WeatherData) {
        val conditions = mutableListOf<String>()

        // Temperature assessment
        when {
            data.temperature < 10 -> conditions.add("Cold temperature")
            data.temperature > 35 -> conditions.add("Hot temperature")
            else -> conditions.add("Normal temperature")
        }

        // Humidity assessment
        when {
            data.humidity < 30 -> conditions.add("Low humidity")
            data.humidity > 70 -> conditions.add("High humidity")
            else -> conditions.add("Normal humidity")
        }

        // Air quality assessment
        when {
            data.coLevel > 400 -> conditions.add("Dangerous CO levels")
            data.coLevel > 250 -> conditions.add("Elevated CO levels")
            data.co2Level > 600 -> conditions.add("High CO2 levels")
            data.airQuality > 300 -> conditions.add("Poor air quality (smoke detected)")
        }

        // Signal strength assessment
        when {
            data.signalStrength > -70 -> conditions.add("Good signal strength")
            data.signalStrength > -85 -> conditions.add("Fair signal strength")
            else -> conditions.add("Poor signal strength")
        }

        logger.info("Environmental Assessment: ${conditions.joinToString(", ")}")
    }

    // Methods for index page data
    fun getLatestReadings(): List<WeatherData> {
        return try {
            // Get all unique contexts first
            val allData = weatherDataRepository.findAll()
            val contexts = allData.map { it.context }.distinct()

            // Get latest reading for each context
            contexts.mapNotNull { context ->
                weatherDataRepository.findTopByContextOrderByTimestampDesc(context)
            }
        } catch (e: Exception) {
            logger.error("Error fetching latest readings: ${e.message}", e)
            emptyList()
        }
    }

    fun getRecentData(hours: Long = 24): List<WeatherData> {
        return try {
            val since = LocalDateTime.now().minusHours(hours)
            weatherDataRepository.findByTimestampBetween(since, LocalDateTime.now())
                .sortedByDescending { it.timestamp }
                .take(50) // Limit to 50 most recent entries
        } catch (e: Exception) {
            logger.error("Error fetching recent data: ${e.message}", e)
            emptyList()
        }
    }

    fun getRecentAlerts(hours: Long = 24): List<WeatherData> {
        return try {
            val since = LocalDateTime.now().minusHours(hours)
            weatherDataRepository.findByTimestampBetween(since, LocalDateTime.now())
                .filter { it.alert.isNotEmpty() }
                .sortedByDescending { it.timestamp }
                .take(20) // Limit to 20 most recent alerts
        } catch (e: Exception) {
            logger.error("Error fetching recent alerts: ${e.message}", e)
            emptyList()
        }
    }

    fun getDataSummary(): Map<String, Any> {
        return try {
            val allData = weatherDataRepository.findAll()
            val recentData = getRecentData(24)
            val lastUpdateTime = allData.maxByOrNull { it.timestamp }?.timestamp

            mapOf<String, Any>(
                "totalRecords" to allData.size,
                "activeContexts" to allData.map { it.context }.distinct().size,
                "recentRecords24h" to recentData.size,
                "lastUpdate" to (lastUpdateTime ?: LocalDateTime.now()),
                "hasLastUpdate" to (lastUpdateTime != null),
                "alertsLast24h" to recentData.count { it.alert.isNotEmpty() }
            )
        } catch (e: Exception) {
            logger.error("Error generating data summary: ${e.message}", e)
            mapOf<String, Any>(
                "totalRecords" to 0,
                "activeContexts" to 0,
                "recentRecords24h" to 0,
                "lastUpdate" to LocalDateTime.now(),
                "hasLastUpdate" to false,
                "alertsLast24h" to 0
            )
        }
    }
}
