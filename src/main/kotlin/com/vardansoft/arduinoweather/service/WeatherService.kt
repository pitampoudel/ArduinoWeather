package com.vardansoft.arduinoweather.service

import com.vardansoft.arduinoweather.model.WeatherData
import com.vardansoft.arduinoweather.repository.WeatherDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WeatherService(private val weatherDataRepository: WeatherDataRepository) {

    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    // Kathmandu average weather constants for fallback when sensors fail
    companion object {
        private const val KATHMANDU_AVERAGE_TEMPERATURE = 20.0f // Average temperature in Celsius
        private const val KATHMANDU_AVERAGE_HUMIDITY = 65.0f    // Average humidity percentage
    }

    fun processWeatherData(weatherData: WeatherData): Boolean {
        return try {
            // Apply fallback logic for sensor failures (temperature and humidity = 0)
            val processedWeatherData = applyFallbackLogic(weatherData)

            // Log detailed weather information
            logWeatherData(processedWeatherData)

            // Check for alerts
            if (processedWeatherData.alert.isNotEmpty()) {
                logger.warn("ALERT: ${processedWeatherData.alert} - Level: ${processedWeatherData.level}")
            }

            // Save weather data to MongoDB
            val savedData = weatherDataRepository.save(processedWeatherData)
            logger.info("Weather data saved to database with ID: ${savedData.id}")

            true
        } catch (e: Exception) {
            logger.error("Error processing weather data: ${e.message}", e)
            false
        }
    }

    private fun applyFallbackLogic(weatherData: WeatherData): WeatherData {
        val needsTemperatureFallback = weatherData.temperature == 0.0f
        val needsHumidityFallback = weatherData.humidity == 0.0f

        return if (needsTemperatureFallback || needsHumidityFallback) {
            val fallbackTemp = if (needsTemperatureFallback) KATHMANDU_AVERAGE_TEMPERATURE else weatherData.temperature
            val fallbackHumidity = if (needsHumidityFallback) KATHMANDU_AVERAGE_HUMIDITY else weatherData.humidity

            logger.warn("Sensor failure detected! Applying Kathmandu averages - " +
                       "Temperature: ${if (needsTemperatureFallback) "0°C → ${fallbackTemp}°C (Kathmandu avg)" else "${weatherData.temperature}°C"}, " +
                       "Humidity: ${if (needsHumidityFallback) "0% → ${fallbackHumidity}% (Kathmandu avg)" else "${weatherData.humidity}%"}")

            weatherData.copy(
                temperature = fallbackTemp,
                humidity = fallbackHumidity
            )
        } else {
            weatherData
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
