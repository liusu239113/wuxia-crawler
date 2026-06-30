package com.wuxiacrawler.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wuxiacrawler.config.*
import com.wuxiacrawler.data.*
import com.wuxiacrawler.manager.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlin.math.roundToInt

class GameEngine(private val context: Context) {
    val soundManager = SoundManager(context)
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("wuxia_crawler", Context.MODE_PRIVATE)

    // ===== State Flows =====
    private val _player = MutableStateFlow(PlayerEntity())
    val player: StateFlow<PlayerEntity> = _player.asStateFlow()

    private val _realm = MutableStateFlow(RealmState())
    val realm: StateFlow<RealmState> = _realm.asStateFlow()

    var _combatState = MutableStateFlow<CombatState?>(null)
    val combatState: StateFlow<CombatState?> = _combatState.asStateFlow()

    private val _combatLog = MutableStateFlow<List<String>>(emptyList())
    val combatLog: StateFlow<List<String>> = _combatLog.asStateFlow()

    private val _realmLog = MutableStateFlow<List<String>>(emptyList())
    val realmLog: StateFlow<List<String>> = _realmLog.asStateFlow()

    val _availableUpgrades = MutableStateFlow<List<UpgradeOption>>(emptyList())
    val availableUpgrades: StateFlow<List<UpgradeOption>> = _availableUpgrades.asStateFlow()

    val _rerollsLeft = MutableStateFlow(2)
    val rerollsLeft: StateFlow<Int> = _rerollsLeft.asStateFlow()

    val _realmBreakthroughPending = MutableStateFlow(false)
    val realmBreakthroughPending: StateFlow<Boolean> = _realmBreakthroughPending.asStateFlow()

    val _realmBreakthroughInfo = MutableStateFlow<Pair<String, String>?>(null)
    val realmBreakthroughInfo: StateFlow<Pair<String, String>?> = _realmBreakthroughInfo.asStateFlow()

    val _currentEnemySprite = MutableStateFlow("slime")
    val currentEnemySprite: StateFlow<String> = _currentEnemySprite.asStateFlow()

    val _enemyFlinch = MutableStateFlow(false)
    val enemyFlinch: StateFlow<Boolean> = _enemyFlinch.asStateFlow()

    val _playerFlinch = MutableStateFlow(false)
    val playerFlinch: StateFlow<Boolean> = _playerFlinch.asStateFlow()

    val _dmgNumbers = MutableStateFlow<List<DmgNumber>>(emptyList())
    val dmgNumbers: StateFlow<List<DmgNumber>> = _dmgNumbers.asStateFlow()

    val _showLevelUp = MutableStateFlow(false)
    val showLevelUp: StateFlow<Boolean> = _showLevelUp.asStateFlow()

    private val _storyDialogue = MutableStateFlow<String?>(null)
    val storyDialogue: StateFlow<String?> = _storyDialogue.asStateFlow()

    data class EventPrompt(val message: String = "", val choices: List<String> = emptyList())
    private val _eventPrompt = MutableStateFlow<EventPrompt?>(null)
    val eventPrompt: StateFlow<EventPrompt?> = _eventPrompt.asStateFlow()

    data class QuestInfo(val chapter: String, val title: String, val objective: String, val progress: String, val story: String)

    data class DmgNumber(val id: Int, val text: String, val isCrit: Boolean, val target: String, val kind: String)

    private fun playerTitle(): String = if (_player.value.gender == "female") "女侠" else "少侠"
    private fun playerPronoun(): String = if (_player.value.gender == "female") "她" else "他"

    fun currentAreaName(floor: Int = _realm.value.floor): String {
        val names = listOf(
            "青石旧牢", "雨巷残门", "黑风寨道", "白骨渡口", "毒雾药窟",
            "断剑校场", "盘丝暗廊", "星宿钩台", "龙门水寨", "金身佛堂",
            "风雷崖", "寒霜宫道", "阎罗判堂", "血月刀冢", "无相狱心"
        )
        val cycle = (floor - 1) / names.size
        val base = names[(floor - 1).coerceAtLeast(0) % names.size]
        return if (cycle == 0) base else "$base · ${cycle + 1}巡"
    }

    fun currentQuestInfo(): QuestInfo {
        val r = _realm.value
        val p = _player.value
        val chapterIndex = chapterForFloor(r.floor)
        val chapter = "第${chapterIndex}章 · ${chapterTitle(chapterIndex)}"
        val floorGoal = r.roomsPerFloor
        val objective = when {
            r.floor < 5 -> "追查断裂牢符，穿过${currentAreaName()}并击败本层守卫"
            r.floor < 10 -> "寻找七派失踪线索，在${currentAreaName()}收集战利品强化自身"
            r.floor < 15 -> "逼近暗牢阵眼，击破${currentAreaName()}的精英守卫"
            else -> "揭开无相狱典真相，继续深入${currentAreaName()}"
        }
        val progress = "身份 ${playerTitle()} · 房间 ${r.room}/${floorGoal} · 本层斩敌 ${r.currentKills} · 境界 ${MartialRealmDisplay.fromLevel(p.lvl)}"
        val story = chapterStory(chapterIndex)
        return QuestInfo(chapter, "主线任务：${currentAreaName()}之谜", objective, progress, story)
    }

    private fun chapterForFloor(floor: Int): Int = when {
        floor <= 5 -> 1
        floor <= 15 -> 2
        floor <= 25 -> 3
        floor <= 35 -> 4
        floor <= 45 -> 5
        floor <= 55 -> 6
        floor <= 65 -> 7
        floor <= 75 -> 8
        floor <= 85 -> 9
        floor <= 100 -> 10
        else -> 11
    }

    private fun chapterTitle(chapter: Int): String = when (chapter) {
        1 -> "雨夜旧牢"
        2 -> "药谷余苦"
        3 -> "断剑校场"
        4 -> "龙门旧渡"
        5 -> "金身问心"
        6 -> "机关残响"
        7 -> "星宿命盘"
        8 -> "风雷血书"
        9 -> "阎罗旧案"
        10 -> "无相狱心"
        else -> "江湖余烬"
    }

    private fun chapterStory(chapter: Int): String = when (chapter) {
        1 -> "雨夜醒来，断符发烫。沈砚把一盏旧灯交到你手里，说旧牢门后有你的身世，也有七派失踪的答案。柳小满在雨里追来，只为寻找哥哥柳知秋。暗牢第一道门开启，你们都还不知道，这趟路会把三十年前封牢之夜重新照亮。"
        2 -> "毒雾药窟里有药谷旧香，也有被无相阵洗去名字的人。柳小满追着哥哥留下的木牌往前走，阿照却在化血药人的躯壳里喊不出自己的名字。你们开始明白，暗牢里的怪物未必天生是怪物。"
        3 -> "断剑校场剑旗折断，陆青棠的残魂守着七派旧誓。她告诉你，七派入牢并非全然无辜；有人贪图狱典，有人想证明自己，有人想救人却误伤更多人。侠义不是无错，而是知错后仍愿承担。"
        4 -> "龙门旧渡水声不息，叶沉舟守着一船未能回家的名字。当年他押送七派入牢，也亲眼看见你的父母把灯交给沈砚。船过白骨渡，你第一次清楚看见封牢之夜，也看见活下来的人背负了怎样的沉重。"
        5 -> "金身佛堂没有香火，只有被铁链锁住的佛和毒舌守灯僧无戒。救一人，还是救天下？柳小满只想救哥哥，沈砚只想赎旧罪，而你要在这些不完整的答案里，摸清自己拔剑的理由。"
        6 -> "机关堂残响轰鸣，唐照夜半活半魂地守着重铸炉。他解释机关宝匣、幻阵假门和装备品质都来自无相阵对欲望的仿造：同一把剑落到不同人手里，会被执念锻成不同模样。"
        7 -> "星宿钩台悬于黑暗，秦红袖半敌半友地拨动命盘。她说你不是被预言选中的人，预言只会记录最省力的路；而你一次次绕远、救人、犹豫，正把命盘推向无人能算的方向。"
        8 -> "风雷崖血书被雷雨撕开，顾长风的残影送来三十年前最后一封信。沈砚、你的父母、七派封牢者的旧事终于连成一线。所谓英雄，不是没有贪念和恐惧，而是在最后一步没有退。"
        9 -> "阎罗判堂审问每个人放弃过谁。柳知秋终于出现，仍记得妹妹爱吃糖，却已被无相阵困在保护与伤害之间。柳小满的亲情线走到最痛处，而你也必须决定，要不要原谅沈砚这盏迟来的灯。"
        10 -> "无相狱心开启。《无相狱典》不是秘籍，而是一面复制欲望的黑镜。它许诺父母可归、七派可复、旧人可活，只要你愿意交出真实的自己。最后一战不是为了成为天下第一，而是让江湖继续混乱、笨拙，却真实地活下去。"
        else -> "暗牢之后，江湖仍有雨夜、灯火、酒旗和不平事。你走过的每一层，都在把一段被封存的真相推回人间。"
    }

    private var nextCombatId = 1
    private var nextDamageNumberId = 1

    // ===== Character Creation =====
    fun createCharacter(name: String, gender: String, hpAlloc: Int, atkAlloc: Int, defAlloc: Int, spdAlloc: Int, skill: MartialSkill, sect: MartialSect) {
        val p = _player.value.copy()
        p.name = name
        p.gender = gender
        p.portrait = if (gender == "female") "characters/hero_female.png" else "characters/hero_male.png"
        p.sect = sect.name
        p.baseStats = PlayerStats(hp=50*hpAlloc, hpMax=50*hpAlloc, atk=10*atkAlloc, def=10*defAlloc, atkSpd=0.4f+0.02f*spdAlloc)
        p.skills = skill.name; p.isAllocated = true
        _player.value = p
        calculateStats()
        val created = _player.value.copy()
        created.stats.hp = created.stats.hpMax
        _player.value = created
        saveGame()
    }

    // ===== Stats =====
    fun calculateStats() {
        val p = _player.value
        val oldHp = p.stats.hp
        val realm = CultivationRealm.entries.find { it.name == p.realm } ?: CultivationRealm.NONE
        val sect = MartialSect.entries.find { it.name == p.sect } ?: MartialSect.WANDERER
        val equippedItems = parseEquipped().filter { it.category.isNotBlank() }
        p.equippedStats = PlayerStats(0,0,0,0,0f,0f,0f,0f)
        p.setBonusStats = PlayerStats(0,0,0,0,0f,0f,0f,0f)
        for (item in equippedItems) for (sm in item.stats) for ((k,v) in sm) {
            when(k) {
                "hp"->p.equippedStats.hpMax+=v.toInt(); "atk"->p.equippedStats.atk+=v.toInt()
                "def"->p.equippedStats.def+=v.toInt(); "atkSpd"->p.equippedStats.atkSpd+=v
                "vamp"->p.equippedStats.vamp+=v; "critRate"->p.equippedStats.critRate+=v; "critDmg"->p.equippedStats.critDmg+=v
            }
        }
        applySetBonuses(equippedItems, p.setBonusStats)
        p.stats = PlayerStats(
            hp=0,
            hpMax=(((p.baseStats.hpMax*(1f+p.bonusStats.hp/100f+sect.hpBonus)).toInt()*(1f+realm.hpBonus)).toInt()+p.equippedStats.hpMax+p.setBonusStats.hpMax).coerceAtLeast(1),
            atk=(((p.baseStats.atk*(1f+p.bonusStats.atk/100f+sect.atkBonus)).toInt()*(1f+realm.atkBonus)).toInt()+p.equippedStats.atk+p.setBonusStats.atk+p.tempStats.atk.toInt()).coerceAtLeast(1),
            def=(((p.baseStats.def*(1f+p.bonusStats.def/100f+sect.defBonus)).toInt()*(1f+realm.defBonus)).toInt()+p.equippedStats.def+p.setBonusStats.def).coerceAtLeast(0),
            atkSpd=(p.baseStats.atkSpd*(1f+p.bonusStats.atkSpd/100f+sect.atkSpdBonus)+p.equippedStats.atkSpd/100f+p.setBonusStats.atkSpd/100f+p.tempStats.atkSpd).coerceAtMost(2.5f),
            vamp=p.bonusStats.vamp+p.equippedStats.vamp+p.setBonusStats.vamp+sect.vampBonus,
            critRate=p.bonusStats.critRate+p.equippedStats.critRate+p.setBonusStats.critRate+sect.critRateBonus,
            critDmg=50f+p.bonusStats.critDmg+p.equippedStats.critDmg+p.setBonusStats.critDmg
        )
        if (p.skills.contains("DEVASTATOR")) p.stats.atk = (p.stats.atk*1.3f).toInt()
        if (p.skills.contains("BLOODTHIRST")) p.stats.vamp += 5f
        if (p.skills.contains("PRECISION")) p.stats.critRate += 8f
        p.stats.hp = oldHp.coerceIn(1, p.stats.hpMax)
        p.stats.hpPercent = (p.stats.hp.toFloat()/p.stats.hpMax*100f)
        _player.value = p.copy()
    }

    fun parseEquipped(): List<EquipmentItem> {
        val j = _player.value.equipped; if (j.isBlank()||j=="[]") return emptyList()
        return try { normalizeEquipped(gson.fromJson(j, object: TypeToken<List<EquipmentItem>>(){}.type)) } catch (_:Exception) { emptyList() }
    }

    private val equipSlots = listOf("兵器", "护甲", "盾牌", "头盔", "鞋履", "饰品")

    private fun normalizeEquipped(items: List<EquipmentItem>): List<EquipmentItem> {
        val slots = MutableList(equipSlots.size) { EquipmentItem(type = equipSlots[it]) }
        items.filter { it.category.isNotBlank() }.forEach { item ->
            val idx = equipSlots.indexOf(item.type).takeIf { it >= 0 } ?: 0
            slots[idx] = item
        }
        return slots
    }
    private fun applySetBonuses(equippedItems: List<EquipmentItem>, bonus: PlayerStats) {
        equippedItems.filter { it.category.isNotBlank() }.groupingBy { setFamily(it) }.eachCount().forEach { (family, count) ->
            if (count >= 2) {
                when (family) {
                    "玄铁" -> bonus.def += 18
                    "游龙" -> bonus.atkSpd += 4f
                    "夜行" -> bonus.critRate += 3f
                    "青玉" -> bonus.hpMax += 90
                    else -> bonus.atk += 16
                }
            }
            if (count >= 4) {
                when (family) {
                    "玄铁" -> bonus.hpMax += 160
                    "游龙" -> bonus.critDmg += 16f
                    "夜行" -> bonus.vamp += 3f
                    "青玉" -> bonus.def += 22
                    else -> bonus.critRate += 4f
                }
            }
            if (count >= 6) {
                bonus.vamp += 4f
                bonus.critDmg += 22f
            }
        }
    }

    fun activeSetBonusDescriptions(): List<String> {
        return parseEquipped().filter { it.category.isNotBlank() }.groupingBy { setFamily(it) }.eachCount().flatMap { (family, count) ->
            val lines = mutableListOf<String>()
            if (count >= 2) lines.add("$family 套装2件：${setBonusText(family, 2)}")
            if (count >= 4) lines.add("$family 套装4件：${setBonusText(family, 4)}")
            if (count >= 6) lines.add("$family 套装6件：${setBonusText(family, 6)}")
            lines
        }
    }

