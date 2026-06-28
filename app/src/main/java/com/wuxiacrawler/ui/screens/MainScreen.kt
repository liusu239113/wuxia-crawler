package com.wuxiacrawler.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import com.wuxiacrawler.config.EquipmentRarity
import com.wuxiacrawler.config.CultivationRealm
import com.wuxiacrawler.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ===== 配色 =====
private val BgDark = Color(0xFF0D0D0D)
private val BgPanel = Color(0xFF1A1A1A)
private val BorderWhite = Color(0xFF555555)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF999999)
private val HpRed = Color(0xFFE40000)
private val GoldAccent = Color(0xFFFFD700)

private val RarityCol = mapOf(
    "凡品" to Color(0xFFFFFFFF), "良品" to Color(0xFF1EFF00),
    "稀有" to Color(0xFF0070DD), "史诗" to Color(0xFFA335EE),
    "传说" to Color(0xFFFFD700), "太古" to Color(0xFFE30B5C)
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

    var tab by remember { mutableIntStateOf(0) }

    // 弹窗
    BreakthroughDialog(btPending, btInfo, engine)
    LevelUpDialog(showLvlUp, upgrades, rerolls, player, engine)

    // 时钟
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val r = engine.realm.value
            if (r.isExploring && !r.isEventActive) engine.tickRealm()
        }
    }

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

    // ===== 主体 =====
    Box(Modifier.fillMaxSize().background(BgDark)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { TopHeader(player, muted) { engine.soundManager.toggleMute() } },
            bottomBar = { BottomTabs(tab) { tab = it } }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> AdventureTab(realm, player, rLog, engine)
                    1 -> InventoryTab(engine, player)
                    2 -> CharacterTab(player, engine)
                    3 -> SettingsTab(engine, player, muted) { onDeath() }
                }
            }
        }

        // 战斗覆盖层
        val combatCs = cs
        if (combatCs != null && !combatCs.enemyDead && !combatCs.playerDead)
            CombatOverlay(combatCs, cLog, sprite, eFlinch, pFlinch, dmgNums, engine)
        if (combatCs != null && (combatCs.enemyDead || combatCs.playerDead))
            CombatResultOverlay(combatCs, engine, onDeath)
    }
}

