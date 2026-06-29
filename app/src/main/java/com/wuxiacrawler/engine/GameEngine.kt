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

    data class DmgNumber(val text: String, val isCrit: Boolean)

    private var nextCombatId = 1

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
        val equippedItems = parseEquipped()
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
        return try { gson.fromJson(j, object: TypeToken<List<EquipmentItem>>(){}.type) } catch (_:Exception) { emptyList() }
    }
    private fun applySetBonuses(equippedItems: List<EquipmentItem>, bonus: PlayerStats) {
        equippedItems.groupingBy { it.type }.eachCount().forEach { (type, count) ->
            if (count >= 2) {
                when (type) {
                    "兵器" -> bonus.atk += 20
                    "护甲" -> bonus.hpMax += 80
                    "盾牌" -> bonus.def += 14
                    "头盔" -> bonus.critRate += 2f
                }
            }
            if (count >= 4) {
                when (type) {
                    "兵器" -> bonus.critDmg += 12f
                    "护甲" -> bonus.def += 20
                    "盾牌" -> bonus.hpMax += 120
                    "头盔" -> bonus.atkSpd += 4f
                }
            }
            if (count >= 6) {
                when (type) {
                    "兵器" -> bonus.vamp += 3f
                    "护甲" -> bonus.vamp += 2f
                    "盾牌" -> bonus.critRate += 4f
                    "头盔" -> bonus.critDmg += 18f
                }
            }
        }
    }

    fun activeSetBonusDescriptions(): List<String> {
        return parseEquipped().groupingBy { it.type }.eachCount().flatMap { (type, count) ->
            val lines = mutableListOf<String>()
            if (count >= 2) lines.add("$type 2件：${setBonusText(type, 2)}")
            if (count >= 4) lines.add("$type 4件：${setBonusText(type, 4)}")
            if (count >= 6) lines.add("$type 6件：${setBonusText(type, 6)}")
            lines
        }
    }

    private fun setBonusText(type: String, pieces: Int): String = when (type to pieces) {
        "兵器" to 2 -> "攻击+20"
        "兵器" to 4 -> "暴伤+12%"
        "兵器" to 6 -> "吸血+3%"
        "护甲" to 2 -> "气血+80"
        "护甲" to 4 -> "防御+20"
        "护甲" to 6 -> "吸血+2%"
        "盾牌" to 2 -> "防御+14"
        "盾牌" to 4 -> "气血+120"
        "盾牌" to 6 -> "暴率+4%"
        "头盔" to 2 -> "暴率+2%"
        "头盔" to 4 -> "身法+4%"
        "头盔" to 6 -> "暴伤+18%"
        else -> ""
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
        val types = mutableListOf("blessing","curse","treasure","enemy","enemy","route","merchant","healer","manual","nothing","nothing","monarch")
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
            "treasure" -> addRealmLog("发现一间藏宝室，里面有一个宝箱。", listOf("打开宝箱", "无视"))
            "route" -> addRealmLog("前方出现三条岔路：左侧安静，右侧血腥，中路传来锁链声。", listOf("稳步前行", "冒险深入", "搜寻密道"))
            "merchant" -> addRealmLog("一名蒙面游商坐在灯下，摊上摆着伤药和旧兵器。", listOf("买伤药", "买兵器", "离开"))
            "healer" -> addRealmLog("破庙里有一位老医师，愿以银两换一线生机。", listOf("疗伤", "请教", "离开"))
            "manual" -> addRealmLog("石匣中藏着三页残破功谱，只够参悟其中一页。", listOf("刀法", "身法", "铁骨"))
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

    fun chooseOption(idx: Int) {
        val r = _realm.value.copy()
        val eventBefore = r.currentEvent
        when (r.currentEvent) {
            "enemy" -> {
                if (idx == 0) { startCombat("battle"); addRealmLog("拔剑迎战！") }
                else if (idx == 1) {
                    if (Random.nextBoolean()) {
                        addRealmLog("施展轻功脱身，继续前行。")
                        _player.value = _player.value.copy(inCombat = false)
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
            else -> { ignoreEvent(); r.isEventActive = false; r.currentEvent = "" }
        }
        val now = _realm.value
        if (!_player.value.inCombat && _combatState.value == null && now.currentEvent == eventBefore && now.currentEvent != "combat_result") _realm.value = r
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
        addRealmLog("祝福获得${k}+${v}%！（祝福${p.blessing}重→${p.blessing+1}重）"); p.blessing++
        calculateStats(); saveGame(); _player.value = p.copy()
    }

    private fun roomTransition() {
        when(Random.nextInt(3)){
            0->{ incrementRoom(); generateEnemy("door"); addRealmLog("幻阵假门显形！"); startCombat("battle") }
            1->{ incrementRoom(); addRealmLog("进入新房间，发现宝箱！",listOf("打开宝箱","无视")); _realm.value = _realm.value.copy(isEventActive = true, currentEvent = "treasure") }
            else->{ incrementRoom(); addRealmLog("进入了下一个房间。"); _realm.value = _realm.value.copy(isEventActive = false, currentEvent = "") }
        }
    }

    private fun guardianBattle() { incrementRoom(); generateEnemy("guardian"); startCombat("guardian"); addCombatLog("江湖护法【${_combatState.value?.enemyName}】挡住了去路！"); addRealmLog("进入了下一处据点。") }
    private fun specialBossBattle() { generateEnemy("sboss"); startCombat("boss"); addCombatLog("武林至尊【${_combatState.value?.enemyName}】苏醒！"); addRealmLog("武林至尊【${_combatState.value?.enemyName}】苏醒！") }
    private fun ignoreEvent() { soundManager.playSfx("wood_confirm"); addRealmLog("选择无视，继续前行。") }
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
        val msg = when (floor) {
            2 -> "石壁上刻着半句残诗：入暗牢者，先失其名，再失其心。"
            3 -> "你在牢门铜环上发现与身上断符相同的纹路，掌柜并未说谎。"
            5 -> "暗处有人低声唤你的名字，可那声音不像活人。"
            8 -> "一具旧甲旁压着血书：七派入牢，皆为一人所邀。"
            13 -> "前方风声忽停，像有一位旧日高手正在等你赴约。"
            21 -> "你听见掌柜的声音从墙后传来：别信你看到的门，暗牢会学人说话。"
            34 -> "一枚完整牢符嵌在石台中央，符背刻着你的姓氏。"
            55 -> "暗牢深处传来钟声，七大门派失踪的真相，似乎只差最后一扇门。"
            89 -> "你终于明白，暗牢不是囚牢，而是一座筛选江湖继承者的旧阵。"
            else -> null
        }
        if (msg != null) addRealmLog("剧情：$msg")
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
        if(p.skills.contains("REMNANT_EDGE"))dmg+=(cs.enemyHp*0.08f).toInt()
        if(p.skills.contains("TITAN_WILL"))dmg+=(cs.playerHpMax*0.05f).toInt()
        if(p.skills.contains("DEVASTATOR"))dmg=(dmg*1.3f).toInt().toFloat()
        if(p.skills.contains("RAMPAGER")){p.baseStats.atk+=5;p.tempStats.atk+=5f;calculateStats()}
        if(p.skills.contains("BLADE_DANCE")){p.baseStats.atkSpd+=0.01f;p.tempStats.atkSpd+=0.01f;calculateStats()}
        val ls=(dmg*cs.playerVamp/100f).toInt()
        cs.enemyHp=maxOf(0,cs.enemyHp-dmg.toInt()); cs.playerHp=minOf(cs.playerHpMax,cs.playerHp+ls)
        val cm=if(isCrit)"【暴击！】" else ""
        addCombatLog("${p.name}对${cs.enemyName}造成${dmg.toInt()}点${dType} $cm")
        _dmgNumbers.value=(_dmgNumbers.value+DmgNumber(dmg.toInt().toString(),isCrit)).takeLast(8); _enemyFlinch.value=true
        _combatState.value=cs.copy(); hpValidation()
        return cs.enemyHp>0&&cs.playerHp>0
    }

    fun enemyAttack():Boolean {
        val cs=_combatState.value?:return false; val p=_player.value; if(!p.inCombat)return false
        soundManager.playSfx("sword_slash")
        var dmg=cs.enemyAtk*(cs.enemyAtk.toFloat()/(cs.enemyAtk+cs.playerDef)); dmg*=(0.9f+Random.nextFloat()*0.2f)
        if(Random.nextFloat()*100<cs.enemyCritRate)dmg=(dmg*(1f+cs.enemyCritDmg/100f)).toInt().toFloat() else dmg=dmg.toInt().toFloat()
        if (cs.enemyLvl >= _realm.value.enemyLevelGap * _realm.value.floor && Random.nextInt(5) == 0) {
            dmg *= 1.35f
            addCombatLog("${cs.enemyName}施展破势重击！")
            soundManager.playSfx("realm_breakthrough")
        }
        if(p.skills.contains("PALADIN_HEART"))dmg=(dmg*0.75f).toInt().toFloat()
        if (cs.bossPhase >= 2 && cs.battleType == "sboss" && Random.nextInt(4) == 0) {
            dmg *= 1.45f
            addCombatLog("${cs.enemyName}施展狂澜连斩！")
            soundManager.playSfx("realm_breakthrough")
        }
        cs.playerHp=maxOf(0,cs.playerHp-dmg.toInt())
        if (cs.bossPhase >= 2 && cs.battleType == "guardian" && Random.nextInt(3) == 0) {
            val shock = (cs.enemyDef * 0.35f).toInt().coerceAtLeast(1)
            cs.playerHp = maxOf(0, cs.playerHp - shock)
            addCombatLog("${cs.enemyName}以护体真气震伤${p.name}，追加${shock}点伤害")
        }
        if(p.skills.contains("AEGIS_THORNS"))cs.enemyHp=maxOf(0,cs.enemyHp-(dmg*0.15f).toInt())
        addCombatLog("${cs.enemyName}对${p.name}造成${dmg.toInt()}点伤害")
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
        calculateStats(); saveGame(); _realm.value = _realm.value.copy(isExploring = false, isEventActive = true, currentEvent = "combat_result")
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
        _combatLog.value = emptyList()
        _player.value = _player.value.copy(inCombat = false)
        _realm.value = _realm.value.copy(isExploring = false, isPaused = true, isEventActive = false, currentEvent = "")
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

    fun createEquipment():EquipmentItem {
        val attr=if(Random.nextBoolean())EquipmentAttribute.DAMAGE else EquipmentAttribute.DEFENSE
        val types=EquipmentType.entries.filter{it.attr==attr}
        val type=types.random()
        val roll=Random.nextFloat(); var cum=0f; var rarity=EquipmentRarity.COMMON
        for(r in EquipmentRarity.entries){cum+=r.chance; if(roll<=cum){rarity=r;break}}
        val r=_realm.value; val maxLvl=r.floor*r.enemyLevelGap+(r.enemyBaseLevel-1); val minLvl=maxOf(1,maxLvl-(r.enemyLevelGap-1))
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
            type in listOf(EquipmentType.PLATE_ARMOR, EquipmentType.CHAIN_ARMOR, EquipmentType.LEATHER_ARMOR) -> "护甲"
            type in listOf(EquipmentType.TOWER_SHIELD, EquipmentType.KITE_SHIELD, EquipmentType.BUCKLER) -> "盾牌"
            else -> "头盔"
        }
        val equip=EquipmentItem(category=type.displayName,attribute=attr.name,type=equipTypeName,rarity=rarity.displayName,lvl=lvl,tier=tier,value=sellVal,stats=stats)

        val inv=parseInventory().toMutableList(); inv.add(equip); _player.value=_player.value.copy(inventory=gson.toJson(inv))
        return equip
    }

    private fun equipmentIcon(cat:String)=when(cat){"青锋剑"->"剑";"开山斧"->"斧";"镇岳锤"->"锤";"袖里刃"->"刃";"游龙鞭"->"鞭";"月牙镰"->"镰";"玄铁甲"->"甲";"金丝软甲"->"甲";"夜行衣"->"衣";"玄武盾"->"盾";"雁翎盾"->"盾";"八卦盾"->"盾";"狮首盔"->"盔";"龙纹冠"->"冠";else->"物"}

    // ===== Inventory ops =====
    fun equipItem(idx:Int):Boolean {
        val inv=parseInventory().toMutableList(); if(idx>=inv.size)return false
        val equipped=parseEquipped().toMutableList(); if(equipped.size>=9){soundManager.playSfx("blocked");return false}
        val item=inv.removeAt(idx); equipped.add(item)
        _player.value=_player.value.copy(inventory=gson.toJson(inv),equipped=gson.toJson(equipped))
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

    fun enhanceEquipped(idx:Int):Boolean {
        val equipped=parseEquipped().toMutableList(); if(idx>=equipped.size)return false
        val p=_player.value.copy()
        val item=equipped[idx]
        val cost=(item.lvl*180L+_realm.value.floor*60L).coerceAtLeast(120L)
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
        val cost=(item.lvl*260L+_realm.value.floor*90L).coerceAtLeast(180L)
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