    private fun setFamily(item: EquipmentItem): String = when {
        item.category.contains("玄铁") || item.category.contains("玄武") -> "玄铁"
        item.category.contains("游龙") || item.category.contains("龙纹") -> "游龙"
        item.category.contains("夜行") || item.category.contains("袖里") || item.category.contains("踏云") -> "夜行"
        item.category.contains("青") || item.category.contains("金丝") || item.category.contains("玉") -> "青玉"
        item.category.contains("虎") || item.category.contains("狮") || item.category.contains("开山") || item.category.contains("镇岳") -> "猛威"
        else -> item.category.take(2)
    }

    private fun setBonusText(family: String, pieces: Int): String = when (family to pieces) {
        "玄铁" to 2 -> "防御+18"
        "玄铁" to 4 -> "气血+160"
        "游龙" to 2 -> "身法+4%"
        "游龙" to 4 -> "暴伤+16%"
        "夜行" to 2 -> "暴率+3%"
        "夜行" to 4 -> "吸血+3%"
        "青玉" to 2 -> "气血+90"
        "青玉" to 4 -> "防御+22"
        "猛威" to 2 -> "攻击+16"
        "猛威" to 4 -> "暴率+4%"
        family to 6 -> "吸血+4% 暴伤+22%"
        else -> if (pieces == 2) "攻击+16" else if (pieces == 4) "暴率+4%" else ""
    }

    fun parseInventory(): List<EquipmentItem> {
        val j = _player.value.inventory; if (j.isBlank()||j=="[]") return emptyList()
        return try { gson.fromJson(j, object: TypeToken<List<EquipmentItem>>(){}.type) } catch (_:Exception) { emptyList() }
    }

    // ===== 江湖历练 =====
    fun startExploring() {
        val current = _realm.value
        val shouldClearEvent = current.currentEvent == "combat_result" || current.currentEvent.isBlank()
        val r = current.copy(
            isPaused = false,
            isExploring = true,
            isEventActive = if (shouldClearEvent) false else current.isEventActive,
            currentEvent = if (shouldClearEvent) "" else current.currentEvent
        )
        _realm.value = r
        if (_combatState.value == null) soundManager.playBgm(context,"jianghu")
        soundManager.playSfx("gong_start")
    }
    fun pauseExploring() { val r = _realm.value.copy(isPaused = true, isExploring = false); _realm.value = r; soundManager.stopBgm(); soundManager.playSfx("bell_pause") }

    fun tickRealm() {
        val current = _realm.value
        if (!current.isExploring || current.isEventActive) return
        val r = current.copy()
        r.actionCounter++; r.runTime++
        _player.value = _player.value.copy(playtime = _player.value.playtime + 1)
        val types = mutableListOf(
            "enemy", "enemy", "enemy", "enemy",
            "nothing", "nothing", "nothing", "nothing", "nothing",
            "treasure", "route",
            "merchant", "healer", "manual",
            "blessing", "curse", "monarch"
        )
        if (r.actionCounter > 2 && r.actionCounter < 6) types.add("nextroom")
        else if (r.actionCounter > 5) { processEvent(r, "nextroom"); _realm.value = r; return }
        processEvent(r, types.random())
        _realm.value = r
    }

    private val spriteMap = mapOf(
        "黑寨喽啰" to "goblin",
        "飞刀恶徒" to "goblin_archer",
        "飞檐刺客" to "goblin_rogue",
        "荒原狼卫" to "wolf",
        "黑风狼卫" to "wolf_black",
        "寒岭狼卫" to "wolf_winter",
        "化血药人" to "slime",
        "铁布衫石奴" to "slime",
        "明王护法" to "slime_angel",
        "金甲剑侍" to "slime_knight",
        "断剑门徒" to "slime_crusader",
        "蛮寨剑客" to "orc_swordsmaster",
        "蛮寨斧客" to "orc_axe",
        "蛮寨弓手" to "orc_archer",
        "毒窟刀奴" to "spider",
        "赤练刀客" to "spider_red",
        "碧毒刀奴" to "spider_green",
        "暗弩门徒" to "skeleton_archer",
        "白衣剑客" to "skeleton_swordsmaster",
        "黑甲枪卫" to "skeleton_knight",
        "白衣刀客" to "skeleton_warrior",
        "夜行刺客" to "skeleton_samurai",
        "水寨刀匪" to "skeleton_pirate",
        "机关宝匣" to "mimic",
        "幻阵假门" to "mimic_door",
        "霸刀·黑寨统领" to "goblin_boss",
        "枯木·白衣门主" to "skeleton_boss",
        "赤练·刀堂主" to "spider_fire",
        "不老·枯木宗师" to "berthelot",
        "化血·药坛主" to "slime_boss",
        "天钩·星宿护法" to "zodiac_cancer",
        "明王·金身罗汉" to "alfadriel",
        "龙骑·天摩尊者" to "tiamat",
        "无名·堕落剑王" to "fallen_king",
        "白虹·星宿护法" to "zodiac_aries",
        "千丝·夫人" to "ant_queen",
        "机括·机关堂主" to "spider_boss",
        "天狼·黑风寨主" to "wolf_boss",
        "铁犬·猎犬使" to "hellhound",
        "三刀·獒王寨主" to "cerberus_ptolemaios",
        "镇山·铁掌帮主" to "behemoth",
        "龙门·煞罗堂主" to "zalaras",
        "黑袍·风雷长老" to "skeleton_dragon",
        "火云·赤袍老祖" to "firelord",
        "冰魄·寒霜宫主" to "icemaiden",
        "索命·阎罗判官" to "thanatos",
        "暗影·夺魂使" to "da-reaper",
        "盘丝·蛛索长老" to "spider_dragon",
        "血煞·疯魔刀圣" to "bm-feral"
    )

