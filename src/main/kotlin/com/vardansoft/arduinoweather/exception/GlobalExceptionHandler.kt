package com.vardansoft.arduinoweather.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.vardansoft.arduinoweather.model.WeatherResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import jakarta.servlet.http.HttpServletRequest

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    private val objectMapper = ObjectMapper()

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<WeatherResponse> {

        // Get the invalid object that was sent
        val invalidObject = ex.bindingResult.target

        // Log the validation errors and the actual data that was sent
        val validationErrors = ex.bindingResult.fieldErrors.joinToString(", ") {
            "${it.field}: ${it.defaultMessage}"
        }

        try {
            val requestDataJson = objectMapper.writeValueAsString(invalidObject)
            logger.error("Validation failed for request. Errors: [$validationErrors]. Client sent: $requestDataJson")
        } catch (jsonException: Exception) {
            logger.error("Validation failed for request. Errors: [$validationErrors]. Client sent: $invalidObject (JSON serialization failed: ${jsonException.message})")
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                WeatherResponse(
                    success = false,
                    message = "Validation failed: $validationErrors"
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<WeatherResponse> {

        // Log the JSON parsing error with available information
        val requestInfo = "URI: ${request.requestURI}, Method: ${request.method}, Content-Type: ${request.contentType}"
        logger.error("JSON parsing failed for request. Error: ${ex.message}. Request info: $requestInfo")

        // Try to extract more details from the exception
        val rootCause = ex.rootCause
        if (rootCause != null) {
            logger.error("Root cause of JSON parsing error: ${rootCause.message}")
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                WeatherResponse(
                    success = false,
                    message = "Invalid JSON format or data type mismatch: ${ex.localizedMessage}"
                )
            )
    }
}
