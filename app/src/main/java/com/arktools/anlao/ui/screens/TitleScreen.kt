package com.arktools.anlao.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arktools.anlao.viewmodel.GameViewModel

private val BgDark = Color(0xFF0D0D0D)
private val BgPanel = Color(0xE617110D)
private val BorderGold = Color(0xFF7B5A2B)
private val BorderDim = Color(0xFF4A3A26)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFB8AEA1)
private val GoldAccent = Color(0xFFFFC15A)
private val ButtonWood = Color(0xE62A1B12)
private val ButtonWoodLight = Color(0xCC3A281A)
private val DangerRed = Color(0xFFC83A2A)

@Composable
fun TitleScreen(viewModel: GameViewModel, onNewGame: () -> Unit, onContinue: () -> Unit) {
    val ctx = LocalContext.current
    val logo = remember {
        try { BitmapFactory.decodeStream(ctx.assets.open("ui/logo.png"))?.asImageBitmap() } catch (_: Exception) { null }
    }
    val bg = remember {
        try { BitmapFactory.decodeStream(ctx.assets.open("ui/backgrounds/title.png"))?.asImageBitmap() } catch (_: Exception) { null }
    }
    val pulse = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1f, animationSpec = tween(800))
            pulse.animateTo(0.55f, animationSpec = tween(800))
        }
    }

    Box(Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
        if (bg != null) {
            Image(bitmap = bg, contentDescription = "开始背景", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Column(
            Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (logo != null) {
                Image(
                    bitmap = logo,
                    contentDescription = "暗牢江湖行",
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("暗牢江湖行", color = GoldAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                Modifier.fillMaxWidth()
                    .border(2.dp, Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .background(BgPanel, RoundedCornerShape(6.dp))
                    .border(1.dp, BorderGold, RoundedCornerShape(6.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("暗牢未开，江湖已远", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "从无名牢影中醒来，择一身本领，闯一条江湖路。",
                        color = TextGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            PixelMenuButton(
                text = if (viewModel.engine.hasSave()) "继续游戏" else "开始游戏",
                textColor = TextWhite,
                borderColor = GoldAccent,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                viewModel.engine.soundManager.playSfx("wood_confirm")
                if (viewModel.engine.hasSave()) onContinue() else onNewGame()
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelMenuButton(
                    text = "新开江湖",
                    textColor = TextGray,
                    borderColor = BorderDim,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    viewModel.engine.soundManager.playSfx("wood_confirm")
                    onNewGame()
                }

                PixelMenuButton(
                    text = "重置存档",
                    textColor = DangerRed,
                    borderColor = DangerRed,
                    modifier = Modifier.weight(1f).height(48.dp),
                    danger = true
                ) {
                    viewModel.engine.soundManager.playSfx("decline")
                    viewModel.engine.deleteSave()
                    onNewGame()
                }
            }

            Text(
                "点击开始，进入角色创建",
                color = TextWhite.copy(alpha = pulse.value),
                fontSize = 12.sp,
                modifier = Modifier.clickable { if (viewModel.engine.hasSave()) onContinue() else onNewGame() }
            )
        }
    }
}

@Composable
private fun PixelMenuButton(
    text: String,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val fill = if (danger) Color(0xE6211110) else ButtonWood
    val inner = if (danger) Color(0x993A1512) else ButtonWoodLight
    Box(
        modifier
            .border(2.dp, Color.Black.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
            .background(fill, RoundedCornerShape(3.dp))
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(inner, RoundedCornerShape(2.dp))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
