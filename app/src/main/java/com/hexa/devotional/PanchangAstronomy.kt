
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

data class PanchangDailyData(
    val tithi: String,
    val paksha: String,
    val tithiNumber: Int,
    val sunrise: String,
    val sunset: String,
    val calculationStatus: String
)

object PanchangAstronomy {

    private val indiaZone = ZoneId.of("Asia/Kolkata")

    private val timeFormat =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

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

        val sunLongitude = sunPosition(sunrise).elon
        val moonLongitude = eclipticGeoMoon(sunrise).lon

        val difference = normalizeAngle(
            moonLongitude - sunLongitude
        )

        val tithiIndex = (difference / 12.0)
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

        return PanchangDailyData(
            tithi = tithi,
            paksha = paksha,
            tithiNumber = numberInPaksha,
            sunrise = formatTime(sunrise),
            sunset = formatTime(sunset),
            calculationStatus = "खगोलीय गणना पूर्ण"
        )
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

    private fun normalizeAngle(angle: Double): Double {
        return ((angle % 360.0) + 360.0) % 360.0
    }

    private fun formatTime(time: Time?): String {
        if (time == null) return "उपलब्ध नहीं"

        return Instant.ofEpochMilli(
            time.toMillisecondsSince1970()
        ).atZone(indiaZone).format(timeFormat)
    }
}
