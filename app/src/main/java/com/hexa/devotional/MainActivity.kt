
package com.hexa.devotional

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Gold = Color(0xFFE4C58A)
private val DarkBg = Color(0xFF12141B)
private val DarkCard = Color(0xFF222633)
private val LightBg = Color(0xFFF5F1E8)
private val LightCard = Color.White

private val IndiaZone = ZoneId.of("Asia/Kolkata")

data class Deity(val name: String, val subtitle: String)
data class Category(val name: String, val description: String)

private val deities = listOf(
    Deity("भगवान शिव", "ॐ नमः शिवाय"),
    Deity("श्री गणेश", "विघ्नहर्ता"),
    Deity("श्री विष्णु", "नारायण"),
    Deity("माता दुर्गा", "आदिशक्ति"),
    Deity("श्री कृष्ण", "गोविन्द"),
    Deity("श्री हनुमान", "बजरंगबली"),
    Deity("माता लक्ष्मी", "श्री"),
    Deity("माता सरस्वती", "विद्या की देवी"),
    Deity("श्री राम", "मर्यादा पुरुषोत्तम"),
    Deity("श्री कार्तिकेय", "स्कन्द"),
    Deity("श्री कश्यप ऋषि", "प्रजापति ऋषि"),
    Deity("श्री अग्रसेन मुनि", "अग्रवंश के प्रवर्तक"),
    Deity("श्री बृहस्पति देव", "देवगुरु"),
    Deity("श्री सत्यनारायण", "सत्यनारायण भगवान"),
    Deity("श्रीमद्भागवत", "पवित्र महापुराण"),
    Deity("श्री रामायण", "महाकाव्य"),
    Deity("गंगा मैया", "माँ गंगा"),
    Deity("श्री शनि देव", "शनिदेव"),
    Deity("श्री सूर्य नारायण", "सूर्यदेव"),
    Deity("गरुड़ पुराण", "पवित्र महापुराण")
)

private val categories = listOf(
    Category("आरती", "आरती संग्रह"),
    Category("मंत्र", "संस्कृत एवं देवनागरी मंत्र"),
    Category("पूजा सामग्री", "पूजा में आवश्यक सामग्री"),
    Category("यज्ञ सामग्री", "यज्ञ एवं हवन सामग्री"),
    Category("प्राण प्रतिष्ठा अनुशासन", "विधि एवं निर्देश")
)

private val hindiMonths = listOf(
    "जनवरी", "फ़रवरी", "मार्च", "अप्रैल",
    "मई", "जून", "जुलाई", "अगस्त",
    "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
)

private val weekdays = listOf(
    "रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि"
)

private fun formatHindiDate(date: LocalDate): String {
    val month = hindiMonths[date.monthValue - 1]
    return "${date.dayOfMonth} $month ${date.year}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DevotionalApp()
        }
    }
}

