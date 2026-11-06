package com.hexa.devotional

import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.sunPosition
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor

data class SankrantiResult(
    val fromRashi: String,
    val toRashi: String,
    val dateTime: String,
    val instantMillis: Long,
    val ayanamsha: Double,
    val calculationStatus: String
)

object SankrantiCalculator {

    private val indiaZone = ZoneId.of("Asia/Kolkata")

    private val dateTimeFormat =
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy, hh:mm:ss a",
            Locale.ENGLISH
        )

    private val rashiNames = listOf(
        "मेष",
        "वृषभ",
        "मिथुन",
        "कर्क",
        "सिंह",
        "कन्या",
        "तुला",
        "वृश्चिक",
        "धनु",
        "मकर",
        "कुंभ",
        "मीन"
    )

    private const val SEARCH_INTERVAL_MILLIS =
        6L * 60L * 60L * 1000L

    private const val TOLERANCE_MILLIS = 1000L

    /**
     * Finds the next sidereal solar sign ingress.
     *
     * Uses Astronomy Engine tropical solar longitude
     * and the project's Lahiri ayanamsha calculation.
     *
     * Maximum search period: 40 days.
     */
    fun findNextSankranti(
        start: Instant
    ): SankrantiResult? {

        val startMillis = start.toEpochMilli()

        val initialLongitude = siderealSunLongitude(start)

        val fromIndex = floor(
            initialLongitude / 30.0
        ).toInt().coerceIn(0, 11)

        val toIndex = (fromIndex + 1) % 12

        val targetAngle = normalize(
            (fromIndex + 1) * 30.0
        )

        var previousMillis = startMillis
        var previousDifference = signedDifference(
            initialLongitude,
            targetAngle
        )

        val maximumMillis =
            startMillis + 40L * 24L * 60L * 60L * 1000L

        var currentMillis =
            previousMillis + SEARCH_INTERVAL_MILLIS

        while (currentMillis <= maximumMillis) {

            val currentInstant =
                Instant.ofEpochMilli(currentMillis)

            val currentLongitude =
                siderealSunLongitude(currentInstant)

            val currentDifference = signedDifference(
                currentLongitude,
                targetAngle
            )

            if (
                previousDifference < 0.0 &&
                currentDifference >= 0.0
            ) {

                val boundaryMillis = findExactBoundary(
                    previousMillis,
                    currentMillis,
                    targetAngle
                )

                val boundaryInstant =
                    Instant.ofEpochMilli(boundaryMillis)

                return SankrantiResult(
                    fromRashi = rashiNames[fromIndex],
                    toRashi = rashiNames[toIndex],
                    dateTime = boundaryInstant
                        .atZone(indiaZone)
                        .format(dateTimeFormat),
                    instantMillis = boundaryMillis,
                    ayanamsha =
                        LahiriAyanamsha.calculate(boundaryInstant),
                    calculationStatus =
                        "प्रारंभिक खगोलीय संक्रांति गणना पूर्ण"
                )
            }

            previousMillis = currentMillis
            previousDifference = currentDifference

            currentMillis += SEARCH_INTERVAL_MILLIS
        }

        return null
    }

    /**
     * Gets the current sidereal solar sign.
     */
    fun getCurrentSolarRashi(
        instant: Instant
    ): String {

        val longitude = siderealSunLongitude(instant)

        val index = floor(
            longitude / 30.0
        ).toInt().coerceIn(0, 11)

        return rashiNames[index]
    }

    private fun siderealSunLongitude(
        instant: Instant
    ): Double {

        val time = createTime(instant)

        val tropicalLongitude =
            sunPosition(time).elon

        return LahiriAyanamsha.siderealLongitude(
            tropicalLongitude,
            instant
        )
    }

    private fun findExactBoundary(
        lowerMillis: Long,
        upperMillis: Long,
        targetAngle: Double
    ): Long {

        var low = lowerMillis
        var high = upperMillis

        while (high - low > TOLERANCE_MILLIS) {

            val middle = low + (high - low) / 2

            val instant =
                Instant.ofEpochMilli(middle)

            val longitude =
                siderealSunLongitude(instant)

            val difference = signedDifference(
                longitude,
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

    private fun createTime(instant: Instant): Time {

        val utc = LocalDateTime.ofInstant(
            instant,
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

    private fun signedDifference(
        angle: Double,
        target: Double
    ): Double {

        return ((angle - target + 540.0) % 360.0) - 180.0
    }

    private fun normalize(angle: Double): Double {
        return ((angle % 360.0) + 360.0) % 360.0
    }
}
