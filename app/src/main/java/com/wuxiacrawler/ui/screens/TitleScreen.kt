package com.wuxiacrawler.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.viewmodel.GameViewModel

private val BgDark = Color(0xFF0D0D0D)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF999999)

@Composable
fun TitleScreen(viewModel: GameViewModel, onNewGame: () -> Unit, onContinue: () -> Unit) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            alpha.animateTo(0f, animationSpec = tween(500))
            alpha.animateTo(1f, animationSpec = tween(500))
        }
    }

    Box(Modifier.fillMaxSize().background(BgDark).clickable {
        if (viewModel.engine.hasSave() && !viewModel.engine.player.value.isAllocated) onNewGame()
        else if (viewModel.engine.hasSave()) { viewModel.engine.loadGame(); onContinue() }
        else onNewGame()
    }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚔️", fontSize = 48.sp)
            Text("武林秘境", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            Text("Wǔ Lín Mì Jìng", color = TextGray, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Text("点击屏幕踏入江湖", color = TextWhite.copy(alpha = alpha.value), fontSize = 15.sp)

            if (viewModel.engine.hasSave()) {
                Spacer(Modifier.height(12.dp))
                Text("（检测到存档，点击继续）", color = TextGray, fontSize = 12.sp)
            }
        }
    }
}