@Composable
private fun DevotionalApp() {
    val context = LocalContext.current

    var selectedOffice by remember {
        mutableStateOf(PinCodeRepository.loadSavedOffice(context))
    }

    if (selectedOffice == null) {
        PinSetupScreen { office ->
            selectedOffice = office
        }
        return
    }

    val office = selectedOffice ?: return
    val location = office.displayAddress

    var dark by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf("home") }
    var deity by remember { mutableStateOf(deities.first()) }
    var category by remember { mutableStateOf(categories.first()) }

    var currentTime by remember {
        mutableStateOf(ZonedDateTime.now(IndiaZone))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ZonedDateTime.now(IndiaZone)
            delay(1000)
        }
    }

    val today = currentTime.toLocalDate()

    var selectedDate by remember { mutableStateOf(today) }
    var displayedMonth by remember {
        mutableStateOf(YearMonth.from(today))
    }

    var calendarDay by remember {
        mutableStateOf<HinduCalendarDay?>(null)
    }

    var panchangError by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(
        selectedDate,
        office.latitude,
        office.longitude
    ) {
        calendarDay = null
        panchangError = null

        val latitude = office.latitude
        val longitude = office.longitude

        if (latitude == null || longitude == null) {
            panchangError =
                "इस Post Office के coordinates उपलब्ध नहीं हैं।"
        } else {
            try {
                calendarDay = withContext(Dispatchers.Default) {
                    HinduCalendarEngine.calculateDay(
                        date = selectedDate,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            } catch (e: Exception) {
                panchangError = "पंचांग गणना नहीं हो सकी।"
            }
        }
    }

    val bg = if (dark) DarkBg else LightBg
    val card = if (dark) DarkCard else LightCard
    val fg = if (dark) Color(0xFFF2EBDD) else Color(0xFF25221D)
    val muted = if (dark) Color(0xFFBDB5A6) else Color(0xFF71695E)

    val tithiText = when {
        calendarDay != null ->
            "${calendarDay!!.panchang.tithi} · ${calendarDay!!.panchang.paksha}"

        panchangError != null ->
            "हिंदू तिथि: उपलब्ध नहीं"

        else ->
            "हिंदू तिथि: गणना हो रही है..."
    }

    MaterialTheme(
        colorScheme = if (dark)
            darkColorScheme(
                primary = Gold,
                background = bg,
                surface = card
            )
        else
            lightColorScheme(
                primary = Color(0xFF795A24),
                background = bg,
                surface = card
            )
    ) {
        Surface(
            Modifier.fillMaxSize(),
            color = bg
        ) {
            Column(Modifier.fillMaxSize()) {

                Header(
                    dark = dark,
                    fg = fg,
                    muted = muted,
                    currentTime = currentTime,
                    tithiText = tithiText
                ) {
                    page = "settings"
                }

                when (page) {
                    "home" -> Home(fg, muted) {
                        page = "menu"
                    }

                    "menu" -> Menu(deities, fg, muted, card) {
                        deity = it
                        page = "deity"
                    }

                    "deity" -> DeityPage(
                        deity,
                        fg,
                        muted,
                        card,
                        { page = "menu" }
                    ) {
                        category = it
                        page = "reader"
                    }

                    "reader" -> Reader(
                        deity,
                        category,
                        fg,
                        muted
                    ) {
                        page = "deity"
                    }

                    "settings" -> Settings(
                        dark,
                        location,
                        fg,
                        muted,
                        card,
                        { dark = it },
                        { page = "panchang" }
                    )

                    "panchang" -> PanchangCalendarScreen(
                        location = location,
                        selectedDate = selectedDate,
                        displayedMonth = displayedMonth,
                        calendarDay = calendarDay,
                        error = panchangError,
                        today = today,
                        fg = fg,
                        muted = muted,
                        card = card,
                        onBack = { page = "settings" },
                        onPreviousMonth = {
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                        onNextMonth = {
                            displayedMonth = displayedMonth.plusMonths(1)
                        },
                        onSelectDate = {
                            selectedDate = it
                        },
                        onToday = {
                            selectedDate = today
                            displayedMonth = YearMonth.from(today)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    dark: Boolean,
    fg: Color,
    muted: Color,
    currentTime: ZonedDateTime,
    tithiText: String,
    settings: () -> Unit
) {
    val time = currentTime.format(
        DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH)
    )

    val date = currentTime.format(
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy · EEEE",
            Locale("hi", "IN")
        )
    )

    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            Modifier.size(42.dp).clickable { settings() },
            shape = CircleShape,
            color = if (dark) DarkCard else Color(0xFFE9E0D0)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Settings,
                    "Settings",
                    tint = Gold
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                time,
                color = fg,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(date, color = muted, fontSize = 11.sp)

            Text(
                tithiText,
                color = Gold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun Home(
    fg: Color,
    muted: Color,
    openMenu: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.fillMaxWidth()
                .weight(1f)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color(0xFF263449),
                            Color(0xFF171B25),
                            Color(0xFF3A2B22)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "नटराज",
                    color = Gold,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "आपका चुना हुआ 4K नटराज वॉलपेपर यहाँ आएगा",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    "3D VISUAL HOME — PLACEHOLDER",
                    color = Gold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("डबल टैप करें", color = muted, fontSize = 13.sp)

        Text(
            "॥ श्री गणेशाय नमः ॥",
            Modifier.clickable { openMenu() }.padding(18.dp),
            color = fg,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "मेन्यू खोलने के लिए यहाँ टैप करें",
            color = muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun Menu(
    deities: List<Deity>,
    fg: Color,
    muted: Color,
    card: Color,
    select: (Deity) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "॥ श्री गणेशाय नमः ॥",
            color = fg,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deities.size) { index ->
                val item = deities[index]

                Column(
                    Modifier.fillMaxWidth()
                        .background(card, RoundedCornerShape(16.dp))
                        .clickable { select(item) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .aspectRatio(.9f)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF514333),
                                        Color(0xFF252A36)
                                    )
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ॐ", color = Gold, fontSize = 54.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        item.name,
                        color = fg,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        item.subtitle,
                        color = muted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DeityPage(
    deity: Deity,
    fg: Color,
    muted: Color,
    card: Color,
    back: () -> Unit,
    open: (Category) -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "‹ वापस",
            color = Gold,
            modifier = Modifier.clickable { back() }
                .padding(vertical = 8.dp)
        )

        Text(
            deity.name,
            color = fg,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(deity.subtitle, color = muted)

        Spacer(Modifier.height(18.dp))

        categories.forEach { item ->
            Row(
                Modifier.fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(card, RoundedCornerShape(14.dp))
                    .clickable { open(item) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        color = fg,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        item.description,
                        color = muted,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Gold
                )
            }
        }
    }
}

@Composable
private fun Reader(
    deity: Deity,
    category: Category,
    fg: Color,
    muted: Color,
    back: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "‹ वापस",
            color = Gold,
            modifier = Modifier.clickable { back() }
        )

        Spacer(Modifier.height(24.dp))

        Text(deity.name, color = muted, fontSize = 13.sp)

        Text(
            category.name,
            color = fg,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(22.dp))

        Text(
            "यहाँ धार्मिक ग्रंथ का वास्तविक पाठ जोड़ा जाएगा।",
            color = fg,
            fontSize = 19.sp,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "यह नमूना पृष्ठ है। वास्तविक संस्कृत या देवनागरी सामग्री बाद में जोड़ी जाएगी।",
            color = muted,
            fontSize = 15.sp,
            lineHeight = 26.sp
        )

        Spacer(Modifier.height(30.dp))

        Text(
            "पृष्ठ 1",
            color = Gold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun Settings(
    dark: Boolean,
    location: String,
    fg: Color,
    muted: Color,
    card: Color,
    setDark: (Boolean) -> Unit,
    openPanchang: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "सेटिंग्स",
            color = fg,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        Text("दिखावट", color = Gold, fontWeight = FontWeight.SemiBold)

        Row(
            Modifier.fillMaxWidth()
                .background(card, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dark Mode",
                color = fg,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = dark,
                onCheckedChange = setDark
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("देव पंचांग", color = Gold, fontWeight = FontWeight.SemiBold)

        Column(
            Modifier.fillMaxWidth()
                .background(card, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                "Saved Location",
                color = fg,
                fontWeight = FontWeight.Medium
            )

            Text(
                location,
                color = muted,
                fontSize = 12.sp
            )

            TextButton(onClick = openPanchang) {
                Text("हिंदू कैलेंडर खोलें")
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Kalki · Version 0.1.0",
            color = muted,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "All Rights Served By पं शिवम सोनी.",
            color = muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PanchangCalendarScreen(
    location: String,
    selectedDate: LocalDate,
    displayedMonth: YearMonth,
    calendarDay: HinduCalendarDay?,
    error: String?,
    today: LocalDate,
    fg: Color,
    muted: Color,
    card: Color,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit
) {
    val data = calendarDay?.panchang
    val monthData = calendarDay

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "‹ सेटिंग्स",
            color = Gold,
            modifier = Modifier.clickable { onBack() }
                .padding(vertical = 8.dp)
        )

        Text(
            "॥ हिंदू कैलेंडर ॥",
            color = fg,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "स्थान: $location",
            color = muted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth()
                .background(card, RoundedCornerShape(14.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "पिछला महीना",
                    tint = Gold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${hindiMonths[displayedMonth.monthValue - 1]} ${displayedMonth.year}",
                    color = fg,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Gregorian Calendar",
                    color = muted,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "अगला महीना",
                    tint = Gold
                )
            }
        }

        TextButton(
            onClick = onToday,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("आज पर जाएँ")
        }

        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                    color = Gold,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val firstDay = displayedMonth.atDay(1)
        val offset = firstDay.dayOfWeek.value % 7
        val totalCells =
            ((offset + displayedMonth.lengthOfMonth() + 6) / 7) * 7

        for (weekStart in 0 until totalCells step 7) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0..6) {
                    val cellIndex = weekStart + column
                    val dayNumber = cellIndex - offset + 1

                    if (dayNumber in 1..displayedMonth.lengthOfMonth()) {
                        val date = displayedMonth.atDay(dayNumber)
                        val isSelected = date == selectedDate
                        val isToday = date == today

                        Column(
                            Modifier.weight(1f)
                                .padding(2.dp)
                                .background(
                                    when {
                                        isSelected -> Gold.copy(alpha = 0.28f)
                                        isToday -> card.copy(alpha = 0.9f)
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(9.dp)
                                )
                                .clickable { onSelectDate(date) }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                dayNumber.toString(),
                                color = if (isSelected) Gold else fg,
                                fontWeight = if (isSelected || isToday)
                                    FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )

                            if (isToday) {
                                Text(
                                    "आज",
                                    color = Gold,
                                    fontSize = 8.sp
                                )
                            } else {
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    } else {
                        Spacer(
                            Modifier.weight(1f)
                                .height(43.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "चयनित तारीख",
            color = Gold,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            formatHindiDate(selectedDate),
            color = fg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            selectedDate.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.FULL,
                Locale("hi", "IN")
            ),
            color = muted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "हिंदू मास",
            color = Gold,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (monthData == null) {
            Text(
                error ?: "मास की गणना हो रही है...",
                color = muted,
                fontSize = 13.sp
            )
        } else {
            val monthName = if (monthData.isAdhikaMonth) {
                "अधिक ${monthData.hinduMonth.hindiName}"
            } else {
                monthData.hinduMonth.hindiName
            }

            CalendarInfoRow("मास", monthName, fg, muted, card)
            CalendarInfoRow(
                "गणना स्थिति",
                monthData.calculationStatus,
                fg,
                muted,
                card
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "दैनिक पंचांग",
            color = Gold,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        when {
            error != null -> {
                Text(error, color = Gold, fontSize = 13.sp)
            }

            data == null -> {
                Text(
                    "पंचांग की गणना हो रही है...",
                    color = muted,
                    fontSize = 13.sp
                )
            }

            else -> {
                CalendarInfoRow("तिथि", data.tithi, fg, muted, card)
                CalendarInfoRow("पक्ष", data.paksha, fg, muted, card)
                CalendarInfoRow(
                    "तिथि संख्या",
                    data.tithiNumber.toString(),
                    fg,
                    muted,
                    card
                )
                CalendarInfoRow("तिथि प्रारंभ", data.tithiStart, fg, muted, card)
                CalendarInfoRow("तिथि समाप्त", data.tithiEnd, fg, muted, card)

                CalendarInfoRow("नक्षत्र", data.nakshatra, fg, muted, card)
                CalendarInfoRow(
                    "नक्षत्र पाद",
                    data.nakshatraPada.toString(),
                    fg,
                    muted,
                    card
                )
                CalendarInfoRow(
                    "नक्षत्र प्रारंभ",
                    data.nakshatraStart,
                    fg,
                    muted,
                    card
                )
                CalendarInfoRow(
                    "नक्षत्र समाप्त",
                    data.nakshatraEnd,
                    fg,
                    muted,
                    card
                )

                CalendarInfoRow("योग", data.yoga, fg, muted, card)
                CalendarInfoRow("योग प्रारंभ", data.yogaStart, fg, muted, card)
                CalendarInfoRow("योग समाप्त", data.yogaEnd, fg, muted, card)

                CalendarInfoRow("करण", data.karana, fg, muted, card)
                CalendarInfoRow("करण प्रारंभ", data.karanaStart, fg, muted, card)
                CalendarInfoRow("करण समाप्त", data.karanaEnd, fg, muted, card)

                CalendarInfoRow("सूर्योदय", data.sunrise, fg, muted, card)
                CalendarInfoRow("सूर्यास्त", data.sunset, fg, muted, card)

                Spacer(Modifier.height(8.dp))

                Text(
                    data.calculationStatus,
                    color = muted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "All Rights Served By पं शिवम सोनी.",
            color = muted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CalendarInfoRow(
    title: String,
    value: String,
    fg: Color,
    muted: Color,
    card: Color
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(card, RoundedCornerShape(10.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = fg,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp
        )

        Text(
            value,
            color = muted,
            textAlign = TextAlign.End,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.2f)
        )
    }
}
