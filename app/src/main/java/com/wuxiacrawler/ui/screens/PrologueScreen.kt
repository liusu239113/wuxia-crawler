package com.wuxiacrawler.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.viewmodel.GameViewModel
import kotlinx.coroutines.delay

private val StoryBg = Color(0xFF0D0D0D)
private val StoryPanel = Color(0xE61A1410)
private val StoryBorder = Color(0xFF7B5A2B)
private val StoryGold = Color(0xFFFFC15A)
private val StoryText = Color(0xFFF4E7D0)
private val StoryDim = Color(0xFFB8AEA1)

private data class StoryLine(val speaker: String, val text: String)

private fun prologueLines(gender: String) = listOf(
    StoryLine("旁白", "三十年前，暗牢一夜沉入地底。江湖传言，那里关着一部能改写天下武学的《无相狱典》。"),
    StoryLine("旁白", "三十年后，七大门派相继失踪，押镖人死在空巷，客栈雨夜只剩一盏不灭的灯。"),
    StoryLine("掌柜", "${if (gender == "female") "姑娘" else "少侠"}，你醒了？昨夜有人把你送到我这儿，只留下一枚断裂的牢符。"),
    StoryLine("掌柜", "他说若你想知道自己的身世，就去城外旧牢门。可那地方，进去的人十个有九个回不来。"),
    StoryLine("你", "若真有人在暗处摆局，那我便入局。若这江湖要我做棋子，我便先掀了棋盘。"),
    StoryLine("掌柜", "好。拿上这盏灯，顺着青石路走。暗牢第一道门后，是你的第一场江湖。")
)

@Composable
fun PrologueScreen(viewModel: GameViewModel, onFinished: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var visibleCount by remember { mutableIntStateOf(0) }
    var finishedTyping by remember { mutableStateOf(false) }
    val lines = remember(viewModel.engine.player.value.gender) { prologueLines(viewModel.engine.player.value.gender) }
    val line = lines[index]

    LaunchedEffect(index) {
        visibleCount = 0
        finishedTyping = false
        while (visibleCount < line.text.length) {
            delay(28)
            visibleCount++
            if (visibleCount % 3 == 0) viewModel.engine.soundManager.playSfx("hover")
        }
        finishedTyping = true
    }

    Box(Modifier.fillMaxSize().background(StoryBg).padding(18.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暗牢江湖行", color = StoryGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("序章 · 雨夜旧牢", color = StoryDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))

            Column(
                Modifier.fillMaxWidth().height(360.dp)
                    .border(1.dp, StoryBorder, RoundedCornerShape(10.dp))
                    .background(StoryPanel, RoundedCornerShape(10.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(line.speaker, color = StoryGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        line.text.take(visibleCount),
                        color = StoryText,
                        fontSize = 17.sp,
                        lineHeight = 28.sp
                    )
                }
                Text("点击继续", color = StoryDim, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.engine.soundManager.playSfx("wood_confirm")
                        if (!finishedTyping) {
                            visibleCount = line.text.length
                            finishedTyping = true
                        } else if (index < lines.lastIndex) {
                            index++
                        } else {
                            viewModel.engine.markPrologueSeen()
                            onFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(48.dp).border(1.dp, StoryBorder, RoundedCornerShape(6.dp))
                ) { Text(if (index == lines.lastIndex && finishedTyping) "踏入暗牢" else "继续", color = StoryText, fontSize = 16.sp) }

                Button(
                    onClick = {
                        viewModel.engine.soundManager.playSfx("wood_confirm")
                        viewModel.engine.markPrologueSeen()
                        onFinished()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(width = 96.dp, height = 48.dp).border(1.dp, StoryBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                ) { Text("跳过", color = StoryDim, fontSize = 14.sp) }
            }
        }
    }
}
