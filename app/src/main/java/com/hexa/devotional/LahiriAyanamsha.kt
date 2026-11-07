
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

    // Mean precession coefficients in arcseconds.
    private const val PRECESSION_RATE = 5029.0966
    private const val PRECESSION_SECOND_ORDER = 1.11161

    /**
     * Approximate mean Lahiri ayanamsha in degrees.
     *
     * This polynomial does not include complete
     * nutation or a full ephemeris correction.
     */
    fun calculate(instant: Instant): Double {

        val julianDay =
            2440587.5 +
                instant.epochSecond / 86400.0 +
                instant.nano / 86400000000000.0

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
     * Converts tropical longitude into sidereal longitude.
     * Input range: 0° to 360°.
     */
    fun siderealLongitude(
        tropicalLongitude: Double,
        instant: Instant
    ): Double {

        require(
            tropicalLongitude.isFinite() &&
                tropicalLongitude in 0.0..360.0
        ) {
            "Tropical longitude must be between 0 and 360 degrees."
        }

        return normalize(
            tropicalLongitude - calculate(instant)
        )
    }

    /**
     * Returns sidereal longitude and Rashi information.
     *
     * 1 = Mesha
     * 2 = Vrishabha
     * 3 = Mithuna
     * 4 = Karka
     * 5 = Simha
     * 6 = Kanya
     * 7 = Tula
     * 8 = Vrischika
     * 9 = Dhanu
     * 10 = Makara
     * 11 = Kumbha
     * 12 = Meena
     */
    fun calculatePosition(
        tropicalLongitude: Double,
        instant: Instant
    ): SiderealPosition {

        require(
            tropicalLongitude.isFinite() &&
                tropicalLongitude in 0.0..360.0
        ) {
            "Tropical longitude must be between 0 and 360 degrees."
        }

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