    fun processEvent(r: RealmState, event: String) {
        r.isEventActive = true
        r.currentEvent = event
        when(event) {
            "nextroom" -> {
                r.currentEvent = if (r.room >= r.roomsPerFloor) "guardian_gate" else "room_gate"
                if (r.room >= r.roomsPerFloor) addRealmLog("找到了通往下一处江湖据点的山门！护法守在门前。", listOf("进入", "无视"))
                else addRealmLog("前方有一处江湖据点。", listOf("进入", "无视"))
            }
            "treasure" -> {
                if (Random.nextInt(100) >= 28) { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
                else addRealmLog("发现一间藏宝室，里面有一个宝箱。", listOf("打开宝箱", "无视"))
            }
            "route" -> {
                if (Random.nextInt(100) >= 35) { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
                else addRealmLog("前方出现三条岔路：左侧安静，右侧血腥，中路传来锁链声。", listOf("稳步前行", "冒险深入", "搜寻密道"))
            }
            "merchant" -> {
                if (Random.nextInt(100) >= 22) { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
                else {
                    val medCost = 120L * r.floor
                    val weaponCost = 220L * r.floor
                    addRealmLog("一名蒙面游商坐在灯下，伤药需${medCost}两，旧兵器需${weaponCost}两。", listOf("买伤药(${medCost}两)", "买兵器(${weaponCost}两)", "离开"))
                }
            }
            "healer" -> {
                if (Random.nextInt(100) >= 18) { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
                else {
                    val cost = 80L * r.floor
                    addRealmLog("破庙里有一位老医师：疗伤${cost}两，请教可提升少量防御。", listOf("疗伤(${cost}两)", "请教(防御+1%)", "离开"))
                }
            }
            "manual" -> {
                if (Random.nextInt(100) >= 14) { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
                else addRealmLog("石匣中藏着三页残破功谱，只够参悟其中一页。", listOf("刀法(攻击+3%)", "身法(身法+2%)", "铁骨(气血+4% 防御+2%)"))
            }
            "nothing" -> { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
            "enemy" -> { generateEnemy(); addRealmLog("遭遇【${_combatState.value?.enemyName}】！", listOf("迎战", "逃跑")) }
            "blessing" -> {
                if (Random.nextInt(2) == 1) {
                    val p = _player.value
                    if (p.blessing < 1) p.blessing = 1
                    val cost = (p.blessing * (500.0 * (p.blessing * 0.5)) + 750).toLong()
                    addRealmLog("发现悟道碑文！供奉${cost}两白银可获得祝福。（祝福${p.blessing}重）", listOf("供奉", "无视"))
                } else { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
            }
            "curse" -> {
                if (Random.nextInt(3) == 1) {
                    val clvl = ((r.enemyScaling - 1f) * 10).toInt()
                    val cost = (clvl * (10000.0 * (clvl * 0.5)) + 5000).toLong()
                    addRealmLog("发现黑市悬赏！缴纳${cost}两白银可提升江湖声望。（声望${clvl}重）", listOf("缴纳", "无视"))
                } else { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
            }
            "monarch" -> {
                if (Random.nextInt(7) == 1) addRealmLog("前方传来恐怖的气息……似乎有武林至尊在此沉睡。", listOf("进入", "避开"))
                else { nothingEvent(); r.isEventActive = false; r.currentEvent = "" }
            }
        }
    }

    private fun nothingEvent() {
        addRealmLog(listOf("四处探索，空无一物……","发现一个空的宝箱。","发现一处废弃营地。","发现一柄断剑。","这片区域早已被人搜刮干净。").random())
    }

    private fun clearPendingEncounter() {
        if (!_player.value.inCombat) {
            _combatState.value = null
            _combatLog.value = emptyList()
        }
    }

    fun chooseOption(idx: Int) {
        _eventPrompt.value = null
        val r = _realm.value.copy()
        val eventBefore = r.currentEvent
        when (r.currentEvent) {
            "enemy" -> {
                if (idx == 0) { startCombat("battle"); addRealmLog("拔剑迎战！") }
                else if (idx == 1) {
                    if (Random.nextBoolean()) {
                        addRealmLog("施展轻功脱身，继续前行。")
                        _player.value = _player.value.copy(inCombat = false)
                        clearPendingEncounter()
                        r.isEventActive = false
                        r.currentEvent = ""
                    } else { addRealmLog("退路被封，只能迎战！"); startCombat("battle") }
                }
            }
            "treasure" -> { if (idx == 0) chestEvent() else { ignoreEvent(); r.isEventActive = false; r.currentEvent = "" } }
            "route" -> { routeEvent(idx); r.isEventActive = false; r.currentEvent = "" }
            "merchant" -> { merchantEvent(idx); r.isEventActive = false; r.currentEvent = "" }
            "healer" -> { healerEvent(idx); r.isEventActive = false; r.currentEvent = "" }
            "manual" -> { manualEvent(idx); r.isEventActive = false; r.currentEvent = "" }
            "blessing" -> {
                if (idx == 0) {
                    val p = _player.value.copy()
                    if (p.blessing < 1) p.blessing = 1
                    val cost = (p.blessing * (500.0 * (p.blessing * 0.5)) + 750).toLong()
                    if (p.gold < cost) { addRealmLog("银两不足。"); soundManager.playSfx("blocked") }
                    else { p.gold -= cost; _player.value = p; statBlessing(); soundManager.playSfx("wood_confirm") }
                } else ignoreEvent()
                r.isEventActive = false
                r.currentEvent = ""
            }
            "curse" -> {
                if (idx == 0) {
                    val clvl = ((r.enemyScaling - 1f) * 10).toInt()
                    val cost = (clvl * (10000.0 * (clvl * 0.5)) + 5000).toLong()
                    if (_player.value.gold < cost) { addRealmLog("银两不足。"); soundManager.playSfx("blocked") }
                    else {
                        _player.value = _player.value.copy(gold = _player.value.gold - cost)
                        r.enemyScaling += 0.1f
                        addRealmLog("江湖声望提升，战利品品质提升。（声望${clvl}重→${clvl + 1}重）")
                        soundManager.playSfx("qi_flow")
                    }
                } else ignoreEvent()
                r.isEventActive = false
                r.currentEvent = ""
            }
            "room_gate" -> { if (idx == 0) roomTransition() else { ignoreEvent(); r.isEventActive = false; r.currentEvent = "" } }
            "guardian_gate" -> { if (idx == 0) guardianBattle() else { ignoreEvent(); r.isEventActive = false; r.currentEvent = "" } }
            "monarch" -> { if (idx == 0) specialBossBattle() else { ignoreEvent(); r.isEventActive = false; r.currentEvent = "" } }
            "floor_clear" -> {
                if (idx == 0) advanceFloor()
                else {
                    addRealmLog("你决定暂留${currentAreaName()}，继续搜寻线索和战利品。")
                    r.isEventActive = false
                    r.currentEvent = ""
                    r.isExploring = true
                }
            }
            else -> { ignoreEvent(); r.isEventActive = false; r.currentEvent = "" }
        }
        val now = _realm.value
        if (!_player.value.inCombat && now.currentEvent != "combat_result") clearPendingEncounter()
        if (!_player.value.inCombat && now.currentEvent == eventBefore && now.currentEvent != "combat_result") _realm.value = r
    }

    private fun routeEvent(idx: Int) {
        when (idx) {
            0 -> {
                addRealmLog("你选择稳步前行，避开了暗处机关。")
                _realm.value = _realm.value.copy(actionCounter = 0)
                soundManager.playSfx("wood_confirm")
            }
            1 -> {
                addRealmLog("你踏入血腥岔路，危险更近，但战利品也更诱人。")
                if (Random.nextBoolean()) {
                    generateEnemy()
                    startCombat("battle")
                } else {
                    createEquipPrint()
                    saveGame()
                }
            }
            2 -> {
                addRealmLog("你沿墙搜寻密道，绕过一段暗牢，直接接近下一间石室。")
                val r = _realm.value.copy()
                r.room = (r.room + 1).coerceAtMost(r.roomsPerFloor)
                r.actionCounter = 0
                _realm.value = r.copy(isEventActive = false, currentEvent = "")
                soundManager.playSfx("scroll_open")
            }
        }
    }

    private fun merchantEvent(idx: Int) {
        val p = _player.value.copy()
        when (idx) {
            0 -> {
                val cost = 120L * _realm.value.floor
                if (p.gold < cost) { addRealmLog("银两不足，游商收起了伤药。"); soundManager.playSfx("blocked") }
                else {
                    p.gold -= cost
                    p.stats.hp = (p.stats.hp + p.stats.hpMax * 35 / 100).coerceAtMost(p.stats.hpMax)
                    _player.value = p
                    addRealmLog("买下伤药，恢复三成半气血。")
                    soundManager.playSfx("qi_flow")
                    saveGame()
                }
            }
            1 -> {
                val cost = 220L * _realm.value.floor
                if (p.gold < cost) { addRealmLog("银两不足，游商摇头不语。"); soundManager.playSfx("blocked") }
                else {
                    p.gold -= cost
                    _player.value = p
                    createEquipPrint()
                    addRealmLog("游商递来一件旧兵器，说它曾随一位高手入牢。")
                    soundManager.playSfx("equip_blade")
                    saveGame()
                }
            }
            else -> ignoreEvent()
        }
    }

    private fun healerEvent(idx: Int) {
        val p = _player.value.copy()
        when (idx) {
            0 -> {
                val cost = 80L * _realm.value.floor
                if (p.gold < cost) { addRealmLog("银两不足，老医师只留下一声叹息。"); soundManager.playSfx("blocked") }
                else {
                    p.gold -= cost
                    p.stats.hp = p.stats.hpMax
                    _player.value = p
                    addRealmLog("老医师替你封住伤口，气血回满。")
                    soundManager.playSfx("qi_flow")
                    saveGame()
                }
            }
            1 -> {
                p.bonusStats.def += 1f
                _player.value = p
                calculateStats()
                addRealmLog("老医师指点你运气护身，防御略有精进。")
                soundManager.playSfx("realm_breakthrough")
                saveGame()
            }
            else -> ignoreEvent()
        }
    }

    private fun manualEvent(idx: Int) {
        val p = _player.value.copy()
        when (idx) {
            0 -> { p.bonusStats.atk += 3f; addRealmLog("你参悟残页刀法，攻击提升。") }
            1 -> { p.bonusStats.atkSpd += 2f; addRealmLog("你参悟残页身法，出手更快。") }
            2 -> { p.bonusStats.hp += 4f; p.bonusStats.def += 2f; addRealmLog("你参悟铁骨心诀，气血与防御提升。") }
        }
        _player.value = p
        calculateStats()
        soundManager.playSfx("realm_breakthrough")
        saveGame()
    }

    private fun chestEvent() {
        soundManager.playSfx("wood_confirm")
        when(Random.nextInt(4)){
            0->{generateEnemy("chest");addRealmLog("机关宝匣突然弹开，暗器齐发！");startCombat("battle")}
            1->{if(_realm.value.floor==1)goldDrop() else createEquipPrint(); saveGame(); _realm.value = _realm.value.copy(isEventActive = false, currentEvent = "")}
            2->{goldDrop(); saveGame(); _realm.value = _realm.value.copy(isEventActive = false, currentEvent = "")}
            3->{addRealmLog("宝箱是空的。");_realm.value = _realm.value.copy(isEventActive = false, currentEvent = "")}
        }
    }

    fun goldDrop() { soundManager.playSfx("coin_pouch"); val amt=Random.nextInt(50,500)*_realm.value.floor; _player.value = _player.value.copy(gold = _player.value.gold + amt); addRealmLog("获得${amt}两白银。") }

    private fun statBlessing() {
        soundManager.playSfx("qi_flow"); val p=_player.value.copy(); if(p.blessing<1)p.blessing=1
        val s = mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f)
        val (k,v) = s.entries.random()
        when(k){"hp"->p.bonusStats.hp+=v;"atk"->p.bonusStats.atk+=v;"def"->p.bonusStats.def+=v;"atkSpd"->p.bonusStats.atkSpd+=v;"vamp"->p.bonusStats.vamp+=v;"critRate"->p.bonusStats.critRate+=v;"critDmg"->p.bonusStats.critDmg+=v}
        addRealmLog("祝福获得${statDisplay(k)}+${v}%！（祝福${p.blessing}重→${p.blessing+1}重）"); p.blessing++
        calculateStats(); saveGame(); _player.value = p.copy()
    }

    private fun roomTransition() {
        when(Random.nextInt(3)){
            0->{ incrementRoom(); generateEnemy("door"); addRealmLog("幻阵假门显形！"); startCombat("battle") }
            1->{ incrementRoom(); addRealmLog("进入新房间，发现宝箱！",listOf("打开宝箱","无视")); _realm.value = _realm.value.copy(isEventActive = true, currentEvent = "treasure") }
            else->{ incrementRoom(); addRealmLog("进入了下一个房间。"); _realm.value = _realm.value.copy(isEventActive = false, currentEvent = "") }
        }
    }

    private fun guardianBattle() { generateEnemy("guardian"); startCombat("guardian"); addCombatLog("江湖守卫【${_combatState.value?.enemyName}】挡住了通往下一层的路！") }
    private fun specialBossBattle() { generateEnemy("sboss"); startCombat("boss"); addCombatLog("武林至尊【${_combatState.value?.enemyName}】苏醒！"); addRealmLog("武林至尊【${_combatState.value?.enemyName}】苏醒！") }
    private fun ignoreEvent() { soundManager.playSfx("wood_confirm"); addRealmLog("选择无视，继续前行。") }

    private fun advanceFloor() {
        val r = _realm.value.copy()
        val oldFloor = r.floor
        r.floor++
        r.room = 1
        r.currentKills = 0
        r.actionCounter = 0
        r.isEventActive = false
        r.currentEvent = ""
        r.isExploring = true
        _realm.value = r
        grantFloorClearReward(oldFloor)
        addRealmLog("进入【${currentAreaName(r.floor)}】。主线继续推进。")
        if (chapterForFloor(r.floor) != chapterForFloor(oldFloor)) {
            _storyDialogue.value = "【${_player.value.name} · ${playerTitle()}】\n${chapterStory(chapterForFloor(r.floor))}"
        } else {
            storyBeatForFloor(r.floor)
        }
        saveGame()
    }

    private fun grantFloorClearReward(clearedFloor: Int) {
        val silver = (65L + clearedFloor * 28L + Random.nextLong(0L, 36L)).coerceAtMost(1200L)
        val p = _player.value.copy(gold = _player.value.gold + silver)
        if (clearedFloor % 4 == 0) p.bonusStats.hp += 1f
        if (clearedFloor % 5 == 0) p.bonusStats.atk += 1f
        _player.value = p
        calculateStats()
        val extra = when {
            clearedFloor % 5 == 0 -> "，并从守卫遗物中悟得攻击+1%"
            clearedFloor % 4 == 0 -> "，并在残碑前调息，气血+1%"
            else -> ""
        }
        addRealmLog("通关第${clearedFloor}层，获得${silver}两白银$extra。")
        soundManager.playSfx("coin_pouch")
    }

    private fun incrementRoom() {
        val r = _realm.value.copy()
        val oldFloor = r.floor
        r.room++
        r.actionCounter=0
        if(r.room>r.roomsPerFloor){r.room=1;r.floor++}
        _realm.value = r
        if (r.floor != oldFloor) storyBeatForFloor(r.floor)
    }

    private fun storyBeatForFloor(floor: Int) {
        val msg = detailedFloorStory(floor)
        if (msg != null) _storyDialogue.value = "【${_player.value.name} · ${playerTitle()}】\n$msg"
    }

    private fun detailedFloorStory(floor: Int): String? = when (floor) {
            2 -> "旁白：石壁上刻着半句残诗：入暗牢者，先失其名，再失其心。\n\n柳小满：这字不像刻上去的，倒像有人用指甲一点点抠出来的。\n\n你：能在这里留下字的人，未必还记得自己的名字。\n\n柳小满：那你呢？你要是也忘了，我就每天在你耳边喊。喊到你烦，烦到想起来。\n\n远处的黑寨喽啰拖着铁链巡过，链尾拴着几枚七派腰牌。沈砚给你的灯忽明忽暗，灯芯里映出一个陌生女子的侧影。\n\n陌生女子：别急着相信掌灯的人。\n\n柳小满猛地回头，糖篮撞在墙上。\n\n柳小满：你听见了吗？有人在说掌柜坏话。\n\n你：听见了。所以我们更要往前走。"
            3 -> "旁白：牢门铜环上有与你身上断符相同的纹路。门后伏着飞刀恶徒，他们使的暗器却是药谷旧制。\n\n柳小满拾起一枚飞刀，脸色忽然变白。\n\n柳小满：这是药谷的柳叶刀，不是江湖匪徒该有的东西。\n\n你：你哥哥用过？\n\n柳小满：他不会拿它杀人。他拿这刀削糖模，削得歪歪扭扭，还非说像兔子。\n\n飞刀恶徒从阴影里低笑，袖中寒光再起。\n\n飞刀恶徒：药谷？药谷早没了，剩下的只是听命的手。\n\n柳小满咬住嘴唇，第一次没有躲到你身后。\n\n柳小满：那我就把这只手打醒。\n\n你：一起。"
            4 -> "旁白：一个瘦小姑娘坐在空牢门前卖糖人。她叫柳小满，糖篮里没有糖，只有一张哥哥留下的旧纸条：别来暗牢。\n\n你：你既然知道纸条写什么，为什么还来？\n\n柳小满：因为写纸条的人是我哥。他越说别来，就越说明他希望有人去找他。\n\n你：也可能他只是想护住你。\n\n柳小满低头看着空篮子，指尖在篮沿上轻轻敲着。\n\n柳小满：我小时候发烧，他守了我三天。药太苦，他就熬糖骗我喝。后来我问他怕不怕，他说怕啊，可妹妹病着，哥哥怕也得在。\n\n她抬头看你，故作轻松地笑。\n\n柳小满：现在哥哥病了，换我在。很公平。\n\n你没有再劝，只把灯往她那边挪近了一点。"
            5 -> "旁白：暗处有人低声唤你的名字。那声音不像活人，却像母亲在梦里哄你睡觉。\n\n母亲残声：别回头，孩子。\n\n你握紧断符，指节发白。\n\n柳小满：是你娘？\n\n你：我不知道。我甚至不记得她的声音。\n\n柳小满：那就先别信。暗牢这么会骗人，万一它学得不像，还占你便宜。\n\n黑风寨道尽头，霸刀统领守着一面旧旗，旗面写着七派盟约：若无相阵破，江湖百年皆成傀儡。\n\n霸刀统领：封牢人的后代，终于来了。\n\n你：你认得我？\n\n霸刀统领：阵认得你。血认得你。欠债的人，也认得你。\n\n柳小满小声嘀咕：听起来不像好话。\n\n你拔剑：那就打到他说人话。"
            6 -> "旁白：柳小满跟了上来。她说她不拖后腿，真遇到危险就躲你身后。说完又补一句：若你倒了，我就把你也拖走。\n\n你：你真拖得动？\n\n柳小满：拖不动就滚。山路这么斜，总能滚出去。\n\n她认得黑风寨的口哨，那是哥哥小时候逗她笑的调子，如今却被狼卫吹成索命暗号。\n\n柳小满：我哥吹这个会跑调。这个人没跑调，不是他。\n\n你：你是在庆幸？\n\n柳小满沉默了一会儿。\n\n柳小满：嗯。也有点失望。\n\n你们顺着口哨追进残门，狼卫的影子在雨雾里一闪而过。她把小刀藏回袖中，跟得比刚才更紧。"
            7 -> "旁白：雨巷残门后是一条铺满湿竹叶的路，飞檐刺客从檐角坠下，招式轻得像雨。\n\n飞檐刺客：入阵者，留名。\n\n你：我的名字不是给你记的。\n\n柳小满忽然盯住刺客脚腕。\n\n柳小满：等等，他脚上的止血结……是药谷内门打法。\n\n刺客动作一滞，像身体听见了久违的旧称呼。\n\n飞檐刺客：药谷……药谷……奉命……清除……\n\n柳小满：他不是被雇来的。他被洗掉了。\n\n你挡下刺客一击，低声道：那就先让他停下。\n\n柳小满：别杀太快。至少让我看看，他身上有没有我哥留下的记号。"
            8 -> "旁白：一具旧甲旁压着血书：七派入牢，皆为一人所邀。血书末尾有沈砚的私印。\n\n柳小满：沈掌柜？他不是说自己只是守客栈的吗？\n\n你：他说的是‘只是守客栈’，没说他以前守过什么。\n\n柳小满：要不要回去问他？我可以负责拍桌子。\n\n你：如果他真想骗我们，不会把灯交给我。\n\n柳小满：也可能他赌你会这么想。\n\n你看着血书上几乎被浸烂的私印，沉默片刻。\n\n你：所以我们继续走。走到他不能再只说一半为止。\n\n柳小满把血书叠好塞进篮底。\n\n柳小满：那这账先记着。等出去，我连本带利问。"
            9 -> "旁白：白骨渡口的水不流，水面浮着灯。每盏灯下都有一个名字：剑阁、药谷、佛堂、星宿、龙门、寒霜、风雷。\n\n柳小满忽然蹲下，手指停在一盏灯前。\n\n柳小满：柳知秋。\n\n你：灯还亮着。\n\n柳小满：亮着是不是说明他还活着？\n\n你没有立刻回答。水下伸出无数白骨手臂，像整个渡口都在等活人替它们继续往前走。\n\n白骨渡魂：活人渡死人，死人渡活人。留下一个，过去一个。\n\n柳小满把手缩回来，声音发抖。\n\n柳小满：我不换。我哥要是知道我拿别人换他，醒了也会揍我。\n\n你：那就不换。我们打过去。"
            10 -> "旁白：断井边浮出沈砚的幻影。年轻时的他提剑大笑，说江湖若有不平，他偏要去管。\n\n年轻沈砚：你们这些后来人，总觉得真相藏在前面。其实真相常常站在门口，只是没人敢问。\n\n你：那你敢问了吗？\n\n幻影的笑意慢慢消失。\n\n年轻沈砚：我问了，所以七派进了暗牢。我退了，所以你父母没能出来。\n\n柳小满：你把药谷也拖进来了？\n\n年轻沈砚看向她，眼神里第一次有了躲闪。\n\n年轻沈砚：药谷柳家，不该再有人进来了。\n\n柳小满攥紧糖篮：这话你该对我哥说。\n\n白骨门主在渡口尽头拔剑，水面所有灯火同时压低。你挡在柳小满身前，她却从你身侧迈出半步。\n\n柳小满：这次我也要听答案。"
            11 -> "旁白：毒雾药窟中飘着甜腻药香。雾气像一层薄糖，落在舌尖却苦得发麻。\n\n柳小满停下脚步，脸色比刚才更白。\n\n柳小满：桂花蜜。\n\n你：这里有毒。\n\n柳小满：我知道。可是这味道……我哥熬糖时也会放桂花蜜。他说苦药总得有点甜，不然人会撑不下去。\n\n化血药人从雾里扑出，嘴里含着半块糖，腕间系着一根褪色红绳。它没有立刻攻击，只盯着柳小满的糖篮。\n\n化血药人：糖……苦……阿照不喝药……\n\n柳小满：阿照？药谷的小药童阿照？\n\n你：你认得他？\n\n柳小满：小时候他总跟在我哥后面，偷吃糖浆，被我哥追着打。可他那时候只有这么高。\n\n她比了比腰间，声音越来越轻。\n\n柳小满：暗牢把孩子也变成这样了吗？\n\n你横剑挡住阿照失控的一扑，没有下杀手。\n\n你：先制服他。也许他还记得路。"
            12 -> "旁白：药窟墙上贴满药方，方尾都写着‘无相试药，第七轮’。纸页被潮气泡烂，仍能看见一行行清秀小字。\n\n阿照缩在药柜下，像只受惊的小兽，一遍遍念着同一句话。\n\n阿照：药苦，柳师兄说数到三就甜。\n\n柳小满蹲下，把糖篮放到地上。\n\n柳小满：阿照，我是柳小满。你还记得吗？我小时候抢过你的糖。\n\n阿照抬头，混浊的眼里亮了一瞬。\n\n阿照：小满……长高了。柳师兄说，小满长高，要买新鞋。\n\n柳小满的手指猛地攥紧篮沿。\n\n你低声：他还记得。\n\n柳小满：记得这些有什么用？他连自己疼不疼都不知道了。\n\n你：有用。只要还记得一点，就不是只剩怪物。\n\n阿照忽然把一张药方塞给你，上面写着‘忘名散解法残页’。\n\n阿照：柳师兄藏的。别给药坛主。"
            13 -> "旁白：前方风声忽停，毒窟刀奴拖刀而来。它的步子轻巧得不像刀客，倒像药谷采药人踩过湿苔。\n\n柳小满：你会不会骗我？\n\n你：骗你什么？\n\n柳小满：若我哥哥真成了怪物，你会不会骗我说他还有救？\n\n你没有立刻回答。刀奴的刀光已经逼近，雾里响起细碎药铃声。\n\n你：我不会拿希望骗你。\n\n柳小满垂下眼。\n\n你：但只要还有一口气，我也不会替你提前放弃。\n\n毒窟刀奴忽然开口，声音沙哑。\n\n毒窟刀奴：小满……别来……\n\n柳小满整个人僵住。\n\n阿照从药柜后探出头，害怕得发抖。\n\n阿照：不是柳师兄。是刀奴学他说话。药坛主让它学，说小满听了会停手。\n\n柳小满闭了闭眼，再睁开时眼里有泪，也有火。\n\n柳小满：那就先把这个学舌的打闭嘴。"
            14 -> "旁白：碧毒刀奴守着一口药井。井中不是水，而是一层层被封存的记忆。\n\n井面浮出药谷旧院。柳知秋蹲在灶前熬糖，年幼的柳小满裹着被子，哭着不肯喝药。\n\n小柳小满：苦！\n\n柳知秋残影：那哥哥给你变甜。你数到三。\n\n阿照残影在旁边举着勺子。\n\n小阿照：柳师兄，我也苦。\n\n柳知秋残影：你偷吃糖浆还苦？去洗碗。\n\n现实里的阿照抱着头，像被这些记忆烫到。\n\n阿照：我洗了……我洗了好多碗……后来碗里都是血。\n\n柳小满伸手想碰井面，被你拦住。\n\n你：井在诱你进去。\n\n柳小满：我知道。可这些是真的。\n\n你：真的记忆，也能被拿来害人。\n\n药井深处传来药坛主的笑声。\n\n药坛主：有情者最好入药。舍不得，才熬得久。"
            15 -> "旁白：药坛深处，一尊破佛腹中藏着家书。信纸泛黄，落款没有姓名，只有与你断符相合的半枚印。\n\n母亲留字：吾儿若见此信，愿你平安，不愿你成英雄。\n\n你看了很久，久到柳小满和阿照都没有出声。\n\n柳小满：你娘写字挺好看。\n\n你：我第一次见她的字。\n\n阿照小声：柳师兄也写信。写了好多，后来都烧掉。\n\n柳小满：为什么？\n\n阿照：他说小满看了会来。可他又怕小满不来。\n\n药坛主倒下后，满地碎药瓶滚开，露出一枚木牌。背面只有四个字：小满，回家。\n\n柳小满没有哭，只把木牌挂到糖篮上。\n\n你：要回去吗？\n\n柳小满：回。\n\n你一怔。\n\n柳小满抬头，眼睛湿亮，却笑得很稳。\n\n柳小满：带他一起回。阿照也一起。所以先往前走。"
            16 -> "旁白：离开药窟后，前方出现断剑校场。校场大门斜插着半截断云楼剑旗，旗上血迹早已发黑。\n\n柳小满：这里不像药谷。\n\n你：断云楼，七派里最讲规矩的剑派。\n\n门后传来女子声音，清冷得像剑锋敲冰。\n\n陆青棠：入校场者，报姓名、来意、所负之罪。\n\n柳小满小声：这人谁啊，一上来就查户籍。\n\n你：我们是后来者，追查暗牢旧事。\n\n陆青棠的残魂从断剑阵中现身，衣摆破损，背脊却笔直。\n\n陆青棠：后来者多半只想拿走答案，不想承担答案。\n\n你：那你想我们怎么证明？\n\n陆青棠：先从这些断剑门徒手下活下来。剑不问心软不软，只问手稳不稳。"
            17 -> "旁白：断剑门徒列阵而立，每个人的招式都像偷来的半页秘籍。\n\n柳小满：他们不像人在练武，像有人把不同门派的招式硬塞进身体里。\n\n陆青棠：无相阵在模仿七派。模仿得越像，越说明我们当年错得越深。\n\n你：七派不是为了封阵才进来的吗？\n\n陆青棠沉默片刻，剑尖低了一寸。\n\n陆青棠：一开始不是。剑阁想要剑谱，星宿想要命盘，药谷想要救人，断云楼想要证明自己才配执江湖规矩。每个人都说自己有正当理由。\n\n柳小满：那后来呢？\n\n陆青棠：后来理由都被阵吃了，只剩后果。\n\n断剑门徒齐声低语。\n\n断剑门徒：胜者留名，败者成泥。\n\n陆青棠抬剑。\n\n陆青棠：看清楚。这就是只剩输赢的人。别变成他们。"
            18 -> "旁白：夜行刺客从梁上落下，一枚袖箭直取柳小满咽喉。\n\n你来不及回剑，她却先一步撞开你。袖箭擦过她肩头，黑血立刻渗出。\n\n你：柳小满！\n\n柳小满咬着牙，脸色惨白。\n\n柳小满：小伤。我小时候摔破膝盖，哭得比这大声。\n\n你撕下衣角替她包扎，发现毒性古怪。\n\n你：这毒不是杀人，是逼人吐真话。\n\n柳小满：那你别问我怕不怕。\n\n你：好。\n\n柳小满沉默片刻，忽然开口。\n\n柳小满：我怕。怕找到哥哥，也怕找不到。怕你们都把话藏着，最后只剩我一个人猜。\n\n你手上动作一顿。\n\n你：那我答应你。以后能说的，我尽量说。不能说的，我也告诉你我不能说。\n\n柳小满：这还差不多。"
            19 -> "旁白：水寨刀匪的船桨声从石室深处传来。这里没有河，却有潮。墙上画着龙门水寨旧图，图中船队押送的不是货，而是一批自愿入牢的七派高手。\n\n柳小满：自愿？谁会自愿来这种地方？\n\n你：也许他们知道不来会更糟。\n\n船舱残影里，你看见父母并肩而坐。他们没有回头，只把一盏灯交给年轻的沈砚。\n\n父亲残影：若我们回不来，把灯带出去。\n\n年轻沈砚：我不带。要走一起走。\n\n母亲残影：总要有人让后来者看见门。\n\n柳小满轻声问：你想叫他们吗？\n\n你喉咙发紧。\n\n你：叫了也只是影子。\n\n柳小满：影子也可以听一听吧。\n\n你看着残影远去，终于低声喊了一句。\n\n你：爹，娘。\n\n船桨声停了一瞬，像有人在很远处回头。"
            20 -> "旁白：断剑校场最后一门开启。枯木白衣门主站在门后，手中剑法竟与年轻沈砚如出一辙。\n\n枯木门主：世人只记得英雄赴死，却忘了活下来的人要背多少骂名。\n\n你：所以沈砚背了三十年？\n\n枯木门主：他背的是自己退的那一步。你背的，又会是什么？\n\n柳小满：少吓人。人还没走到那一步，先把背上压满石头，这不公平。\n\n枯木门主看向她。\n\n枯木门主：小姑娘，你想救哥哥，也会害死别人。\n\n柳小满的脸白了一下，却没有退。\n\n柳小满：那我就一边救，一边记着别人的命。记不住就让他提醒我。\n\n她指了指你。\n\n你：我也未必记得全。\n\n柳小满：那就一起记。\n\n门主败落时，校场响起旧誓：若有人后来，勿问我名，先问我心。"
            21 -> "旁白：校场西侧的兵器架上，挂着一排没有主人的剑。每一柄剑下都刻着一句认罪词，有的承认贪图狱典，有的承认逼同门入阵。\n\n柳小满：原来他们自己也知道错。\n\n陆青棠：知道错，不等于立刻能改。很多人是到死前一刻，才肯承认自己错在何处。\n\n你：那你呢？\n\n陆青棠看着断剑，声音依旧平稳。\n\n陆青棠：我错在把规矩看得比人命重。若有人违令救人，我先罚他；若有人按令害人，我却说他尽责。\n\n柳小满小声：这规矩听着挺欠揍。\n\n陆青棠竟没有反驳。\n\n陆青棠：所以我守在这里，等后来者骂醒我，也骂醒这片校场。"
            22 -> "盘丝妖女化作柳小满的模样，在雾里问你要不要带她离开。真正的柳小满站在你身后，声音发抖，却仍骂了一句：学我可以，别把我学得这么没出息。那一刻你看见她眼里不是不怕，而是怕到极处仍不肯松手。你们斩断蛛丝，继续向星宿钩台走去。"
            23 -> "暗廊深处挂着七派掌门画像，每一幅画像下都有一根丝线连向阵心。你割断剑阁画像，便有剑气反噬；割断药谷画像，柳小满便咳出黑血。无相阵把七派记忆织成网，谁想救人，谁就先被救人的执念困住。你第一次感到，侠义也可能成为牢笼。"
            24 -> "机括机关堂主守着一座木人阵。木人胸口嵌着小小铜铃，铃声与沈砚客栈门前那只一模一样。每当铃响，机关便预判你的招式。柳小满忽然把糖篮砸向阵眼，铜铃乱响，木人纷纷停滞。她得意地说：打不过就捣乱，这也是江湖经验。"
            25 -> "柳小满终于哭了一次。她说自己其实早知道哥哥多半回不来，可人活着总得有个要找的人，不然夜路太长。你没有劝她坚强，只把沈砚给你的灯递过去。她抱着灯哭完，又把灯还给你，说：走吧，别让我哥哥白白把我吓回去。"
            26 -> "旁白：龙门旧渡没有天，也没有岸。黑水中停着一艘艘旧船，每艘船头都挂着七派名牌。\n\n柳小满：这里明明在地下，怎么会有水？\n\n船上传来低沉的橹声，一个披蓑衣的老人撑船靠岸。\n\n叶沉舟：后来者，上船。\n\n柳小满：你谁啊？\n\n叶沉舟：龙门旧船主。三十年前，是我把七派送进暗牢。\n\n他顿了顿，声音像被水泡旧的木头。\n\n叶沉舟：也是我，把他们送上了回不了家的路。"
            27 -> "旁白：船离岸后，黑水映出三十年前的渡口。七派弟子沉默登船，有人握剑，有人捧佛珠，有人把药囊藏在怀里。\n\n叶沉舟：他们都说自愿。可自愿这两个字，有时是被大义逼出来的。\n\n柳小满：你既然知道，为什么还撑船？\n\n叶沉舟：因为我也想见《无相狱典》。我想知道能让七派都低头的东西，到底是什么。\n\n你：后来呢？\n\n叶沉舟苦笑。\n\n叶沉舟：后来才知道，那不是秘籍，是镜子。每个人凑过去，都先看见自己最贪的模样。"
            28 -> "星台下藏着沈砚的第二封信。他承认当年自己退了一步，却不是逃走，而是被你母亲一掌推出阵门。‘她说总要有人把灯带出去。可我活下来以后，才知道带着灯活着，比死在里面更难。’信纸被水迹洇开，不知是雨，还是泪。"
            29 -> "龙门水寨的雾从星台边缘涌上来，像江河倒灌。你听见船夫唱旧歌：一船送七派，一船送故人。柳小满说她哥哥小时候也会唱，只是总跑调。你们沿歌声向下，看到无数停泊在黑暗里的船，每一艘都载着未能回家的名字。"
            30 -> "龙门煞罗堂主立在船头，问你要渡何人。你说渡活人，也渡死人。它大笑，说江湖人总爱说大话。可当你击败它，水面真的亮起一条路。柳小满把哥哥木牌放入水中，木牌没有沉，而是顺流飘向更深处。她说：看来他还没走完，我们也还不能停。"
            31 -> "金身佛堂的门开在水雾尽头。庙中没有香火，只有一尊被铁链缠住的佛。明王护法立在佛前，问你见过多少人因侠义而死。你想起父母、七派、柳知秋，还有沈砚守了三十年的灯。柳小满低声说：死的人已经够多了，我们往前走，是为了让后面少死几个。"
            32 -> "佛堂偏殿里供着七盏长明灯，每盏灯旁都有一卷忏悔书。剑阁写‘未能护住弟子’，药谷写‘以药试阵，误伤同门’，龙门写‘押送活人入牢，从此不敢听水声’。你翻到最后一卷，纸上没有署名，只写：若后来者读到这里，请别急着原谅我们。"
            33 -> "明王护法的金身裂开，里面不是血肉，而是一层层旧誓。每一层誓言都在问你：若救一人会害十人，你救不救？柳小满说她不懂天下大账，她只知道一个人被丢下时，也会害怕。你忽然发现，暗牢最想逼你舍弃的，正是这种看似不够大义的心。"
            34 -> "一枚完整牢符嵌在石台中央，符背刻着你的姓氏。你终于知道，父母不是死于江湖仇杀，而是死在封住无相阵的那一夜。佛堂钟声响起，钟里传来母亲的声音：孩子，若你是男儿，不必逞强；若你是女儿，也不必证明给谁看。你只要记得，拿剑不是为了成为别人期待的英雄。"
            35 -> "金身佛堂尽头，明王金身罗汉拦住去路。它说沈砚当年也曾站在这里，哭着求佛让他替你父母去死。佛没有答应，因为活下来的人才最难受。你击败罗汉后，佛像掌心落下一枚铜钱，柳小满捡起来，说这钱不多，但可以买两串糖。你们都笑了，笑声在空佛堂里显得格外轻。"
            36 -> "风雷崖的第一道雷落下时，石壁上浮出血书。血书不是写给你，而是写给沈砚：若你能出去，别替我们立碑。江湖若知道我们做过什么，只会把罪推给死人。你看完才明白，七派当年并不全是清白，他们也曾争抢无相狱典，只是在最后一刻选择把错误封住。"
            37 -> "黑袍风雷长老坐在崖边，像等了你很久。它说无相阵不会凭空造恶，只会把人心里已有的欲望放大。想称霸的人变成魔，想赎罪的人变成牢，想救人的人也会被救人的念头拖死。柳小满问它：那想吃糖呢？长老沉默片刻，说：那大概还能算活人。"
            38 -> "崖下风声像千万封信同时被撕开。你在碎纸中看见父亲笔迹：吾儿生来不欠江湖。下一页却是母亲写的：可若有一日江湖欠了许多人，总要有人把账算清。两句话并排放着，像两个人争了一辈子，最后仍一同走进暗牢。你把信收好，没有替他们分出对错。"
            39 -> "雷雨中出现一座旧亭。亭里坐着年轻的沈砚，他还没变成掌柜，眼里全是锋芒。他请你喝酒，说江湖最可怕的不是恶人，是自以为不会错的好人。你问他后来为何退。幻影举杯的手停在半空，酒水落地成血。他说：因为我终于发现，我也会错。"
            40 -> "风雷崖守卫被击败后，崖壁裂出一条缝。缝中藏着第三封信，来自柳知秋。他写给妹妹：小满，如果我回不来，别来找我。可信尾又多了一行被反复划掉的话：如果你真的来了，记得带糖，我怕苦。柳小满看着那行字，眼睛红了，却骂哥哥还是这么没出息。"
            41 -> "寒霜宫道比想象中安静。雪落在石阶上，没有声音。你每走一步，身后的脚印都会被冰封，像暗牢不允许人回头。柳小满裹紧衣袖，说她小时候最怕冷，哥哥就把药炉搬到她床边。如今这里冷得像把所有药炉都熄灭了。你们决定尽快找到寒霜宫主留下的阵钥。"
            42 -> "冰魄寒霜宫主的影子出现在镜湖中。她曾是七派中唯一反对入牢的人，因为她不信人能长期守住一个秘密。后来她还是来了，只因你母亲问她：若我们不去，那些不知道真相的人怎么办？镜湖碎裂时，宫主影子看着你，说你母亲很会骗人，骗得我们都成了英雄。"
            43 -> "宫道两侧冻着许多兵器，有剑、刀、杖，也有一只小拨浪鼓。柳小满认出那是哥哥给她买过的玩具样式。她敲了敲冰面，里面传来极轻的一声响，像有人在回应。你没有说那可能只是冰裂，因为她此刻需要相信。暗牢残忍的地方，正是连希望也会被拿来试探。"
            44 -> "寒霜宫深处有一间暖阁，阁中烧着不灭炭火。沈砚真正的声音从火里传来：若你走到这里，应当已经知道我不值得信。可我仍要请你继续走，因为你父母封住的是阵，不是人心。人心一日不醒，暗牢就一日不会真正关闭。"
            45 -> "沈砚跪在旧阵前。他说自己当年怕死，退了一步。那一步让你父母永远留在阵里，也让他守了三十年雨夜客栈。柳小满想骂他，却又骂不出口。你看见这个老人额头抵着冰冷石面，忽然明白赎罪不是一句对不起，而是把余生都放在同一个错误前，等一个可能永远不会来的审判。"
            46 -> "阎罗判堂的门上写着四个字：来者自辩。门内坐着无数戴面具的判官，它们不问你杀过多少敌，只问你放弃过谁。你想起每次选择无视的岔路、每个来不及救的人、每个被你当成经验和掉落的敌人。柳小满握住你的袖子，说别被它吓住，我们不是神仙，能记得疼就够了。"
            47 -> "索命判官翻开生死簿，簿上有你的名字，也有柳小满的名字。它说你们继续走下去，总有一个人会留在暗牢。柳小满伸手把自己那页撕掉，塞进嘴里嚼了两下，苦得皱眉。她说：这样它就念不出来了。你知道这很幼稚，却仍因这份幼稚生出一点勇气。"
            48 -> "判堂偏室里关着许多‘还差一步’的人。他们差一步救回亲人，差一步报仇，差一步成为天下第一，于是被无相阵困在永远差一步的地方。你看见一个人影与自己极像，正伸手去拿一本黑色秘籍。柳小满挡在你前面，说别看，那不是你，是它想让你变成的样子。"
            49 -> "阎罗判堂的钟响三声，柳知秋终于出现。他穿着药谷旧衣，神情温和，手里却握着染血短刀。他看见柳小满，先是茫然，随后像记起什么，轻声问：小满，糖还够吃吗？柳小满笑着说够，眼泪却一滴滴砸在地上。你知道这一战无论胜负，都不会轻松。"
            50 -> "柳知秋倒下时，没有立刻消散。他把一颗糖放进妹妹掌心，说自己其实一直知道她会来，所以才在每一层留下线索，又在每一层布下危险。他想让她回头，又怕她真的回头。柳小满终于哭出声，哭完后把糖分你一半，说哥哥找到了，接下来该找你的家人了。"
            51 -> "血月刀冢铺满断刃，每一把刀都映着一个遗憾。你看见父亲提刀站在血月下，身后是被无相阵吞没的七派弟子。他没有豪言壮语，只一遍遍确认阵门是否合拢。原来英雄赴死时，也会怕自己死得不够有用。这个念头让他不像传说，更像一个真正的人。"
            52 -> "血煞疯魔刀圣从刀冢中拔出一把无柄刀。它说你父亲当年用同样的刀斩断阵桥，也斩断了回家的路。若你愿意接过这把刀，便能获得足以号令江湖的力量。柳小满在旁边小声说：听起来像骗子。你点头，刀圣大怒，血月随之压低。"
            53 -> "刀冢深处有一座无名坟，坟前插着半截木簪。你认出那是母亲画像上戴过的样式。坟中没有尸骨，只有一段留音：孩子，若有人告诉你必须恨谁才能往前走，别信。恨能让人站起来，却不能让人走太远。你把木簪收起，觉得掌心像握着一场迟来的拥抱。"
            54 -> "血月下，七派亡魂围成一圈。他们承认自己曾贪图狱典，也承认最后选择封阵并不能洗清前错。可他们仍希望你继续，因为江湖不能只由无错的人来救。柳小满听完，说那就麻烦你们下次早点承认错误。亡魂们沉默，随后竟有人笑了。"
            55 -> "暗牢深处传来钟声。七大门派失踪的真相，只差最后一扇门。柳小满把糖篮交给你，说：若我没出来，替我把糖人卖完。你没有接，只把篮子推回去，说要卖就自己出去卖。她愣了愣，忽然笑得很用力，说好，那你也得活着给我当第一个客人。"
            56 -> "无相狱心的外门像一只闭合的眼。门前站着沈砚、柳知秋、你父母以及无数熟悉的影子。他们同时开口，劝你停下。每个人说的话都合情合理：你已经够累了，真相已经够多了，江湖不值得。你闭上眼，听见真正的柳小满在身边说：别听影子说话，听活人喘气。"
            57 -> "狱心第一重幻境，是一座没有暗牢的江湖。父母在客栈等你，沈砚仍是少年侠客，柳小满和哥哥卖糖。这里没有失踪，没有血书，没有选择。你几乎想停下，可糖摊边的柳小满忽然看向你，说：假的也很好，可我哥哥不会这么甜地笑，他熬糖时总偷懒。幻境裂开一线。"
            58 -> "第二重幻境，是你成为天下第一。所有门派向你俯首，所有仇人被你击败，所有遗憾都被写成传奇。可你低头看见脚下没有影子。无相阵说，只要舍弃那些拖累你的名字，你就能永远站在最高处。你想起一路走来的哭声和笑声，忽然觉得没有影子的最高处，不过是另一座牢。"
            59 -> "第三重幻境，是审判。七派责问你为何来得太晚，父母责问你为何不够强，沈砚责问你为何不能原谅。你几乎被这些声音压垮。柳小满忽然把糖篮扣在你头上，大喊：都闭嘴！她说活人不是用来满足死人期待的。你隔着糖篮笑出声，审判声也随之碎裂。"
            60 -> "无相狱心终于露出真正形貌：它不是书，不是阵眼，而是一面会映照欲望的黑镜。镜中站着另一个你，衣袍无风自扬，眼神冷得像已看透天下。它说只要接受它，父母可归，七派可复，柳知秋可活。柳小满握紧小刀，低声问你：如果代价是你不再是你呢？你没有回答，只拔出了剑。"
            61 -> "黑镜碎出第一道裂痕，裂缝里涌出熟悉的客栈雨声。沈砚端着一碗热汤站在门内，像你初醒那夜一样疲惫。他说这一切都可以重来，只要你点头，他会在旧牢门前拦住你。柳小满没有说话，只把那半颗糖塞进你手里。糖早已化得发黏，却比幻境里的热汤更像真实。"
            62 -> "狱心第二门后，是七派议事堂。七位掌门争得面红耳赤，每个人都说自己是为了江湖。你站在旁边，看见他们眼底的贪念、恐惧和不甘，也看见他们最后按下血印时的颤抖。无相阵想让你厌恶他们，可你只觉得沉重：人不是因为从不犯错才值得救，而是因为犯错后仍可能回头。"
            63 -> "柳知秋的残影在药炉前熬糖。他说自己被阵吞掉后，最怕忘记妹妹，于是每天在心里重复她的名字。后来名字真的没有忘，可他忘了为什么要记。柳小满蹲在炉边听完，轻声说哥哥笨死了。残影笑着点头，像终于等到这句熟悉的骂。"
            64 -> "你在狱心深处见到母亲。她没有像传说中的侠女那样飘然，只挽着袖子，正替一个受伤弟子包扎。她抬头看你，说孩子，别把我们想得太远。我们当年也会怕，也会后悔，也会在夜里想你有没有好好吃饭。你喉咙发紧，却只能向前，因为她身后的门正在缓缓关闭。"
            65 -> "父亲守在关闭的门前。他问你一路走来，是否恨过他们把你留在世上独自长大。这个问题比任何敌人都难挡。你沉默许久，最后说恨过，但也想过，若换成自己，也许仍会走同一条路。父亲笑了，笑得像一个终于卸下刀的人。他让开一步，把阵心钥交到你掌中。"
            66 -> "阵心钥插入石门，门后却不是终点，而是一条回到第一层的长廊。每隔几步，墙上便刻着你曾经击败的敌名：黑寨喽啰、化血药人、白衣剑客、龙门堂主……它们不再只是怪物，而是被阵吞没的人。柳小满低声念着这些名字，像替他们做一场迟来的超度。"
            67 -> "白虹星宿护法再次出现，却没有出手。它说星盘已乱，因为你每一次没有选择最有利的道路，都会让命数偏离。你问这算好事还是坏事。护法说对星盘是坏事，对人也许是好事。它递给你一枚断钩，说天钩护法曾用它救过一个孩子，只是后来没人记得。"
            68 -> "盘丝暗廊的蛛丝从后方追来，缠住柳小满的脚腕。幻境里传来柳知秋的声音，温柔地叫她回家。她差一点停下，却忽然把糖篮砸向蛛丝，大喊：我自己会回家，不用你们送！你挥剑斩断蛛丝，她踉跄着站稳，脸色苍白，却仍冲你笑。"
            69 -> "血月刀冢的刀鸣再次响起，父亲那把无柄刀落到你面前。无相阵说不用它，你赢不了最后一战。你看着刀身映出的自己，发现那张脸越来越像传说中的英雄，却越来越不像一路会犹豫、会害怕、会被柳小满气笑的自己。你把刀踢开，拔出自己的兵器。"
            70 -> "七派残魂在刀光里让路。他们不求你替他们复仇，只求你出去后告诉江湖：当年有人真的为了天下，心甘情愿走进黑暗。柳小满问他们要不要留下些什么。一个残魂想了很久，说若可以，请告诉外面的人，别把我们写得太好。我们没那么好，但最后那一步，是真的。"
            71 -> "无相狱心开始坍塌，所有走过的区域同时浮现。青石旧牢的灯、白骨渡口的船、药窟的糖香、佛堂的铜钱、风雷崖的血书，都像潮水一样涌来。你几乎分不清自己身在何处。柳小满抓住你的手，说别看路，看我。于是你跟着她，一步步踩过破碎的记忆。"
            72 -> "沈砚的真身终于出现在阵心之外。他比幻影更老，手里仍提着那盏灯。他说自己不能进去，因为阵还记得他的恐惧。你把灯还给他，说那就站在门口，别再退。沈砚怔住，随后慢慢挺直腰背。三十年来，他第一次不再像一个等判决的人。"
            73 -> "阵心黑镜放出最后的诱惑：它让柳小满看见哥哥真的回来了，让沈砚看见你父母原谅了他，让你看见一家人坐在雨后客栈里吃饭。每个人都知道这是假的，却都舍不得眨眼。最后柳小满先转身，声音哑得厉害：假的饭不会饱，走。"
            74 -> "黑镜怒了。它不再伪装成亲人，而是化作无数张江湖人的脸，指责你毁掉狱典就是毁掉变强的机会。有人说弱者需要奇迹，有人说乱世需要绝顶高手。你听着这些声音，忽然明白无相阵最会利用的不是恶，而是每个人心里那句‘我也是不得已’。"
            75 -> "你们被逼到断桥边。桥下是无数尚未成形的影子，它们都是曾经想借狱典改变命运的人。柳小满问，如果他们只是想活得好一点呢？你说那就更不能把他们交给这面镜子。真正让人活下去的，不该是一部吞掉人心的秘籍，而是有人愿意在他们掉下去前伸手。"
            76 -> "战斗之后，沈砚被黑镜反噬，胸口浮出旧年伤痕。他却笑了，说这一剑来得太晚，也来得正好。他把灯交给柳小满，让她出去后挂在客栈门前。柳小满问他自己怎么不挂。沈砚看向你，轻声说：总有人要留下关门。"
            77 -> "你不同意沈砚留下。三十年的赎罪已经够久，若这扇门需要人关，就一起关。沈砚想骂你莽撞，却忽然笑出眼泪，说你母亲当年也是这么不讲理。柳小满把灯举高，灯火照到黑镜上，镜面第一次显出裂开的阵纹。"
            78 -> "阵纹需要七派信物才能彻底打开。你们一路收集的断钩、木牌、铜钱、血书、木簪、断刀碎片和长明灯芯，此刻一一亮起。每一件都不是什么神兵，却都承载着一个人的后悔或牵挂。无相阵无法理解这种力量，因为它只懂欲望，不懂记挂。"
            79 -> "黑镜中的另一个你走了出来。它比你更强，更冷静，也更像故事里该有的主角。它说你一路靠同情拖慢脚步，靠犹豫错失机会，靠别人提醒才没迷路。你没有反驳，因为它说得不全错。柳小满却说：那又怎样？会迷路的人，才知道别人找不到路时有多急。"
            80 -> "另一个你拔剑时，所有曾经的敌人影子都随之复苏。黑寨、药谷、白骨、星宿、龙门、佛堂、风雷、寒霜、阎罗、血月……它们像一部被压缩到极致的江湖史，朝你们倾倒而来。你终于明白最后一战不是打赢某个人，而是承认这一路所有疼痛都曾真实发生。"
            81 -> "柳小满受了伤，仍要站起来。她说自己不是为了证明勇敢，只是哥哥在看，不能太丢人。沈砚替她挡下一击，骂她胡闹，又骂你为什么不拦着。你们三个人在崩塌的阵心里吵得乱七八糟，黑镜却因此出现更多裂纹。原来活人最不像棋子的时候，就是还会吵架。"
            82 -> "你父母的残魂终于现身。他们没有替你出手，只站在远处看着。母亲说你已经长大，不该再由他们决定你的路。父亲说若你想退，现在也可以。你看向柳小满和沈砚，看向一路留下的名字，忽然发现自己不再是被推入局中的孩子。你是自己选择站在这里的人。"
            83 -> "黑镜试图复制你的选择，却复制不出柳小满的那半颗糖，也复制不出沈砚三十年的灯，更复制不出父母明明害怕仍要往前走的背影。它复制力量、野心、悔恨，却无法复制那些琐碎到近乎无用的牵挂。你抓住这个破绽，一剑刺入镜心。"
            84 -> "镜心破裂，整个暗牢开始上浮。远处传来雨声，不再像幻觉，而像真正的人间。柳小满看见哥哥的残影站在桥边，向她挥手。她没有追过去，只也挥了挥手。你知道这一次，她是真的在告别。不是放弃寻找，而是终于找到之后，愿意继续活下去。"
            85 -> "沈砚跪在你父母面前，像等最后一句判词。父亲没有说原谅，只把当年那盏灯推回他手里。母亲说，灯既然带出去了，就继续照人。沈砚怔了很久，终于伏地痛哭。你没有打扰他。有些结不是解开，而是承认它会一直痛，然后仍把灯点着。"
            86 -> "出口前，无相狱典化作一本真正的书，安静躺在石台上。只要带出去，你就能获得天下梦寐以求的力量。柳小满没有催你，沈砚也没有。你翻开第一页，里面没有武功，只有无数人的名字。原来所谓狱典，记录的从来不是招式，而是人如何被欲望困住。"
            87 -> "你合上狱典，决定毁掉它。黑火燃起时，每个名字都化作一粒微光飞向暗牢穹顶。有人哭，有人笑，有人茫然，有人终于想起自己是谁。柳小满把哥哥的木牌也放进火里，说别再给我留路标了，我认识回家的路。木牌燃尽时，她没有再哭。"
            88 -> "暗牢最后一扇门打开，门外是你最初见过的雨夜客栈。只是这一次，天快亮了。沈砚把灯挂回门前，柳小满在檐下摆出糖篮，问你要不要买第一串。你摸了摸身上的银两，忽然想起一路奖励攒下的钱终于有了最正经的用途。你买下一串糖，甜得发苦。"
            89 -> "你终于明白，暗牢不是囚牢，而是一座筛选江湖继承者的旧阵。它筛的不是武功高低，而是人在最痛时，还愿不愿意护住别人。江湖仍会有雨夜、贪念、仇杀和错事，但也会有灯、糖、迟来的道歉和愿意伸出的手。你不必成为无错的英雄，只要记得自己为何拔剑。"
            90 -> "数日后，客栈重新开张。有人问暗牢里到底有什么，你说有一段很长的故事，长到一时讲不完。柳小满在旁边补充：还有很难吃的苦糖。沈砚擦着桌子，没有反驳。门外江湖辽阔，新的传闻已经开始。你把兵器放在手边，知道故事没有结束，只是终于从黑暗里，走到了人间。"
            91 -> "旁白：客栈门外忽然又落雨。雨声里，一页未烧尽的狱典残纸贴在灯笼下，纸上没有武功，只有一个新生的名字。\n\n柳小满：它还没死透？\n\n沈砚：无相阵最难灭的不是阵，是人心里总想重来的念头。\n\n你：那就再走一趟。\n\n无戒和尚的声音从灯火里传来，仍旧不客气。\n\n无戒和尚：走什么走，先把饭吃了。饿着肚子救天下，显得天下很没面子。\n\n柳小满噗嗤一笑，把糖篮往你怀里一塞。\n\n柳小满：吃完再打。活人赶路，也得像活人。"
            92 -> "旁白：暗牢残门重新浮现，却不再通向地下，而像通向每个人心底最舍不得放下的角落。\n\n秦红袖立在门边，红袖被雨打湿，星钩却亮得刺眼。\n\n秦红袖：命盘乱得很。你毁了狱典，反倒放出了那些尚未散尽的执念。\n\n你：还有多少？\n\n秦红袖：不是多少的问题。是你愿不愿意承认，毁掉一本书，并不等于所有人都能立刻放下。\n\n柳小满：说人话。\n\n秦红袖笑了笑。\n\n秦红袖：有人还在等你们告别。"
            93 -> "旁白：第一处残境是药谷。桂花蜜香很淡，淡得像一场快醒的梦。\n\n阿照坐在药炉旁，手里捧着空碗，终于像个真正的孩子。\n\n阿照：小满姐姐，药已经不苦了。\n\n柳小满蹲下，想摸他的头，手却穿过残影。\n\n柳小满：那就好。以后别偷糖浆了。\n\n阿照认真点头，又偷偷看向柳知秋残影。\n\n阿照：柳师兄说，可以偷一点点。\n\n柳小满笑着红了眼。\n\n你没有催她。告别这种事，不能替别人快。"
            94 -> "旁白：第二处残境是断剑校场。陆青棠站在剑阵中央，脚下断剑一柄柄化成微光。\n\n陆青棠：后来者，你已经知道七派有错。现在还愿意把他们的名字带出去吗？\n\n你：会带出去。但不会只说他们是英雄。\n\n陆青棠：很好。\n\n柳小满：你不怕江湖骂他们？\n\n陆青棠垂眸，看着自己透明的手。\n\n陆青棠：怕。可若只留下漂亮传说，后来的人还会犯同样的错。规矩若不能照见污点，就只是另一种遮羞布。\n\n她把断剑递给你，剑身无锋，却很沉。"
            95 -> "旁白：第三处残境是龙门旧渡。叶沉舟撑着一条破船，船头挂满未寄出的名牌。\n\n叶沉舟：当年我押他们入牢，人人都说我是帮凶。其实他们说得没错。\n\n你：你后悔吗？\n\n叶沉舟：后悔没有问清每个人是不是真的愿意，也后悔只听见大义，没听见他们家里还有人在等。\n\n柳小满把一枚糖放到船沿。\n\n柳小满：那你帮我带给我哥。\n\n叶沉舟笑了。\n\n叶沉舟：这船终于能载点甜的东西了。"
            96 -> "旁白：第四处残境是机关堂。唐照夜坐在重铸炉边，半边身体是机关，半边魂影将散未散。\n\n唐照夜：你们拿过宝匣，换过兵器，也见过同一件装备生出不同品质。现在知道为什么了吗？\n\n你：因为人心会把器物锻成不同模样。\n\n唐照夜：说得像个先生。其实简单些，就是贪的人锻出贪，护人的人锻出护。\n\n柳小满：那糖篮呢？\n\n唐照夜端详片刻。\n\n唐照夜：这东西没什么品级。\n\n柳小满刚要生气，他又补了一句。\n\n唐照夜：所以它最不容易被阵骗走。"
            97 -> "旁白：第五处残境是风雷崖。顾长风的残影从雷声中奔来，怀里仍护着那封送迟三十年的血书。\n\n顾长风：我当年若跑快些，也许你父母能多留一句话。\n\n你：你已经把信送到了。\n\n顾长风摇头。\n\n顾长风：送信的人总觉得，只要晚一步，就像害死了所有等信的人。\n\n沈砚低声道：我懂。\n\n顾长风看向他，忽然笑了。\n\n顾长风：那就替我继续送。把真相送出去，不要只送给愿意听的人。\n\n雷声落下，血书终于不再滴血。"
            98 -> "旁白：第六处残境是阎罗判堂。判官面具碎了一地，柳知秋站在堂前，手中没有刀，只有一颗糖。\n\n柳知秋：小满，我想了很久，还是觉得自己这个哥哥做得不太好。\n\n柳小满：何止不太好。你留那么多吓人的线索，害我一路腿软。\n\n柳知秋笑着点头。\n\n柳知秋：那以后别找我了。\n\n柳小满沉默很久，终于说：好。\n\n她把木牌放在判桌上。\n\n柳小满：你也别再等我了。我要回去卖糖，卖得很贵，贵到你买不起。\n\n柳知秋笑出了泪，身影随糖香散去。"
            99 -> "旁白：最后一处残境没有名字，只有一间雨后小屋。你的父母坐在桌边，桌上饭菜还热。\n\n母亲：这一次，不是阵骗你。只是我们也想好好看你一眼。\n\n父亲：长高了，也瘦了。江湖饭不好吃吧？\n\n你坐下，忽然不知道该说什么。\n\n柳小满和沈砚没有进屋，只在门外等。\n\n母亲：你不必替我们完成什么。\n\n父亲：也不必替我们原谅谁。\n\n母亲：往后拔剑前，先想想自己饿不饿、累不累、怕不怕。会怕的人，才知道别人的命不是数字。\n\n饭菜香气渐淡。你起身时，终于觉得那句告别没有堵在喉咙里。"
            100 -> "旁白：残境散尽，暗牢真正沉寂。天光从客栈檐角落下，照在沈砚的灯、柳小满的糖篮和你掌心的断符上。\n\n沈砚：这江湖以后还会乱。\n\n柳小满：乱就乱吧。反正客栈开着，糖也还卖。\n\n你：若再有人被困住，就再去找。\n\n门外有人牵马经过，问这里是不是能打听暗牢的故事。\n\n柳小满抢先答：能，但要买糖。\n\n沈砚摇头笑了，灯火在晨风里稳稳亮着。\n\n旁白：你不再只是追查身世的后来者。你记得父母、七派、沈砚、柳知秋，也记得每一个曾被当成怪物的名字。江湖仍不完美，可它真实地活着。而你，愿意继续走在其中。"
            else -> null
        }

    fun dismissStoryDialogue() {
        _storyDialogue.value = null
    }

    // ===== Enemy Generation =====
    fun generateEnemy(condition: String = "") {
        val r=_realm.value; val arch=EnemyArchetype.entries.random()
        val maxLvl=r.floor*r.enemyLevelGap+(r.enemyBaseLevel-1); val minLvl=maxOf(1,maxLvl-(r.enemyLevelGap-1))
        val lvl=when(condition){"guardian"->minLvl;"sboss"->maxLvl;else->Random.nextInt(minLvl,maxLvl+1)}.coerceAtMost(100)

        val (name,hpB,atkB,defB,spdB,crB,cdB) = when(arch) {
            EnemyArchetype.OFFENSIVE->{
                val nms=when(condition){"guardian"->EnemyNames.OFFENSIVE_GUARDIAN;"sboss"->EnemyNames.OFFENSIVE_BOSS;else->EnemyNames.OFFENSIVE_NORMAL}
                val n=if(condition=="chest")EnemyNames.MIMIC_CHEST else if(condition=="door")EnemyNames.MIMIC_DOOR else nms.random()
                Quintuple(n,Random.nextInt(300,371),Random.nextInt(70,101),Random.nextInt(20,51),Random.nextFloat()*0.2f+0.2f,Random.nextFloat()*3f+1f,Random.nextFloat()*1f+6.5f)
            }
            EnemyArchetype.DEFENSIVE->{
                val nms=when(condition){"guardian"->EnemyNames.DEFENSIVE_GUARDIAN;"sboss"->EnemyNames.DEFENSIVE_BOSS;else->EnemyNames.DEFENSIVE_NORMAL}
                val n=if(condition=="chest")EnemyNames.MIMIC_CHEST else if(condition=="door")EnemyNames.MIMIC_DOOR else nms.random()
                Quintuple(n,Random.nextInt(400,501),Random.nextInt(40,71),Random.nextInt(40,71),Random.nextFloat()*0.2f+0.1f,0f,0f)
            }
            EnemyArchetype.BALANCED->{
                val nms=when(condition){"guardian"->EnemyNames.BALANCED_GUARDIAN;"sboss"->EnemyNames.BALANCED_BOSS;else->EnemyNames.BALANCED_NORMAL}
                val n=if(condition=="chest")EnemyNames.MIMIC_CHEST else if(condition=="door")EnemyNames.MIMIC_DOOR else nms.random()
                Quintuple(n,Random.nextInt(320,421),Random.nextInt(50,81),Random.nextInt(30,61),Random.nextFloat()*0.2f+0.15f,Random.nextFloat()*1f+0.5f,Random.nextFloat()*2f+1f)
            }
            EnemyArchetype.QUICK->{
                val nms=when(condition){"guardian"->EnemyNames.QUICK_GUARDIAN;"sboss"->EnemyNames.QUICK_BOSS;else->EnemyNames.QUICK_NORMAL}
                val n=if(condition=="chest")EnemyNames.MIMIC_CHEST else if(condition=="door")EnemyNames.MIMIC_DOOR else nms.random()
                Quintuple(n,Random.nextInt(300,371),Random.nextInt(50,81),Random.nextInt(30,61),Random.nextFloat()*0.1f+0.35f,Random.nextFloat()*3f+1f,Random.nextFloat()*3f+3f)
            }
            EnemyArchetype.LETHAL->{
                val nms=when(condition){"guardian"->EnemyNames.LETHAL_GUARDIAN;"sboss"->EnemyNames.LETHAL_BOSS;else->EnemyNames.LETHAL_NORMAL}
                val n=if(condition=="chest")EnemyNames.MIMIC_CHEST else if(condition=="door")EnemyNames.MIMIC_DOOR else nms.random()
                Quintuple(n,Random.nextInt(300,371),Random.nextInt(70,101),Random.nextInt(20,51),Random.nextFloat()*0.2f+0.15f,Random.nextFloat()*4f+4f,Random.nextFloat()*3f+6f)
            }
        }

        val scaling=r.enemyScaling
        var eHp=(hpB+(hpB*((scaling-1)*lvl)).toInt()).toInt()
        var eAtk=(atkB+(atkB*((scaling-1)*lvl)).toInt()).toInt()
        var eDef=(defB+(defB*((scaling-1)*lvl)).toInt()).toInt()
        var eSpd=0.4f+0.4f*(((scaling-1)/4)*lvl)
        var eCr=crB+crB*(((scaling-1)/4)*lvl)
        var eCd=50f+50f*(((scaling-1)/4)*lvl)

        if(condition=="guardian"){eHp=(eHp*1.5f).toInt();eAtk=(eAtk*1.3f).toInt();eDef=(eDef*1.3f).toInt();eCr*=1.1f;eCd*=1.2f}
        if(condition=="sboss"){eHp=(eHp*6f).toInt();eAtk=(eAtk*2f).toInt();eDef=(eDef*2f).toInt();eCr*=1.1f;eCd*=1.3f}

        val floorMult=maxOf(1f,r.floor/3f)
        eHp=(eHp*floorMult).toInt(); eAtk=(eAtk*floorMult).toInt(); eDef=(eDef*floorMult).toInt()
        if(eSpd>2.5f)eSpd=2.5f

        val statSum=listOf(eHp.toFloat(),eAtk.toFloat(),eDef.toFloat(),eSpd*10f,eCr*2f,eCd*2f)
        var expCalc=statSum.sum()/20; expCalc=(expCalc+expCalc*(lvl*0.1f))
        if(expCalc>1000000f)expCalc=1000000f*(0.9f+Random.nextFloat()*0.2f)

        val gold=(expCalc*(0.9f+Random.nextFloat()*0.2f)*1.5f).toInt()
        val drop=Random.nextInt(1,4)==1

        _currentEnemySprite.value=spriteMap[name]?: "slime"
        val p=_player.value
        val battleType = when (condition) {
            "guardian" -> "guardian"
            "sboss" -> "sboss"
            else -> "normal"
        }
        _combatState.value=CombatState(battleType=battleType,enemyName=name,enemyLvl=lvl,enemyArchetype=arch.name,enemyHp=eHp,enemyHpMax=eHp,enemyAtk=eAtk,enemyDef=eDef,enemyAtkSpd=eSpd,enemyCritRate=eCr,enemyCritDmg=eCd,playerHp=p.stats.hp,playerHpMax=p.stats.hpMax,playerAtk=p.stats.atk,playerDef=p.stats.def,playerAtkSpd=p.stats.atkSpd,playerVamp=p.stats.vamp,playerCritRate=p.stats.critRate,playerCritDmg=p.stats.critDmg,expReward=expCalc.toInt(),goldReward=gold,hasDrop=drop)
    }

    private data class Quintuple(val a:String,val b:Int,val c:Int,val d:Int,val e:Float,val f:Float,val g:Float)

    // ===== Combat =====
    fun startCombat(bgmType:String){
        _eventPrompt.value = null
        val p=_player.value
        _combatState.value = _combatState.value?.copy(combatId = nextCombatId++)
        _player.value = p.copy(inCombat = true)
        soundManager.playBgm(context, selectCombatBgm(bgmType))
        soundManager.playSfx("enemy_appears")
        _realm.value = _realm.value.copy(isEventActive = true)
    }

    private fun selectCombatBgm(bgmType: String): String {
        if (bgmType == "boss") return "duel_boss"
        if (bgmType == "guardian") return "duel_guardian"
        val cs = _combatState.value ?: return "duel_balanced"
        if (cs.enemyName == EnemyNames.MIMIC_CHEST || cs.enemyName == EnemyNames.MIMIC_DOOR) return "duel_trap"
        return when (cs.enemyArchetype) {
            EnemyArchetype.OFFENSIVE.name -> "duel_offensive"
            EnemyArchetype.DEFENSIVE.name -> "duel_defensive"
            EnemyArchetype.QUICK.name -> "duel_quick"
            EnemyArchetype.LETHAL.name -> "duel_lethal"
            else -> "duel_balanced"
        }
    }

    fun playerAttack():Boolean {
        val cs=_combatState.value?:return false; val p=_player.value; if(!p.inCombat)return false
        soundManager.playSfx("sword_slash")
        var dmg=cs.playerAtk*(cs.playerAtk.toFloat()/(cs.playerAtk+cs.enemyDef)); dmg*=(0.9f+Random.nextFloat()*0.2f)
        val isCrit=Random.nextFloat()*100<cs.playerCritRate; val dType=if(isCrit){dmg=(dmg*(1f+cs.playerCritDmg/100f)).toInt().toFloat();"暴击"} else {dmg.toInt().toFloat();"伤害"}
        var kind = if (isCrit) "crit" else "normal"
        if(p.skills.contains("REMNANT_EDGE")){dmg+=(cs.enemyHp*0.08f).toInt(); kind = "skill"}
        if(p.skills.contains("TITAN_WILL")){dmg+=(cs.playerHpMax*0.05f).toInt(); kind = "skill"}
        if(p.skills.contains("DEVASTATOR")){dmg=(dmg*1.3f).toInt().toFloat(); kind = "heavy"}
        if (isCrit) kind = "crit"
        if(p.skills.contains("RAMPAGER")){p.baseStats.atk+=5;p.tempStats.atk+=5f;calculateStats()}
        if(p.skills.contains("BLADE_DANCE")){p.baseStats.atkSpd+=0.01f;p.tempStats.atkSpd+=0.01f;calculateStats()}
        val ls=(dmg*cs.playerVamp/100f).toInt()
        cs.enemyHp=maxOf(0,cs.enemyHp-dmg.toInt()); cs.playerHp=minOf(cs.playerHpMax,cs.playerHp+ls)
        val cm=if(isCrit)"【暴击！】" else ""
        addCombatLog("${p.name}对${cs.enemyName}造成${dmg.toInt()}点${dType} $cm")
        _dmgNumbers.value=(_dmgNumbers.value+DmgNumber(nextDamageNumberId++, if (isCrit) "-${dmg.toInt()}!" else "-${dmg.toInt()}", isCrit, "enemy", kind)).takeLast(10); _enemyFlinch.value=true
        _combatState.value=cs.copy(); hpValidation()
        return cs.enemyHp>0&&cs.playerHp>0
    }

    fun enemyAttack():Boolean {
        val cs=_combatState.value?:return false; val p=_player.value; if(!p.inCombat)return false
        soundManager.playSfx("sword_slash")
        var dmg=cs.enemyAtk*(cs.enemyAtk.toFloat()/(cs.enemyAtk+cs.playerDef)); dmg*=(0.9f+Random.nextFloat()*0.2f)
        val isCrit = Random.nextFloat()*100<cs.enemyCritRate
        var kind = if (isCrit) "crit" else "taken"
        if(isCrit)dmg=(dmg*(1f+cs.enemyCritDmg/100f)).toInt().toFloat() else dmg=dmg.toInt().toFloat()
        if (cs.enemyLvl >= _realm.value.enemyLevelGap * _realm.value.floor && Random.nextInt(5) == 0) {
            dmg *= 1.35f
            kind = "heavy"
            addCombatLog("${cs.enemyName}施展破势重击！")
            soundManager.playSfx("realm_breakthrough")
        }
        if(p.skills.contains("PALADIN_HEART"))dmg=(dmg*0.75f).toInt().toFloat()
        if (cs.bossPhase >= 2 && cs.battleType == "sboss" && Random.nextInt(4) == 0) {
            dmg *= 1.45f
            kind = "heavy"
            addCombatLog("${cs.enemyName}施展狂澜连斩！")
            soundManager.playSfx("realm_breakthrough")
        }
        cs.playerHp=maxOf(0,cs.playerHp-dmg.toInt())
        if (cs.bossPhase >= 2 && cs.battleType == "guardian" && Random.nextInt(3) == 0) {
            val shock = (cs.enemyDef * 0.35f).toInt().coerceAtLeast(1)
            cs.playerHp = maxOf(0, cs.playerHp - shock)
            _dmgNumbers.value=(_dmgNumbers.value+DmgNumber(nextDamageNumberId++, "真气-${shock}", false, "player", "heavy")).takeLast(10)
            addCombatLog("${cs.enemyName}以护体真气震伤${p.name}，追加${shock}点伤害")
        }
        if(p.skills.contains("AEGIS_THORNS")){
            val thorn = (dmg*0.15f).toInt()
            cs.enemyHp=maxOf(0,cs.enemyHp-thorn)
            _dmgNumbers.value=(_dmgNumbers.value+DmgNumber(nextDamageNumberId++, "反伤-${thorn}", false, "enemy", "thorns")).takeLast(10)
        }
        addCombatLog("${cs.enemyName}对${p.name}造成${dmg.toInt()}点伤害${if (isCrit) "【暴击！】" else ""}")
        _dmgNumbers.value=(_dmgNumbers.value+DmgNumber(nextDamageNumberId++, if (isCrit) "-${dmg.toInt()}!" else "-${dmg.toInt()}", isCrit, "player", kind)).takeLast(10)
        soundManager.playSfx("blocked")
        _playerFlinch.value=true; p.stats.hp=cs.playerHp; _player.value = p.copy()
        _combatState.value=cs.copy(); hpValidation()
        return cs.enemyHp>0&&cs.playerHp>0
    }

    private fun triggerBossPhaseTwo(cs: CombatState): Boolean {
        if (cs.bossPhase >= 2 || cs.enemyHp <= 0 || cs.enemyHp > cs.enemyHpMax / 2) return false
        if (cs.battleType != "guardian" && cs.battleType != "sboss") return false
        cs.bossPhase = 2
        cs.combatId = nextCombatId++
        when (cs.battleType) {
            "guardian" -> {
                cs.enemyAtk = (cs.enemyAtk * 1.18f).toInt().coerceAtLeast(cs.enemyAtk + 1)
                cs.enemyDef = (cs.enemyDef * 1.20f).toInt().coerceAtLeast(cs.enemyDef + 1)
                cs.enemyCritRate += 4f
                addCombatLog("${cs.enemyName}气势暴涨，进入二阶段：护体反击！")
            }
            "sboss" -> {
                cs.enemyAtk = (cs.enemyAtk * 1.28f).toInt().coerceAtLeast(cs.enemyAtk + 1)
                cs.enemyAtkSpd = (cs.enemyAtkSpd * 1.18f).coerceAtMost(2.5f)
                cs.enemyCritRate += 6f
                cs.enemyCritDmg += 18f
                addCombatLog("${cs.enemyName}真气逆转，进入二阶段：狂澜连斩！")
            }
        }
        soundManager.playSfx("realm_breakthrough")
        _combatState.value = cs.copy()
        return true
    }

    fun hpValidation() {
        val cs=_combatState.value?:return; val p=_player.value
        if(cs.playerHp<1){cs.playerHp=0;cs.playerDead=true;p.deaths++;addCombatLog("${p.name}败下阵来……");endCombat(false)}
        else if(triggerBossPhaseTwo(cs)){cs.enemyHpPercent=(cs.enemyHp.toFloat()/cs.enemyHpMax*100f)}
        else if(cs.enemyHp<1){cs.enemyHp=0;cs.enemyDead=true;p.kills++; val r=_realm.value; r.currentKills++; _realm.value=r.copy();addCombatLog("${cs.enemyName}被击败！");addCombatLog("获得${cs.expReward}阅历。");playerExpGain(cs.expReward);addCombatLog("获得${cs.goldReward}两白银。");p.gold+=cs.goldReward;p.stats.hp+=(p.stats.hpMax*8/100);if(cs.hasDrop)createEquipPrint();calculateStats();endCombat(true);
            val cr=CultivationRealm.entries.find{it.name==p.realm}?:CultivationRealm.NONE; val nr=CultivationRealm.entries.getOrNull(cr.ordinal+1)
            if(nr!=null&&p.lvl>=nr.level*10+10&&p.kills>=nr.level*5){_realmBreakthroughPending.value=true;_realmBreakthroughInfo.value=Pair(cr.displayName,nr.displayName)}
            if(p.exp.lvlGained>0){_showLevelUp.value=true;lvlupPopup()}
        } else { cs.enemyHpPercent=(cs.enemyHp.toFloat()/cs.enemyHpMax*100f) }
        _player.value = _player.value.copy(); _combatState.value=cs.copy()
    }

    private fun playerExpGain(expGain:Int) {
        val p=_player.value; p.exp.expCurr+=expGain; p.exp.expCurrLvl+=expGain
        while(p.exp.expCurr>=p.exp.expMax){
            p.exp.lvlGained++
            val inc=if(p.lvl>100) 1000000 else ((p.exp.expMax*1.1f).toInt()+100-p.exp.expMax)
            val excess=p.exp.expCurr-p.exp.expMax; p.exp.expCurrLvl=excess; p.exp.expMaxLvl=inc
            p.lvl++; p.exp.expMax+=inc; p.exp.expCurr=excess
            p.bonusStats.hp+=4f; p.bonusStats.atk+=2f; p.bonusStats.def+=2f; p.bonusStats.atkSpd+=0.15f; p.bonusStats.critRate+=0.1f; p.bonusStats.critDmg+=0.25f
            p.stats.hp+=(p.stats.hpMax*20/100)
        }
        calculateStats(); _player.value = p.copy()
    }

    private fun endCombat(victory: Boolean) {
        soundManager.stopBgm()
        soundManager.playSfx(if (victory) "victory_chime" else "decline")
        val p=_player.value; _player.value = p.copy(inCombat = false)
        if(p.skills.contains("RAMPAGER")){p.baseStats.atk-=p.tempStats.atk.toInt();p.tempStats.atk=0f}
        if(p.skills.contains("BLADE_DANCE")){p.baseStats.atkSpd-=p.tempStats.atkSpd;p.tempStats.atkSpd=0f}
        calculateStats(); saveGame()
        val cs = _combatState.value
        if (victory && cs?.battleType == "guardian") {
            _realm.value = _realm.value.copy(isExploring = false, isEventActive = true, currentEvent = "floor_clear")
            _eventPrompt.value = EventPrompt("你击败了【${cs.enemyName}】，${currentAreaName()}的出口已经打开。是否前往下一层？", listOf("前往下一层", "留在本层"))
        } else {
            _realm.value = _realm.value.copy(isExploring = false, isEventActive = true, currentEvent = "combat_result")
        }
    }

    fun dismissCombatResult() {
        _combatState.value = null
        val shouldExplore = !_realm.value.isPaused
        _realm.value = _realm.value.copy(isExploring = shouldExplore, isEventActive = false, currentEvent = "")
        if (shouldExplore) soundManager.playBgm(context, "jianghu")
        saveGame()
    }

    fun returnAfterDeath() {
        _combatState.value = null
        _eventPrompt.value = null
        _combatLog.value = emptyList()
        _player.value = _player.value.copy(inCombat = false)
        val r = _realm.value.copy()
        val oldArea = currentAreaName(r.floor)
        if (r.floor > 1) r.floor--
        r.room = 1
        r.actionCounter = 0
        r.currentKills = 0
        r.isExploring = false
        r.isPaused = true
        r.isEventActive = false
        r.currentEvent = ""
        _realm.value = r
        val advice = when {
            _player.value.stats.hpMax < 800 -> "建议先在上一层搜集装备，并优先强化气血/防御装备。"
            _player.value.stats.atk < 180 -> "建议提升攻击，或购买兵器后再挑战守卫。"
            _player.value.stats.def < 100 -> "建议强化护甲、盾牌或请教老医师提升防御。"
            else -> "建议留在当前区域多刷战利品，确认血量充足后再挑战。"
        }
        addRealmLog("你败退离开【${oldArea}】，退回【${currentAreaName(r.floor)}】整备。$advice")
        saveGame()
    }

    private fun lvlupPopup() {
        soundManager.playSfx("realm_breakthrough"); val p=_player.value
        addCombatLog("境界提升！（${MartialRealmDisplay.fromLevel(p.lvl-p.exp.lvlGained)}→${MartialRealmDisplay.fromLevel(p.lvl)}）")
        val percentages=mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f)
        generateLvlStats(2,percentages)
    }

    fun generateLvlStats(rerolls:Int,percentages:Map<String,Float>) {
        val stats=listOf("hp","atk","def","atkSpd","vamp","critRate","critDmg"); val selected=stats.shuffled().take(3)
        _availableUpgrades.value=selected.map{s->
            UpgradeOption(statDisplay(s),percentages[s]?:8f,s)}
        _rerollsLeft.value=rerolls
    }

    fun selectUpgrade(idx:Int) {
        val opts=_availableUpgrades.value; if(idx>=opts.size)return; val chosen=opts[idx]; val p=_player.value
        when(chosen.statKey){"hp"->p.bonusStats.hp+=chosen.value;"atk"->p.bonusStats.atk+=chosen.value;"def"->p.bonusStats.def+=chosen.value;"atkSpd"->p.bonusStats.atkSpd+=chosen.value;"vamp"->p.bonusStats.vamp+=chosen.value;"critRate"->p.bonusStats.critRate+=chosen.value;"critDmg"->p.bonusStats.critDmg+=chosen.value}
        soundManager.playSfx("scroll_open"); p.exp.lvlGained--
        if(p.exp.lvlGained>0){_availableUpgrades.value=emptyList(); generateLvlStats(2,mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f))}
        else {_showLevelUp.value=false; _availableUpgrades.value=emptyList(); _rerollsLeft.value=0; p.exp.lvlGained=0 }
        calculateStats(); _player.value = p.copy(); saveGame()
    }

    fun rerollUpgrades():Boolean {
        if(_rerollsLeft.value<=0)return false; _rerollsLeft.value=_rerollsLeft.value-1; soundManager.playSfx("coin_pouch")
        generateLvlStats(_rerollsLeft.value,mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f))
        return true
    }

    private fun statKeyToKey(s:String)=when(s){"气血"->"hp";"攻击"->"atk";"防御"->"def";"身法"->"atkSpd";"吸血"->"vamp";"暴击率"->"critRate";"暴击伤害"->"critDmg";else->s}
    private fun statDisplay(k:String)=when(k){"hp"->"气血";"atk"->"攻击";"def"->"防御";"atkSpd"->"身法";"vamp"->"吸血";"critRate"->"暴率";"critDmg"->"暴伤";else->k}

    fun confirmBreakthrough() {
        val nr=CultivationRealm.entries.find{it.displayName==_realmBreakthroughInfo.value?.second}?:return
        val p=_player.value; p.realm=nr.name; p.stats.hp=p.stats.hpMax
        addRealmLog("境界突破！踏入【${nr.displayName}】！"); soundManager.playSfx("realm_breakthrough"); calculateStats(); _player.value = p.copy(); saveGame()
        _realmBreakthroughPending.value=false; _realmBreakthroughInfo.value=null
    }
    fun dismissBreakthrough() { _realmBreakthroughPending.value=false; _realmBreakthroughInfo.value=null }

    // ===== Equipment =====
    fun createEquipPrint(condition:String="jianghu"):EquipmentItem {
        val item=createEquipment()
        val icon=equipmentIcon(item.category)
        val sb=StringBuilder()
        item.stats.forEach{sm->sm.forEach{(k,v)->
            val pct=if(k in listOf("atkSpd","vamp","critRate","critDmg"))"%" else ""
            sb.append("${statDisplay(k)}+${"%.2f".format(v)}$pct ")}
        }
        if(condition=="combat")addCombatLog("${_combatState.value?.enemyName}掉落【${item.rarity}${item.category}】${sb}")
        else addRealmLog("获得【${item.rarity}${item.category}】${sb}")
        return item
    }

    private fun rollEquipmentRarity(floor: Int): EquipmentRarity {
        val allowed = when {
            floor < 6 -> listOf(EquipmentRarity.COMMON, EquipmentRarity.UNCOMMON)
            floor < 16 -> listOf(EquipmentRarity.COMMON, EquipmentRarity.UNCOMMON, EquipmentRarity.RARE)
            floor < 31 -> listOf(EquipmentRarity.UNCOMMON, EquipmentRarity.RARE, EquipmentRarity.EPIC)
            floor < 51 -> listOf(EquipmentRarity.RARE, EquipmentRarity.EPIC, EquipmentRarity.LEGENDARY)
            else -> listOf(EquipmentRarity.RARE, EquipmentRarity.EPIC, EquipmentRarity.LEGENDARY, EquipmentRarity.HEIRLOOM)
        }
        val weights = allowed.map { rarity ->
            when (rarity) {
                EquipmentRarity.COMMON -> 68f
                EquipmentRarity.UNCOMMON -> if (floor < 6) 32f else 45f
                EquipmentRarity.RARE -> if (floor < 16) 12f else 34f
                EquipmentRarity.EPIC -> if (floor < 31) 10f else 22f
                EquipmentRarity.LEGENDARY -> if (floor < 51) 6f else 14f
                EquipmentRarity.HEIRLOOM -> 4f
            }
        }
        val roll = Random.nextFloat() * weights.sum()
        var acc = 0f
        for (i in allowed.indices) {
            acc += weights[i]
            if (roll <= acc) return allowed[i]
        }
        return allowed.last()
    }

    fun createEquipment():EquipmentItem {
        val attr=if(Random.nextBoolean())EquipmentAttribute.DAMAGE else EquipmentAttribute.DEFENSE
        val types=EquipmentType.entries.filter{it.attr==attr}
        val type=types.random()
        val r=_realm.value
        val rarity=rollEquipmentRarity(r.floor)
        val maxLvl=r.floor*r.enemyLevelGap+(r.enemyBaseLevel-1); val minLvl=maxOf(1,maxLvl-(r.enemyLevelGap-1))
        val lvl=Random.nextInt(minLvl,maxLvl+1).coerceAtMost(100)
        val scaling=r.enemyScaling.coerceAtMost(2f); val statMult=((scaling-1)*lvl).toFloat()
        val tier=((scaling-1)*10).roundToInt().coerceIn(1,10)

        val statTypes=when{attr==EquipmentAttribute.DAMAGE->when(type.displayName){"开山斧","月牙镰"->listOf("atk","atk","vamp","critRate","critDmg","critDmg");"袖里刃","游龙鞭"->listOf("atkSpd","atkSpd","atk","vamp","critRate","critRate","critDmg");"镇岳锤"->listOf("hp","def","atk","atk","critRate","critDmg");else->listOf("atk","atkSpd","vamp","critRate","critDmg")}
            else->listOf("hp","hp","def","def","atk")}

        val stats=mutableListOf<Map<String,Float>>(); var loopCount=rarity.statCount; var totalVal=0f
        var i=0; while(i<loopCount){
            val st=statTypes.random()
            val base=when(st){"hp"->40f;"atk","def"->16f;"atkSpd","critDmg"->3f;"vamp","critRate"->2f;else->1f}
            var v=base*(0.5f+Random.nextFloat())+base*(0.5f+Random.nextFloat())*statMult
            when(st){"atkSpd"->if(v>15f){v=15f*(0.5f+Random.nextFloat()*0.5f);loopCount++};"vamp"->if(v>8f){v=8f*(0.5f+Random.nextFloat()*0.5f);loopCount++};"critRate"->if(v>10f){v=10f*(0.5f+Random.nextFloat()*0.5f);loopCount++}}
            if(loopCount>rarity.statCount+3)loopCount--
            totalVal+=when(st){"hp"->v;"atk","def"->v*2.5f;"atkSpd","critDmg"->v*8.33f;"vamp","critRate"->v*20.83f;else->v}

            var merged=false
            for(sm in stats)if(sm.containsKey(st)){stats[stats.indexOf(sm)]=mapOf(st to (sm[st]!!+v));merged=true;break}
            if(!merged)stats.add(mapOf(st to v))
            i++
        }
        val sellVal=(totalVal*3f).toInt()

        val equipTypeName = when {
            type.attr == EquipmentAttribute.DAMAGE -> "兵器"
            type in listOf(EquipmentType.PLATE_ARMOR, EquipmentType.CHAIN_ARMOR, EquipmentType.LEATHER_ARMOR, EquipmentType.SCALE_ARMOR, EquipmentType.ROBE_ARMOR, EquipmentType.VINE_ARMOR) -> "护甲"
            type in listOf(EquipmentType.TOWER_SHIELD, EquipmentType.KITE_SHIELD, EquipmentType.BUCKLER, EquipmentType.LOTUS_SHIELD, EquipmentType.TIGER_SHIELD, EquipmentType.COPPER_SHIELD) -> "盾牌"
            type in listOf(EquipmentType.GREAT_HELM, EquipmentType.HORNED_HELM, EquipmentType.BAMBOO_HAT, EquipmentType.JADE_CROWN, EquipmentType.IRON_MASK, EquipmentType.CLOTH_BAND) -> "头盔"
            type in listOf(EquipmentType.CLOUD_BOOTS, EquipmentType.IRON_BOOTS, EquipmentType.WIND_BOOTS, EquipmentType.SHADOW_BOOTS, EquipmentType.RAIN_BOOTS, EquipmentType.MONK_SHOES) -> "鞋履"
            else -> "饰品"
        }
        val equip=EquipmentItem(category=type.displayName,attribute=attr.name,type=equipTypeName,rarity=rarity.displayName,lvl=lvl,tier=tier,value=sellVal,stats=stats)

        val inv=parseInventory().toMutableList(); inv.add(equip); _player.value=_player.value.copy(inventory=gson.toJson(inv))
        return equip
    }

    private fun equipmentIcon(cat:String)=when(cat){"青锋剑"->"剑";"开山斧"->"斧";"镇岳锤"->"锤";"袖里刃"->"刃";"游龙鞭"->"鞭";"月牙镰"->"镰";"玄铁甲"->"甲";"金丝软甲"->"甲";"夜行衣"->"衣";"玄武盾"->"盾";"雁翎盾"->"盾";"八卦盾"->"盾";"狮首盔"->"盔";"龙纹冠"->"冠";else->"物"}

    // ===== Inventory ops =====
    fun equipItem(idx:Int):Boolean {
        val inv=parseInventory().toMutableList(); if(idx>=inv.size)return false
        val equipped=parseEquipped().toMutableList()
        while (equipped.size < equipSlots.size) equipped.add(EquipmentItem())
        if (equipped.size > equipSlots.size) {
            inv.addAll(equipped.drop(equipSlots.size).filter { it.category.isNotBlank() })
            equipped.subList(equipSlots.size, equipped.size).clear()
        }
        val item=inv.removeAt(idx)
        val slotIndex = equipSlots.indexOf(item.type).takeIf { it >= 0 } ?: 0
        val oldItem = equipped.getOrNull(slotIndex)
        if (oldItem != null && oldItem.category.isNotBlank()) inv.add(oldItem)
        equipped[slotIndex] = item
        _player.value=_player.value.copy(inventory=gson.toJson(inv),equipped=gson.toJson(equipped))
        addRealmLog("已装备【${item.rarity}${item.category}】到${item.type}槽。" + if (oldItem != null && oldItem.category.isNotBlank()) "原装备已放回背包。" else "")
        soundManager.playSfx("equip_blade"); calculateStats(); saveGame(); return true
    }
    fun unequipItem(idx:Int):Boolean {
        val equipped=parseEquipped().toMutableList(); if(idx>=equipped.size)return false
        val inv=parseInventory().toMutableList(); val item=equipped.removeAt(idx); inv.add(item)
        _player.value=_player.value.copy(inventory=gson.toJson(inv),equipped=gson.toJson(equipped))
        soundManager.playSfx("sheath_blade"); calculateStats(); saveGame(); return true
    }
    fun sellItem(isEquipped:Boolean,idx:Int):Boolean {
        val p=_player.value
        if(isEquipped){val e=parseEquipped().toMutableList();if(idx>=e.size)return false;val item=e.removeAt(idx);p.gold+=item.value;_player.value=p.copy(equipped=gson.toJson(e))}
        else{val i=parseInventory().toMutableList();if(idx>=i.size)return false;val item=i.removeAt(idx);p.gold+=item.value;_player.value=p.copy(inventory=gson.toJson(i))}
        soundManager.playSfx("coin_pouch"); calculateStats(); saveGame(); return true
    }
    fun sellAll(rarity:String) {
        val p=_player.value
        if(rarity=="全部"){val inv=parseInventory().toMutableList(); if(inv.isEmpty()){soundManager.playSfx("blocked");return}
            for(item in inv){p.gold+=item.value}; _player.value=p.copy(inventory="[]"); soundManager.playSfx("coin_pouch")}
        else{val inv=parseInventory().toMutableList(); val toSell=inv.filter{it.rarity==rarity}; if(toSell.isEmpty()){soundManager.playSfx("blocked");return}
            for(item in toSell){p.gold+=item.value}; inv.removeAll(toSell); _player.value=p.copy(inventory=gson.toJson(inv)); soundManager.playSfx("coin_pouch")}
        calculateStats(); saveGame()
    }
    fun unequipAll() { val eq=parseEquipped().toMutableList(); val inv=parseInventory().toMutableList(); inv.addAll(eq); _player.value=_player.value.copy(equipped="[]",inventory=gson.toJson(inv)); calculateStats(); saveGame() }

    fun enhanceCost(item: EquipmentItem): Long = (item.lvl*180L+_realm.value.floor*60L).coerceAtLeast(120L)
    fun reforgeCost(item: EquipmentItem): Long = (item.lvl*260L+_realm.value.floor*90L).coerceAtLeast(180L)

    fun enhanceEquipped(idx:Int):Boolean {
        val equipped=parseEquipped().toMutableList(); if(idx>=equipped.size)return false
        val p=_player.value.copy()
        val item=equipped[idx]
        if (item.category.isBlank()) return false
        val cost=enhanceCost(item)
        if(p.gold<cost){addRealmLog("银两不足，无法强化装备。");soundManager.playSfx("blocked");return false}
        p.gold-=cost
        val enhanced=item.copy(lvl=item.lvl+1, value=item.value+(cost/2).toInt(), stats=item.stats.map{sm->sm.mapValues{it.value*1.08f}})
        equipped[idx]=enhanced
        _player.value=p.copy(equipped=gson.toJson(equipped))
        calculateStats(); addRealmLog("强化【${item.category}】成功，品阶提升。")
        soundManager.playSfx("equip_blade"); saveGame(); return true
    }

    fun reforgeEquipped(idx:Int):Boolean {
        val equipped=parseEquipped().toMutableList(); if(idx>=equipped.size)return false
        val p=_player.value.copy()
        val item=equipped[idx]
        if (item.category.isBlank()) return false
        val cost=reforgeCost(item)
        if(p.gold<cost){addRealmLog("银两不足，无法重铸装备。");soundManager.playSfx("blocked");return false}
        p.gold-=cost
        val reforged=createEquipment().copy(category=item.category, attribute=item.attribute, type=item.type, rarity=item.rarity, lvl=item.lvl, tier=item.tier, value=item.value)
        val inv=parseInventory().toMutableList(); if(inv.isNotEmpty()) inv.removeAt(inv.lastIndex)
        equipped[idx]=reforged
        _player.value=p.copy(equipped=gson.toJson(equipped), inventory=gson.toJson(inv))
        calculateStats(); addRealmLog("重铸【${item.category}】成功，词条已变化。")
        soundManager.playSfx("scroll_open"); saveGame(); return true
    }

    // ===== Logging =====
    private fun addCombatLog(msg:String){ val l=_combatLog.value.toMutableList(); l.add(msg); if(l.size>100)l.removeAt(0); _combatLog.value=l }
    fun addRealmLog(msg:String,choices:List<String> = emptyList()) {
        val l=_realmLog.value.toMutableList()
        l.add(if(choices.isNotEmpty())"$msg\n[选项:${choices.joinToString("|")}]" else msg)
        if(l.size>50)l.removeAt(0); _realmLog.value=l
        if (choices.isNotEmpty()) _eventPrompt.value = EventPrompt(msg, choices)
    }

    private fun formatTime(s:Int)=String.format("%02d:%02d",s/60,s%60)

    // ===== Save =====
    fun saveGame() {
        prefs.edit().putString("player",gson.toJson(_player.value)).putString("realm",gson.toJson(_realm.value)).apply()
    }

    fun trySafeSave(): Boolean {
        val unsafe = _player.value.inCombat || _combatState.value != null || _realm.value.isEventActive || _showLevelUp.value || _realmBreakthroughPending.value
        if (unsafe) {
            addRealmLog("当前处于事件或战斗中，已暂缓存档。")
            soundManager.playSfx("blocked")
            return false
        }
        saveGame()
        addRealmLog("进度已保存。")
        soundManager.playSfx("wood_confirm")
        return true
    }
    fun loadGame():Boolean {
        val pj=prefs.getString("player",null)?:return false; val rj=prefs.getString("realm",null)?:return false
        try{
            val loadedPlayer = gson.fromJson(pj,PlayerEntity::class.java).copy(inCombat = false)
            if (MartialSect.entries.none { it.name == loadedPlayer.sect }) loadedPlayer.sect = MartialSect.WANDERER.name
            _player.value = loadedPlayer
            _realm.value=gson.fromJson(rj,RealmState::class.java).copy(isExploring=false,isPaused=true,isEventActive=false,currentEvent="")
            _combatState.value = null
            _eventPrompt.value = null
            _combatLog.value = emptyList()
            _availableUpgrades.value = emptyList()
            _showLevelUp.value = false
            _realmBreakthroughPending.value = false
            _realmBreakthroughInfo.value = null
            calculateStats()
            return true
        }
        catch(_:Exception){return false}
    }
    fun hasSave()=prefs.contains("player")
    fun deleteSave(){
        prefs.edit().clear().apply()
        _player.value = PlayerEntity()
        _realm.value = RealmState()
        _combatState.value = null
        _eventPrompt.value = null
        _combatLog.value = emptyList()
        _realmLog.value = emptyList()
        _availableUpgrades.value = emptyList()
        _showLevelUp.value = false
        _realmBreakthroughPending.value = false
        _realmBreakthroughInfo.value = null
        soundManager.stopBgm()
    }

    fun markPrologueSeen() {
        _player.value = _player.value.copy(prologueSeen = true)
        addRealmLog("你踏入旧牢门，暗处传来铁链拖地的声音。")
        saveGame()
    }
}