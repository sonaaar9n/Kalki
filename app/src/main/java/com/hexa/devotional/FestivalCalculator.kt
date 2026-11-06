
package com.hexa.devotional

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class PurnimantaMonth {
    CHAITRA,
    VAISHAKHA,
    JYESHTHA,
    ASHADHA,
    SHRAVANA,
    BHADRAPADA,
    ASHWINA,
    KARTIKA,
    MARGASHIRSHA,
    PAUSHA,
    MAGHA,
    PHALGUNA,
    UNKNOWN
}

data class TithiInterval(
    val tithiIndex: Int,
    val month: PurnimantaMonth,
    val startMillis: Long,
    val endMillis: Long
)

data class FestivalDayInput(
    val date: LocalDate,
    val sunriseMillis: Long,
    val sunsetMillis: Long,
    val tithiIntervals: List<TithiInterval>,
    val bhadraEndMillis: Long? = null
)

data class FestivalOccurrence(
    val festivalName: String,
    val date: LocalDate,
    val startTime: String,
    val endTime: String,
    val status: String,
    val calculationNote: String
)

object FestivalCalculator {

    private val indiaZone = ZoneId.of("Asia/Kolkata")

    private const val PRADOSH_DURATION_MILLIS =
        144L * 60L * 1000L

    private const val AMAVASYA_INDEX = 29
    private const val TRAYODASHI_INDEX = 27
    private const val PURNIMA_INDEX = 14

    fun calculate(input: FestivalDayInput): List<FestivalOccurrence> {

        require(input.sunriseMillis < input.sunsetMillis) {
            "सूर्योदय सूर्यास्त से पहले होना चाहिए"
        }

        val results = mutableListOf<FestivalOccurrence>()

        val pradoshStart = input.sunsetMillis
        val pradoshEnd =
            input.sunsetMillis + PRADOSH_DURATION_MILLIS

        for (tithi in input.tithiIntervals) {

            if (tithi.startMillis >= tithi.endMillis) {
                continue
            }

            val overlapStart = maxOf(
                tithi.startMillis,
                pradoshStart
            )

            val overlapEnd = minOf(
                tithi.endMillis,
                pradoshEnd
            )

            if (overlapStart >= overlapEnd) {
                continue
            }

            when {
                tithi.month == PurnimantaMonth.KARTIKA &&
                    tithi.tithiIndex == TRAYODASHI_INDEX -> {

                    results.add(
                        createOccurrence(
                            name = "धनतेरस",
                            input = input,
                            start = overlapStart,
                            end = overlapEnd,
                            status = "प्रदोष में त्रयोदशी उपलब्ध",
                            note = "यह प्रदोष और त्रयोदशी का मिलान है। पूर्ण पूजा मुहूर्त के लिए लग्न की गणना बाकी है।"
                        )
                    )
                }

                tithi.month == PurnimantaMonth.KARTIKA &&
                    tithi.tithiIndex == AMAVASYA_INDEX -> {

                    results.add(
                        createOccurrence(
                            name = "दीपावली - लक्ष्मी पूजा",
                            input = input,
                            start = overlapStart,
                            end = overlapEnd,
                            status = "प्रदोष में अमावस्या उपलब्ध",
                            note = "यह प्रदोष और अमावस्या का मिलान है। अंतिम पूजा मुहूर्त के लिए अतिरिक्त नियम और लग्न गणना बाकी है।"
                        )
                    )
                }

                tithi.month == PurnimantaMonth.PHALGUNA &&
                    tithi.tithiIndex == PURNIMA_INDEX -> {

                    val bhadraEnd = input.bhadraEndMillis

                    if (bhadraEnd == null) {
                        results.add(
                            FestivalOccurrence(
                                festivalName = "होलिका दहन",
                                date = input.date,
                                startTime = "अभी निर्धारित नहीं",
                                endTime = "अभी निर्धारित नहीं",
                                status = "भद्रा गणना आवश्यक",
                                calculationNote = "पूर्णिमा प्रदोष में उपलब्ध है, लेकिन सुरक्षित मुहूर्त तय करने से पहले भद्रा की समाप्ति जानना जरूरी है।"
                            )
                        )
                    } else {

                        val safeStart = maxOf(
                            overlapStart,
                            bhadraEnd
                        )

                        if (safeStart < overlapEnd) {
                            results.add(
                                createOccurrence(
                                    name = "होलिका दहन",
                                    input = input,
                                    start = safeStart,
                                    end = overlapEnd,
                                    status = "भद्रा के बाद प्रदोष में पूर्णिमा उपलब्ध",
                                    note = "यह प्रारंभिक समय है। पारंपरिक भद्रा अपवादों और क्षेत्रीय नियमों की पुष्टि बाकी है।"
                                )
                            )
                        } else {
                            results.add(
                                FestivalOccurrence(
                                    festivalName = "होलिका दहन",
                                    date = input.date,
                                    startTime = "अभी निर्धारित नहीं",
                                    endTime = "अभी निर्धारित नहीं",
                                    status = "विशेष नियम की जाँच आवश्यक",
                                    calculationNote = "प्रदोष और भद्रा के मेल के कारण अन्य पारंपरिक नियमों की जाँच जरूरी है।"
                                )
                            )
                        }
                    }
                }
            }
        }

        return results
    }

    private fun createOccurrence(
        name: String,
        input: FestivalDayInput,
        start: Long,
        end: Long,
        status: String,
        note: String
    ): FestivalOccurrence {

        return FestivalOccurrence(
            festivalName = name,
            date = input.date,
            startTime = formatTime(start),
            endTime = formatTime(end),
            status = status,
            calculationNote = note
        )
    }

    private fun formatTime(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(indiaZone)
            .toLocalTime()
            .toString()
            .take(5)
    }
}
