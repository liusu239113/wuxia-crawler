package com.wuxiacrawler.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.data.CombatState
import com.wuxiacrawler.data.EquipmentItem
import com.wuxiacrawler.config.EquipmentRarity
import com.wuxiacrawler.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 原版配色：纯黑背景 + 白字 + 白边框
private val BgDark = Color(0xFF0D0D0D)
private val BgPanel = Color(0xFF1A1A1A)
private val BorderWhite = Color(0xFF555555)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF999999)
private val HpRed = Color(0xFFE40000)
private val BarPurple = Color(0xFF9A00B9)
private val BtnHover = Color(0xFF333333)

private val RarityCol = mapOf(
    "凡品" to Color(0xFFFFFFFF),
    "良品" to Color(0xFF1EFF00),
    "稀有" to Color(0xFF0070DD),
    "史诗" to Color(0xFFA335EE),
    "传说" to Color(0xFFFFD700),
    "太古" to Color(0xFFE30B5C)
)

@Composable
fun MainScreen(viewModel: GameViewModel, onDeath: () -> Unit) {
    val engine = viewModel.engine
    val player by engine.player.collectAsState()
    val realm by engine.realm.collectAsState()
    val cs by engine.combatState.collectAsState()
    val cLog by engine.combatLog.collectAsState()
    val rLog by engine.realmLog.collectAsState()
    val upgrades by engine.availableUpgrades.collectAsState()
    val rerolls by engine.rerollsLeft.collectAsState()
    val btPending by engine.realmBreakthroughPending.collectAsState()
    val btInfo by engine.realmBreakthroughInfo.collectAsState()
    val sprite by engine.currentEnemySprite.collectAsState()
    val eFlinch by engine.enemyFlinch.collectAsState()
    val pFlinch by engine.playerFlinch.collectAsState()
    val dmgNums by engine.dmgNumbers.collectAsState()
    val showLvlUp by engine.showLevelUp.collectAsState()
    val muted by engine.soundManager.muted.collectAsState()

    var invOpen by remember { mutableStateOf(false) }

    // 境界突破弹窗
    if (btPending && btInfo != null) {
        AlertDialog(onDismissRequest = {}, containerColor = BgPanel,
            title = { Text("🌟 境界突破", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("从【${btInfo!!.first}】突破到【${btInfo!!.second}】！\n突破后获得大幅属性加成，气血回满。", color = TextWhite) },
            confirmButton = { Button(onClick = { engine.confirmBreakthrough() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00))) { Text("突破！", color = Color.Black) } },
            dismissButton = { TextButton(onClick = { engine.dismissBreakthrough() }) { Text("暂缓", color = TextGray) } }
        )
    }

    // 升级弹窗
    if (showLvlUp && upgrades.isNotEmpty()) {
        AlertDialog(onDismissRequest = {}, containerColor = BgPanel,
            title = { Text("⬆️ 境界提升", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("剩余: ${player.exp.lvlGained}  重骰: ${rerolls}/2", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    upgrades.forEachIndexed { i, opt ->
                        Button(onClick = { engine.selectUpgrade(i) }, colors = ButtonDefaults.buttonColors(containerColor = BorderWhite), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${opt.stat} +${"%.1f".format(opt.value)}%", color = TextWhite)
                        }
                    }
                    if (rerolls > 0) TextButton(onClick = { engine.rerollUpgrades() }) { Text("重骰（${rerolls}次）", color = TextWhite) }
                }
            }, confirmButton = {}, dismissButton = {}
        )
    }

    // 秘境探索时钟
    LaunchedEffect(realm.isExploring, realm.isEventActive) {
        while (realm.isExploring && !realm.isEventActive) { delay(1000); engine.tickRealm() }
    }

    // 战斗时钟
    LaunchedEffect(cs) {
        val c = cs ?: return@LaunchedEffect
        if (c.enemyDead || c.playerDead) return@LaunchedEffect
        val pInt = (1000f / c.playerAtkSpd).toLong().coerceAtLeast(400)
        val eInt = (1000f / c.enemyAtkSpd).toLong().coerceAtLeast(400)
        launch { while (engine.combatState.value?.enemyDead == false && engine.combatState.value?.playerDead == false) { delay(pInt); if (!engine.playerAttack()) break } }
        launch { while (engine.combatState.value?.enemyDead == false && engine.combatState.value?.playerDead == false) { delay(eInt); if (!engine.enemyAttack()) break } }
    }

    LaunchedEffect(eFlinch) { if (eFlinch) { delay(150); engine._enemyFlinch.value = false } }
    LaunchedEffect(pFlinch) { if (pFlinch) { delay(150); engine._playerFlinch.value = false } }

    // 主界面盒子模式
    Box(Modifier.fillMaxSize().background(BgDark)) {
        Column(Modifier.fillMaxSize()) {
            // ========== HEADER ========== 
            Row(Modifier.fillMaxWidth().padding(6.dp).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(player.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Lv.${player.lvl} | 白银:${player.gold} | 修为:${player.exp.expCurr}/${player.exp.expMax}", color = TextGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (muted) "🔇" else "🔊", color = TextWhite, fontSize = 14.sp, modifier = Modifier.clickable { engine.soundManager.toggleMute() })
                    Spacer(Modifier.width(8.dp))
                    Text("☰", color = TextWhite, fontSize = 18.sp, modifier = Modifier.clickable { invOpen = true })
                }
            }

            // ========== STAT PANEL (双列) ==========
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 基础属性
                Box(Modifier.weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(6.dp)) {
                    Column {
                        Text("属性", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        StatLine("气血", "${player.stats.hp}/${player.stats.hpMax}", HpRed)
                        StatLine("攻击", "${player.stats.atk}", TextWhite)
                        StatLine("防御", "${player.stats.def}", TextWhite)
                        StatLine("身法", "%.1f".format(player.stats.atkSpd), TextWhite)
                        StatLine("吸血", "%.1f%%".format(player.stats.vamp), TextWhite)
                        StatLine("暴率", "%.1f%%".format(player.stats.critRate), TextWhite)
                        StatLine("暴伤", "%.1f%%".format(player.stats.critDmg), TextWhite)
                    }
                }
                // 加成属性
                Box(Modifier.weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(6.dp)) {
                    Column {
                        Text("加成", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        StatLine("气血", "+${"%.1f".format(player.bonusStats.hp)}%", TextWhite)
                        StatLine("攻击", "+${"%.1f".format(player.bonusStats.atk)}%", TextWhite)
                        StatLine("防御", "+${"%.1f".format(player.bonusStats.def)}%", TextWhite)
                        StatLine("身法", "+${"%.1f".format(player.bonusStats.atkSpd)}%", TextWhite)
                        StatLine("吸血", "+${"%.1f".format(player.bonusStats.vamp)}%", TextWhite)
                        StatLine("暴率", "+${"%.1f".format(player.bonusStats.critRate)}%", TextWhite)
                        StatLine("暴伤", "+${"%.1f".format(player.bonusStats.critDmg)}%", TextWhite)
                    }
                }
            }

            // ========== DUNGEON HEAD ==========
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${realm.floor}层 ${realm.room}/${realm.roomsPerFloor}室", color = TextGray, fontSize = 12.sp)
                Text("击杀:${realm.currentKills}", color = TextGray, fontSize = 12.sp)
                Button(
                    onClick = { if (realm.isExploring) engine.pauseExploring() else engine.startExploring() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(32.dp).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Text(if (realm.isExploring) "暂停" else "探索", color = TextWhite, fontSize = 13.sp)
                }
            }

            // ========== EVENT CHOICES ==========
            if (realm.isEventActive && rLog.isNotEmpty()) {
                val last = rLog.last()
                if (last.contains("[选项:")) {
                    val opts = last.substringAfter("[选项:").substringBefore("]").split("|").map { it.trim() }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        opts.forEachIndexed { i, o ->
                            Button(
                                onClick = { engine.chooseOption(i) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(34.dp).border(1.dp, BorderWhite, RoundedCornerShape(4.dp))
                            ) { Text(o, color = TextWhite, fontSize = 13.sp) }
                        }
                    }
                }
            }

            // ========== LOG AREA (fills remaining space) ==========
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp).padding(bottom = 6.dp).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(6.dp)) {
                LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                    items(rLog.reversed().take(40)) { log ->
                        Text(
                            log.replace("[选项:.*]".toRegex(), ""),
                            color = TextWhite, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // ========== COMBAT OVERLAY ==========
        if (cs != null && !cs.enemyDead && !cs.playerDead) {
            CombatOverlay(cs, cLog, sprite, eFlinch, pFlinch, dmgNums, engine)
        }

        // ========== COMBAT RESULT OVERLAY ==========
        if (cs?.enemyDead == true || cs?.playerDead == true) {
            CombatResultOverlay(cs!!, engine, onDeath)
        }

        // ========== INVENTORY MODAL ==========
        if (invOpen) {
            InventoryModal(engine, player, { invOpen = false })
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, color: Color) {
    Row(Modifier.padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label:", color = TextGray, fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ===== COMBAT OVERLAY =====
@Composable
private fun CombatOverlay(cs: CombatState, log: List<String>, sprite: String, eFl: Boolean, pFl: Boolean, dmgNums: List<com.wuxiacrawler.engine.GameEngine.DmgNumber>, engine: com.wuxiacrawler.engine.GameEngine) {
    val ctx = LocalContext.current
    val eShake by animateFloatAsState(if (eFl) 8f else 0f, spring(stiffness = Spring.StiffnessHigh))
    val pShake by animateFloatAsState(if (pFl) 4f else 0f, spring(stiffness = Spring.StiffnessHigh))

    val bitmap = remember(sprite) {
        try { BitmapFactory.decodeStream(ctx.assets.open("sprites/${sprite}.png"))?.asImageBitmap() } catch (_: Exception) { null }
    }

    Box(Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.95f)).padding(8.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))

            // 敌人区域
            Column(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${cs.enemyName} Lv.${cs.enemyLvl}", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(130.dp).graphicsLayer { translationX = eShake }, contentAlignment = Alignment.Center) {
                    if (bitmap != null) Image(bitmap, "enemy", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Text("👹", fontSize = 56.sp)
                }
                Spacer(Modifier.height(8.dp))
                // HP Bar
                val ePct = (cs.enemyHp.toFloat() / cs.enemyHpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                    Box(Modifier.fillMaxWidth(ePct).fillMaxHeight().background(HpRed))
                }
                Text("${cs.enemyHp}/${cs.enemyHpMax}", color = TextWhite, fontSize = 11.sp)
                // 伤害飘字
                Box(Modifier.height(24.dp)) {
                    dmgNums.takeLast(3).forEachIndexed { i, dn ->
                        Text(dn.text, color = if (dn.isCrit) Color(0xFFFFD700) else TextWhite,
                            fontSize = (16 - i * 3).sp, fontWeight = if (dn.isCrit) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.offset(y = (-i * 14).dp).graphicsLayer { alpha = 1f - i * 0.3f })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 玩家区域
            Column(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(engine.player.value.name, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(60.dp).graphicsLayer { translationX = pShake }, contentAlignment = Alignment.Center) { Text("⚔️", fontSize = 32.sp) }
                val pPct = (cs.playerHp.toFloat() / cs.playerHpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                    Box(Modifier.fillMaxWidth(pPct).fillMaxHeight().background(HpRed))
                }
                Text("${cs.playerHp}/${cs.playerHpMax}", color = TextWhite, fontSize = 11.sp)
                Text("攻:${cs.playerAtk} 防:${cs.playerDef} 速:${"%.1f".format(cs.playerAtkSpd)}", color = TextGray, fontSize = 10.sp)
            }

            Spacer(Modifier.height(6.dp))

            // 战斗日志
            Box(Modifier.fillMaxWidth().weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(4.dp)) {
                LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                    items(log.reversed().take(20)) { Text(it, color = TextWhite, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp)) }
                }
            }
        }
    }
}

// ===== COMBAT RESULT =====
@Composable
private fun CombatResultOverlay(cs: CombatState, engine: com.wuxiacrawler.engine.GameEngine, onDeath: () -> Unit) {
    Box(Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.95f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cs.playerDead) {
                Text("身死道消", color = HpRed, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("境界跌落，装备保留。", color = TextWhite, fontSize = 16.sp)
                Button(onClick = onDeath, colors = ButtonDefaults.buttonColors(containerColor = HpRed), shape = RoundedCornerShape(6.dp)) {
                    Text("返回江湖", color = TextWhite, fontSize = 18.sp)
                }
            } else {
                Text("大获全胜！", color = Color(0xFFFFD700), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("${cs.expReward}修为  ${cs.goldReward}白银", color = TextWhite, fontSize = 16.sp)
                Button(onClick = { engine.dismissCombatResult() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00)), shape = RoundedCornerShape(6.dp)) {
                    Text("继续探索", color = Color.Black, fontSize = 18.sp)
                }
            }
        }
    }
}

// ===== INVENTORY MODAL =====
@Composable
private fun InventoryModal(engine: com.wuxiacrawler.engine.GameEngine, player: com.wuxiacrawler.data.PlayerEntity, onClose: () -> Unit) {
    val eq = remember(player.equipped) { engine.parseEquipped() }
    val inv = remember(player.inventory) { engine.parseInventory() }
    var sellRarity by remember { mutableStateOf("全部") }
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.95f))) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("行囊", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("✕", color = TextWhite, fontSize = 20.sp, modifier = Modifier.clickable { onClose() })
            }
            Spacer(Modifier.height(4.dp))

            // 批量出售
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { expanded = true }) { Text("批量出售", color = HpRed, fontSize = 12.sp) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (listOf("全部") + EquipmentRarity.entries.map { it.displayName }).forEach { r ->
                            DropdownMenuItem(text = { Text(r, fontSize = 12.sp, color = RarityCol[r] ?: TextWhite) }, onClick = { sellRarity = r; expanded = false; engine.sellAll(r) })
                        }
                    }
                }
                Text("白银:${player.gold}", color = TextWhite, fontSize = 13.sp)
            }

            // 装备区
            Text("装备 (${eq.size}/6)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (eq.isEmpty()) Text("未穿戴装备", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                eq.forEachIndexed { i, item ->
                    Box(Modifier.weight(1f).border(1.dp, RarityCol[item.rarity] ?: BorderWhite, RoundedCornerShape(4.dp)).padding(4.dp).clickable { engine.unequipItem(i) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.category, color = RarityCol[item.rarity] ?: TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Lv${item.lvl}", color = TextGray, fontSize = 9.sp)
                        }
                    }
                }
            }
            if (eq.isNotEmpty()) TextButton(onClick = { engine.unequipAll() }) { Text("全部卸下", color = HpRed, fontSize = 11.sp) }
            Spacer(Modifier.height(8.dp))

            // 背包列表
            Text("背包 (${inv.size})", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (inv.isEmpty()) Text("背包空空如也", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(inv) { i, item ->
                    Row(Modifier.fillMaxWidth().border(1.dp, RarityCol[item.rarity] ?: BorderWhite, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { engine.equipItem(i) }) {
                            Text("${item.rarity} ${item.category}", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Lv.${item.lvl} | ${item.value}两", color = TextGray, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item.stats.forEach { sm -> sm.forEach { (k, v) ->
                                    val u = if (k in listOf("atkSpd", "vamp", "critRate", "critDmg")) "%" else ""
                                    Text("${statDisp(k)}+${"%.1f".format(v)}$u", color = TextWhite, fontSize = 10.sp)
                                } }
                            }
                        }
                        TextButton(onClick = { engine.sellItem(false, i) }) { Text("出售", color = HpRed, fontSize = 11.sp) }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

private fun statDisp(k: String) = when (k) {
    "hp" -> "气血"; "atk" -> "攻击"; "def" -> "防御"; "atkSpd" -> "身法"
    "vamp" -> "吸血"; "critRate" -> "暴率"; "critDmg" -> "暴伤"; else -> k
}