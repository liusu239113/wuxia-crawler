package com.wuxiacrawler.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuxiacrawler.data.CombatState
import com.wuxiacrawler.data.EquipmentItem
import com.wuxiacrawler.ui.theme.*
import com.wuxiacrawler.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val muted by engine.soundManager.muted.collectAsState()
    val btPending by engine.realmBreakthroughPending.collectAsState()
    val btInfo by engine.realmBreakthroughInfo.collectAsState()
    val sprite by engine.currentEnemySprite.collectAsState()
    val eFlinch by engine.enemyFlinch.collectAsState()
    val pFlinch by engine.playerFlinch.collectAsState()
    val dmgNums by engine.dmgNumbers.collectAsState()
    val showLvlUp by engine.showLevelUp.collectAsState()

    var tab by remember { mutableIntStateOf(0) }

    // 境界突破弹窗
    if (btPending && btInfo != null) {
        AlertDialog(onDismissRequest = { engine.dismissBreakthrough() }, containerColor = InkDark,
            title = { Text("🌟 境界突破", color = GoldBright, fontWeight = FontWeight.Bold) },
            text = { Text("从【${btInfo!!.first}】突破到【${btInfo!!.second}】！\n突破后获得大幅属性加成，气血回满。", color = ParchmentLight) },
            confirmButton = { Button(onClick = { engine.confirmBreakthrough() }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) { Text("突破！", color = InkBlack) } },
            dismissButton = { TextButton(onClick = { engine.dismissBreakthrough() }) { Text("暂缓", color = GrayStone) } }
        )
    }

    // 升级弹窗
    if (showLvlUp && upgrades.isNotEmpty()) {
        AlertDialog(onDismissRequest = {}, containerColor = InkDark,
            title = { Text("⬆️ 境界提升", color = GoldAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("剩余: ${player.exp.lvlGained}  重骰: ${rerolls}/2", color = GrayStone, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    upgrades.forEachIndexed { i, opt ->
                        Button(onClick = { engine.selectUpgrade(i) }, colors = ButtonDefaults.buttonColors(containerColor = GreenJade), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${opt.stat} +${"%.1f".format(opt.value)}%")
                        }
                    }
                    if (rerolls > 0) TextButton(onClick = { engine.rerollUpgrades() }) { Text("重骰（$rerolls次）", color = GoldAccent) }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // 秘境时钟
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

    // Flinch auto-reset
    LaunchedEffect(eFlinch) { if (eFlinch) { delay(150); engine._enemyFlinch.value = false } }
    LaunchedEffect(pFlinch) { if (pFlinch) { delay(150); engine._playerFlinch.value = false } }

    Scaffold(containerColor = InkBlack,
        topBar = {
            Surface(color = InkDark, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("${player.name} Lv.${player.lvl}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("境界: ${player.realm}", color = GreenJade, fontSize = 11.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("${realm.floor}层${realm.room}/${realm.roomsPerFloor}室", color = Color.White, fontSize = 13.sp); IconButton(onClick = { engine.soundManager.toggleMute() }) { Text(if (muted) "🔇" else "🔊", fontSize = 16.sp) } }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatChip("气血", "${player.stats.hp}/${player.stats.hpMax}", RedAccent, Modifier.weight(1f))
                        StatChip("攻击", "${player.stats.atk}", GoldAccent, Modifier.weight(1f))
                        StatChip("防御", "${player.stats.def}", BlueSteel, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatChip("白银", "${player.gold}", GoldBright, Modifier.weight(1f))
                        StatChip("身法", "%.1f".format(player.stats.atkSpd), GreenJade, Modifier.weight(1f))
                        StatChip("击杀", "${player.kills}", RedAccent, Modifier.weight(1f))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = InkDark) {
                listOf("秘境探索" to "⚔️", "行囊背包" to "🎒").forEachIndexed { i, (t, icon) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(icon, fontSize = 18.sp) }, label = { Text(t, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = GoldAccent, selectedTextColor = GoldAccent, unselectedIconColor = GrayStone, unselectedTextColor = GrayStone, indicatorColor = InkBlack))
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> ExplorationPanel(engine, realm, cs, cLog, rLog, upgrades, rerolls, sprite, eFlinch, pFlinch, dmgNums, padding, onDeath)
            1 -> InventoryPanel(engine, padding)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, mod: Modifier) {
    Surface(color = InkBlack, shape = RoundedCornerShape(4.dp), modifier = mod.padding(horizontal = 1.dp)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = GrayStone, fontSize = 9.sp); Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExplorationPanel(engine: com.wuxiacrawler.engine.GameEngine, realm: com.wuxiacrawler.data.RealmState, cs: CombatState?, cLog: List<String>, rLog: List<String>, upgrades: List<com.wuxiacrawler.config.UpgradeOption>, rerolls: Int, sprite: String, eFlinch: Boolean, pFlinch: Boolean, dmgNums: List<com.wuxiacrawler.engine.GameEngine.DmgNumber>, padding: PaddingValues, onDeath: () -> Unit) {
    val ctx = LocalContext.current
    Box(Modifier.padding(padding).fillMaxSize()) {
        if (cs != null && !cs.enemyDead && !cs.playerDead) CombatPanel(cs, cLog, sprite, eFlinch, pFlinch, dmgNums, ctx)
        else if (cs?.enemyDead == true || cs?.playerDead == true) ResultPanel(engine, cs!!, upgrades, rerolls, onDeath)
        else ExplorePanel(engine, realm, rLog)
    }
}

@Composable
private fun CombatPanel(cs: CombatState, log: List<String>, sprite: String, eFl: Boolean, pFl: Boolean, dmgNums: List<com.wuxiacrawler.engine.GameEngine.DmgNumber>, ctx: android.content.Context) {
    val eShake by animateFloatAsState(if (eFl) 6f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val pShake by animateFloatAsState(if (pFl) 6f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

    // 加载精灵图
    val bitmap = remember(sprite) {
        try { BitmapFactory.decodeStream(ctx.assets.open("sprites/${sprite}.png"))?.asImageBitmap() } catch (_: Exception) { null }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // 敌人
        Surface(color = RedDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${cs.enemyName} Lv.${cs.enemyLvl}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(140.dp).graphicsLayer { translationX = eShake }, contentAlignment = Alignment.Center) {
                    if (bitmap != null) Image(bitmap, "enemy", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Text("👹", fontSize = 64.sp)
                }
                HpBar(cs.enemyHp, cs.enemyHpMax, RedAccent)
                Text("${cs.enemyHp}/${cs.enemyHpMax}", color = Color.White, fontSize = 11.sp)
                // 伤害飘字
                Box(Modifier.height(30.dp).fillMaxWidth()) {
                    dmgNums.takeLast(3).forEachIndexed { i, dn ->
                        Text(dn.text, color = if (dn.isCrit) GoldBright else Color.White, fontSize = (14 - i * 2).sp, fontWeight = if (dn.isCrit) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.align(Alignment.Center).offset(y = (-i * 16).dp).graphicsLayer { alpha = 1f - i * 0.3f })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // 玩家
        Surface(color = InkDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).graphicsLayer { translationX = pShake }, contentAlignment = Alignment.Center) { Text("⚔️", fontSize = 44.sp) }
                HpBar(cs.playerHp, cs.playerHpMax, GreenJade)
                Text("${cs.playerHp}/${cs.playerHpMax}", color = Color.White, fontSize = 11.sp)
                Text("攻:${cs.playerAtk} 防:${cs.playerDef} 速:${"%.1f".format(cs.playerAtkSpd)}", color = GrayStone, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("战斗记录", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
            items(log.reversed().take(15)) { Text(it, color = ParchmentLight, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp)) }
        }
    }
}

@Composable
private fun ResultPanel(engine: com.wuxiacrawler.engine.GameEngine, cs: CombatState, upgrades: List<com.wuxiacrawler.config.UpgradeOption>, rerolls: Int, onDeath: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (cs.playerDead) {
            Text("身死道消", color = RedAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("境界跌落，装备保留。", color = ParchmentLight, fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDeath, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), shape = RoundedCornerShape(12.dp)) { Text("返回江湖", fontSize = 18.sp) }
        } else {
            Text("大获全胜！", color = GoldAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${cs.expReward}修为  ${cs.goldReward}白银", color = ParchmentLight, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { engine.dismissCombatResult() }, colors = ButtonDefaults.buttonColors(containerColor = GreenJade), shape = RoundedCornerShape(12.dp)) { Text("继续探索", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun ExplorePanel(engine: com.wuxiacrawler.engine.GameEngine, realm: com.wuxiacrawler.data.RealmState, rLog: List<String>) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (realm.isExploring) Button(onClick = { engine.pauseExploring() }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), shape = RoundedCornerShape(8.dp)) { Text("暂停探索") }
            else Button(onClick = { engine.startExploring() }, colors = ButtonDefaults.buttonColors(containerColor = GreenJade), shape = RoundedCornerShape(8.dp)) { Text("开始探索") }
            Text(if (realm.isExploring) "探索中……" else "已暂停", color = if (realm.isExploring) GreenJade else GrayStone, fontSize = 13.sp)
        }

        if (realm.isEventActive && rLog.isNotEmpty()) {
            val last = rLog.last()
            if (last.contains("[选项:")) {
                val opts = last.substringAfter("[选项:").substringBefore("]").split("|").map { it.trim() }
                Surface(color = InkDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("选择行动", color = GoldAccent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        opts.forEachIndexed { i, o ->
                            Button(onClick = { engine.chooseOption(i) }, colors = ButtonDefaults.buttonColors(containerColor = BlueSteel), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(o, color = Color.White) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Text("秘境日志", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 8.dp), reverseLayout = true) {
            items(rLog.reversed().take(40)) { log -> Text(log.replace("[选项:.*]".toRegex(), ""), color = ParchmentLight, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp)) }
        }
    }
}

@Composable
fun HpBar(current: Int, max: Int, color: Color) {
    val pct = (current.toFloat() / max).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray)) {
        Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(color))
    }
}

@Composable
private fun InventoryPanel(engine: com.wuxiacrawler.engine.GameEngine, padding: PaddingValues) {
    val player by engine.player.collectAsState()
    val eq = remember(player.equipped) { engine.parseEquipped() }
    val inv = remember(player.inventory) { engine.parseInventory() }
    var sellRarity by remember { mutableStateOf("全部") }

    LazyColumn(Modifier.padding(padding).padding(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已装备 (${eq.size}/6)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("白银: ${player.gold}", color = GoldBright, fontSize = 13.sp)
            }
        }
        if (eq.isEmpty()) item { Text("未穿戴装备", color = GrayStone, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp)) }
        itemsIndexed(eq) { i, it -> EquipRow(it, "卸下", "出售", RarityColors[it.rarity] ?: GrayStone, { engine.unequipItem(i) }, { engine.sellItem(true, i) }) }
        if (eq.isNotEmpty()) item { TextButton(onClick = { engine.unequipAll() }) { Text("全部卸下", color = RedAccent, fontSize = 12.sp) } }

        item { Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("背包 (${inv.size})", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val rarities = listOf("全部") + EquipmentRarity.entries.map { it.displayName }
                var expanded by remember { mutableStateOf(false) }
                Box { TextButton(onClick = { expanded = true }) { Text("批量出售", color = RedAccent, fontSize = 11.sp) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rarities.forEach { r -> DropdownMenuItem(text = { Text(r, fontSize = 12.sp) }, onClick = { sellRarity = r; expanded = false; engine.sellAll(r) }) }
                    }
                }
            }
        } }
        if (inv.isEmpty()) item { Text("背包空空如也", color = GrayStone, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp)) }
        itemsIndexed(inv) { i, it -> EquipRow(it, "装备", "出售", RarityColors[it.rarity] ?: GrayStone, { engine.equipItem(i) }, { engine.sellItem(false, i) }) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun EquipRow(item: EquipmentItem, pLabel: String, sLabel: String, rCol: Color, onP: () -> Unit, onS: () -> Unit) {
    Surface(color = InkDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${item.rarity} ${item.category}", color = rCol, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Lv.${item.lvl} 品阶${item.tier} | ${item.value}两", color = GrayStone, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.stats.forEach { sm -> sm.forEach { (k, v) -> val u = if (k in listOf("atkSpd", "vamp", "critRate", "critDmg")) "%" else ""; Text("${statDisp(k)}+${"%.1f".format(v)}$u", color = ParchmentLight, fontSize = 10.sp) } }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onP, colors = ButtonDefaults.buttonColors(containerColor = rCol.copy(alpha = 0.3f)), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(28.dp)) { Text(pLabel, color = rCol, fontSize = 11.sp) }
                Button(onClick = onS, colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.3f)), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(28.dp)) { Text("${sLabel}(${item.value}两)", color = RedAccent, fontSize = 10.sp) }
            }
        }
    }
}

private fun statDisp(k: String) = when (k) { "hp" -> "气血"; "atk" -> "攻击"; "def" -> "防御"; "atkSpd" -> "身法"; "vamp" -> "吸血"; "critRate" -> "暴率"; "critDmg" -> "暴伤"; else -> k }