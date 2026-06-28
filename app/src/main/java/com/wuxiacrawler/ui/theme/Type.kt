package com.wuxiacrawler.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.R

val HuawenFangsong = FontFamily(Font(R.font.huawen_fangsong))

val DefaultTextStyle = TextStyle(
    fontFamily = HuawenFangsong,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal
)

val WuxiaTypography = Typography(
    displayLarge = TextStyle(fontFamily = HuawenFangsong),
    displayMedium = TextStyle(fontFamily = HuawenFangsong),
    displaySmall = TextStyle(fontFamily = HuawenFangsong),
    headlineLarge = TextStyle(fontFamily = HuawenFangsong),
    headlineMedium = TextStyle(fontFamily = HuawenFangsong),
    headlineSmall = TextStyle(fontFamily = HuawenFangsong),
    titleLarge = TextStyle(fontFamily = HuawenFangsong),
    titleMedium = TextStyle(fontFamily = HuawenFangsong),
    titleSmall = TextStyle(fontFamily = HuawenFangsong),
    bodyLarge = TextStyle(fontFamily = HuawenFangsong),
    bodyMedium = TextStyle(fontFamily = HuawenFangsong),
    bodySmall = TextStyle(fontFamily = HuawenFangsong),
    labelLarge = TextStyle(fontFamily = HuawenFangsong),
    labelMedium = TextStyle(fontFamily = HuawenFangsong),
    labelSmall = TextStyle(fontFamily = HuawenFangsong),
)
