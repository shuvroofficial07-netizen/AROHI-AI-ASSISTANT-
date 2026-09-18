package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.CheckInType
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * Real weather (Open-Meteo) + the pro-active check-in controls.
 */
@Composable
fun WeatherScreen(
    viewModel: ArohiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val weather by viewModel.weather.collectAsState()
    val isRefreshing by viewModel.isWeatherRefreshing.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()
    val city by viewModel.weatherCityFlow.collectAsState()
    val suggestion by viewModel.proactiveSuggestion.collectAsState()
    val morningEnabled by viewModel.morningCheckInFlow.collectAsState()
    val eveningEnabled by viewModel.eveningCheckInFlow.collectAsState()

    var cityInput by remember(city) { mutableStateOf(city) }
    var morningTime by remember { mutableStateOf("08:30") }
    var eveningTime by remember { mutableStateOf("21:30") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.refreshWeather(true)
    }

    fun requestLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) {
            viewModel.refreshWeather(true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (weather == null && !isRefreshing) {
        viewModel.refreshWeather(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "WEATHER & PRO-ACTIVE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            color = CyanPrimary
        )
        Text(
            text = "আবহাওয়া ও স্মার্ট সাজেশন",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), glowBorder = true) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MagentaAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = weather?.city ?: "আবহাওয়া লোড হচ্ছে...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isRefreshing) {
                        Text("...", fontSize = 12.sp, color = CyanPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (weather != null) {
                    val snapshot = weather
                    if (snapshot != null) {
                        Text(
                            text = "${snapshot.temperatureC.toInt()}°C",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${snapshot.condition} · অনুভূত ${snapshot.feelsLikeC.toInt()}°C",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            WeatherFact("সর্বোচ্চ", "${snapshot.maxC.toInt()}°")
                            WeatherFact("সর্বনিম্ন", "${snapshot.minC.toInt()}°")
                            WeatherFact("বৃষ্টি", "${snapshot.rainChancePercent}%")
                            WeatherFact("আর্দ্রতা", "${snapshot.humidityPercent}%")
                            WeatherFact("বাতাস", "${snapshot.windKph.toInt()} km/h")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = snapshot.spokenSummary(),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Text(
                        text = weatherError ?: "সেটিংসে শহরের নাম দিয়ে রাখুন অথবা লোকেশন পারমিশন দিন।",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.refreshWeather(true) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিফ্রেশ", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { requestLocation() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("আমার লোকেশন", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "শহর (নেট-ভিত্তিক অবস্থান)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("শহরের নাম", color = TextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weather_city_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedContainerColor = Color(0x0DFFFFFF),
                            unfocusedContainerColor = Color(0x0DFFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.setWeatherCity(cityInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("সেভ", color = Color(0xFF020205), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (suggestion != null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = VioletBright.copy(alpha = 0.4f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = VioletBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "প্রো-অ্যাকটিভ সাজেশন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = suggestion ?: "", fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সকাল / রাতের চেক-ইন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "আরোহী নিজে থেকে হালকা করে খোঁজ নেবে — আবহাওয়া আর আজকের কাজ মিলিয়ে।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                ToggleRow(
                    title = "সকালের চেক-ইন",
                    checked = morningEnabled,
                    onCheckedChange = { viewModel.setMorningCheckIn(it) }
                )
                ToggleRow(
                    title = "রাতের চেক-ইন",
                    checked = eveningEnabled,
                    onCheckedChange = { viewModel.setEveningCheckIn(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = morningTime,
                        onValueChange = { morningTime = it },
                        label = { Text("সকাল", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = eveningTime,
                        onValueChange = { eveningTime = it },
                        label = { Text("রাত", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.setCheckInTimes(morningTime, eveningTime) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("সময় সেভ করুন", color = Color(0xFF020205), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.previewCheckIn(CheckInType.MORNING) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VioletBright.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletBright),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("প্রিভিউ শুনুন", fontSize = 11.sp)
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "চেক-ইন নোটিফিকেশন পেতে নোটিফিকেশন পারমিশন চালু রাখুন।",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WeatherFact(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Spacer(modifier = Modifier.height(6.dp))
}
