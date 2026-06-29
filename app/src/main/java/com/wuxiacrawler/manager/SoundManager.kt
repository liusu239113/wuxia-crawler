package com.wuxiacrawler.manager

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("sound_settings", Context.MODE_PRIVATE)
    private val soundPool: SoundPool
    private val sfxMap = mutableMapOf<String, Int>()
    private var bgmPlayer: MediaPlayer? = null
    private var currentBgmType: String? = null
    private var currentLoop: Boolean = true
    private var isMuted = MutableStateFlow(prefs.getBoolean("muted", false))
    private var bgmVolume = MutableStateFlow(prefs.getFloat("bgm_volume", 0.5f))
    private var sfxVolume = MutableStateFlow(prefs.getFloat("sfx_volume", 0.7f))

    val muted: StateFlow<Boolean> = isMuted.asStateFlow()
    val bgmLevel: StateFlow<Float> = bgmVolume.asStateFlow()
    val sfxLevel: StateFlow<Float> = sfxVolume.asStateFlow()

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()

        val sfxFiles = mapOf(
            "sword_slash" to "sfx/sword_slash.ogg",
            "qi_flow" to "sfx/qi_flow.ogg",
            "victory_chime" to "sfx/victory_chime.ogg",
            "wood_confirm" to "sfx/wood_confirm.ogg",
            "decline" to "sfx/decline.wav",
            "blocked" to "sfx/blocked.ogg",
            "enemy_appears" to "sfx/enemy_appears.ogg",
            "equip_blade" to "sfx/equip_blade.ogg",
            "hover" to "sfx/hover.wav",
            "scroll_open" to "sfx/scroll_open.ogg",
            "realm_breakthrough" to "sfx/realm_breakthrough.ogg",
            "bell_pause" to "sfx/bell_pause.ogg",
            "coin_pouch" to "sfx/coin_pouch.ogg",
            "sheath_blade" to "sfx/sheath_blade.ogg",
            "gong_start" to "sfx/gong_start.ogg"
        )

        sfxFiles.forEach { (name, path) ->
            try {
                val afd: AssetFileDescriptor = appContext.assets.openFd(path)
                sfxMap[name] = soundPool.load(afd, 1)
            } catch (_: Exception) {}
        }
    }

    fun playSfx(name: String) {
        if (isMuted.value) return
        sfxMap[name]?.let { soundPool.play(it, sfxVolume.value, sfxVolume.value, 1, 0, 1f) }
    }

    fun playBgm(context: Context, type: String, loop: Boolean = true) {
        currentBgmType = type
        currentLoop = loop
        if (isMuted.value) return
        stopBgm(keepCurrent = true)
        val path = when (type) {
            "jianghu", "dungeon" -> "bgm/jianghu_secret_realm.ogg"
            "duel", "duel_balanced", "battle" -> "bgm/battle_balanced_sword_rain.ogg"
            "duel_offensive" -> "bgm/battle_offensive_iron_mountain.ogg"
            "duel_defensive" -> "bgm/battle_defensive_golden_bell.ogg"
            "duel_quick" -> "bgm/battle_quick_shadow_step.ogg"
            "duel_lethal" -> "bgm/battle_lethal_blood_moon.ogg"
            "duel_trap" -> "bgm/battle_trap_mechanism.ogg"
            "duel_guardian", "guardian" -> "bgm/battle_guardian_secret_realm.ogg"
            "duel_boss", "boss" -> "bgm/battle_boss_martial_supreme.ogg"
            else -> null
        } ?: return
        try {
            val afd = context.assets.openFd(path)
            bgmPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = loop
                setVolume(bgmVolume.value, bgmVolume.value)
                prepare()
                start()
            }
        } catch (_: Exception) {}
    }

    fun stopBgm() {
        stopBgm(keepCurrent = false)
    }

    private fun stopBgm(keepCurrent: Boolean) {
        bgmPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        bgmPlayer = null
        if (!keepCurrent) currentBgmType = null
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        prefs.edit().putBoolean("muted", isMuted.value).apply()
        if (isMuted.value) stopBgm(keepCurrent = true)
        else currentBgmType?.let { playBgm(appContext, it, currentLoop) }
    }

    fun setBgmVolume(value: Float) {
        bgmVolume.value = value.coerceIn(0f, 1f)
        prefs.edit().putFloat("bgm_volume", bgmVolume.value).apply()
        bgmPlayer?.setVolume(bgmVolume.value, bgmVolume.value)
    }

    fun setSfxVolume(value: Float) {
        sfxVolume.value = value.coerceIn(0f, 1f)
        prefs.edit().putFloat("sfx_volume", sfxVolume.value).apply()
    }

    fun release() {
        stopBgm()
        soundPool.release()
    }
}
