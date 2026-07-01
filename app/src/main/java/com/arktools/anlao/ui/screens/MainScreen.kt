package com.arktools.anlao.ui.screens

import android.app.Activity
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
import androidx.compose.ui.window.Dialog
import com.arktools.anlao.adsdk.AdHelper
import com.arktools.anlao.ui.components.AdLoadingOverlay
import com.arktools.anlao.data.CombatState
import com.arktools.anlao.config.EquipmentRarity
import com.arktools.anlao.config.CultivationRealm
import com.arktools.anlao.config.MartialRealmDisplay
import com.arktools.anlao.config.MartialSect
import com.arktools.anlao.viewmodel.GameViewModel
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
    val storyDialogue by engine.storyDialogue.collectAsState()
    val eventPrompt by engine.eventPrompt.collectAsState()
    val muted by engine.soundManager.muted.collectAsState()
    val bgmVolume by engine.soundManager.bgmLevel.collectAsState()
    val sfxVolume by engine.soundManager.sfxLevel.collectAsState()
    val showShop by engine.showShop.collectAsState()
    val showBlacksmith by engine.showBlacksmith.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (engine.combatState.value == null) engine.soundManager.playBgm(context, "jianghu")
    }

    // 弹窗
    BreakthroughDialog(btPending, btInfo, engine)
    LevelUpDialog(showLvlUp, upgrades, rerolls, player, engine)

    // 时钟 + 定期自动存档
    LaunchedEffect(Unit) {
        var counter = 0
        while (true) {
            delay(1000)
            val r = engine.realm.value
            if (r.isExploring && !r.isEventActive) engine.tickRealm()
            // 每30秒自动存档一次
            counter++
            if (counter >= 30 && engine.player.value.isAllocated && engine.player.value.inCombat.not()) {
                engine.trySafeSave()
                counter = 0
            }
        }
    }

    LaunchedEffect(cs?.combatId) {
        val c = cs ?: return@LaunchedEffect
        if (c.enemyDead || c.playerDead) return@LaunchedEffect
        val combatId = c.combatId
        val pInt = (1000f / c.playerAtkSpd).toLong().coerceAtLeast(400)
        val eInt = (1000f / c.enemyAtkSpd).toLong().coerceAtLeast(400)
        launch {
            while (engine.combatState.value?.combatId == combatId &&
                engine.combatState.value?.enemyDead == false &&
                engine.combatState.value?.playerDead == false
            ) {
                delay(pInt)
                if (engine.combatState.value?.combatId != combatId || !engine.playerAttack()) break
            }
        }
        launch {
            while (engine.combatState.value?.combatId == combatId &&
                engine.combatState.value?.enemyDead == false &&
                engine.combatState.value?.playerDead == false
            ) {
                delay(eInt)
                if (engine.combatState.value?.combatId != combatId || !engine.enemyAttack()) break
            }
        }
    }

    LaunchedEffect(eFlinch) { if (eFlinch) { delay(150); engine._enemyFlinch.value = false } }
    LaunchedEffect(pFlinch) { if (pFlinch) { delay(150); engine._playerFlinch.value = false } }
    LaunchedEffect(toastMessage) { if (toastMessage != null) { delay(1500); toastMessage = null } }

    // ===== 主体 =====
    Box(Modifier.fillMaxSize().background(BgDark).safeDrawingPadding()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { TopHeader(player, muted) { engine.soundManager.toggleMute() } },
            bottomBar = { BottomTabs(tab) { engine.soundManager.playSfx("wood_confirm"); tab = it } }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> key("tab_0") { AdventureTab(realm, player, rLog, engine) }
                    1 -> key("tab_1") { InventoryTab(engine, player) { toastMessage = it } }
                    2 -> key("tab_2") { CharacterTab(player, engine) }
                    3 -> key("tab_3") { SettingsTab(engine, player, muted, bgmVolume, sfxVolume, onSaveFeedback = { success ->
                        toastMessage = if (success) "保存成功" else "当前处于事件或战斗中，暂不能保存"
                    }) { onDeath() } }
                }
            }
        }

        // 战斗覆盖层
        val combatCs = cs
        if (combatCs != null) {
            if (player.inCombat && !combatCs.enemyDead && !combatCs.playerDead) {
                CombatOverlay(combatCs, cLog, sprite, eFlinch, pFlinch, dmgNums, engine)
            }
            if (combatCs.playerDead || realm.currentEvent == "combat_result") {
                CombatResultOverlay(combatCs, engine, onDeath)
            }
        }
        StoryDialogue(storyDialogue, engine) { engine.dismissStoryDialogue() }
        EventChoiceDialog(eventPrompt, realm, engine)
        ToastBubble(toastMessage)

        // 商城弹窗
        if (showShop) ShopOverlay(engine, player)

        // 铁匠铺弹窗
        if (showBlacksmith) BlacksmithOverlay(engine, player)
    }
}

