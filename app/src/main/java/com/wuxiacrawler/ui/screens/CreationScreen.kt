package com.wuxiacrawler.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.wuxiacrawler.config.MartialSect
import com.wuxiacrawler.config.MartialSkill
import com.wuxiacrawler.viewmodel.GameViewModel

private val BgDark = Color(0xFF0D0D0D)
private val BgPanel = Color(0xFF17110D)
private val BorderGold = Color(0xFF7B5A2B)
private val BorderDim = Color(0xFF4A3A26)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFB8AEA1)
private val HpRed = Color(0xFFE0543D)
private val GoldAccent = Color(0xFFFFC15A)

@Composable
fun CreationScreen(viewModel: GameViewModel, onCreated: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var error by remember { mutableStateOf("") }

    var hpAlloc by remember { mutableIntStateOf(5) }
    var atkAlloc by remember { mutableIntStateOf(5) }
    var defAlloc by remember { mutableIntStateOf(5) }
    var spdAlloc by remember { mutableIntStateOf(5) }
    var points by remember { mutableIntStateOf(20) }
    var skill by remember { mutableStateOf(MartialSkill.REMNANT_EDGE) }
    var sect by remember { mutableStateOf(MartialSect.WANDERER) }

    Box(Modifier.fillMaxSize().background(BgDark)) {
        when (step) {
            0 -> NameAndGenderStep(
                name = name,
                gender = gender,
                error = error,
                onName = { name = it; error = "" },
                onGender = { gender = it },
                onNext = {
                    val trimmed = name.trim()
                    if (trimmed.length < 2 || trimmed.length > 8) {
                        error = "名字需2-8个字"; return@NameAndGenderStep
                    }
                    if (Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?￥]").containsMatchIn(trimmed)) {
                        error = "不能包含特殊字符"; return@NameAndGenderStep
                    }
                    viewModel.engine.soundManager.playSfx("wood_confirm")
                    name = trimmed
                    step = 1
                }
            )
            1 -> AllocationStep(
                name = name,
                gender = gender,
                hpAlloc = hpAlloc,
                atkAlloc = atkAlloc,
                defAlloc = defAlloc,
                spdAlloc = spdAlloc,
                points = points,
                skill = skill,
                sect = sect,
                onHp = { delta -> if (applyAlloc(delta, hpAlloc, points)) { hpAlloc += delta; points -= delta } },
                onAtk = { delta -> if (applyAlloc(delta, atkAlloc, points)) { atkAlloc += delta; points -= delta } },
                onDef = { delta -> if (applyAlloc(delta, defAlloc, points)) { defAlloc += delta; points -= delta } },
                onSpd = { delta -> if (applyAlloc(delta, spdAlloc, points)) { spdAlloc += delta; points -= delta } },
                onSkill = { skill = it },
                onSect = { sect = it },
                onBack = { step = 0 },
                onStart = {
                    viewModel.engine.soundManager.playSfx("wood_confirm")
                    viewModel.engine.createCharacter(name, gender, hpAlloc, atkAlloc, defAlloc, spdAlloc, skill, sect)
                    onCreated()
                }
            )
        }
    }
}

private fun applyAlloc(delta: Int, current: Int, points: Int): Boolean {
    if (delta > 0) return points > 0
    return current > 0
}

