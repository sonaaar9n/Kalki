
package com.hexa.devotional

import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.sunPosition
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.floor

enum class HinduMonth(val hindiName: String) {
    CHAITRA("चैत्र"),
    VAISHAKHA("वैशाख"),
    JYESHTHA("ज्येष्ठ"),
    ASHADHA("आषाढ़"),
    SHRAVANA("श्रावण"),
    BHADRAPADA("भाद्रपद"),
    ASHWINA("आश्विन"),
    KARTIKA("कार्तिक"),
    MARGASHIRSHA("मार्गशीर्ष"),
    PAUSHA("पौष"),
    MAGHA("माघ"),
    PHALGUNA("फाल्गुन"),
    UNKNOWN("अज्ञात")
}

data class HinduMonthResult(
    val month: HinduMonth,
    val isAdhikaMonth: Boolean,
    val calculationStatus: String,
    val previousPurnimaMillis: Long? = null,
    val nextPurnimaMillis: Long? = null,
    val referenceAmavasyaMillis: Long? = null
)

object HinduMonthCalculator {

    private val indiaZone = ZoneId.of("Asia/Kolkata")

    private const val DAY_MILLIS = 86400000L
    private const val SEARCH_STEP = 6 * 60 * 60 * 1000L
    private const val TOLERANCE = 1000L

    private data class PhaseEvent(
        val millis: Long,
        val phase: Double
    )

    /**
     * North Indian Purnimanta month calculation.
     *
     * A Purnimanta month runs from one full moon
     * to the next full moon.
     *
     * The new moon inside that interval is used
     * to identify the lunar month through the
     * sidereal solar sign.
     */
    fun calculate(date: LocalDate): HinduMonthResult {

        val dateMillis = date.atStartOfDay(indiaZone)
            .toInstant()
            .toEpochMilli()

        val events = findPhaseEvents(
            dateMillis - 65L * DAY_MILLIS,
            dateMillis + 65L * DAY_MILLIS
        )

        val fullMoons = events.filter {
            abs(it.phase - 180.0) < 0.0001
        }

        val newMoons = events.filter {
            abs(it.phase) < 0.0001 ||
                abs(it.phase - 360.0) < 0.0001
        }

        val previousFullMoon = fullMoons.lastOrNull {
            it.millis <= dateMillis
        }

        val nextFullMoon = fullMoons.firstOrNull {
            it.millis > dateMillis
        }

        if (previousFullMoon == null || nextFullMoon == null) {
            return unknownResult("पूर्णिमा की सीमा नहीं मिली")
        }

        val referenceNewMoon = newMoons.firstOrNull {
            it.millis > previousFullMoon.millis &&
                it.millis < nextFullMoon.millis
        }

        if (referenceNewMoon == null) {
            return unknownResult("अमावस्या की सीमा नहीं मिली")
        }

        val previousNewMoon = newMoons.lastOrNull {
            it.millis < referenceNewMoon.millis
        }

        val nextNewMoon = newMoons.firstOrNull {
            it.millis > referenceNewMoon.millis
        }

        if (previousNewMoon == null || nextNewMoon == null) {
            return unknownResult("आसपास की अमावस्या नहीं मिली")
        }

        val sunLongitude = sunPosition(
            createTime(referenceNewMoon.millis)
        ).elon

        val siderealSun = LahiriAyanamsha.siderealLongitude(
            normalize(sunLongitude),
            Instant.ofEpochMilli(referenceNewMoon.millis)
        )

        val solarSign = floor(siderealSun / 30.0)
            .toInt()
            .coerceIn(0, 11)

        val month = monthFromSolarSign(solarSign)

        val previousSunSign = solarSignAt(previousNewMoon.millis)
        val nextSunSign = solarSignAt(nextNewMoon.millis)

        val adhika = previousSunSign != null &&
            nextSunSign != null &&
            previousSunSign == solarSign &&
            nextSunSign == solarSign

        return HinduMonthResult(
            month = month,
            isAdhikaMonth = adhika,
            calculationStatus =
                "प्रारंभिक पूर्णिमांत खगोलीय गणना पूर्ण",
            previousPurnimaMillis = previousFullMoon.millis,
            nextPurnimaMillis = nextFullMoon.millis,
            referenceAmavasyaMillis = referenceNewMoon.millis
        )
    }

