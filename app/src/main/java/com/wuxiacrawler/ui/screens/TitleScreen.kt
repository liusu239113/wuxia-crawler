package com.wuxiacrawler.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.config.MartialSkill
import com.wuxiacrawler.ui.theme.*
import com.wuxiacrawler.viewmodel.GameViewModel

@Composable
fun TitleScreen(viewModel: GameViewModel, onNewGame: () -> Unit, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("武 林 秘 境", color = GoldAccent, fontSize = 42.sp, fontWeight = FontWeight.Black)
            Text("Wǔ Lín Mì Jìng", color = GrayStone, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text("—— 一入江湖，生死由命 ——", color = RedAccent, fontSize = 16.sp)

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("初入江湖", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            }

            if (viewModel.engine.hasSave()) {
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueSteel),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("再续前缘", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { viewModel.engine.deleteSave() }) {
                    Text("删除存档", color = RedAccent, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Roguelite · 自动战斗 · 装备刷取 · 武侠风", color = GrayStone, fontSize = 12.sp)
        }
    }
}

@Composable
fun CreationScreen(viewModel: GameViewModel, onCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    var hp by remember { mutableIntStateOf(5) }
    var atk by remember { mutableIntStateOf(5) }
    var def by remember { mutableIntStateOf(5) }
    var speed by remember { mutableIntStateOf(5) }
    var points by remember { mutableIntStateOf(20) }
    var selectedSkill by remember { mutableStateOf(MartialSkill.REMNANT_EDGE) }

    val calcHp = 50 * hp
    val calcAtk = 10 * atk
    val calcDef = 10 * def
    val calcSpeed = 0.4f + 0.02f * speed

    Scaffold(containerColor = InkBlack) {
        Column(Modifier.padding(it).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("创建侠客", color = GoldAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            // 姓名
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(8); nameError = null },
                label = { Text("侠客之名") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent, unfocusedBorderColor = GrayStone,
                    focusedTextColor = ParchmentLight, unfocusedTextColor = ParchmentLight,
                    cursorColor = GoldAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            nameError?.let { Text(it, color = RedAccent, fontSize = 12.sp) }

            Text("剩余修行点: $points", color = GoldBright, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // 属性分配
            StatAllocRow("气血", hp, calcHp, RedAccent, { if (points > 0) { hp++; points-- } }) { if (hp > 5) { hp--; points++ } }
            StatAllocRow("攻击", atk, calcAtk, GoldAccent, { if (points > 0) { atk++; points-- } }) { if (atk > 5) { atk--; points++ } }
            StatAllocRow("防御", def, calcDef, BlueSteel, { if (points > 0) { def++; points-- } }) { if (def > 5) { def--; points++ } }
            StatAllocRow("身法", speed, calcSpeed, GreenJade, { if (points > 0) { speed++; points-- } }) { if (speed > 5) { speed--; points++ } }

            // 武学选择
            Text("选择武学", color = GoldAccent, fontWeight = FontWeight.Bold)
            Surface(color = InkDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    MartialSkill.entries.forEach { skill ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedSkill == skill,
                                onClick = { selectedSkill = skill },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                            )
                            Column {
                                Text(skill.displayName, color = ParchmentLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(skill.description, color = GrayStone, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.length < 2 || name.length > 8) {
                        nameError = "名字需2-8个字"
                        return@Button
                    }
                    viewModel.engine.createCharacter(name, hp, atk, def, speed, selectedSkill)
                    onCreated()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("踏入江湖", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkBlack)
            }
        }
    }
}

@Composable
private fun StatAllocRow(
    label: String, value: Int, calcValue: Any,
    color: androidx.compose.ui.graphics.Color,
    onAdd: () -> Unit, onMinus: () -> Unit
) {
    val displayValue = when (calcValue) {
        is Int -> calcValue.toString()
        is Float -> "%.1f".format(calcValue)
        else -> calcValue.toString()
    }
    Surface(color = InkDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$label: $displayValue", color = color, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(onClick = onMinus, colors = IconButtonDefaults.filledIconButtonColors(containerColor = RedDark)) {
                    Text("-", color = ParchmentLight, fontSize = 18.sp)
                }
                Text("$value", color = ParchmentLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                FilledIconButton(onClick = onAdd, colors = IconButtonDefaults.filledIconButtonColors(containerColor = GreenJade)) {
                    Text("+", color = ParchmentLight, fontSize = 18.sp)
                }
            }
        }
    }
}