@Composable
private fun NameAndGenderStep(
    name: String,
    gender: String,
    error: String,
    onName: (String) -> Unit,
    onGender: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("创建角色", color = GoldAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("选择身份，留下姓名", color = TextGray, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GenderCard("male", "男主", "characters/hero_male.png", gender, onGender, Modifier.weight(1f))
            GenderCard("female", "女主", "characters/hero_female.png", gender, onGender, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            placeholder = { Text("输入姓名（2-8字）", color = TextGray) },
            label = { Text("姓名", color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = BorderDim,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = GoldAccent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotEmpty()) Text(error, color = HpRed, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(18.dp))
        PixelButton("下一步：分配属性", onNext, Modifier.fillMaxWidth().height(48.dp), GoldAccent)
    }
}

@Composable
private fun GenderCard(
    value: String,
    label: String,
    asset: String,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    val ctx = LocalContext.current
    val bitmap = remember(asset) {
        try { BitmapFactory.decodeStream(ctx.assets.open(asset))?.asImageBitmap() } catch (_: Exception) { null }
    }
    val border = if (selected == value) GoldAccent else BorderDim
    Column(
        modifier.border(1.dp, border, RoundedCornerShape(8.dp))
            .background(BgPanel, RoundedCornerShape(8.dp))
            .clickable { onSelect(value) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap, label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Text(label, color = TextWhite)
        }
        Text(label, color = if (selected == value) GoldAccent else TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AllocationStep(
    name: String,
    gender: String,
    hpAlloc: Int,
    atkAlloc: Int,
    defAlloc: Int,
    spdAlloc: Int,
    points: Int,
    skill: MartialSkill,
    sect: MartialSect,
    onHp: (Int) -> Unit,
    onAtk: (Int) -> Unit,
    onDef: (Int) -> Unit,
    onSpd: (Int) -> Unit,
    onSkill: (MartialSkill) -> Unit,
    onSect: (MartialSect) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("分配属性", color = GoldAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("$name · ${if (gender == "female") "女侠" else "少侠"}", color = TextGray, fontSize = 13.sp)
            }
            Text("剩余 $points", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        AllocRow("气血", hpAlloc, "${50 * hpAlloc}", onAdd = { onHp(1) }, onMinus = { onHp(-1) })
        AllocRow("攻击", atkAlloc, "${10 * atkAlloc}", onAdd = { onAtk(1) }, onMinus = { onAtk(-1) })
        AllocRow("防御", defAlloc, "${10 * defAlloc}", onAdd = { onDef(1) }, onMinus = { onDef(-1) })
        AllocRow("身法", spdAlloc, "%.2f".format(0.4 + 0.02 * spdAlloc), onAdd = { onSpd(1) }, onMinus = { onSpd(-1) })

        Spacer(Modifier.height(14.dp))
        Text("选择门派", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().border(1.dp, BorderDim, RoundedCornerShape(8.dp)).background(BgPanel, RoundedCornerShape(8.dp)).padding(8.dp)) {
            MartialSect.entries.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSect(s) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sect == s,
                        onClick = { onSect(s) },
                        colors = RadioButtonDefaults.colors(selectedColor = GoldAccent, unselectedColor = TextGray),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.displayName, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(s.description, color = TextGray, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("初始天赋", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().border(1.dp, BorderDim, RoundedCornerShape(8.dp)).background(BgPanel, RoundedCornerShape(8.dp)).padding(8.dp)) {
            MartialSkill.entries.filter { !listOf("BLOODTHIRST", "PRECISION").contains(it.name) }.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSkill(s) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = skill == s,
                        onClick = { onSkill(s) },
                        colors = RadioButtonDefaults.colors(selectedColor = GoldAccent, unselectedColor = TextGray),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.displayName, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(s.description, color = TextGray, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelButton("返回", onBack, Modifier.weight(1f).height(46.dp), BorderDim)
            PixelButton("踏入江湖", onStart, Modifier.weight(1f).height(46.dp), GoldAccent)
        }
    }
}

@Composable
private fun AllocRow(label: String, value: Int, displayValue: String, onAdd: () -> Unit, onMinus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .border(1.dp, BorderDim, RoundedCornerShape(6.dp))
            .background(BgPanel, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("当前值 $displayValue", color = TextGray, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallStepButton("-", onMinus)
            Text("$value", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
            SmallStepButton("+", onAdd)
        }
    }
}

@Composable
private fun SmallStepButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.size(34.dp).border(1.dp, BorderGold, RoundedCornerShape(4.dp)),
        contentPadding = PaddingValues(0.dp)
    ) { Text(text, color = TextWhite, fontSize = 18.sp) }
}

@Composable
private fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier, border: Color) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(1.dp, border, RoundedCornerShape(6.dp)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) { Text(text, color = TextWhite, fontSize = 15.sp) }
}
