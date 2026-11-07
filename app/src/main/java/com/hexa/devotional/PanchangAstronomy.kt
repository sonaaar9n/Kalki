
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
import kotlin.math.abs
import kotlin.math.floor

data class PanchangDailyData(
    val tithi: String,
    val paksha: String,
    val tithiNumber: Int,
    val sunrise: String,
    val sunset: String,
    val calculationStatus: String,
    val tithiStart: String = "उपलब्ध नहीं",
    val tithiEnd: String = "उपलब्ध नहीं",
    val nakshatra: String = "उपलब्ध नहीं",
    val nakshatraPada: Int = 0,
    val nakshatraStart: String = "उपलब्ध नहीं",
    val nakshatraEnd: String = "उपलब्ध नहीं",
    val yoga: String = "उपलब्ध नहीं",
    val yogaStart: String = "उपलब्ध नहीं",
    val yogaEnd: String = "उपलब्ध नहीं",
    val karana: String = "उपलब्ध नहीं",
    val karanaStart: String = "उपलब्ध नहीं",
    val karanaEnd: String = "उपलब्ध नहीं"
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

    private const val TITHI_SPAN = 12.0
    private const val KARANA_SPAN = 6.0
    private const val NAKSHATRA_SPAN = 360.0 / 27.0
    private const val YOGA_SPAN = 360.0 / 27.0

    private val tithiNames = listOf(
        "प्रतिपदा", "द्वितीया", "तृतीया",
        "चतुर्थी", "पंचमी", "षष्ठी",
        "सप्तमी", "अष्टमी", "नवमी",
        "दशमी", "एकादशी", "द्वादशी",
        "त्रयोदशी", "चतुर्दशी", "पूर्णिमा"
    )

    private val nakshatraNames = listOf(
        "अश्विनी", "भरणी", "कृत्तिका",
        "रोहिणी", "मृगशिरा", "आर्द्रा",
        "पुनर्वसु", "पुष्य", "आश्लेषा",
        "मघा", "पूर्वा फाल्गुनी", "उत्तरा फाल्गुनी",
        "हस्त", "चित्रा", "स्वाती",
        "विशाखा", "अनुराधा", "ज्येष्ठा",
        "मूल", "पूर्वाषाढ़ा", "उत्तराषाढ़ा",
        "श्रवण", "धनिष्ठा", "शतभिषा",
        "पूर्वाभाद्रपद", "उत्तराभाद्रपद", "रेवती"
    )

    private val yogaNames = listOf(
        "विष्कम्भ", "प्रीति", "आयुष्मान",
        "सौभाग्य", "शोभन", "अतिगण्ड",
        "सुकर्मा", "धृति", "शूल",
        "गण्ड", "वृद्धि", "ध्रुव",
        "व्याघात", "हर्षण", "वज्र",
        "सिद्धि", "व्यतीपात", "वरीयान",
        "परिघ", "शिव", "सिद्ध",
        "साध्य", "शुभ", "शुक्ल",
        "ब्रह्म", "इन्द्र", "वैधृति"
    )

    private val movableKaranaNames = listOf(
        "बव", "बालव", "कौलव", "तैतिल",
        "गर", "वणिज", "विष्टि (भद्रा)"
    )

    private val fixedKaranaNames = listOf(
        "किंस्तुघ्न", "शकुनि", "चतुष्पद", "नाग"
    )

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double
    ): PanchangDailyData {

        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Invalid latitude"
        }

        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Invalid longitude"
        }

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

        val sunriseMillis = sunrise.toMillisecondsSince1970()
        val instant = Instant.ofEpochMilli(sunriseMillis)

        val sunTropical = normalizeAngle(sunPosition(sunrise).elon)
        val moonTropical = normalizeAngle(eclipticGeoMoon(sunrise).lon)

        val sunSidereal = LahiriAyanamsha.siderealLongitude(
            sunTropical,
            instant
        )

        val moonSidereal = LahiriAyanamsha.siderealLongitude(
            moonTropical,
            instant
        )

        // Tithi
        val elongation = normalizeAngle(moonTropical - sunTropical)

        val tithiIndex = floor(elongation / TITHI_SPAN)
            .toInt()
            .coerceIn(0, 29)

        val tithiNumber = (tithiIndex % 15) + 1

        val paksha = if (tithiIndex < 15) {
            "शुक्ल पक्ष"
        } else {
            "कृष्ण पक्ष"
        }

        val tithi = when (tithiIndex) {
            14 -> "पूर्णिमा"
            29 -> "अमावस्या"
            else -> tithiNames[tithiNumber - 1]
        }

        val tithiStart = findBoundary(
            sunriseMillis,
            tithiIndex * TITHI_SPAN,
            TITHI_SPAN,
            { time -> lunarElongation(time) },
            true
        )

        val tithiEnd = findBoundary(
            sunriseMillis,
            (tithiIndex + 1) * TITHI_SPAN,
            TITHI_SPAN,
            { time -> lunarElongation(time) },
            false
        )

        // Nakshatra and Pada
        val nakshatraIndex = floor(
            moonSidereal / NAKSHATRA_SPAN
        ).toInt().coerceIn(0, 26)

        val degreesInNakshatra =
            moonSidereal - nakshatraIndex * NAKSHATRA_SPAN

        val nakshatraPada = (
            floor(degreesInNakshatra / (NAKSHATRA_SPAN / 4.0))
                .toInt() + 1
            ).coerceIn(1, 4)

        val nakshatraStart = findBoundary(
            sunriseMillis,
            nakshatraIndex * NAKSHATRA_SPAN,
            NAKSHATRA_SPAN,
            { time -> moonSiderealLongitude(time) },
            true
        )

        val nakshatraEnd = findBoundary(
            sunriseMillis,
            (nakshatraIndex + 1) * NAKSHATRA_SPAN,
            NAKSHATRA_SPAN,
            { time -> moonSiderealLongitude(time) },
            false
        )

        // Yoga
        val yogaAngle = normalizeAngle(sunSidereal + moonSidereal)

        val yogaIndex = floor(yogaAngle / YOGA_SPAN)
            .toInt()
            .coerceIn(0, 26)

        val yogaStart = findBoundary(
            sunriseMillis,
            yogaIndex * YOGA_SPAN,
            YOGA_SPAN,
            { time -> yogaLongitude(time) },
            true
        )

        val yogaEnd = findBoundary(
            sunriseMillis,
            (yogaIndex + 1) * YOGA_SPAN,
            YOGA_SPAN,
            { time -> yogaLongitude(time) },
            false
        )

        // Karana
        val karanaIndex = floor(elongation / KARANA_SPAN)
            .toInt()
            .coerceIn(0, 59)

        val karana = karanaName(karanaIndex)

        val karanaStart = findBoundary(
            sunriseMillis,
            karanaIndex * KARANA_SPAN,
            KARANA_SPAN,
            { time -> lunarElongation(time) },
            true
        )

        val karanaEnd = findBoundary(
            sunriseMillis,
            (karanaIndex + 1) * KARANA_SPAN,
            KARANA_SPAN,
            { time -> lunarElongation(time) },
            false
        )

        val allBoundariesFound =
            tithiStart != null && tithiEnd != null &&
            nakshatraStart != null && nakshatraEnd != null &&
            yogaStart != null && yogaEnd != null &&
            karanaStart != null && karanaEnd != null

        return PanchangDailyData(
            tithi = tithi,
            paksha = paksha,
            tithiNumber = tithiNumber,
            sunrise = formatTime(sunrise),
            sunset = formatTime(sunset),
            calculationStatus = if (allBoundariesFound) {
                "खगोलीय गणना पूर्ण - सत्यापन आवश्यक"
            } else {
                "कुछ समय-सीमाओं की गणना अधूरी"
            },
            tithiStart = formatDateTime(tithiStart),
            tithiEnd = formatDateTime(tithiEnd),
            nakshatra = nakshatraNames[nakshatraIndex],
            nakshatraPada = nakshatraPada,
            nakshatraStart = formatDateTime(nakshatraStart),
            nakshatraEnd = formatDateTime(nakshatraEnd),
            yoga = yogaNames[yogaIndex],
            yogaStart = formatDateTime(yogaStart),
            yogaEnd = formatDateTime(yogaEnd),
            karana = karana,
            karanaStart = formatDateTime(karanaStart),
            karanaEnd = formatDateTime(karanaEnd)
        )
    }

    private fun karanaName(index: Int): String {
        return when (index) {
            0 -> fixedKaranaNames[0]
            in 1..56 -> movableKaranaNames[(index - 1) % 7]
            57 -> fixedKaranaNames[1]
            58 -> fixedKaranaNames[2]
            else -> fixedKaranaNames[3]
        }
    }

    private fun lunarElongation(time: Time): Double {
        val sun = sunPosition(time).elon
        val moon = eclipticGeoMoon(time).lon
        return normalizeAngle(moon - sun)
    }

    private fun moonSiderealLongitude(time: Time): Double {
        val tropical = normalizeAngle(eclipticGeoMoon(time).lon)
        val instant = Instant.ofEpochMilli(
            time.toMillisecondsSince1970()
        )

        return LahiriAyanamsha.siderealLongitude(tropical, instant)
    }

    private fun yogaLongitude(time: Time): Double {
        val sun = normalizeAngle(sunPosition(time).elon)
        val moon = normalizeAngle(eclipticGeoMoon(time).lon)

        val instant = Instant.ofEpochMilli(
            time.toMillisecondsSince1970()
        )

        val sunSidereal =
            LahiriAyanamsha.siderealLongitude(sun, instant)

        val moonSidereal =
            LahiriAyanamsha.siderealLongitude(moon, instant)

        return normalizeAngle(sunSidereal + moonSidereal)
    }

    /**
     * Finds the nearest previous or next angular boundary.
     * The expected segment span is checked to avoid accepting
     * an unrelated angular crossing.
     */
    private fun findBoundary(
        startMillis: Long,
        targetAngle: Double,
        span: Double,
        angleAt: (Time) -> Double,
        searchBackward: Boolean
    ): Long? {

        require(span > 0.0 && span < 180.0)

        val target = normalizeAngle(targetAngle)

        fun differenceAt(millis: Long): Double {
            return signedAngleDifference(
                angleAt(createTime(millis)),
                target
            )
        }

        var currentMillis = startMillis
        var currentDifference = differenceAt(currentMillis)

        if (abs(currentDifference) < 0.000001) {
            return currentMillis
        }

        // The target must be within the current segment.
        if (abs(currentDifference) > span + 0.000001) {
            return null
        }

        repeat(160) {
            val candidateMillis = if (searchBackward) {
                currentMillis - SEARCH_STEP_MILLIS
            } else {
                currentMillis + SEARCH_STEP_MILLIS
            }

            val candidateDifference = differenceAt(candidateMillis)

            val bracketFound = if (searchBackward) {
                candidateDifference <= 0.0 &&
                    currentDifference >= 0.0
            } else {
                currentDifference <= 0.0 &&
                    candidateDifference >= 0.0
            }

            if (bracketFound) {
                val low = minOf(currentMillis, candidateMillis)
                val high = maxOf(currentMillis, candidateMillis)

                return bisectBoundary(
                    low,
                    high,
                    target,
                    angleAt
                )
            }

            currentMillis = candidateMillis
            currentDifference = candidateDifference
        }

        return null
    }

    private fun bisectBoundary(
        lowerMillis: Long,
        upperMillis: Long,
        targetAngle: Double,
        angleAt: (Time) -> Double
    ): Long {

        var low = lowerMillis
        var high = upperMillis

        while (high - low > BISECTION_TOLERANCE_MILLIS) {
            val middle = low + (high - low) / 2

            val difference = signedAngleDifference(
                angleAt(createTime(middle)),
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
            utc.second + utc.nano / 1_000_000_000.0
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
