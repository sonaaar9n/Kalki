
package com.hexa.devotional

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.sunPosition
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor

data class PanchangDailyData(
    val tithi: String,
    val paksha: String,
    val tithiNumber: Int,
    val sunrise: String,
    val sunset: String,
    val calculationStatus: String,
    val tithiStart: String = "उपलब्ध नहीं",
    val tithiEnd: String = "उपलब्ध नहीं"
)

object PanchangAstronomy {

    private val indiaZone = ZoneId.of("Asia/Kolkata")

    private val timeFormat =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    private val dateTimeFormat =
        DateTimeFormatter.ofPattern(
            "dd MMM, hh:mm a",
            Locale("hi", "IN")
        )

    private const val SEARCH_STEP_MILLIS = 30 * 60 * 1000L
    private const val BISECTION_TOLERANCE_MILLIS = 1000L

    private val tithiNames = listOf(
        "प्रतिपदा",
        "द्वितीया",
        "तृतीया",
        "चतुर्थी",
        "पंचमी",
        "षष्ठी",
        "सप्तमी",
        "अष्टमी",
        "नवमी",
        "दशमी",
        "एकादशी",
        "द्वादशी",
        "त्रयोदशी",
        "चतुर्दशी",
        "पूर्णिमा"
    )

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double
    ): PanchangDailyData {

        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)

        val observer = Observer(latitude, longitude, 0.0)
        val startTime = createTime(date)

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

        if (sunrise == null || sunset == null) {
            return PanchangDailyData(
                tithi = "उपलब्ध नहीं",
                paksha = "उपलब्ध नहीं",
                tithiNumber = 0,
                sunrise = formatTime(sunrise),
                sunset = formatTime(sunset),
                calculationStatus = "सूर्योदय/सूर्यास्त नहीं मिला"
            )
        }

        val difference = lunarElongation(sunrise)
        val tithiIndex = floor(difference / 12.0)
            .toInt()
            .coerceIn(0, 29)

        val numberInPaksha = (tithiIndex % 15) + 1

        val paksha = if (tithiIndex < 15) {
            "शुक्ल पक्ष"
        } else {
            "कृष्ण पक्ष"
        }

        val tithi = if (tithiIndex == 29) {
            "अमावस्या"
        } else {
            tithiNames[numberInPaksha - 1]
        }

        val previousBoundary = floor(difference / 12.0) * 12.0
        val nextBoundary = normalizeAngle(previousBoundary + 12.0)

        val startMillis = findBoundary(
            sunrise,
            previousBoundary,
            searchBackward = true
        )

        val endMillis = findBoundary(
            sunrise,
            nextBoundary,
            searchBackward = false
        )

        return PanchangDailyData(
            tithi = tithi,
            paksha = paksha,
            tithiNumber = numberInPaksha,
            sunrise = formatTime(sunrise),
            sunset = formatTime(sunset),
            calculationStatus = if (
                startMillis != null && endMillis != null
            ) {
                "खगोलीय गणना पूर्ण"
            } else {
                "तिथि सीमा की गणना अधूरी"
            },
            tithiStart = formatDateTime(startMillis),
            tithiEnd = formatDateTime(endMillis)
        )
    }

    private fun lunarElongation(time: Time): Double {
        val sunLongitude = sunPosition(time).elon
        val moonLongitude = eclipticGeoMoon(time).lon

        return normalizeAngle(moonLongitude - sunLongitude)
    }

    private fun findBoundary(
        startTime: Time,
        targetAngle: Double,
        searchBackward: Boolean
    ): Long? {

        val startMillis = startTime.toMillisecondsSince1970()

        fun differenceAt(millis: Long): Double {
            val time = createTime(millis)
            return signedAngleDifference(
                lunarElongation(time),
                targetAngle
            )
        }

        var newerMillis = startMillis
        var newerDifference = differenceAt(newerMillis)

        if (kotlin.math.abs(newerDifference) < 0.000001) {
            return startMillis
        }

        var olderMillis = startMillis
        var olderDifference = newerDifference

        repeat(120) {
            val candidate = if (searchBackward) {
                olderMillis - SEARCH_STEP_MILLIS
            } else {
                olderMillis + SEARCH_STEP_MILLIS
            }

            val candidateDifference = differenceAt(candidate)

            val bracketFound = if (searchBackward) {
                candidateDifference <= 0.0 &&
                    newerDifference >= 0.0
            } else {
                newerDifference <= 0.0 &&
                    candidateDifference >= 0.0
            }

            if (bracketFound) {
                val low: Long
                val high: Long

                if (searchBackward) {
                    low = candidate
                    high = newerMillis
                } else {
                    low = olderMillis
                    high = candidate
                }

                return bisectBoundary(
                    low,
                    high,
                    targetAngle
                )
            }

            olderMillis = candidate
            olderDifference = candidateDifference
            newerMillis = candidate
            newerDifference = candidateDifference
        }

        return null
    }

    private fun bisectBoundary(
        lowerMillis: Long,
        upperMillis: Long,
        targetAngle: Double
    ): Long {

        var low = lowerMillis
        var high = upperMillis

        while (high - low > BISECTION_TOLERANCE_MILLIS) {
            val middle = low + (high - low) / 2
            val difference = signedAngleDifference(
                lunarElongation(createTime(middle)),
                targetAngle
            )

            if (difference < 0.0) {
                low = middle
            } else {
                high = middle
            }
        }

        return low + (high - low) / 2
    }

    private fun signedAngleDifference(
        angle: Double,
        target: Double
    ): Double {
        return ((angle - target + 540.0) % 360.0) - 180.0
    }

    private fun createTime(date: LocalDate): Time {
        val utc = LocalDateTime.ofInstant(
            date.atStartOfDay(indiaZone).toInstant(),
            ZoneOffset.UTC
        )

        return Time(
            utc.year,
            utc.monthValue,
            utc.dayOfMonth,
            utc.hour,
            utc.minute,
            utc.second.toDouble()
        )
    }

    private fun createTime(millis: Long): Time {
        val utc = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneOffset.UTC
        )

        return Time(
            utc.year,
            utc.monthValue,
            utc.dayOfMonth,
            utc.hour,
            utc.minute,
            utc.second +
                utc.nano / 1_000_000_000.0
        )
    }

    private fun normalizeAngle(angle: Double): Double {
        return ((angle % 360.0) + 360.0) % 360.0
    }

    private fun formatTime(time: Time?): String {
        if (time == null) return "उपलब्ध नहीं"

        return Instant.ofEpochMilli(
            time.toMillisecondsSince1970()
        ).atZone(indiaZone).format(timeFormat)
    }

    private fun formatDateTime(millis: Long?): String {
        if (millis == null) return "उपलब्ध नहीं"

        return Instant.ofEpochMilli(millis)
            .atZone(indiaZone)
            .format(dateTimeFormat)
    }
}
