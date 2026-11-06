
package com.hexa.devotional

enum class HinduMonth(
    val hindiName: String
) {
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
    val calculationStatus: String
)

object HinduMonthCalculator {

    /**
     * पूर्णिमांत महीने की पहचान।
     *
     * monthNumber:
     * 1 = चैत्र ... 12 = फाल्गुन
     *
     * यह अभी खगोलीय महीने की गणना नहीं करता।
     * monthNumber और adhikaMonth की जानकारी
     * अगले खगोलीय चरण से प्राप्त होगी।
     */
    fun fromAstronomicalData(
        monthNumber: Int,
        adhikaMonth: Boolean = false
    ): HinduMonthResult {

        if (monthNumber !in 1..12) {
            return HinduMonthResult(
                month = HinduMonth.UNKNOWN,
                isAdhikaMonth = false,
                calculationStatus = "अमान्य महीने का नंबर"
            )
        }

        val month = HinduMonth.values()[monthNumber - 1]

        return HinduMonthResult(
            month = month,
            isAdhikaMonth = adhikaMonth,
            calculationStatus = "महीने की पहचान पूर्ण"
        )
    }
}
