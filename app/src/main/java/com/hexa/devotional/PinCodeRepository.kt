
package com.hexa.devotional

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PinOffice(
    val id: Int,
    val name: String,
    val pincode: String,
    val officeType: String,
    val deliveryStatus: String,
    val district: String,
    val state: String,
    val latitude: Double?,
    val longitude: Double?
) {
    val displayAddress: String
        get() = "$name, $district, $state, India"
}

object PinCodeRepository {

    private const val PREFS = "hexa_location"
    private const val SAVED_OFFICE = "saved_post_office"

    suspend fun searchPin(pin: String): List<PinOffice> =
        withContext(Dispatchers.IO) {

            require(pin.matches(Regex("[0-9]{6}"))) {
                "कृपया 6 अंकों का PIN Code डालें।"
            }

            val connection = URL(
                "https://api.pincodeapi.in/api/v1/pincode/$pin"
            ).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val status = connection.responseCode

                if (status !in 200..299) {
                    throw Exception("PIN सेवा उपलब्ध नहीं है। HTTP $status")
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val root = JSONObject(response)

                if (!root.optBoolean("success", false)) {
                    throw Exception("इस PIN Code की जानकारी नहीं मिली।")
                }

                val offices = root
                    .getJSONObject("data")
                    .getJSONArray("post_offices")

                buildList {
                    for (i in 0 until offices.length()) {
                        val item = offices.getJSONObject(i)

                        val lat = if (item.isNull("latitude")) {
                            null
                        } else {
                            item.optDouble("latitude").takeIf {
                                it.isFinite() && it in -90.0..90.0
                            }
                        }

                        val lon = if (item.isNull("longitude")) {
                            null
                        } else {
                            item.optDouble("longitude").takeIf {
                                it.isFinite() && it in -180.0..180.0
                            }
                        }

                        add(
                            PinOffice(
                                id = item.optInt("id"),
                                name = item.optString("office_name"),
                                pincode = item.optString("pincode"),
                                officeType = item.optString("office_type"),
                                deliveryStatus = item.optString("delivery_status"),
                                district = item.optString("district"),
                                state = item.optString("state"),
                                latitude = lat,
                                longitude = lon
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

    fun saveOffice(context: Context, office: PinOffice) {
        val json = JSONObject().apply {
            put("id", office.id)
            put("name", office.name)
            put("pincode", office.pincode)
            put("officeType", office.officeType)
            put("deliveryStatus", office.deliveryStatus)
            put("district", office.district)
            put("state", office.state)

            if (office.latitude != null) put("latitude", office.latitude)
            if (office.longitude != null) put("longitude", office.longitude)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SAVED_OFFICE, json.toString())
            .apply()
    }

    fun loadSavedOffice(context: Context): PinOffice? {
        val saved = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getString(SAVED_OFFICE, null) ?: return null

        return try {
            val json = JSONObject(saved)

            PinOffice(
                id = json.optInt("id"),
                name = json.getString("name"),
                pincode = json.getString("pincode"),
                officeType = json.optString("officeType"),
                deliveryStatus = json.optString("deliveryStatus"),
                district = json.getString("district"),
                state = json.getString("state"),
                latitude = if (json.has("latitude")) {
                    json.getDouble("latitude")
                } else null,
                longitude = if (json.has("longitude")) {
                    json.getDouble("longitude")
                } else null
            )
        } catch (_: Exception) {
            null
        }
    }
}
