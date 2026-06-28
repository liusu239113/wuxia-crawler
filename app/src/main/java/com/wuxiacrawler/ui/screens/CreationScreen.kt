package com.wuxiacrawler.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.config.MartialSkill
import com.wuxiacrawler.viewmodel.GameViewModel

private val BgDark = Color(0xFF0D0D0D)
private val BorderWhite = Color(0xFF555555)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF999999)
private val HpRed = Color(0xFFE40000)

// 整合创建流程：先输入名字，再分配属性
@Composable
fun CreationScreen(viewModel: GameViewModel, onCreated: () -> Unit) {
    var step by remember { mutableIntStateOf(0) } // 0=name input, 1=allocation
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // 分配属性状态
    var hpAlloc by remember { mutableIntStateOf(5) }
    var atkAlloc by remember { mutableIntStateOf(5) }
    var defAlloc by remember { mutableIntStateOf(5) }
    var spdAlloc by remember { mutableIntStateOf(5) }
    var points by remember { mutableIntStateOf(20) }
    var skill by remember { mutableStateOf(MartialSkill.REMNANT_EDGE) }

    Box(Modifier.fillMaxSize().background(BgDark)) {
        when (step) {
            0 -> {
                // ===== NAME INPUT =====
                Column(Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("敢问侠客大名？", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = name, onValueChange = { name = it; error = "" },
                        singleLine = true,
                        placeholder = { Text("输入名字（2-8字）", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextWhite, unfocusedBorderColor = BorderWhite,
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            cursorColor = TextWhite, focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error.isNotEmpty()) Text(error, color = HpRed, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp))

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (name.length < 2 || name.length > 8) {
                                error = "名字需2-8个字"; return@Button
                            }
                            if (Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?￥￥]").containsMatchIn(name)) {
                                error = "不能包含特殊字符"; return@Button
                            }
                            step = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .border(1.dp, BorderWhite, RoundedCornerShape(4.dp))
                    ) { Text("确认姓名", color = TextWhite, fontSize = 18.sp) }
                }
            }
            1 -> {
                // ===== ALLOCATION =====
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text("分配属性 — $name", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("剩余修行点: $points", color = TextGray, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))

                    AllocRow("气血", hpAlloc, 50 * hpAlloc,
                        { if (points > 0) { hpAlloc++; points-- } },
                        { if (hpAlloc > 5) { hpAlloc--; points++ } })
                    AllocRow("攻击", atkAlloc, 10 * atkAlloc,
                        { if (points > 0) { atkAlloc++; points-- } },
                        { if (atkAlloc > 5) { atkAlloc--; points++ } })
                    AllocRow("防御", defAlloc, 10 * defAlloc,
                        { if (points > 0) { defAlloc++; points-- } },
                        { if (defAlloc > 5) { defAlloc--; points++ } })
                    AllocRow("身法", spdAlloc, "%.1f".format(0.4 + 0.02 * spdAlloc),
                        { if (points > 0) { spdAlloc++; points-- } },
                        { if (spdAlloc > 5) { spdAlloc--; points++ } })

                    Spacer(Modifier.height(12.dp))
                    Text("武学选择", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Box(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(4.dp)) {
                        Column {
                            MartialSkill.entries.filter { !listOf("BLOODTHIRST", "PRECISION").contains(it.name) }.forEach { s ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = skill == s, onClick = { skill = s },
                                        colors = RadioButtonDefaults.colors(selectedColor = TextWhite, unselectedColor = TextGray),
                                        modifier = Modifier.size(32.dp))
                                    Column {
                                        Text(s.displayName, color = TextWhite, fontSize = 13.sp)
                                        Text(s.description, color = TextGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            viewModel.engine.createCharacter(name, hpAlloc, atkAlloc, defAlloc, spdAlloc, skill)
                            onCreated()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .border(1.dp, BorderWhite, RoundedCornerShape(4.dp))
                    ) { Text("踏入江湖", color = TextWhite, fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun AllocRow(
    label: String, value: Int, displayValue: Any,
    onAdd: () -> Unit, onMinus: () -> Unit
) {
    Row(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(4.dp))
        .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $displayValue", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onMinus,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.size(32.dp).border(1.dp, BorderWhite, RoundedCornerShape(3.dp)),
                contentPadding = PaddingValues(0.dp)) { Text("-", color = TextWhite, fontSize = 16.sp) }
            Text("$value", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.size(32.dp).border(1.dp, BorderWhite, RoundedCornerShape(3.dp)),
                contentPadding = PaddingValues(0.dp)) { Text("+", color = TextWhite, fontSize = 16.sp) }
        }
    }
}
