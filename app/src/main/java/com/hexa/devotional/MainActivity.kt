
package com.hexa.devotional

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

    var panchangData by remember {
        mutableStateOf<PanchangDailyData?>(null)
    }

    var panchangError by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(today, office.latitude, office.longitude) {
        panchangData = null
        panchangError = null

        val latitude = office.latitude
        val longitude = office.longitude

        if (latitude == null || longitude == null) {
            panchangError = "इस Post Office के coordinates उपलब्ध नहीं हैं।"
        } else {
            try {
                panchangData = withContext(Dispatchers.Default) {
                    PanchangAstronomy.calculate(
                        date = today,
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
        panchangData != null ->
            "${panchangData!!.tithi} · ${panchangData!!.paksha} पक्ष"

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

                    "panchang" -> Panchang(
                        location = location,
                        data = panchangData,
                        error = panchangError,
                        fg = fg,
                        muted = muted,
                        dark = dark
                    ) {
                        page = "settings"
                    }
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

        LazyVerticalGrid(
            GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deities) { item ->
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
                Text("देव पंचांग खोलें")
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
private fun Panchang(
    location: String,
    data: PanchangDailyData?,
    error: String?,
    fg: Color,
    muted: Color,
    dark: Boolean,
    back: () -> Unit
) {
    val cardColor = if (dark) DarkCard else LightCard

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "‹ सेटिंग्स",
            color = Gold,
            modifier = Modifier.clickable { back() }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "॥ देव पंचांग ॥",
            color = fg,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "स्थान: $location",
            color = muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(18.dp))

        if (error != null) {
            Text(
                error,
                color = Gold,
                fontSize = 13.sp,
                lineHeight = 22.sp
            )
        } else if (data == null) {
            Text(
                "पंचांग की गणना हो रही है...",
                color = Gold,
                fontSize = 13.sp
            )
        } else {
            Text(
                "आज का पंचांग",
                color = Gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            PanchangRow("तिथि", data.tithi, fg, muted, cardColor)
            PanchangRow("पक्ष", data.paksha, fg, muted, cardColor)
            PanchangRow("तिथि संख्या", data.tithiNumber.toString(), fg, muted, cardColor)
            PanchangRow("सूर्योदय", data.sunrise, fg, muted, cardColor)
            PanchangRow("सूर्यास्त", data.sunset, fg, muted, cardColor)

            Spacer(Modifier.height(10.dp))

            Text(
                data.calculationStatus,
                color = muted,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "शुक्ल पक्ष की तिथियाँ",
            color = fg,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "एकम 1 · दूज 2 · तीज 3 · चतुर्थी 4 · पंचमी 5 · षष्ठी 6 · सप्तमी 7 · अष्टमी 8 · नवमी 9 · दशमी 10 · ग्यारस 11 · बारस 12 · तेरस 13 · चौदस 14 · पूर्णिमा 15",
            color = muted,
            fontSize = 13.sp,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "कृष्ण पक्ष की तिथियाँ",
            color = fg,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "एकम 1 · दूज 2 · तीज 3 · चतुर्थी 4 · पंचमी 5 · षष्ठी 6 · सप्तमी 7 · अष्टमी 8 · नवमी 9 · दशमी 10 · ग्यारस 11 · बारस 12 · तेरस 13 · चौदस 14 · अमावस्या 15",
            color = muted,
            fontSize = 13.sp,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "अभी गणना में शामिल नहीं",
            color = Gold,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        PanchangRow("नक्षत्र", "गणना लंबित", fg, muted, cardColor)
        PanchangRow("योग", "गणना लंबित", fg, muted, cardColor)
        PanchangRow("करण", "गणना लंबित", fg, muted, cardColor)
        PanchangRow("संवत", "गणना लंबित", fg, muted, cardColor)

        Spacer(Modifier.height(20.dp))

        Text(
            "All Rights Served By पं शिवम सोनी.",
            color = muted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PanchangRow(
    title: String,
    value: String,
    fg: Color,
    muted: Color,
    card: Color
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(card, RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = fg)
        Text(
            value,
            color = muted,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}
