package com.vardansoft.arduinoweather.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "weather_data")
data class WeatherData(
    @Id
    val id: String? = null,

    @JsonProperty("ctx")
    @field:NotBlank(message = "Context is required")
    val context: String,

    @JsonProperty("t")
    @field:NotNull(message = "Temperature is required")
    val temperature: Float,

    @JsonProperty("h")
    @field:NotNull(message = "Humidity is required")
    val humidity: Float,

    @JsonProperty("co")
    @field:NotNull(message = "CO level is required")
    val coLevel: Int,

    @JsonProperty("c2")
    @field:NotNull(message = "CO2 level is required")
    val co2Level: Int,

    @JsonProperty("aq")
    @field:NotNull(message = "Air quality is required")
    val airQuality: Int,

    @JsonProperty("ldr")
    @field:NotNull(message = "Light sensor reading is required")
    val lightLevel: Int,

    @JsonProperty("prs")
    @field:NotNull(message = "Pressure is required")
    val pressure: Float,

    @JsonProperty("sig")
    @field:NotNull(message = "Signal strength is required")
    val signalStrength: Int,

    @JsonProperty("al")
    val alert: String = "",

    @JsonProperty("lvl")
    val level: String = "",

    @JsonProperty("tok")
    @field:NotBlank(message = "Token is required")
    val token: String,

    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class WeatherResponse(
    val success: Boolean,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