    private fun monthFromSolarSign(sign: Int): HinduMonth {
        return when (sign) {
            0 -> HinduMonth.VAISHAKHA
            1 -> HinduMonth.JYESHTHA
            2 -> HinduMonth.ASHADHA
            3 -> HinduMonth.SHRAVANA
            4 -> HinduMonth.BHADRAPADA
            5 -> HinduMonth.ASHWINA
            6 -> HinduMonth.KARTIKA
            7 -> HinduMonth.MARGASHIRSHA
            8 -> HinduMonth.PAUSHA
            9 -> HinduMonth.MAGHA
            10 -> HinduMonth.PHALGUNA
            11 -> HinduMonth.CHAITRA
            else -> HinduMonth.UNKNOWN
        }
    }

    private fun solarSignAt(millis: Long): Int? {
        return try {
            val tropical = normalize(
                sunPosition(createTime(millis)).elon
            )

            val sidereal = LahiriAyanamsha.siderealLongitude(
                tropical,
                Instant.ofEpochMilli(millis)
            )

            floor(sidereal / 30.0).toInt().coerceIn(0, 11)
        } catch (_: Exception) {
            null
        }
    }

    private fun findPhaseEvents(
        startMillis: Long,
        endMillis: Long
    ): List<PhaseEvent> {

        val result = mutableListOf<PhaseEvent>()

        var previousMillis = startMillis
        var previousPhase = lunarPhase(previousMillis)

        var currentMillis = previousMillis + SEARCH_STEP

        while (currentMillis <= endMillis) {

            val currentPhase = lunarPhase(currentMillis)

            val forwardChange = normalize(
                currentPhase - previousPhase
            )

            if (forwardChange < 30.0) {

                val targets = listOf(0.0, 180.0)

                for (target in targets) {

                    val distance = normalize(target - previousPhase)

                    if (distance <= forwardChange) {

                        val eventMillis = findPhaseBoundary(
                            previousMillis,
                            currentMillis,
                            target
                        )

                        if (result.none {
                                abs(it.millis - eventMillis) < 60000L
                            }) {
                            result.add(
                                PhaseEvent(eventMillis, target)
                            )
                        }
                    }
                }
            }

            previousMillis = currentMillis
            previousPhase = currentPhase
            currentMillis += SEARCH_STEP
        }

        return result.sortedBy { it.millis }
    }

    private fun findPhaseBoundary(
        startMillis: Long,
        endMillis: Long,
        target: Double
    ): Long {

        var low = startMillis
        var high = endMillis

        while (high - low > TOLERANCE) {

            val middle = low + (high - low) / 2

            val phase = lunarPhase(middle)

            val difference = normalize(
                phase - target + 180.0
            ) - 180.0

            if (difference < 0.0) {
                low = middle
            } else {
                high = middle
            }
        }

        return low + (high - low) / 2
    }

    private fun lunarPhase(millis: Long): Double {
        val time = createTime(millis)

        val moon = eclipticGeoMoon(time).lon
        val sun = sunPosition(time).elon

        return normalize(moon - sun)
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
            utc.second + utc.nano / 1_000_000_000.0
        )
    }

    private fun normalize(angle: Double): Double {
        return ((angle % 360.0) + 360.0) % 360.0
    }

    private fun unknownResult(reason: String): HinduMonthResult {
        return HinduMonthResult(
            month = HinduMonth.UNKNOWN,
            isAdhikaMonth = false,
            calculationStatus = reason
        )
    }

    /**
     * Compatibility helper for older callers.
     */
    fun fromAstronomicalData(
        monthNumber: Int,
        adhikaMonth: Boolean = false
    ): HinduMonthResult {

        if (monthNumber !in 1..12) {
            return unknownResult("अमान्य महीने का नंबर")
        }

        return HinduMonthResult(
            month = HinduMonth.values()[monthNumber - 1],
            isAdhikaMonth = adhikaMonth,
            calculationStatus = "महीने की पहचान पूर्ण"
        )
    }
}
