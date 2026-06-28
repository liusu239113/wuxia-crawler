package com.wuxiacrawler.ui.theme

import androidx.compose.ui.graphics.Color

// 武侠风格配色
val ParchmentLight = Color(0xFFF5E6C8)     // 宣纸色
val ParchmentDark = Color(0xFFD4B896)       // 古卷深色
val InkBlack = Color(0xFF1A1A2E)            // 墨色
val InkDark = Color(0xFF16213E)             // 墨蓝
val GoldAccent = Color(0xFFD4A017)          // 金色
val GoldBright = Color(0xFFFFD700)          // 亮金
val RedAccent = Color(0xFFC0392B)           // 朱砂红
val RedDark = Color(0xFF7B241C)             // 暗红
val GreenJade = Color(0xFF27AE60)           // 翡翠绿
val BlueSteel = Color(0xFF2980B9)           // 青钢
val PurpleMystic = Color(0xFF8E44AD)        // 紫气
val OrangeMythic = Color(0xFFE67E22)        // 传说橙
val GrayStone = Color(0xFF7F8C8D)           // 石灰色

// 稀有度颜色映射
val RarityColors = mapOf(
    "凡品" to GrayStone,
    "良品" to GreenJade,
    "稀有" to BlueSteel,
    "史诗" to PurpleMystic,
    "传说" to OrangeMythic,
    "太古" to RedAccent
)

val rarityTextColors = mapOf(
    "凡品" to Color(0xFF9D9D9D),
    "良品" to Color(0xFF4CAF50),
    "稀有" to Color(0xFF2196F3),
    "史诗" to Color(0xFF9C27B0),
    "传说" to Color(0xFFFF9800),
    "太古" to Color(0xFFF44336)
)