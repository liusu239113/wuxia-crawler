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

    // ===== Character Creation =====
    fun createCharacter(name: String, hpAlloc: Int, atkAlloc: Int, defAlloc: Int, spdAlloc: Int, skill: MartialSkill) {
        val p = _player.value.copy()
        p.name = name
        p.baseStats = PlayerStats(hp=50*hpAlloc, hpMax=50*hpAlloc, atk=10*atkAlloc, def=10*defAlloc, atkSpd=0.4f+0.02f*spdAlloc)
        p.skills = skill.name; p.isAllocated = true
        calculateStats(); p.stats.hp = p.stats.hpMax
        _player.value = p; saveGame()
    }

    // ===== Stats =====
    fun calculateStats() {
        val p = _player.value
        val realm = CultivationRealm.entries.find { it.name == p.realm } ?: CultivationRealm.NONE
        p.equippedStats = PlayerStats(0,0,0,0,0f,0f,0f,0f)
        for (item in parseEquipped()) for (sm in item.stats) for ((k,v) in sm) {
            when(k) {
                "hp"->p.equippedStats.hpMax+=v.toInt(); "atk"->p.equippedStats.atk+=v.toInt()
                "def"->p.equippedStats.def+=v.toInt(); "atkSpd"->p.equippedStats.atkSpd+=v
                "vamp"->p.equippedStats.vamp+=v; "critRate"->p.equippedStats.critRate+=v; "critDmg"->p.equippedStats.critDmg+=v
            }
        }
        p.stats = PlayerStats(
            hp=0,
            hpMax=(((p.baseStats.hpMax*(1f+p.bonusStats.hp/100f)).toInt()*(1f+realm.hpBonus)).toInt()+p.equippedStats.hpMax).coerceAtLeast(1),
            atk=(((p.baseStats.atk*(1f+p.bonusStats.atk/100f)).toInt()*(1f+realm.atkBonus)).toInt()+p.equippedStats.atk+p.tempStats.atk.toInt()).coerceAtLeast(1),
            def=(((p.baseStats.def*(1f+p.bonusStats.def/100f)).toInt()*(1f+realm.defBonus)).toInt()+p.equippedStats.def).coerceAtLeast(0),
            atkSpd=(p.baseStats.atkSpd*(1f+p.bonusStats.atkSpd/100f)+p.equippedStats.atkSpd/100f+p.tempStats.atkSpd).coerceAtMost(2.5f),
            vamp=p.bonusStats.vamp+p.equippedStats.vamp,
            critRate=p.bonusStats.critRate+p.equippedStats.critRate,
            critDmg=50f+p.bonusStats.critDmg+p.equippedStats.critDmg
        )
        if (p.skills.contains("DEVASTATOR")) p.stats.atk = (p.stats.atk*1.3f).toInt()
        if (p.skills.contains("BLOODTHIRST")) p.stats.vamp += 5f
        if (p.skills.contains("PRECISION")) p.stats.critRate += 8f
        p.stats.hp = p.stats.hpMax
        p.stats.hpPercent = (p.stats.hp.toFloat()/p.stats.hpMax*100f)
        _player.value = p.copy()
    }

    fun parseEquipped(): List<EquipmentItem> {
        val j = _player.value.equipped; if (j.isBlank()||j=="[]") return emptyList()
        return try { gson.fromJson(j, object: TypeToken<List<EquipmentItem>>(){}.type) } catch (_:Exception) { emptyList() }
    }
    fun parseInventory(): List<EquipmentItem> {
        val j = _player.value.inventory; if (j.isBlank()||j=="[]") return emptyList()
        return try { gson.fromJson(j, object: TypeToken<List<EquipmentItem>>(){}.type) } catch (_:Exception) { emptyList() }
    }

    // ===== Dungeon =====
    fun startExploring() { val r = _realm.value.copy(isPaused = false, isExploring = true); _realm.value = r; soundManager.playBgm(context,"dungeon"); soundManager.playSfx("unpause") }
    fun pauseExploring() { val r = _realm.value.copy(isPaused = true, isExploring = false); _realm.value = r; soundManager.stopBgm(); soundManager.playSfx("pause") }

    fun tickRealm() {
        val current = _realm.value
        if (!current.isExploring || current.isEventActive) return
        val r = current.copy()
        r.actionCounter++; r.runTime++
        val types = mutableListOf("blessing","curse","treasure","enemy","enemy","nothing","nothing","nothing","nothing","monarch")
        if (r.actionCounter > 2 && r.actionCounter < 6) types.add("nextroom")
        else if (r.actionCounter > 5) { processEvent(r, "nextroom"); _realm.value = r; return }
        processEvent(r, types.random())
        _realm.value = r
    }

    private val spriteMap = mapOf("山贼" to "goblin","山贼弓手" to "goblin_archer","山贼刺客" to "goblin_rogue","狼" to "wolf","黑狼" to "wolf_black","冰原狼" to "wolf_winter","魔液兽" to "slime","石魔像" to "slime","圣光天使" to "slime_angel","金甲剑奴" to "slime_knight","剑奴" to "slime_crusader","兽人剑师" to "orc_swordsmaster","兽人斧卫" to "orc_axe","兽人射手" to "orc_archer","蜘蛛" to "spider","赤蜘蛛" to "spider_red","绿毒蜘蛛" to "spider_green","骷髅射手" to "skeleton_archer","骷髅剑师" to "skeleton_swordsmaster","骷髅骑士" to "skeleton_knight","骷髅武士" to "skeleton_warrior","骷髅刺客" to "skeleton_samurai","骷髅海盗" to "skeleton_pirate","宝箱怪" to "mimic","秘境假门" to "mimic_door","霸天·妖兽统领" to "goblin_boss","骨皇·骷髅君主" to "skeleton_boss","炽热蜘蛛王" to "spider_fire","不死·骸骨帝王" to "berthelot","魔液君主" to "slime_boss","星灵·天蟹圣者" to "zodiac_cancer","圣光·白昼泰坦" to "alfadriel","龙骑·泰玛特" to "tiamat","无名·堕落之王" to "fallen_king","星灵·白羊圣者" to "zodiac_aries","蚁后·莉拉德" to "ant_queen","发条·机械蜘蛛" to "spider_boss","致命·弑神狼" to "wolf_boss","冥犬·赫尔猎犬" to "hellhound","三头·刻耳柏洛斯" to "cerberus_ptolemaios","灭世·比希摩斯" to "behemoth","龙帝·煞拉洛斯" to "zalaras","死灵·乌利奥特" to "skeleton_dragon","熔岩·伊弗利特" to "firelord","冰霜·希瓦" to "icemaiden","死神·萨纳托斯" to "thanatos","暗影·天使收割者" to "da-reaper","蛛龙·奈兹彻" to "spider_dragon","血煞·狂化妖" to "bm-feral")

    fun processEvent(r: RealmState, event: String) {
        r.isEventActive = true
        when(event) {
            "nextroom"->{ if(r.room>=r.roomsPerFloor) addRealmLog("找到了通往下一层的秘境之门！护法守在门前。",listOf("进入","无视")) else addRealmLog("前方有一扇秘境之门。",listOf("进入","无视")) }
            "treasure"->addRealmLog("发现一间藏宝室，里面有一个宝箱。",listOf("打开宝箱","无视"))
            "nothing"->{ nothingEvent(); r.isEventActive = false }
            "enemy"->{ generateEnemy(); addRealmLog("遭遇【${_combatState.value?.enemyName}】！",listOf("迎战","逃跑")); _player.value = _player.value.copy(inCombat = true) }
            "blessing"->{ if(Random.nextInt(2)==1){ val p=_player.value; if(p.blessing<1)p.blessing=1; val cost=(p.blessing*(500.0*(p.blessing*0.5))+750).toLong(); addRealmLog("发现悟道碑文！供奉${cost}两白银可获得祝福。（祝福Lv.${p.blessing}）",listOf("供奉","无视")) } else { nothingEvent(); r.isEventActive = false } }
            "curse"->{ if(Random.nextInt(3)==1){ val clvl=((r.enemyScaling-1f)*10).toInt(); val cost=(clvl*(10000.0*(clvl*0.5))+5000).toLong(); addRealmLog("发现魔道祭坛！献祭${cost}两白银可强化魔物。（魔染Lv.${clvl}）",listOf("献祭","无视")) } else { nothingEvent(); r.isEventActive = false } }
            "monarch"->{ if(Random.nextInt(7)==1) addRealmLog("前方传来恐怖的气息……似乎有绝世强者在此沉睡。",listOf("进入","避开")) else { nothingEvent(); r.isEventActive = false } }
        }
    }

    private fun nothingEvent() {
        addRealmLog(listOf("四处探索，空无一物……","发现一个空的宝箱。","发现一具妖兽尸骸。","发现一具枯骨。","这片区域早已被人搜刮干净。").random())
    }

    fun chooseOption(idx: Int) {
        val current = _realm.value; val r = current.copy()
        val last = _realmLog.value.lastOrNull() ?: return
        when {
            last.contains("迎战")&&idx==0->{ startCombat("battle"); addRealmLog("迎战！") }
            last.contains("逃跑")&&idx==1->{ if(Random.nextBoolean()){addRealmLog("成功逃脱！");_player.value = _player.value.copy(inCombat = false);r.isEventActive=false} else {addRealmLog("逃跑失败！");startCombat("battle")} }
            last.contains("打开宝箱")&&idx==0->chestEvent()
            last.contains("供奉")&&idx==0->{ val p=_player.value.copy(); if(p.blessing<1)p.blessing=1; val cost=(p.blessing*(500.0*(p.blessing*0.5))+750).toLong(); if(p.gold<cost){addRealmLog("银两不足。");soundManager.playSfx("denied")} else {p.gold-=cost;statBlessing();soundManager.playSfx("confirm"); _player.value = p}; r.isEventActive=false }
            last.contains("献祭")&&idx==0->{ val clvl=((r.enemyScaling-1f)*10).toInt(); val cost=(clvl*(10000.0*(clvl*0.5))+5000).toLong(); if(_player.value.gold<cost){addRealmLog("银两不足。");soundManager.playSfx("denied")} else { _player.value = _player.value.copy(gold = _player.value.gold - cost); r.enemyScaling+=0.1f; addRealmLog("魔物变强，战利品品质提升。（魔染Lv.${clvl}→${clvl+1}）");soundManager.playSfx("buff")}; r.isEventActive=false }
            last.contains("进入")&&idx==0->{ if(last.contains("至尊")||last.contains("绝世强者"))specialBossBattle() else if(r.room>=r.roomsPerFloor)guardianBattle() else roomTransition() }
            else->ignoreEvent()
        }
        _realm.value = r
    }

    private fun chestEvent() {
        soundManager.playSfx("confirm")
        when(Random.nextInt(4)){
            0->{generateEnemy("chest");addRealmLog("宝箱怪出现！");startCombat("battle")}
            1->{if(_realm.value.floor==1)goldDrop() else createEquipPrint();_realm.value = _realm.value.copy(isEventActive = false)}
            2->{goldDrop();_realm.value = _realm.value.copy(isEventActive = false)}
            3->{addRealmLog("宝箱是空的。");_realm.value = _realm.value.copy(isEventActive = false)}
        }
    }

    fun goldDrop() { soundManager.playSfx("sell"); val amt=Random.nextInt(50,500)*_realm.value.floor; _player.value = _player.value.copy(gold = _player.value.gold + amt); addRealmLog("获得${amt}两白银。") }

    private fun statBlessing() {
        soundManager.playSfx("buff"); val p=_player.value.copy(); if(p.blessing<1)p.blessing=1
        val s = mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f)
        val (k,v) = s.entries.random()
        when(k){"hp"->p.bonusStats.hp+=v;"atk"->p.bonusStats.atk+=v;"def"->p.bonusStats.def+=v;"atkSpd"->p.bonusStats.atkSpd+=v;"vamp"->p.bonusStats.vamp+=v;"critRate"->p.bonusStats.critRate+=v;"critDmg"->p.bonusStats.critDmg+=v}
        addRealmLog("祝福获得${k}+${v}%！（祝福Lv.${p.blessing}→${p.blessing+1}）"); p.blessing++
        calculateStats(); _player.value = p.copy()
    }

    private fun roomTransition() {
        val r = _realm.value.copy()
        when(Random.nextInt(3)){
            0->{incrementRoom();generateEnemy("door");addRealmLog("秘境假门！");startCombat("battle")}
            1->{incrementRoom();addRealmLog("进入新房间，发现宝箱！",listOf("打开宝箱","无视"));r.isEventActive=true}
            else->{r.isEventActive=false;incrementRoom();addRealmLog("进入了下一个房间。")}
        }
        _realm.value = r
    }

    private fun guardianBattle() { val r = _realm.value.copy(); incrementRoom(); generateEnemy("guardian"); startCombat("guardian"); addCombatLog("秘境护法【${_combatState.value?.enemyName}】挡住了去路！"); addRealmLog("进入了下一层。"); _realm.value = r }
    private fun specialBossBattle() { val r = _realm.value.copy(); generateEnemy("sboss"); startCombat("boss"); addCombatLog("武林至尊【${_combatState.value?.enemyName}】苏醒！"); addRealmLog("武林至尊【${_combatState.value?.enemyName}】苏醒！"); _realm.value = r }
    private fun ignoreEvent() { soundManager.playSfx("confirm"); addRealmLog("选择无视，继续前行。") }
    private fun incrementRoom() { val r = _realm.value.copy(); r.room++; r.actionCounter=0; if(r.room>r.roomsPerFloor){r.room=1;r.floor++}; _realm.value = r }

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
        _combatState.value=CombatState(enemyName=name,enemyLvl=lvl,enemyArchetype=arch.name,enemyHp=eHp,enemyHpMax=eHp,enemyAtk=eAtk,enemyDef=eDef,enemyAtkSpd=eSpd,enemyCritRate=eCr,enemyCritDmg=eCd,playerHp=p.stats.hp,playerHpMax=p.stats.hpMax,playerAtk=p.stats.atk,playerDef=p.stats.def,playerAtkSpd=p.stats.atkSpd,playerVamp=p.stats.vamp,playerCritRate=p.stats.critRate,playerCritDmg=p.stats.critDmg,expReward=expCalc.toInt(),goldReward=gold,hasDrop=drop)
    }

    private data class Quintuple(val a:String,val b:Int,val c:Int,val d:Int,val e:Float,val f:Float,val g:Float)

    // ===== Combat =====
    fun startCombat(bgmType:String){ val p=_player.value; _player.value = p.copy(inCombat = true); val bgm=if(bgmType=="boss")"boss" else if(bgmType=="guardian")"guardian" else "battle"; soundManager.playBgm(context,bgm); soundManager.playSfx("encounter"); _realm.value = _realm.value.copy(isEventActive = true) }

    fun playerAttack():Boolean {
        val cs=_combatState.value?:return false; val p=_player.value; if(!p.inCombat)return false
        soundManager.playSfx("attack")
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
        _dmgNumbers.value=_dmgNumbers.value+DmgNumber(dmg.toInt().toString(),isCrit); _enemyFlinch.value=true
        _combatState.value=cs.copy(); hpValidation()
        return cs.enemyHp>0&&cs.playerHp>0
    }

    fun enemyAttack():Boolean {
        val cs=_combatState.value?:return false; val p=_player.value; if(!p.inCombat)return false
        soundManager.playSfx("attack")
        var dmg=cs.enemyAtk*(cs.enemyAtk.toFloat()/(cs.enemyAtk+cs.playerDef)); dmg*=(0.9f+Random.nextFloat()*0.2f)
        if(Random.nextFloat()*100<cs.enemyCritRate)dmg=(dmg*(1f+cs.enemyCritDmg/100f)).toInt().toFloat() else dmg=dmg.toInt().toFloat()
        if(p.skills.contains("PALADIN_HEART"))dmg=(dmg*0.75f).toInt().toFloat()
        cs.playerHp=maxOf(0,cs.playerHp-dmg.toInt())
        if(p.skills.contains("AEGIS_THORNS"))cs.enemyHp=maxOf(0,cs.enemyHp-(dmg*0.15f).toInt())
        addCombatLog("${cs.enemyName}对${p.name}造成${dmg.toInt()}点伤害")
        _playerFlinch.value=true; p.stats.hp=cs.playerHp; _player.value = p.copy()
        _combatState.value=cs.copy(); hpValidation()
        return cs.enemyHp>0&&cs.playerHp>0
    }

    fun hpValidation() {
        val cs=_combatState.value?:return; val p=_player.value
        if(cs.playerHp<1){cs.playerHp=0;cs.playerDead=true;p.deaths++;addCombatLog("${p.name}身死道消……");endCombat()}
        else if(cs.enemyHp<1){cs.enemyHp=0;cs.enemyDead=true;p.kills++; val r=_realm.value; r.currentKills++; _realm.value=r.copy();addCombatLog("${cs.enemyName}被击败！");addCombatLog("获得${cs.expReward}修为。");playerExpGain(cs.expReward);addCombatLog("获得${cs.goldReward}两白银。");p.gold+=cs.goldReward;p.stats.hp+=(p.stats.hpMax*20/100);if(cs.hasDrop)createEquipPrint();calculateStats();endCombat();
            val cr=CultivationRealm.entries.find{it.name==p.realm}?:CultivationRealm.NONE; val nr=CultivationRealm.entries.getOrNull(cr.ordinal+1)
            if(nr!=null&&p.lvl>=nr.level*10+10&&p.kills>=nr.level*5){_realmBreakthroughPending.value=true;_realmBreakthroughInfo.value=Pair(cr.displayName,nr.displayName)}
            if(p.exp.lvlGained>0){_showLevelUp.value=true;lvlupPopup()}
        } else { cs.enemyHpPercent=(cs.enemyHp.toFloat()/cs.enemyHpMax*100f) }
        _player.value = p.copy(); _combatState.value=cs.copy()
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

    private fun endCombat() {
        soundManager.stopBgm(); soundManager.playSfx("combat_end"); val p=_player.value; _player.value = p.copy(inCombat = false)
        if(p.skills.contains("RAMPAGER")){p.baseStats.atk-=p.tempStats.atk.toInt();p.tempStats.atk=0f}
        if(p.skills.contains("BLADE_DANCE")){p.baseStats.atkSpd-=p.tempStats.atkSpd;p.tempStats.atkSpd=0f}
        calculateStats(); _realm.value = _realm.value.copy(isEventActive = false)
    }

    fun dismissCombatResult() { _combatState.value=null; if(!_realm.value.isPaused) _realm.value = _realm.value.copy(isExploring = true) }

    private fun lvlupPopup() {
        soundManager.playSfx("level_up"); val p=_player.value
        addCombatLog("境界提升！（Lv.${p.lvl-p.exp.lvlGained}→Lv.${p.lvl}）")
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
        soundManager.playSfx("item_use"); p.exp.lvlGained--
        if(p.exp.lvlGained>0){_availableUpgrades.value=emptyList(); generateLvlStats(2,mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f))}
        else {_showLevelUp.value=false; _availableUpgrades.value=emptyList(); _rerollsLeft.value=0; p.exp.lvlGained=0 }
        calculateStats(); _player.value = p.copy(); saveGame()
    }

    fun rerollUpgrades():Boolean {
        if(_rerollsLeft.value<=0)return false; _rerollsLeft.value=_rerollsLeft.value-1; soundManager.playSfx("sell")
        generateLvlStats(_rerollsLeft.value,mapOf("hp" to 10f,"atk" to 8f,"def" to 8f,"atkSpd" to 3f,"vamp" to 0.5f,"critRate" to 1f,"critDmg" to 6f))
        return true
    }

    private fun statKeyToKey(s:String)=when(s){"气血"->"hp";"攻击"->"atk";"防御"->"def";"身法"->"atkSpd";"吸血"->"vamp";"暴击率"->"critRate";"暴击伤害"->"critDmg";else->s}
    private fun statDisplay(k:String)=when(k){"hp"->"气血";"atk"->"攻击";"def"->"防御";"atkSpd"->"身法";"vamp"->"吸血";"critRate"->"暴率";"critDmg"->"暴伤";else->k}

    fun confirmBreakthrough() {
        val nr=CultivationRealm.entries.find{it.displayName==_realmBreakthroughInfo.value?.second}?:return
        val p=_player.value; p.realm=nr.name; p.stats.hp=p.stats.hpMax
        addRealmLog("🎉境界突破！踏入【${nr.displayName}】！"); soundManager.playSfx("level_up"); calculateStats(); _player.value = p.copy(); saveGame()
        _realmBreakthroughPending.value=false; _realmBreakthroughInfo.value=null
    }
    fun dismissBreakthrough() { _realmBreakthroughPending.value=false; _realmBreakthroughInfo.value=null }

    // ===== Equipment =====
    fun createEquipPrint(condition:String="dungeon"):EquipmentItem {
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

        val statTypes=when{attr==EquipmentAttribute.DAMAGE->when(type.displayName){"巨斧","钩镰"->listOf("atk","atk","vamp","critRate","critDmg","critDmg");"匕首","软鞭"->listOf("atkSpd","atkSpd","atk","vamp","critRate","critRate","critDmg");"重锤"->listOf("hp","def","atk","atk","critRate","critDmg");else->listOf("atk","atkSpd","vamp","critRate","critDmg")}
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

        val equip=EquipmentItem(category=type.displayName,attribute=attr.name,type=when(type.attr){EquipmentAttribute.DAMAGE->"兵器";else->{if(type.displayName.contains("甲"))"护甲" else if(type.displayName.contains("盾"))"盾牌" else "头盔"}},rarity=rarity.displayName,lvl=lvl,tier=tier,value=sellVal,stats=stats)

        val inv=parseInventory().toMutableList(); inv.add(equip); _player.value=_player.value.copy(inventory=gson.toJson(inv))
        return equip
    }

    private fun equipmentIcon(cat:String)=when(cat){"长剑"->"🗡️";"巨斧"->"🪓";"重锤"->"🔨";"匕首"->"🔪";"软鞭"->"⛓️";"钩镰"->"🔱";"板甲"->"🛡️";"锁子甲"->"🛡️";"皮甲"->"🦺";"塔盾"->"🛡️";"鸢盾"->"🛡️";"圆盾"->"🛡️";"重盔"->"⛑️";"角盔"->"👑";else->"📦"}

    // ===== Inventory ops =====
    fun equipItem(idx:Int):Boolean {
        val inv=parseInventory().toMutableList(); if(idx>=inv.size)return false
        val equipped=parseEquipped().toMutableList(); if(equipped.size>=6){soundManager.playSfx("denied");return false}
        val item=inv.removeAt(idx); equipped.add(item)
        _player.value=_player.value.copy(inventory=gson.toJson(inv),equipped=gson.toJson(equipped))
        soundManager.playSfx("equip"); calculateStats(); saveGame(); return true
    }
    fun unequipItem(idx:Int):Boolean {
        val equipped=parseEquipped().toMutableList(); if(idx>=equipped.size)return false
        val inv=parseInventory().toMutableList(); val item=equipped.removeAt(idx); inv.add(item)
        _player.value=_player.value.copy(inventory=gson.toJson(inv),equipped=gson.toJson(equipped))
        soundManager.playSfx("unequip"); calculateStats(); saveGame(); return true
    }
    fun sellItem(isEquipped:Boolean,idx:Int):Boolean {
        val p=_player.value
        if(isEquipped){val e=parseEquipped().toMutableList();if(idx>=e.size)return false;val item=e.removeAt(idx);p.gold+=item.value;_player.value=p.copy(equipped=gson.toJson(e))}
        else{val i=parseInventory().toMutableList();if(idx>=i.size)return false;val item=i.removeAt(idx);p.gold+=item.value;_player.value=p.copy(inventory=gson.toJson(i))}
        soundManager.playSfx("sell"); calculateStats(); saveGame(); return true
    }
    fun sellAll(rarity:String) {
        val p=_player.value
        if(rarity=="全部"){val inv=parseInventory().toMutableList(); if(inv.isEmpty()){soundManager.playSfx("denied");return}
            for(item in inv){p.gold+=item.value}; _player.value=p.copy(inventory="[]"); soundManager.playSfx("sell")}
        else{val inv=parseInventory().toMutableList(); val toSell=inv.filter{it.rarity==rarity}; if(toSell.isEmpty()){soundManager.playSfx("denied");return}
            for(item in toSell){p.gold+=item.value}; inv.removeAll(toSell); _player.value=p.copy(inventory=gson.toJson(inv)); soundManager.playSfx("sell")}
        calculateStats(); saveGame()
    }
    fun unequipAll() { val eq=parseEquipped().toMutableList(); val inv=parseInventory().toMutableList(); inv.addAll(eq); _player.value=_player.value.copy(equipped="[]",inventory=gson.toJson(inv)); calculateStats(); saveGame() }

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
    fun loadGame():Boolean {
        val pj=prefs.getString("player",null)?:return false; val rj=prefs.getString("realm",null)?:return false
        try{_player.value=gson.fromJson(pj,PlayerEntity::class.java); _realm.value=gson.fromJson(rj,RealmState::class.java).copy(isExploring=false,isPaused=true,isEventActive=false); return true}
        catch(_:Exception){return false}
    }
    fun hasSave()=prefs.contains("player")
    fun deleteSave(){prefs.edit().clear().apply()}
}