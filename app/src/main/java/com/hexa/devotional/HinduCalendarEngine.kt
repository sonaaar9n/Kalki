package com.hexa.devotional

import java.time.LocalDate
import java.time.YearMonth

/**
 * HinduCalendarEngine
 *
 * पूरे Gregorian वर्ष या चुने हुए महीने के लिए
 * हिंदू पंचांग डेटा तैयार करता है।
 */
data class HinduCalendarDay(
    val date: LocalDate,
    val hinduMonth: HinduMonth,
    val isAdhikaMonth: Boolean,
    val panchang: PanchangDailyData,
    val calculationStatus: String
)

data class HinduCalendarResult(
    val year: Int,
    val days: List<HinduCalendarDay>,
    val calculationStatus: String
)

object HinduCalendarEngine {

    /**
     * किसी एक तारीख का पूरा उपलब्ध पंचांग तैयार करता है।
     */
    fun calculateDay(
        date: LocalDate,
        latitude: Double,
        longitude: Double
    ): HinduCalendarDay {

        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)

        val panchang = PanchangAstronomy.calculate(
            date = date,
            latitude = latitude,
            longitude = longitude
        )

        val monthResult = HinduMonthCalculator.calculate(date)

        val status = if (
            monthResult.month != HinduMonth.UNKNOWN &&
            panchang.calculationStatus.contains("पूर्ण")
        ) {
            "प्रारंभिक गणना पूर्ण"
        } else {
            "कुछ गणनाएँ अधूरी हैं"
        }

        return HinduCalendarDay(
            date = date,
            hinduMonth = monthResult.month,
            isAdhikaMonth = monthResult.isAdhikaMonth,
            panchang = panchang,
            calculationStatus = status
        )
    }

    /**
     * किसी Gregorian महीने का कैलेंडर तैयार करता है।
     */
    fun calculateMonth(
        yearMonth: YearMonth,
        latitude: Double,
        longitude: Double
    ): List<HinduCalendarDay> {

        val result = mutableListOf<HinduCalendarDay>()

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)

            result.add(
                calculateDay(
                    date = date,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }

        return result
    }

    /**
     * पूरे Gregorian वर्ष का कैलेंडर तैयार करता है।
     */
    fun calculateYear(
        year: Int,
        latitude: Double,
        longitude: Double
    ): HinduCalendarResult {

        require(year in 1900..2100)
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)

        val result = mutableListOf<HinduCalendarDay>()

        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        var date = startDate

        while (!date.isAfter(endDate)) {

            result.add(
                calculateDay(
                    date = date,
                    latitude = latitude,
                    longitude = longitude
                )
            )

            date = date.plusDays(1)
        }

        return HinduCalendarResult(
            year = year,
            days = result,
            calculationStatus = if (
                result.all {
                    it.hinduMonth != HinduMonth.UNKNOWN
                }
            ) {
                "वर्ष की प्रारंभिक गणना पूर्ण"
            } else {
                "कुछ तारीखों की गणना अधूरी है"
            }
        )
    }
}
