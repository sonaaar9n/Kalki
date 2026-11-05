package com.hexa.devotional

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    onLocationSelected: (PinOffice) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("491331") }
    var offices by remember { mutableStateOf<List<PinOffice>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "॥ देव पंचांग ॥",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "अपना क्षेत्र चुनें",
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "अपने क्षेत्र का 6 अंकों का PIN Code डालें।",
                fontSize = 14.sp
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        pin = it
                        offices = emptyList()
                        error = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("India PIN Code") },
                placeholder = { Text("जैसे 491331") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = ""
                        offices = emptyList()

                        try {
                            offices = PinCodeRepository.searchPin(pin)

                            if (offices.isEmpty()) {
                                error = "इस PIN Code के लिए Post Office नहीं मिला।"
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "स्थान खोजने में समस्या हुई।"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length == 6 && !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("खोज रहे हैं...")
                } else {
                    Text("स्थान खोजें")
                }
            }

            if (error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            if (offices.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "अपना Post Office चुनें",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(offices) { office ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (
                                        office.latitude == null ||
                                        office.longitude == null
                                    ) {
                                        error = "इस Post Office के coordinates उपलब्ध नहीं हैं। दूसरा Post Office चुनें।"
                                    } else {
                                        PinCodeRepository.saveOffice(
                                            context,
                                            office
                                        )
                                        onLocationSelected(office)
                                    }
                                },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = office.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = office.displayAddress,
                                    fontSize = 13.sp
                                )

                                Text(
                                    text = "PIN: ${office.pincode} · ${office.officeType}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = if (
                                        office.latitude != null &&
                                        office.longitude != null
                                    ) "Coordinates उपलब्ध हैं"
                                    else "Coordinates उपलब्ध नहीं",
                                    fontSize = 11.sp
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "इस स्थान को चुनें",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "स्थान एक बार चुनने के बाद सेव रहेगा।",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