// ==================== DIALOGS ====================
@Composable
private fun BreakthroughDialog(pending: Boolean, info: Pair<String, String>?, engine: com.wuxiacrawler.engine.GameEngine) {
    if (!pending || info == null) return
    AlertDialog(onDismissRequest = {}, containerColor = BgPanel,
        title = { Text("🌟 境界突破", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = { Text("从【${info.first}】突破到【${info.second}】！\n获得大幅属性加成，气血回满。", color = TextWhite) },
        confirmButton = { Button(onClick = { engine.confirmBreakthrough() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00))) { Text("突破！", color = Color.Black) } },
        dismissButton = { TextButton(onClick = { engine.dismissBreakthrough() }) { Text("暂缓", color = TextGray) } }
    )
}

@Composable
private fun LevelUpDialog(show: Boolean, upgrades: List<com.wuxiacrawler.config.UpgradeOption>, rerolls: Int, player: com.wuxiacrawler.data.PlayerEntity, engine: com.wuxiacrawler.engine.GameEngine) {
    if (!show || upgrades.isEmpty()) return
    AlertDialog(onDismissRequest = {}, containerColor = BgPanel,
        title = { Text("⬆️ 境界提升", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("剩余: ${player.exp.lvlGained}  重骰: ${rerolls}/2", color = TextGray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                upgrades.forEachIndexed { i, opt ->
                    Button(onClick = { engine.selectUpgrade(i) },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderWhite),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { Text("${opt.stat} +${"%.1f".format(opt.value)}%", color = TextWhite) }
                }
                if (rerolls > 0) TextButton(onClick = { engine.rerollUpgrades() }) { Text("重骰（${rerolls}次）", color = TextWhite) }
            }
        }, confirmButton = {}, dismissButton = {}
    )
}

// ==================== TOP HEADER ====================
@Composable
private fun TopHeader(player: com.wuxiacrawler.data.PlayerEntity, muted: Boolean, onToggleMute: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(BgDark).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(player.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Lv.${player.lvl}  ⚔${player.kills}  💀${player.deaths}  🪙${player.gold}", color = TextGray, fontSize = 11.sp)
        }
        Box(Modifier.size(40.dp).clickable { onToggleMute() }, contentAlignment = Alignment.Center) {
            Text(if (muted) "🔇" else "🔊", color = TextWhite, fontSize = 16.sp)
        }
    }
}

// ==================== BOTTOM TABS ====================
@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = BgPanel, tonalElevation = 0.dp) {
        NavigationBarItem(selected = selected == 0, onClick = { onSelect(0) },
            icon = { Text("🏔️", fontSize = 20.sp) },
            label = { Text("江湖", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
        NavigationBarItem(selected = selected == 1, onClick = { onSelect(1) },
            icon = { Text("🎒", fontSize = 20.sp) },
            label = { Text("行囊", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
        NavigationBarItem(selected = selected == 2, onClick = { onSelect(2) },
            icon = { Text("🧘", fontSize = 20.sp) },
            label = { Text("角色", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
        NavigationBarItem(selected = selected == 3, onClick = { onSelect(3) },
            icon = { Text("⚙️", fontSize = 20.sp) },
            label = { Text("设置", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
    }
}

// ==================== TAB 0: 江湖 ====================
@Composable
private fun AdventureTab(realm: com.wuxiacrawler.data.RealmState, player: com.wuxiacrawler.data.PlayerEntity, rLog: List<String>, engine: com.wuxiacrawler.engine.GameEngine) {
    Column(Modifier.fillMaxSize()) {
        // 迷你属性条
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat("气血", "${player.stats.hp}/${player.stats.hpMax}", HpRed)
            MiniStat("攻", "${player.stats.atk}", TextWhite)
            MiniStat("防", "${player.stats.def}", TextWhite)
            MiniStat("速", "%.1f".format(player.stats.atkSpd), TextWhite)
            MiniStat("暴", "%.1f%%".format(player.stats.critRate), TextGray)
        }

        // 探索条
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${realm.floor}层 ${realm.room}/${realm.roomsPerFloor}室", color = TextGray, fontSize = 12.sp)
                Text("击杀:${realm.currentKills}", color = TextGray, fontSize = 11.sp)
            }
            Button(
                onClick = { if (realm.isExploring) engine.pauseExploring() else engine.startExploring() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(34.dp).border(1.dp, if (realm.isExploring) HpRed else BorderWhite, RoundedCornerShape(4.dp)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp)
            ) { Text(if (realm.isExploring) "⏸ 暂停" else "⚔ 探索", color = if (realm.isExploring) HpRed else TextWhite, fontSize = 14.sp) }
        }

        // 事件选项
        if (realm.isEventActive && rLog.isNotEmpty()) {
            val last = rLog.last()
            if (last.contains("[选项:")) {
                val opts = last.substringAfter("[选项:").substringBefore("]").split("|").map { it.trim() }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // 秘籍日志区
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp).padding(bottom = 6.dp)
            .border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(6.dp)) {
            if (rLog.isEmpty()) Text("踏入江湖，开始你的冒险……", color = TextGray, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            else LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                items(rLog.reversed().take(50)) { log ->
                    val clean = log.replace("[选项:.*]".toRegex(), "")
                    Text(clean, color = TextWhite, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Row(Modifier.background(BgPanel, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("$label:", color = TextGray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ==================== TAB 1: 行囊 ====================
@Composable
private fun InventoryTab(engine: com.wuxiacrawler.engine.GameEngine, player: com.wuxiacrawler.data.PlayerEntity) {
    val eq = engine.parseEquipped()
    val inv = engine.parseInventory()
    var sellRarity by remember { mutableStateOf("全部") }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🪙 ${player.gold} 两白银", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Box {
                TextButton(onClick = { expanded = true }) { Text("批量出售", color = HpRed, fontSize = 12.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (listOf("全部") + EquipmentRarity.entries.map { it.displayName }).forEach { r ->
                        DropdownMenuItem(text = { Text(r, fontSize = 12.sp, color = RarityCol[r] ?: TextWhite) },
                            onClick = { sellRarity = r; expanded = false; engine.sellAll(r) })
                    }
                }
            }
        }

        Text("⚔ 已装备 (${eq.size}/6)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (eq.isEmpty()) {
            Text("未穿戴装备", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
        } else {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                eq.forEachIndexed { i, item ->
                    Box(Modifier.weight(1f).border(1.dp, RarityCol[item.rarity] ?: BorderWhite, RoundedCornerShape(4.dp))
                        .padding(4.dp).clickable { engine.unequipItem(i) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.category, color = RarityCol[item.rarity] ?: TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Lv${item.lvl}", color = TextGray, fontSize = 9.sp)
                        }
                    }
                }
            }
            TextButton(onClick = { engine.unequipAll() }) { Text("全部卸下", color = HpRed, fontSize = 11.sp) }
        }

        HorizontalDivider(color = BorderWhite, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

        Text("🎒 背包 (${inv.size})", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (inv.isEmpty()) {
            Text("背包空空如也", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(inv) { i, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        .border(1.dp, (RarityCol[item.rarity] ?: BorderWhite).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                }
            }
        }
    }
}

// ==================== TAB 2: 角色 ====================
@Composable
private fun CharacterTab(player: com.wuxiacrawler.data.PlayerEntity, engine: com.wuxiacrawler.engine.GameEngine) {
    val realm = CultivationRealm.entries.find { it.name == player.realm } ?: CultivationRealm.NONE
    val nextRealm = CultivationRealm.entries.getOrNull(realm.ordinal + 1)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
        // 身份
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(player.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("境界: ${realm.displayName}", color = GoldAccent, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Lv.${player.lvl}", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("修为 ${player.exp.expCurr}/${player.exp.expMax}", color = TextGray, fontSize = 10.sp)
                    }
                }
                if (nextRealm != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("下一境界: ${nextRealm.displayName} (需Lv.${nextRealm.level * 10 + 10} + ${nextRealm.level * 5}击杀)", color = TextGray, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 属性
        SectionTitle("📊 战斗属性")
        StatRow("气血", "${player.stats.hp}/${player.stats.hpMax}", HpRed)
        StatRow("攻击", "${player.stats.atk}  (基础${player.baseStats.atk} +${"%.0f".format(player.bonusStats.atk)}%)", TextWhite)
        StatRow("防御", "${player.stats.def}  (基础${player.baseStats.def} +${"%.0f".format(player.bonusStats.def)}%)", TextWhite)
        StatRow("身法", "%.2f  (基础%.2f +%.0f%%)".format(player.stats.atkSpd, player.baseStats.atkSpd, player.bonusStats.atkSpd), TextWhite)
        StatRow("吸血", "%.1f%%".format(player.stats.vamp), TextWhite)
        StatRow("暴率", "%.1f%%".format(player.stats.critRate), TextWhite)
        StatRow("暴伤", "%.1f%%".format(player.stats.critDmg), TextWhite)

        Spacer(Modifier.height(6.dp))

        // 武学
        SectionTitle("📜 武学")
        val skillNames = player.skills.split(",").filter { it.isNotBlank() }
        if (skillNames.isEmpty()) Text("未习得武学", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        else skillNames.forEach { skill ->
            val info = skillInfo(skill)
            Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).background(BgPanel, RoundedCornerShape(4.dp)).padding(8.dp)) {
                Column {
                    Text(info.first, color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(info.second, color = TextGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 战绩
        SectionTitle("🏆 战绩")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock("击杀", "${player.kills}")
            StatBlock("死亡", "${player.deaths}")
            StatBlock("祝福等级", "${player.blessing}")
            StatBlock("探索时间", formatTime(player.playtime))
        }

        // 装备加成
        val eq = engine.parseEquipped()
        if (eq.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            SectionTitle("🛡 装备加成 (${eq.size}/6)")
            eq.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text("${item.rarity}${item.category} ", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 11.sp)
                    item.stats.forEach { sm -> sm.forEach { (k, v) ->
                        Text("${statDisp(k)}+${"%.1f".format(v)} ", color = TextGray, fontSize = 10.sp)
                    } }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextGray, fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp)
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(BgPanel, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(value, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}

// ==================== TAB 3: 设置 ====================
@Composable
private fun SettingsTab(engine: com.wuxiacrawler.engine.GameEngine, player: com.wuxiacrawler.data.PlayerEntity, muted: Boolean, onReturnTitle: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("⚙️ 设置", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        SettingButton("🔊 音效", if (muted) "已关闭" else "已开启") { engine.soundManager.toggleMute() }
        SettingButton("💾 保存进度", "手动存档") { engine.saveGame(); engine.soundManager.playSfx("confirm") }
        SettingButton("🗑 删除存档", "危险操作", isDanger = true) { engine.deleteSave(); onReturnTitle() }
        SettingButton("🚪 返回标题", "回到主菜单") { onReturnTitle() }

        Spacer(Modifier.height(20.dp))
        Text("武林秘境 v1.0", color = TextGray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("移植自 Dungeon Crawler RPG", color = TextGray, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingButton(label: String, hint: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, BorderWhite, RoundedCornerShape(6.dp))
        .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (isDanger) HpRed else TextWhite, fontSize = 14.sp)
        Text(hint, color = TextGray, fontSize = 11.sp)
    }
}

// ==================== COMBAT OVERLAY ====================
@Composable
private fun CombatOverlay(cs: CombatState, log: List<String>, sprite: String, eFl: Boolean, pFl: Boolean,
                           dmgNums: List<com.wuxiacrawler.engine.GameEngine.DmgNumber>, engine: com.wuxiacrawler.engine.GameEngine) {
    val ctx = LocalContext.current
    val eShake by animateFloatAsState(if (eFl) 8f else 0f, spring(stiffness = Spring.StiffnessHigh))
    val pShake by animateFloatAsState(if (pFl) 4f else 0f, spring(stiffness = Spring.StiffnessHigh))

    val bitmap = remember(sprite) {
        try { BitmapFactory.decodeStream(ctx.assets.open("sprites/${sprite}.png"))?.asImageBitmap() } catch (_: Exception) { null }
    }

    Box(Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.95f)).padding(8.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))

            Column(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${cs.enemyName} Lv.${cs.enemyLvl}", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(130.dp).graphicsLayer { translationX = eShake }, contentAlignment = Alignment.Center) {
                    if (bitmap != null) Image(bitmap, "enemy", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Text("👹", fontSize = 56.sp)
                }
                Spacer(Modifier.height(8.dp))
                val ePct = (cs.enemyHp.toFloat() / cs.enemyHpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                    Box(Modifier.fillMaxWidth(ePct).fillMaxHeight().background(HpRed))
                }
                Text("${cs.enemyHp}/${cs.enemyHpMax}", color = TextWhite, fontSize = 11.sp)
                Box(Modifier.height(24.dp)) {
                    dmgNums.takeLast(3).forEachIndexed { i, dn ->
                        Text(dn.text, color = if (dn.isCrit) GoldAccent else TextWhite,
                            fontSize = (16 - i * 3).sp, fontWeight = if (dn.isCrit) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.offset(y = (-i * 14).dp).graphicsLayer { alpha = 1f - i * 0.3f })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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

            Box(Modifier.fillMaxWidth().weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(4.dp)) {
                LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                    items(log.reversed().take(20)) { Text(it, color = TextWhite, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp)) }
                }
            }
        }
    }
}

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
                Text("大获全胜！", color = GoldAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("${cs.expReward}修为  ${cs.goldReward}白银", color = TextWhite, fontSize = 16.sp)
                Button(onClick = { engine.dismissCombatResult() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00)), shape = RoundedCornerShape(6.dp)) {
                    Text("继续探索", color = Color.Black, fontSize = 18.sp)
                }
            }
        }
    }
}

// ===== 工具函数 =====
private fun statDisp(k: String) = when (k) {
    "hp" -> "气血"; "atk" -> "攻击"; "def" -> "防御"; "atkSpd" -> "身法"
    "vamp" -> "吸血"; "critRate" -> "暴率"; "critDmg" -> "暴伤"; else -> k
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

private fun skillInfo(skill: String): Pair<String, String> = when (skill.trim()) {
    "REMNANT_EDGE" -> "残刃刀法" to "每次攻击附加敌人当前气血8%的伤害"
    "TITAN_WILL" -> "铁骨铮铮" to "每次攻击附加自身气血上限5%的伤害"
    "DEVASTATOR" -> "破军诀" to "攻击力永久提升30%"
    "RAMPAGER" -> "嗜战" to "每次攻击永久+5攻击力(战斗结束后重置)"
    "BLADE_DANCE" -> "影舞步" to "每次攻击永久+0.01攻速(战斗结束后重置)"
    "PALADIN_HEART" -> "金钟罩" to "受到的伤害减少25%"
    "AEGIS_THORNS" -> "荆棘反甲" to "每次被攻击反弹15%伤害给敌人"
    "BLOODTHIRST" -> "嗜血术" to "吸血率永久+5%"
    "PRECISION" -> "心明眼亮" to "暴击率永久+8%"
    else -> skill to ""
}