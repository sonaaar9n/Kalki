package com.hexa.devotional

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SolarLunarTimes(
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String
)

object PanchangAstronomy {

    private val indiaZone = ZoneId.of("Asia/Kolkata")

    private val timeFormat =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    fun calculateRiseSet(
        date: LocalDate,
        latitude: Double,
        longitude: Double
    ): SolarLunarTimes {

        val observer = Observer(
            latitude,
            longitude,
            0.0
        )

        val utcDateTime = LocalDateTime.ofInstant(
            date.atStartOfDay(indiaZone).toInstant(),
            ZoneOffset.UTC
        )

        val startTime = Time(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            utcDateTime.hour,
            utcDateTime.minute,
            utcDateTime.second.toDouble()
        )

        val sunrise = searchRiseSet(
            Body.Sun,
            observer,
            Direction.Rise,
            startTime,
            1.0
        )

        val sunset = searchRiseSet(
            Body.Sun,
            observer,
            Direction.Set,
            startTime,
            1.0
        )

        val moonrise = searchRiseSet(
            Body.Moon,
            observer,
            Direction.Rise,
            startTime,
            1.0
        )

        val moonset = searchRiseSet(
            Body.Moon,
            observer,
            Direction.Set,
            startTime,
            1.0
        )

        return SolarLunarTimes(
            sunrise = formatTime(sunrise?.toMillisecondsSince1970()),
            sunset = formatTime(sunset?.toMillisecondsSince1970()),
            moonrise = formatTime(moonrise?.toMillisecondsSince1970()),
            moonset = formatTime(moonset?.toMillisecondsSince1970())
        )
    }

    private fun formatTime(milliseconds: Long?): String {
        if (milliseconds == null) return "उपलब्ध नहीं"

        return Instant.ofEpochMilli(milliseconds)
            .atZone(indiaZone)
            .format(timeFormat)
    }
}