// ==================== 商城 ====================
@Composable
private fun ShopOverlay(engine: com.arktools.anlao.engine.GameEngine, player: com.arktools.anlao.data.PlayerEntity) {
    var torchQty by remember { mutableIntStateOf(1) }
    var antidoteQty by remember { mutableIntStateOf(1) }
    var feedback by remember { mutableStateOf("") }
    val realmMult = (player.lvl / 10 + 1).toLong()

    Dialog(onDismissRequest = { engine.closeShop() }) {
        Column(Modifier.fillMaxWidth().background(BgPanel, RoundedCornerShape(12.dp)).border(1.dp, GoldAccent, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetImageBox("ui/icons/shop.png", 32, "商城")
                Column { Text("商城", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("银两: ${player.gold}", color = TextGray, fontSize = 11.sp) }
            }
            if (feedback.isNotEmpty()) {
                Text(feedback, color = if (feedback.contains("购入")) Color(0xFF4CAF50) else HpRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            ShopItemRow(
                iconPath = "ui/icons/fire_torch.png", iconDesc = "火折子",
                name = "火折子", desc = "照亮地牢，防止心魔暴涨和持续掉血",
                prices = listOf(80L * realmMult, 200L * realmMult, 500L * realmMult), tierNames = listOf("普通", "精良", "上乘"),
                durations = listOf("60秒", "120秒", "240秒"),
                qty = torchQty, onQtyChange = { torchQty = it },
                onBuy = { tier -> val ok = engine.buyTorch(tier); feedback = if (ok) "购入火折子×${torchQty}！" else "银两不足（需${listOf(80L*realmMult,200L*realmMult,500L*realmMult)[tier]*torchQty}两）" }
            )
            ShopItemRow(
                iconPath = "ui/icons/herb_antidote.png", iconDesc = "解毒散",
                name = "解毒散", desc = "免疫中毒，深层地牢毒雾必备",
                prices = listOf(120L * realmMult, 300L * realmMult, 800L * realmMult), tierNames = listOf("普通", "精良", "上乘"),
                durations = listOf("30秒", "60秒", "120秒"),
                qty = antidoteQty, onQtyChange = { antidoteQty = it },
                onBuy = { tier -> val ok = engine.buyAntidote(tier); feedback = if (ok) "购入解毒散×${antidoteQty}！" else "银两不足（需${listOf(120L*realmMult,300L*realmMult,800L*realmMult)[tier]*antidoteQty}两）" }
            )
            TextButton(onClick = { engine.closeShop() }, modifier = Modifier.fillMaxWidth()) { Text("离开", color = TextGray) }
        }
    }
}

@Composable
private fun ShopItemRow(
    iconPath: String, iconDesc: String, name: String, desc: String,
    prices: List<Long>, tierNames: List<String>, durations: List<String>,
    qty: Int, onQtyChange: (Int) -> Unit, onBuy: (Int) -> Unit
) {
    var selectedTier by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AssetImageBox(iconPath, 40, iconDesc)
            Column(Modifier.weight(1f)) {
                Text(name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = TextGray, fontSize = 10.sp, lineHeight = 14.sp)
                Text("持续: ${durations[selectedTier]}  单价: ${prices[selectedTier]}两", color = GoldAccent, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        // 品质选择
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tierNames.forEachIndexed { i, t ->
                Button(onClick = { selectedTier = i }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTier == i) GoldAccent else Color(0xFF333333)),
                    shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(4.dp)) {
                    Text(t, color = if (selectedTier == i) Color.Black else TextGray, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 数量选择 + 购买
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onQtyChange((qty - 1).coerceAtLeast(1)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("-", color = TextWhite, fontSize = 16.sp)
            }
            Text("${qty}", color = TextWhite, fontSize = 14.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            Button(onClick = { onQtyChange((qty + 1).coerceAtMost(20)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("+", color = TextWhite, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onBuy(selectedTier) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00)),
                shape = RoundedCornerShape(4.dp)) {
                Text("购买 ${prices[selectedTier] * qty}两", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== 铁匠铺 ====================
@Composable
private fun BlacksmithOverlay(engine: com.arktools.anlao.engine.GameEngine, player: com.arktools.anlao.data.PlayerEntity) {
    var selectedSlot by remember { mutableIntStateOf(-1) }
    var feedback by remember { mutableStateOf("") }
    val equipped = engine.parseEquipped()
    val item = equipped.getOrNull(selectedSlot)?.takeIf { it.category.isNotBlank() }

    Dialog(onDismissRequest = { engine.closeBlacksmith() }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).background(BgPanel, RoundedCornerShape(12.dp)).border(1.dp, GoldAccent, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetImageBox("ui/icons/blacksmith.png", 32, "铁匠铺")
                Column { Text("铁匠铺", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("银两: ${player.gold}", color = TextGray, fontSize = 11.sp) }
            }

            // 选中装备信息
            if (item != null) {
                Box(Modifier.fillMaxWidth().border(1.dp, RarityCol[item.rarity] ?: BorderWhite, RoundedCornerShape(8.dp)).background(BgDark, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Column {
                        Text("${item.rarity} ${item.category}  +${item.lvl}  耐久${item.durability}%", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        val statText = item.stats.flatMap { it.entries }.joinToString("  ") { (k, v) -> "${statDisp(k)}+${formatStatValue(k, v)}" }
                        Text(statText.ifBlank { "无额外属性" }, color = TextWhite, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) { Text("选择要操作的装备", color = TextGray, fontSize = 12.sp) }
            }

            // 滚动装备列表
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(equipped.filter { it.category.isNotBlank() }, key = { _, it -> "${it.type}_${it.category}_${it.lvl}" }) { idx, eq ->
                    val realIdx = equipped.indexOf(eq)
                    val isSelected = selectedSlot == realIdx
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).border(1.dp, if (isSelected) GoldAccent else BorderWhite, RoundedCornerShape(6.dp))
                        .background(if (isSelected) GoldAccent.copy(alpha = 0.1f) else BgDark, RoundedCornerShape(6.dp))
                        .clickable { selectedSlot = realIdx; feedback = "" }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        EquipmentBadge(eq, 28)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text("${eq.rarity} ${eq.category}", color = RarityCol[eq.rarity] ?: TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("+${eq.lvl}", color = TextGray, fontSize = 10.sp)
                                Text("${eq.stats.size}词条", color = TextGray, fontSize = 10.sp)
                                Text("耐久${eq.durability}%", color = if (eq.durability > 50) TextGray else if (eq.durability > 20) Color(0xFFFFA000) else HpRed, fontSize = 10.sp)
                            }
                        }
                        if (isSelected) Text("▸", color = GoldAccent, fontSize = 14.sp)
                    }
                }
            }

            // 反馈弹窗
            if (feedback.isNotEmpty()) {
                AlertDialog(onDismissRequest = { feedback = "" }, containerColor = BgPanel,
                    title = { Text(if (feedback.contains("成功")) "操作成功" else "操作失败",
                        color = if (feedback.contains("成功")) Color(0xFF4CAF50) else HpRed,
                        fontWeight = FontWeight.Bold) },
                    text = { Text(feedback, color = TextWhite) },
                    confirmButton = { Button(onClick = { feedback = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) { Text("确定", color = Color.Black) } }
                )
            }

            // 操作按钮
            if (item != null) {
                val enhanceCost = engine.enhanceCost(item)
                val reforgeCost = engine.reforgeCost(item)
                val repairCost = engine.repairCost(item)
                val rate = engine.enhanceSuccessRate(item)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            val ok = engine.enhanceEquipped(selectedSlot)
                            feedback = if (item.lvl >= 30) "已达上限+30" else if (ok) "强化成功！" else "银两不足（需${enhanceCost}两）"
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)), shape = RoundedCornerShape(6.dp)) {
                            Text("强化 ${enhanceCost}两 ${rate}%", color = TextWhite, fontSize = 11.sp)
                        }
                        Button(onClick = {
                            val ok = engine.reforgeEquipped(selectedSlot)
                            feedback = if (ok) "重铸成功！" else "银两不足（需${reforgeCost}两）"
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)), shape = RoundedCornerShape(6.dp)) {
                            Text("重铸 ${reforgeCost}两", color = TextWhite, fontSize = 11.sp)
                        }
                    }
                    if (item.durability < 100) {
                        Button(onClick = {
                            val ok = engine.repairEquipment(selectedSlot)
                            feedback = if (ok) "修复成功！" else "银两不足（需${repairCost}两）"
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), shape = RoundedCornerShape(6.dp)) {
                            Text("修复 ${repairCost}两  恢复耐久至满", color = TextWhite, fontSize = 11.sp)
                        }
                    }
                }
            }
            TextButton(onClick = { engine.closeBlacksmith() }, modifier = Modifier.fillMaxWidth()) { Text("离开", color = TextGray) }
        }
    }
}

// ==================== DIALOGS ====================
@Composable
private fun BreakthroughDialog(pending: Boolean, info: Pair<String, String>?, engine: com.arktools.anlao.engine.GameEngine) {
    if (!pending || info == null) return
    val context = LocalContext.current
    val activity = context as? Activity
    var showAdStage by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = {}, containerColor = BgPanel,
        title = { Text("境界突破", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            if (!showAdStage) {
                Text("从【${info.first}】突破到【${info.second}】！\n获得大幅属性加成，气血回满。", color = TextWhite)
            } else {
                Text("突破成功！额外福利等你领取。", color = TextWhite)
            }
        },
        confirmButton = {
            if (!showAdStage) {
                Button(onClick = { engine.confirmBreakthrough(); showAdStage = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00))) { Text("突破！", color = Color.Black) }
            } else {
                Button(
                    onClick = {
                        if (activity != null) {
                            AdHelper.showRewardAd(
                                activity = activity,
                                onRewarded = { engine.breakthroughBonusByAd() },
                                onLoadStart = { isAdLoading = true },
                                onComplete = { isAdLoading = false; engine.dismissBreakthrough() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) { Text("看广告·突破馈赠", color = Color.Black) }
            }
        },
        dismissButton = {
            if (!showAdStage) {
                TextButton(onClick = { engine.dismissBreakthrough() }) { Text("暂缓", color = TextGray) }
            } else {
                TextButton(onClick = { engine.dismissBreakthrough() }) { Text("关闭", color = TextGray) }
            }
        }
    )
    AdLoadingOverlay(visible = isAdLoading)
}

@Composable
private fun LevelUpDialog(show: Boolean, upgrades: List<com.arktools.anlao.config.UpgradeOption>, rerolls: Int, player: com.arktools.anlao.data.PlayerEntity, engine: com.arktools.anlao.engine.GameEngine) {
    if (!show || upgrades.isEmpty()) return
    val context = LocalContext.current
    val activity = context as? Activity
    var isAdLoading by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = {}, containerColor = BgPanel,
        title = { Text("境界提升", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("剩余: ${player.exp.lvlGained}  重骰: ${rerolls}/3", color = TextGray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                upgrades.forEachIndexed { i, opt ->
                    Button(onClick = { engine.selectUpgrade(i) },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderWhite),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { Text("${opt.stat} +${"%.1f".format(opt.value)}%", color = TextWhite) }
                }
                if (rerolls <= 0) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                AdHelper.showRewardAd(
                                    activity = activity,
                                    onRewarded = { engine.rerollUpgradesByAd() },
                                    onLoadStart = { isAdLoading = true },
                                    onComplete = { isAdLoading = false }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { Text("看广告·重骰3次", color = Color.Black, fontSize = 13.sp) }
                } else {
                    TextButton(onClick = { engine.rerollUpgrades() }) { Text("重骰（${rerolls}次）", color = TextWhite) }
                }
            }
        }, confirmButton = {}, dismissButton = {}
    )
    AdLoadingOverlay(visible = isAdLoading)
}

@Composable
private fun EventChoiceDialog(prompt: com.arktools.anlao.engine.GameEngine.EventPrompt?, realm: com.arktools.anlao.data.RealmState, engine: com.arktools.anlao.engine.GameEngine) {
    if (!realm.isEventActive || realm.currentEvent == "combat_result" || engine.player.value.inCombat || prompt == null) return
    val opts = prompt.choices.filter { it.isNotBlank() }
    if (opts.isEmpty()) return

    val context = LocalContext.current
    val activity = context as? Activity
    var isAdLoading by remember { mutableStateOf(false) }
    val currentEvent = realm.currentEvent

    Dialog(onDismissRequest = {}) {
        Column(
            Modifier.fillMaxWidth()
                .border(1.dp, GoldAccent, RoundedCornerShape(10.dp))
                .background(BgPanel, RoundedCornerShape(10.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("江湖抉择", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(prompt.message, color = TextWhite, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            opts.forEachIndexed { i, option ->
                val isAdOption = (currentEvent == "chest_reroll" || currentEvent == "merchant_bonus") && i == 0
                Button(
                    onClick = {
                        engine.soundManager.playSfx("wood_confirm")
                        if (isAdOption && activity != null) {
                            AdHelper.showRewardAd(
                                activity = activity,
                                onRewarded = {
                                    when (currentEvent) {
                                        "chest_reroll" -> engine.chestRerollByAd()
                                        "merchant_bonus" -> engine.merchantBonusByAd()
                                    }
                                },
                                onLoadStart = { isAdLoading = true },
                                onComplete = { isAdLoading = false }
                            )
                        } else {
                            engine.chooseOption(i)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdOption) GoldAccent else Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).border(1.dp, if (isAdOption) GoldAccent else BorderWhite, RoundedCornerShape(6.dp))
                ) { Text(option, color = if (isAdOption) Color.Black else TextWhite, fontSize = 14.sp) }
            }
        }
    }
    AdLoadingOverlay(visible = isAdLoading)
}

private data class DialogueLine(val speaker: String, val content: String)

private fun parseStoryDialogue(text: String): Pair<String, List<DialogueLine>> {
    val parts = text.split("\n").map { it.trim() }
    val title = parts.firstOrNull()?.takeIf { it.startsWith("【") } ?: "旧牢回声"
    val body = if (parts.firstOrNull()?.startsWith("【") == true) parts.drop(1).joinToString("\n") else text
    val lines = body.split("\n\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { raw ->
            val index = raw.indexOf('：')
            if (index > 0 && index <= 6) DialogueLine(raw.substring(0, index), raw.substring(index + 1).trim())
            else DialogueLine("旁白", raw)
        }
    return title to lines.ifEmpty { listOf(DialogueLine("旁白", body.trim())) }
}

@Composable
private fun StoryDialogue(text: String?, engine: com.arktools.anlao.engine.GameEngine, onFinished: () -> Unit) {
    if (text == null) return
    val (title, lines) = remember(text) { parseStoryDialogue(text) }
    var index by remember(text) { mutableIntStateOf(0) }
    var visibleCount by remember(text) { mutableIntStateOf(0) }
    var finishedTyping by remember(text) { mutableStateOf(false) }
    val line = lines[index]

    LaunchedEffect(text, index) {
        visibleCount = 0
        finishedTyping = false
        while (visibleCount < line.content.length) {
            delay(26)
            visibleCount++
            if (visibleCount % 3 == 0) engine.soundManager.playSfx("hover")
        }
        finishedTyping = true
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth()
                .border(1.dp, GoldAccent, RoundedCornerShape(14.dp))
                .background(BgPanel.copy(alpha = 0.98f), RoundedCornerShape(14.dp))
                .clickable {
                    engine.soundManager.playSfx("wood_confirm")
                    if (!finishedTyping) {
                        visibleCount = line.content.length
                        finishedTyping = true
                    } else if (index < lines.lastIndex) {
                        index++
                    } else {
                        onFinished()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${index + 1}/${lines.size}", color = TextGray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(line.speaker, color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                line.content.take(visibleCount),
                color = TextWhite,
                fontSize = 19.sp,
                lineHeight = 31.sp,
                minLines = 3
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (!finishedTyping) "点击显示整句" else if (index < lines.lastIndex) "点击继续" else "点击结束",
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ToastBubble(message: String?) {
    if (message == null) return
    Box(Modifier.fillMaxSize().padding(bottom = 92.dp), contentAlignment = Alignment.BottomCenter) {
        Text(
            message,
            color = TextWhite,
            fontSize = 13.sp,
            modifier = Modifier.background(Color(0xDD222222), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

// ==================== TOP HEADER ====================
@Composable
private fun TopHeader(player: com.arktools.anlao.data.PlayerEntity, muted: Boolean, onToggleMute: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(BgPanel).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).border(1.dp, GoldAccent, RoundedCornerShape(8.dp)).padding(2.dp), contentAlignment = Alignment.Center) {
            AssetImageBox(player.portrait, 50, player.name)
        }
        Column(Modifier.weight(1f)) {
            val sect = MartialSect.entries.find { it.name == player.sect } ?: MartialSect.WANDERER
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("银两 ${player.gold}", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("${MartialRealmDisplay.fromLevel(player.lvl)} · ${sect.displayName}", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val hpPct = (player.stats.hp.toFloat() / player.stats.hpMax.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF3A2222))) {
                Box(Modifier.fillMaxWidth(hpPct).fillMaxHeight().background(HpRed))
            }
            Text("气血 ${player.stats.hp}/${player.stats.hpMax}   斩敌 ${player.kills}   败北 ${player.deaths}", color = TextGray, fontSize = 10.sp)
        }
        Box(
            Modifier.size(44.dp)
                .border(1.dp, BorderWhite, RoundedCornerShape(8.dp))
                .background(BgDark, RoundedCornerShape(8.dp))
                .clickable { onToggleMute() }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AssetImageBox("ui/icons/sound.png", 26, if (muted) "静音" else "声音")
        }
    }
}

// ==================== BOTTOM TABS ====================
@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = BgPanel, tonalElevation = 0.dp) {
        NavigationBarItem(selected = selected == 0, onClick = { onSelect(0) },
            icon = { AssetImageBox("ui/icons/explore.png", 24, "江湖") },
            label = { Text("江湖", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
        NavigationBarItem(selected = selected == 1, onClick = { onSelect(1) },
            icon = { AssetImageBox("ui/icons/bag.png", 24, "行囊") },
            label = { Text("行囊", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
        NavigationBarItem(selected = selected == 2, onClick = { onSelect(2) },
            icon = { AssetImageBox("ui/icons/character.png", 24, "角色") },
            label = { Text("角色", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
        NavigationBarItem(selected = selected == 3, onClick = { onSelect(3) },
            icon = { AssetImageBox("ui/icons/settings.png", 24, "设置") },
            label = { Text("设置", color = TextWhite, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BorderWhite))
    }
}

// ==================== TAB 0: 江湖 ====================
@Composable
private fun AdventureTab(realm: com.arktools.anlao.data.RealmState, player: com.arktools.anlao.data.PlayerEntity, rLog: List<String>, engine: com.arktools.anlao.engine.GameEngine) {
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
        // 心魔值 + 火折子 + 解毒散
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat("心魔", "${player.stress}/200",
                if (player.stress >= 100) HpRed else if (player.stress >= 50) Color(0xFFFFA000) else TextGray)
            if (player.torchActive) {
                Column(Modifier.weight(1f)) {
                    Text("火折", color = Color(0xFFFFA000), fontSize = 8.sp)
                    Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF333333), RoundedCornerShape(3.dp))) {
                        Box(Modifier.fillMaxWidth(player.torchSecondsLeft.toFloat() / 300f).fillMaxHeight()
                            .background(Color(0xFFFFA000), RoundedCornerShape(3.dp)))
                    }
                }
            } else {
                MiniStat("火折", "未点燃", TextGray)
            }
            if (player.antidoteActive) {
                Column(Modifier.weight(1f)) {
                    Text("解毒", color = Color(0xFF4CAF50), fontSize = 8.sp)
                    Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF333333), RoundedCornerShape(3.dp))) {
                        Box(Modifier.fillMaxWidth(player.antidoteSecondsLeft.toFloat() / 120f).fillMaxHeight()
                            .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                    }
                }
            }
            if (player.stressAffliction.isNotEmpty()) MiniStat("特质", player.stressAffliction, HpRed)
            if (player.stressVirtue.isNotEmpty()) MiniStat("特质", player.stressVirtue, Color(0xFF4CAF50))
        }

        QuestCard(engine.currentQuestInfo())

        // 探索条
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                .border(1.dp, BorderWhite, RoundedCornerShape(8.dp))
                .background(BgPanel, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(engine.currentAreaName(realm.floor), color = GoldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("第 ${realm.floor} 层 · 房间 ${realm.room}/${realm.roomsPerFloor}   本层斩敌 ${realm.currentKills}", color = TextGray, fontSize = 11.sp)
                }
                Button(
                    onClick = { if (realm.isExploring) engine.pauseExploring() else engine.startExploring() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp).border(1.dp, if (realm.isExploring) HpRed else GoldAccent, RoundedCornerShape(4.dp)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp)
                ) { Text(if (realm.isExploring) "暂停" else "探索", color = if (realm.isExploring) HpRed else TextWhite, fontSize = 14.sp) }
            }
            Spacer(Modifier.height(8.dp))
            val roomPct = (realm.room.toFloat() / realm.roomsPerFloor.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF342817))) {
                Box(Modifier.fillMaxWidth(roomPct).fillMaxHeight().background(GoldAccent))
            }
        }

        // 秘籍日志区
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp).padding(bottom = 6.dp)
            .border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).padding(6.dp)) {
            if (rLog.isEmpty()) Text("踏入江湖，开始你的冒险……", color = TextGray, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            else LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                items(rLog.reversed().take(50)) { log ->
                    val clean = log.replace("[选项:.*]".toRegex(), "")
                    Text(clean, color = logColor(clean), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

private fun logColor(log: String): Color = when {
    log.contains("剧情") -> Color(0xFF8FD7FF)
    listOf("遭遇", "迎战", "挡住", "苏醒", "暗器", "退路", "败").any { log.contains(it) } -> HpRed
    listOf("获得", "白银", "宝箱", "掉落", "战利品", "游商", "兵器", "强化", "重铸").any { log.contains(it) } -> GoldAccent
    listOf("伤药", "疗伤", "老医师", "气血", "防御略有精进", "祝福", "悟道", "突破", "境界提升").any { log.contains(it) } -> Color(0xFF66D18F)
    listOf("悬赏", "声望", "黑市").any { log.contains(it) } -> Color(0xFFB68CFF)
    listOf("无视", "空", "废弃", "断剑").any { log.contains(it) } -> TextGray
    else -> TextWhite
}

private fun combatLogColor(log: String): Color = when {
    log.contains("暴击") -> GoldAccent
    log.contains("败") || log.contains("伤害") && !log.contains("对") -> HpRed
    log.contains("获得") || log.contains("掉落") -> GoldAccent
    log.contains("击败") || log.contains("胜") -> Color(0xFF66D18F)
    else -> TextWhite
}

@Composable
private fun QuestCard(quest: com.arktools.anlao.engine.GameEngine.QuestInfo) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
            .border(1.dp, GoldAccent.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .background(Color(0xEE17110D), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(quest.chapter, color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(quest.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        Text(quest.objective, color = TextWhite, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
        Text(quest.progress, color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        Text(quest.story, color = TextGray, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 6.dp))
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
private fun InventoryTab(engine: com.arktools.anlao.engine.GameEngine, player: com.arktools.anlao.data.PlayerEntity, onFeedback: (String) -> Unit) {
    val eq = remember(player.equipped) { engine.parseEquipped() }
    val inv = remember(player.inventory) { engine.parseInventory() }
    val activeSetBonuses = remember(player.equipped) { engine.activeSetBonusDescriptions() }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<com.arktools.anlao.data.EquipmentItem?>(null) }
    var selectedEquippedIndex by remember { mutableIntStateOf(-1) }
    var categoryFilter by remember { mutableIntStateOf(0) }
    var useQty by remember { mutableIntStateOf(1) }

    val categories = listOf("全部", "兵器", "护甲", "盾牌", "头盔", "鞋履", "饰品", "消耗品")
    val filteredInv = if (categoryFilter == 0) inv else if (categoryFilter == 7) emptyList() else inv.filter { it.type == categories[categoryFilter] }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
        // 标题行
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("行囊", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${player.gold} 两白银", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box {
                TextButton(onClick = { expanded = true }) { Text("批量出售", color = HpRed, fontSize = 12.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                    modifier = Modifier.background(BgPanel).border(1.dp, GoldAccent, RoundedCornerShape(8.dp))) {
                    (listOf("全部") + EquipmentRarity.entries.map { it.displayName }).forEach { r ->
                        DropdownMenuItem(text = { Text(r, fontSize = 12.sp, color = RarityCol[r] ?: TextWhite) },
                            colors = MenuDefaults.itemColors(textColor = RarityCol[r] ?: TextWhite),
                            onClick = { expanded = false; engine.sellAll(r); selected = null })
                    }
                }
            }
        }

        // 已装备 6 槽
        Text("已装备 (6槽)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        Row(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(8.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            val slotNames = remember { listOf("兵器", "护甲", "盾牌", "头盔", "鞋履", "饰品") }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (index in 0..2) {
                    EquipmentSlot(eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }, slotNames[index], Modifier.fillMaxWidth()) {
                        eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }?.let { selected = it; selectedEquippedIndex = index }
                    }
                }
            }
            Column(Modifier.width(116.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).border(1.dp, GoldAccent, RoundedCornerShape(8.dp)).padding(4.dp), contentAlignment = Alignment.TopCenter) {
                    AssetImageBox(player.portrait, 88, player.name)
                }
                EquipmentTotalStats(eq)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (index in 3..5) {
                    EquipmentSlot(eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }, slotNames[index], Modifier.fillMaxWidth()) {
                        eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }?.let { selected = it; selectedEquippedIndex = index }
                    }
                }
            }
        }

        // 套装效果
        if (activeSetBonuses.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).background(BgPanel, RoundedCornerShape(6.dp)).padding(8.dp)) {
                Text("已激活套装", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                activeSetBonuses.forEach { Text(it, color = TextGray, fontSize = 10.sp, lineHeight = 14.sp) }
            }
        }

        // 选中装备详情
        val selItem = selected
        if (selItem != null) {
            EquipmentDetail(item = selItem, engine = engine, equippedIndex = selectedEquippedIndex,
                onUnequip = { if (selectedEquippedIndex >= 0) { engine.unequipItem(selectedEquippedIndex); selected = null; selectedEquippedIndex = -1 } })
        }

        // ===== 消耗品区 =====
        val torchCount = engine.torchCount()
        val antidoteCount = engine.antidoteCount()
        if (torchCount > 0 || antidoteCount > 0 || player.torchActive || player.antidoteActive) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("消耗品", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (player.torchActive) Text("  火折 ${player.torchSecondsLeft}s", color = Color(0xFFFFA000), fontSize = 11.sp)
                if (player.antidoteActive) Text("  解毒 ${player.antidoteSecondsLeft}s", color = Color(0xFF4CAF50), fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 火折子
                if (torchCount > 0 || player.torchActive) {
                    Column(Modifier.weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).background(BgPanel, RoundedCornerShape(6.dp)).padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssetImageBox("ui/icons/fire_torch.png", 28, "火折子")
                            Column { Text("火折子 ×${torchCount}", color = Color(0xFFFFA000), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { useQty = (useQty - 1).coerceAtLeast(1) }, contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("-", color = TextWhite, fontSize = 14.sp) }
                            Text("$useQty", color = TextWhite, fontSize = 12.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                            Button(onClick = { useQty = (useQty + 1).coerceAtMost(torchCount) }, contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("+", color = TextWhite, fontSize = 14.sp) }
                        }
                        Button(onClick = { engine.useTorch(useQty); onFeedback("点燃${useQty}个火折子") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)), shape = RoundedCornerShape(4.dp)) {
                            Text("使用", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // 解毒散
                if (antidoteCount > 0 || player.antidoteActive) {
                    Column(Modifier.weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).background(BgPanel, RoundedCornerShape(6.dp)).padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssetImageBox("ui/icons/herb_antidote.png", 28, "解毒散")
                            Column { Text("解毒散 ×${antidoteCount}", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { useQty = (useQty - 1).coerceAtLeast(1) }, contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("-", color = TextWhite, fontSize = 14.sp) }
                            Text("$useQty", color = TextWhite, fontSize = 12.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                            Button(onClick = { useQty = (useQty + 1).coerceAtMost(antidoteCount) }, contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("+", color = TextWhite, fontSize = 14.sp) }
                        }
                        Button(onClick = { engine.useAntidote(useQty); onFeedback("服用${useQty}个解毒散") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(4.dp)) {
                            Text("使用", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ===== 背包分类标签 =====
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.forEachIndexed { i, cat ->
                Box(Modifier.weight(1f).clickable { categoryFilter = i }
                    .background(if (categoryFilter == i) GoldAccent.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text(cat, color = if (categoryFilter == i) GoldAccent else TextGray, fontSize = 11.sp)
                }
            }
        }

        // 背包物品列表
        Text("背包", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        if (categoryFilter == 7) {
            // 消耗品分类
            LazyColumn(Modifier.weight(1f)) {
                item(key = "torch") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).border(1.dp, Color(0xFFFFA000).copy(alpha = 0.7f), RoundedCornerShape(6.dp)).background(BgPanel, RoundedCornerShape(6.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AssetImageBox("ui/icons/fire_torch.png", 36, "火折子")
                        Column(Modifier.weight(1f)) {
                            Text("火折子 ×${engine.torchCount()}", color = Color(0xFFFFA000), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (player.torchActive) Text("燃烧中 ${player.torchSecondsLeft}s", color = Color(0xFFFFA000), fontSize = 10.sp)
                            else Text("照亮地牢，防止心魔暴涨", color = TextGray, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { useQty = (useQty - 1).coerceAtLeast(1) }, contentPadding = PaddingValues(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("-", color = TextWhite) }
                            Text("$useQty", color = TextWhite, fontSize = 13.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Button(onClick = { useQty = (useQty + 1).coerceAtMost(engine.torchCount().coerceAtLeast(1)) }, contentPadding = PaddingValues(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("+", color = TextWhite) }
                        }
                        Button(onClick = { engine.useTorch(useQty); onFeedback("点燃${useQty}个火折子") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)), shape = RoundedCornerShape(4.dp)) {
                            Text("使用", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item(key = "antidote") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.7f), RoundedCornerShape(6.dp)).background(BgPanel, RoundedCornerShape(6.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AssetImageBox("ui/icons/herb_antidote.png", 36, "解毒散")
                        Column(Modifier.weight(1f)) {
                            Text("解毒散 ×${engine.antidoteCount()}", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (player.antidoteActive) Text("生效中 ${player.antidoteSecondsLeft}s", color = Color(0xFF4CAF50), fontSize = 10.sp)
                            else Text("免疫中毒，深层地牢必备", color = TextGray, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { useQty = (useQty - 1).coerceAtLeast(1) }, contentPadding = PaddingValues(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("-", color = TextWhite) }
                            Text("$useQty", color = TextWhite, fontSize = 13.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Button(onClick = { useQty = (useQty + 1).coerceAtMost(engine.antidoteCount().coerceAtLeast(1)) }, contentPadding = PaddingValues(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)), shape = RoundedCornerShape(4.dp)) { Text("+", color = TextWhite) }
                        }
                        Button(onClick = { engine.useAntidote(useQty); onFeedback("服用${useQty}个解毒散") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(4.dp)) {
                            Text("使用", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredInv.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(if (inv.isEmpty()) "背包空空如也" else "该分类下无装备", color = TextGray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(filteredInv, key = { i, item -> "${i}_${item.category}_${item.rarity}_${item.lvl}_${item.value}" }) { i, item ->
                    InventoryItemRow(item,
                        onEquip = { engine.equipItem(i); selected = null },
                        onSell = { engine.sellItem(false, i); selected = null })
                }
            }
        }
    }
}

        Text("已装备 (6槽)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        Row(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(8.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            val slotNames = remember { listOf("兵器", "护甲", "盾牌", "头盔", "鞋履", "饰品") }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (index in 0..2) {
                    EquipmentSlot(eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }, slotNames[index], Modifier.fillMaxWidth()) {
                        eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }?.let { selected = it; selectedEquippedIndex = index }
                    }
                }
            }
            Column(Modifier.width(116.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).border(1.dp, GoldAccent, RoundedCornerShape(8.dp)).padding(4.dp), contentAlignment = Alignment.TopCenter) {
                    AssetImageBox(player.portrait, 88, player.name)
                }
                EquipmentTotalStats(eq)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (index in 3..5) {
                    EquipmentSlot(eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }, slotNames[index], Modifier.fillMaxWidth()) {
                        eq.getOrNull(index)?.takeIf { it.category.isNotBlank() }?.let { selected = it; selectedEquippedIndex = index }
                    }
                }
            }
        }
        if (activeSetBonuses.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).background(BgPanel, RoundedCornerShape(6.dp)).padding(8.dp)) {
                Text("已激活套装", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                activeSetBonuses.forEach { bonus ->
                    Text(bonus, color = TextGray, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }

        val selItem = selected
        if (selItem != null) {
            EquipmentDetail(
                item = selItem,
                engine = engine,
                equippedIndex = selectedEquippedIndex,
                onUnequip = { if (selectedEquippedIndex >= 0) { engine.unequipItem(selectedEquippedIndex); selected = null; selectedEquippedIndex = -1 } }
            )
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("背包 (${inv.size})", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("点击装备，长按出售待后续扩展", color = TextGray, fontSize = 10.sp)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (inv.isEmpty()) {
                item(key = "empty_hint") {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("背包空空如也", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                itemsIndexed(inv, key = { _, item -> "${item.category}_${item.rarity}_${item.lvl}_${item.value}" }) { i, item ->
                    InventoryItemRow(item,
                        onEquip = { engine.equipItem(i); selected = null },
                        onSell = { engine.sellItem(false, i); selected = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentTotalStats(items: List<com.arktools.anlao.data.EquipmentItem>) {
    val totals = linkedMapOf<String, Float>()
    items.filter { it.category.isNotBlank() }.forEach { item ->
        item.stats.forEach { sm -> sm.forEach { (k, v) -> totals[k] = (totals[k] ?: 0f) + v } }
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp)
            .background(BgPanel.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("装备总加成", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        if (totals.isEmpty()) {
            Text("暂无加成", color = TextGray, fontSize = 8.sp, lineHeight = 10.sp, textAlign = TextAlign.Center)
        } else {
            totals.entries.chunked(2).forEach { row ->
                Text(
                    row.joinToString("  ") { (k, v) -> "${statDisp(k)}+${formatStatValue(k, v)}" },
                    color = TextGray,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EquipmentSlot(item: com.arktools.anlao.data.EquipmentItem?, slotName: String, modifier: Modifier, onClick: () -> Unit) {
    val border = item?.let { RarityCol[it.rarity] } ?: BorderWhite.copy(alpha = 0.45f)
    Box(
        modifier.height(52.dp).border(1.dp, border, RoundedCornerShape(6.dp)).background(BgDark, RoundedCornerShape(6.dp)).clickable { onClick() }.padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (item == null) {
            Text(slotName, color = TextGray, fontSize = 10.sp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EquipmentBadge(item, 20)
                Text(item.category.take(3), color = RarityCol[item.rarity] ?: TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                // 耐久条
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF333333), RoundedCornerShape(1.5.dp))) {
                    val durColor = if (item.durability > 50) Color(0xFF4CAF50) else if (item.durability > 20) Color(0xFFFFA000) else HpRed
                    Box(Modifier.fillMaxWidth(item.durability / 100f).fillMaxHeight().background(durColor, RoundedCornerShape(1.5.dp)))
                }
            }
        }
    }
}

@Composable
private fun EquipmentBadge(item: com.arktools.anlao.data.EquipmentItem, sizeDp: Int) {
    val color = RarityCol[item.rarity] ?: TextWhite
    val icon = equipmentIconPath(item.category)
    if (icon != null) {
        Box(
            Modifier.size(sizeDp.dp)
                .border(1.dp, color, RoundedCornerShape(5.dp))
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            AssetImageBox(icon, sizeDp - 4, item.category)
        }
        return
    }
    val mark = when (item.category) {
        "青锋剑" -> "剑"; "开山斧" -> "斧"; "镇岳锤" -> "锤"
        "袖里刃" -> "刃"; "游龙鞭" -> "鞭"; "月牙镰" -> "镰"
        "玄铁甲" -> "甲"; "金丝软甲" -> "衫"; "夜行衣" -> "衣"
        "玄武盾" -> "盾"; "雁翎盾" -> "翎"; "八卦盾" -> "卦"
        "狮首盔" -> "盔"; "龙纹冠" -> "冠"
        else -> item.category.take(1)
    }
    Box(
        Modifier.size(sizeDp.dp)
            .border(1.dp, color, RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(mark, color = color, fontSize = (sizeDp / 2).sp, fontWeight = FontWeight.Bold)
    }
}

private fun equipmentIconPath(category: String): String? = when (category) {
    "青锋剑" -> "ui/equipment/qingfeng_sword.png"
    "开山斧" -> "ui/equipment/kaishan_axe.png"
    "镇岳锤" -> "ui/equipment/zhenyue_hammer.png"
    "袖里刃" -> "ui/equipment/xiuli_dagger.png"
    "游龙鞭" -> "ui/equipment/youlong_whip.png"
    "月牙镰" -> "ui/equipment/yueya_scythe.png"
    "梨花枪" -> "ui/equipment/lihua_spear.png"
    "雁翎刀" -> "ui/equipment/yanling_saber.png"
    "盘龙棍" -> "ui/equipment/panlong_staff.png"
    "铁骨扇" -> "ui/equipment/tiegu_fan.png"
    "暴雨针" -> "ui/equipment/baoyu_needle.png"
    "乾坤轮" -> "ui/equipment/qiankun_ring.png"
    "玄铁甲" -> "ui/equipment/xuantie_armor.png"
    "金丝软甲" -> "ui/equipment/jinsi_armor.png"
    "夜行衣" -> "ui/equipment/yexing_cloth.png"
    "龙鳞甲" -> "ui/equipment/longlin_armor.png"
    "青云袍" -> "ui/equipment/qingyun_robe.png"
    "藤纹甲" -> "ui/equipment/tengwen_armor.png"
    "玄武盾" -> "ui/equipment/xuanwu_shield.png"
    "雁翎盾" -> "ui/equipment/yanling_shield.png"
    "八卦盾" -> "ui/equipment/bagua_shield.png"
    "莲花盾" -> "ui/equipment/lianhua_shield.png"
    "虎面盾" -> "ui/equipment/humian_shield.png"
    "赤铜盾" -> "ui/equipment/chitong_shield.png"
    "狮首盔" -> "ui/equipment/shishou_helm.png"
    "龙纹冠" -> "ui/equipment/longwen_crown.png"
    "斗笠" -> "ui/equipment/douli_hat.png"
    "青玉冠" -> "ui/equipment/qingyu_crown.png"
    "铁面具" -> "ui/equipment/tie_mask.png"
    "束发带" -> "ui/equipment/shufa_band.png"
    "踏云靴" -> "ui/equipment/tayun_boots.png"
    "玄铁靴" -> "ui/equipment/xuantie_boots.png"
    "追风履" -> "ui/equipment/zhuifeng_boots.png"
    "夜影靴" -> "ui/equipment/yeying_boots.png"
    "听雨鞋" -> "ui/equipment/tingyu_shoes.png"
    "罗汉履" -> "ui/equipment/luohan_shoes.png"
    "青玉佩" -> "ui/equipment/qingyu_pendant.png"
    "虎符坠" -> "ui/equipment/hufu_pendant.png"
    "镇魂铃" -> "ui/equipment/zhenhun_bell.png"
    "剑穗" -> "ui/equipment/jian_su.png"
    "血玉戒" -> "ui/equipment/xueyu_ring.png"
    "莲心珠" -> "ui/equipment/lianxin_pearl.png"
    else -> null
}

@Composable
private fun EquipmentDetail(
    item: com.arktools.anlao.data.EquipmentItem,
    engine: com.arktools.anlao.engine.GameEngine,
    equippedIndex: Int,
    onUnequip: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).border(1.dp, RarityCol[item.rarity] ?: BorderWhite, RoundedCornerShape(8.dp)).background(BgPanel, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text("${item.rarity} ${item.category}", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("${item.type} · 品阶+${item.lvl} · 耐久${item.durability}% · 价值${item.value}两", color = TextGray, fontSize = 10.sp)
        val statText = item.stats.flatMap { it.entries }.joinToString("  ") { (k, v) ->
            "${statDisp(k)}+${formatStatValue(k, v)}"
        }
        Text(statText.ifBlank { "无额外属性" }, color = TextWhite, fontSize = 11.sp, lineHeight = 16.sp)
        if (equippedIndex >= 0) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                TextButton(onClick = onUnequip) { Text("卸下", color = HpRed, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun InventoryItemRow(item: com.arktools.anlao.data.EquipmentItem, onEquip: () -> Unit, onSell: () -> Unit) {
    var showDetail by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .border(1.dp, (RarityCol[item.rarity] ?: BorderWhite).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .background(BgPanel, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.clickable { showDetail = true }) { EquipmentBadge(item, 30) }
            Column(Modifier.clickable { onEquip() }) {
                Text("${item.rarity} ${item.category}", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${item.type} · 品阶${item.lvl} · ${item.value}两", color = TextGray, fontSize = 10.sp)
            }
        }
        TextButton(onClick = onSell) { Text("出售", color = HpRed, fontSize = 11.sp) }
    }

    if (showDetail) EquipmentDetailDialog(item, onEquip = { showDetail = false; onEquip() }) { showDetail = false }
}

@Composable
private fun EquipmentDetailDialog(item: com.arktools.anlao.data.EquipmentItem, onEquip: () -> Unit = {}, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(BgPanel, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${item.rarity} ${item.category}", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${item.type} · 品阶+${item.lvl} · 耐久${item.durability}% · 价值${item.value}两", color = TextGray, fontSize = 11.sp)
            if (item.stats.isNotEmpty()) {
                Text("词条属性", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                item.stats.forEach { sm ->
                    sm.forEach { (k, v) ->
                        Text("${statDisp(k)}+${formatStatValue(k, v)}", color = TextWhite, fontSize = 12.sp)
                    }
                }
            } else {
                Text("无额外属性", color = TextGray, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onEquip() }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00)),
                    shape = RoundedCornerShape(6.dp)) { Text("装备", color = Color.Black, fontSize = 13.sp) }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                    shape = RoundedCornerShape(6.dp)) { Text("关闭", color = TextWhite, fontSize = 13.sp) }
            }
        }
    }
}

// ==================== TAB 2: 角色 ====================
@Composable
private fun CharacterTab(player: com.arktools.anlao.data.PlayerEntity, engine: com.arktools.anlao.engine.GameEngine) {
    val realm = CultivationRealm.entries.find { it.name == player.realm } ?: CultivationRealm.NONE
    val sect = MartialSect.entries.find { it.name == player.sect } ?: MartialSect.WANDERER
    val nextRealm = CultivationRealm.entries.getOrNull(realm.ordinal + 1)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
        // 身份
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AssetImageBox(player.portrait, 118, player.name)
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(player.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("身份: ${if (player.gender == "female") "女侠" else "少侠"}", color = TextGray, fontSize = 11.sp)
                            Text("门派: ${sect.displayName}", color = TextGray, fontSize = 11.sp)
                            Text("境界: ${MartialRealmDisplay.fromLevel(player.lvl)}", color = GoldAccent, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("修为", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("阅历 ${player.exp.expCurr}/${player.exp.expMax}", color = TextGray, fontSize = 10.sp)
                        }
                    }
                    if (nextRealm != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("下一境界: ${nextRealm.displayName} (需${MartialRealmDisplay.fromLevel(nextRealm.level * 10 + 10)} + ${nextRealm.level * 5}击杀)", color = TextGray, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 属性
        SectionTitle("战斗属性")
        StatRow("气血", "${player.stats.hp}/${player.stats.hpMax}", HpRed)
        StatRow("攻击", "${player.stats.atk}  (基础${player.baseStats.atk} +${"%.0f".format(player.bonusStats.atk)}%)", TextWhite)
        StatRow("防御", "${player.stats.def}  (基础${player.baseStats.def} +${"%.0f".format(player.bonusStats.def)}%)", TextWhite)
        StatRow("身法", "%.2f  (基础%.2f +%.0f%%)".format(player.stats.atkSpd, player.baseStats.atkSpd, player.bonusStats.atkSpd), TextWhite)
        StatRow("吸血", "%.1f%%".format(player.stats.vamp), TextWhite)
        StatRow("暴率", "%.1f%%".format(player.stats.critRate), TextWhite)
        StatRow("暴伤", "%.1f%%".format(player.stats.critDmg), TextWhite)

        // 累计加成（祝福+等级累计）
        if (player.bonusStats.hp > 0 || player.bonusStats.atk > 0 || player.bonusStats.def > 0 ||
            player.bonusStats.atkSpd > 0 || player.bonusStats.vamp > 0 || player.bonusStats.critRate > 0 || player.bonusStats.critDmg > 0) {
            Spacer(Modifier.height(4.dp))
            SectionTitle("累计加成（${player.blessing}次祝福）")
            val bonusParts = mutableListOf<String>()
            if (player.bonusStats.hp > 0) bonusParts.add("气血+${"%.0f".format(player.bonusStats.hp)}%")
            if (player.bonusStats.atk > 0) bonusParts.add("攻击+${"%.0f".format(player.bonusStats.atk)}%")
            if (player.bonusStats.def > 0) bonusParts.add("防御+${"%.0f".format(player.bonusStats.def)}%")
            if (player.bonusStats.atkSpd > 0) bonusParts.add("身法+${"%.0f".format(player.bonusStats.atkSpd)}%")
            if (player.bonusStats.vamp > 0) bonusParts.add("吸血+${"%.1f".format(player.bonusStats.vamp)}%")
            if (player.bonusStats.critRate > 0) bonusParts.add("暴率+${"%.1f".format(player.bonusStats.critRate)}%")
            if (player.bonusStats.critDmg > 0) bonusParts.add("暴伤+${"%.0f".format(player.bonusStats.critDmg)}%")
            Text(bonusParts.joinToString("  "), color = GoldAccent, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { engine.openShop() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B6914)),
                shape = RoundedCornerShape(6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AssetImageBox("ui/icons/shop.png", 28, "商城")
                    Text("商城", color = TextWhite, fontSize = 12.sp)
                }
            }
            Button(onClick = { engine.openBlacksmith() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4226)),
                shape = RoundedCornerShape(6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AssetImageBox("ui/icons/blacksmith.png", 28, "铁匠铺")
                    Text("铁匠铺", color = TextWhite, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 武学
        SectionTitle("武学")
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
        SectionTitle("战绩")
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
            SectionTitle("装备加成 (${eq.size}/9)")
            eq.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text("${item.rarity}${item.category} ", color = RarityCol[item.rarity] ?: TextWhite, fontSize = 11.sp)
                    item.stats.forEach { sm -> sm.forEach { (k, v) ->
                        Text("${statDisp(k)}+${formatStatValue(k, v)} ", color = TextGray, fontSize = 10.sp)
                    } }
                }
            }
        }

        val setBonuses = engine.activeSetBonusDescriptions()
        if (setBonuses.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            SectionTitle("套装效果")
            setBonuses.forEach { bonus ->
                Text(bonus, color = GoldAccent, fontSize = 11.sp, modifier = Modifier.padding(vertical = 1.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AssetImageBox(assetPath: String, sizeDp: Int, desc: String) {
    val ctx = LocalContext.current
    val bitmap = remember(assetPath) {
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeStream(ctx.assets.open(assetPath), null, options)?.asImageBitmap()
        } catch (_: Exception) { null }
    }
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, desc, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Text(desc.take(1), color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
private fun SettingsTab(
    engine: com.arktools.anlao.engine.GameEngine,
    player: com.arktools.anlao.data.PlayerEntity,
    muted: Boolean,
    bgmVolume: Float,
    sfxVolume: Float,
    onSaveFeedback: (Boolean) -> Unit,
    onReturnTitle: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("设置", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("音量、音效与存档", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

        SettingButton("总声音", if (muted) "已关闭" else "已开启", icon = "ui/icons/sound.png") { engine.soundManager.toggleMute() }
        VolumeRow("音乐音量", bgmVolume) { engine.soundManager.setBgmVolume(it) }
        VolumeRow("音效音量", sfxVolume) { engine.soundManager.setSfxVolume(it) }

        Spacer(Modifier.height(12.dp))
        SettingButton("保存进度", "安全存档", icon = "ui/icons/save.png") { onSaveFeedback(engine.trySafeSave()) }
        SettingButton("删除存档", "危险操作", icon = "ui/icons/delete.png", isDanger = true) { engine.deleteSave(); onReturnTitle() }
        SettingButton("返回标题", "回到主菜单", icon = "ui/icons/back.png") { engine.saveGame(); onReturnTitle() }

        Spacer(Modifier.height(20.dp))
        Text("暗牢江湖行 v1.0", color = TextGray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("已开启自动存档，离开游戏或定时会保存进度", color = TextGray, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun VolumeRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextWhite, fontSize = 14.sp)
            Text("${(value * 100).toInt()}%", color = TextGray, fontSize = 11.sp)
        }
        Slider(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingButton(label: String, hint: String, icon: String? = null, isDanger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, BorderWhite, RoundedCornerShape(6.dp))
        .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) AssetImageBox(icon, 28, label)
            Text(label, color = if (isDanger) HpRed else TextWhite, fontSize = 14.sp)
        }
        Text(hint, color = TextGray, fontSize = 11.sp)
    }
}

// ==================== COMBAT OVERLAY ====================
@Composable
private fun CombatOverlay(cs: CombatState, log: List<String>, sprite: String, eFl: Boolean, pFl: Boolean,
                           dmgNums: List<com.arktools.anlao.engine.GameEngine.DmgNumber>, engine: com.arktools.anlao.engine.GameEngine) {
    val ctx = LocalContext.current
    val eShake by animateFloatAsState(if (eFl) 14f else 0f, spring(stiffness = Spring.StiffnessHigh), label = "enemyShake")
    val pShake by animateFloatAsState(if (pFl) -10f else 0f, spring(stiffness = Spring.StiffnessHigh), label = "playerShake")
    val eScale by animateFloatAsState(if (eFl) 0.94f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "enemyScale")
    val pScale by animateFloatAsState(if (pFl) 0.96f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "playerScale")

    val bitmap = remember(sprite) {
        try { BitmapFactory.decodeStream(ctx.assets.open("武侠png/${sprite}.png"))?.asImageBitmap() } catch (_: Exception) { null }
    }

    Box(Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.95f)).padding(8.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))

            Column(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cs.enemyName, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val phaseText = if (cs.battleType == "guardian" || cs.battleType == "sboss") " · ${cs.bossPhase}阶段" else ""
                val phaseColor = if (cs.bossPhase >= 2) HpRed else GoldAccent
                Text("${MartialRealmDisplay.enemyFromLevel(cs.enemyLvl)}$phaseText", color = phaseColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(width = 190.dp, height = 150.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(130.dp).graphicsLayer { translationX = eShake; scaleX = eScale; scaleY = eScale }, contentAlignment = Alignment.Center) {
                        if (bitmap != null) Image(bitmap, "enemy", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        else Text(cs.enemyName.take(1), color = TextWhite, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                    dmgNums.filter { it.target == "enemy" }.takeLast(4).forEachIndexed { i, dn ->
                        FloatingDamageNumber(dn, i, Alignment.CenterEnd)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val ePct = (cs.enemyHp.toFloat() / cs.enemyHpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                        Box(Modifier.fillMaxWidth(ePct).fillMaxHeight().background(HpRed))
                    }
                    Text("${cs.enemyHp}/${cs.enemyHpMax}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("攻:${cs.enemyAtk} 防:${cs.enemyDef} 速:${"%.1f".format(cs.enemyAtkSpd)} 暴:${"%.1f".format(cs.enemyCritRate)}%", color = TextGray, fontSize = 10.sp)
            }

            Spacer(Modifier.height(8.dp))

            Column(Modifier.fillMaxWidth().border(1.dp, BorderWhite, RoundedCornerShape(6.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(engine.player.value.name, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(MartialRealmDisplay.fromLevel(engine.player.value.lvl), color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(width = 160.dp, height = 110.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(90.dp).graphicsLayer { translationX = pShake; scaleX = pScale; scaleY = pScale }, contentAlignment = Alignment.Center) { AssetImageBox(engine.player.value.portrait, 90, engine.player.value.name) }
                    dmgNums.filter { it.target == "player" }.takeLast(4).forEachIndexed { i, dn ->
                        FloatingDamageNumber(dn, i, Alignment.CenterStart)
                    }
                }
                val pPct = (cs.playerHp.toFloat() / cs.playerHpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                        Box(Modifier.fillMaxWidth(pPct).fillMaxHeight().background(HpRed))
                    }
                    Text("${cs.playerHp}/${cs.playerHpMax}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("攻:${cs.playerAtk} 防:${cs.playerDef} 速:${"%.1f".format(cs.playerAtkSpd)} 吸:${"%.1f".format(cs.playerVamp)}%", color = TextGray, fontSize = 10.sp)
            }

            Spacer(Modifier.height(6.dp))

            Box(Modifier.fillMaxWidth().weight(1f).border(1.dp, BorderWhite, RoundedCornerShape(4.dp)).background(Color(0xEE080808), RoundedCornerShape(4.dp)).padding(8.dp)) {
                LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                    items(log.reversed().take(18)) { Text(it, color = combatLogColor(it), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FloatingDamageNumber(dn: com.arktools.anlao.engine.GameEngine.DmgNumber, stackIndex: Int, alignment: Alignment) {
    val alpha = remember(dn.id) { Animatable(1f) }
    val lift = remember(dn.id) { Animatable(0f) }
    val scale = remember(dn.id) { Animatable(if (dn.isCrit) 1.45f else 1.15f) }
    LaunchedEffect(dn.id) {
        launch { lift.animateTo(-42f - stackIndex * 8f, tween(720)) }
        launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium)) }
        delay(260)
        alpha.animateTo(0f, tween(460))
    }
    val color = when (dn.kind) {
        "crit" -> GoldAccent
        "heavy" -> Color(0xFFFF5A3D)
        "skill" -> Color(0xFF8FD7FF)
        "thorns" -> Color(0xFFB68CFF)
        "heal" -> Color(0xFF5CFF8A)
        "taken" -> Color(0xFFFF6B6B)
        else -> TextWhite
    }
    val xOffset = if (alignment == Alignment.CenterStart) (-18 - stackIndex * 8).dp else (18 + stackIndex * 8).dp
    Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
        Text(
            dn.text,
            color = color,
            fontSize = if (dn.isCrit || dn.kind == "heavy") 24.sp else 19.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .offset(x = xOffset, y = lift.value.dp)
                .graphicsLayer { this.alpha = alpha.value; scaleX = scale.value; scaleY = scale.value }
                .background(Color(0xAA000000), RoundedCornerShape(12.dp))
                .border(1.dp, color.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CombatResultOverlay(cs: CombatState, engine: com.arktools.anlao.engine.GameEngine, onDeath: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(cs.combatId, cs.playerDead, cs.enemyDead) { alpha.animateTo(1f, tween(450)) }

    val context = LocalContext.current
    val activity = context as? Activity
    var showAdOptions by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.95f)).graphicsLayer { this.alpha = alpha.value }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cs.playerDead) {
                Text("败下阵来", color = HpRed, fontSize = 32.sp, fontWeight = FontWeight.Bold)

                if (!showAdOptions) {
                    Text("败退一层，损失少量阅历和白银，装备保留。", color = TextWhite, fontSize = 16.sp)

                    // 广告复活按钮（优先显示）
                    Button(
                        onClick = { showAdOptions = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("看广告·满血复活", color = Color.Black, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { engine.returnAfterDeath() },
                        colors = ButtonDefaults.buttonColors(containerColor = HpRed),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("退回上一层整备", color = TextWhite, fontSize = 18.sp)
                    }

                    OutlinedButton(
                        onClick = onDeath,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("返回标题", color = TextGray, fontSize = 14.sp)
                    }
                } else {
                    // 广告加载/展示中
                    Text("正在为你满血复活...", color = GoldAccent, fontSize = 16.sp)

                    LaunchedEffect(Unit) {
                        if (activity == null) {
                            engine.returnAfterDeath()
                            return@LaunchedEffect
                        }
                        AdHelper.showRewardAd(
                            activity = activity,
                            onRewarded = {
                                engine.reviveByAd()
                            },
                            onFailed = {
                                engine.returnAfterDeath()
                            },
                            onLoadStart = {
                                isAdLoading = true
                            },
                            onComplete = {
                                isAdLoading = false
                                showAdOptions = false
                            }
                        )
                    }
                }
            } else {
                Text("大获全胜！", color = GoldAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("${cs.expReward}阅历  ${cs.goldReward}白银", color = TextWhite, fontSize = 16.sp)

                if (!showAdOptions) {
                    Button(
                        onClick = { engine.dismissCombatResult() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EFF00)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("继续探索", color = Color.Black, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 双倍收益广告按钮
                    Button(
                        onClick = { showAdOptions = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("看广告·双倍收益", color = Color.Black, fontSize = 18.sp)
                    }
                } else {
                    // 广告加载/展示中
                    Text("正在获取双倍收益...", color = GoldAccent, fontSize = 16.sp)

                    LaunchedEffect(Unit) {
                        if (activity == null) {
                            engine.dismissCombatResult()
                            return@LaunchedEffect
                        }
                        AdHelper.showRewardAd(
                            activity = activity,
                            onRewarded = {
                                engine.doubleCombatReward()
                            },
                            onFailed = {
                                // 广告失败不影响游戏，直接继续
                            },
                            onLoadStart = {
                                isAdLoading = true
                            },
                            onComplete = {
                                isAdLoading = false
                                showAdOptions = false
                                engine.dismissCombatResult()
                            }
                        )
                    }
                }
            }
        }

        // 广告加载中遮罩
        AdLoadingOverlay(visible = isAdLoading)
    }
}

// ===== 工具函数 =====
private fun statDisp(k: String) = when (k) {
    "hp" -> "气血"; "atk" -> "攻击"; "def" -> "防御"; "atkSpd" -> "身法"
    "vamp" -> "吸血"; "critRate" -> "暴率"; "critDmg" -> "暴伤"; else -> k
}

private fun formatStatValue(k: String, v: Float): String = if (k in listOf("atkSpd", "vamp", "critRate", "critDmg")) {
    "%.2f%%".format(v)
} else {
    "%.2f".format(v)
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