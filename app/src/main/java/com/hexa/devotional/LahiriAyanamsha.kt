
package com.hexa.devotional

import java.time.Instant
import kotlin.math.floor

data class SiderealPosition(
    val tropicalLongitude: Double,
    val ayanamsha: Double,
    val siderealLongitude: Double,
    val rashiNumber: Int,
    val degreesInRashi: Double
)

object LahiriAyanamsha {

    private const val J2000_JULIAN_DAY = 2451545.0

    // Lahiri / Chitrapaksha reference at J2000.
    private const val INITIAL_AYANAMSHA_ARC_SECONDS =
        23.0 * 3600.0 + 51.0 * 60.0 + 25.532

    // Precession coefficients in arcseconds per Julian century.
    private const val PRECESSION_RATE = 5029.0966
    private const val PRECESSION_SECOND_ORDER = 1.11161

    /**
     * Returns approximate mean Lahiri ayanamsha in degrees.
     *
     * This polynomial does not include the complete
     * true-nutation correction used by full ephemeris systems.
     */
    fun calculate(instant: Instant): Double {

        val julianDay =
            instant.toEpochMilli() / 86400000.0 + 2440587.5

        val centuries =
            (julianDay - J2000_JULIAN_DAY) / 36525.0

        val arcSeconds =
            INITIAL_AYANAMSHA_ARC_SECONDS +
                PRECESSION_RATE * centuries +
                PRECESSION_SECOND_ORDER *
                centuries * centuries

        return arcSeconds / 3600.0
    }

    /**
     * Converts tropical longitude into Lahiri sidereal longitude.
     *
     * Longitude must be between 0 and 360 degrees.
     */
    fun siderealLongitude(
        tropicalLongitude: Double,
        instant: Instant
    ): Double {

        require(tropicalLongitude in 0.0..360.0)

        val result =
            tropicalLongitude - calculate(instant)

        return normalize(result)
    }

    /**
     * Returns sidereal longitude and zodiac sign information.
     *
     * Rashi numbering:
     * 1 = मेष, 2 = वृषभ ... 12 = मीन
     */
    fun calculatePosition(
        tropicalLongitude: Double,
        instant: Instant
    ): SiderealPosition {

        val ayanamsha = calculate(instant)

        val sidereal = normalize(
            tropicalLongitude - ayanamsha
        )

        val rashiNumber =
            floor(sidereal / 30.0).toInt() + 1

        val degreesInRashi =
            sidereal % 30.0

        return SiderealPosition(
            tropicalLongitude = tropicalLongitude,
            ayanamsha = ayanamsha,
            siderealLongitude = sidereal,
            rashiNumber = rashiNumber,
            degreesInRashi = degreesInRashi
        )
    }

    private fun normalize(angle: Double): Double {
        return ((angle % 360.0) + 360.0) % 360.0
    }
}
