package com.vardansoft.arduinoweather.repository

import com.vardansoft.arduinoweather.model.WeatherData
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WeatherDataRepository : MongoRepository<WeatherData, String> {

    // Find weather data by context
    fun findByContext(context: String): List<WeatherData>

    // Find weather data by token
    fun findByToken(token: String): List<WeatherData>

    // Find weather data within a time range
    fun findByTimestampBetween(start: LocalDateTime, end: LocalDateTime): List<WeatherData>

    // Find weather data by context and within a time range
    fun findByContextAndTimestampBetween(context: String, start: LocalDateTime, end: LocalDateTime): List<WeatherData>

    // Find the latest weather data by context
    fun findTopByContextOrderByTimestampDesc(context: String): WeatherData?
}