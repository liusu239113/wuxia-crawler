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
    private val soundPool: SoundPool
    private val sfxMap = mutableMapOf<String, Int>()
    private var bgmPlayer: MediaPlayer? = null
    private var isMuted = MutableStateFlow(false)

    val muted: StateFlow<Boolean> = isMuted.asStateFlow()

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()

        // 加载所有SFX
        listOf(
            "sfx/attack.wav", "sfx/buff.wav", "sfx/combat_end.wav",
            "sfx/confirm.wav", "sfx/decline.wav", "sfx/denied.wav",
            "sfx/encounter.wav", "sfx/equip.wav", "sfx/hover.wav",
            "sfx/item_use.wav", "sfx/level_up.wav", "sfx/pause.wav",
            "sfx/sell.wav", "sfx/unequip.wav", "sfx/unpause.wav"
        ).forEach { path ->
            try {
                val afd: AssetFileDescriptor = context.assets.openFd(path)
                val id = soundPool.load(afd, 1)
                sfxMap[path.substringAfterLast("/").substringBefore(".")] = id
            } catch (_: Exception) {}
        }
    }

    fun playSfx(name: String) {
        if (isMuted.value) return
        sfxMap[name]?.let { soundPool.play(it, 0.7f, 0.7f, 1, 0, 1f) }
    }

    fun playBgm(context: Context, type: String, loop: Boolean = true) {
        if (isMuted.value) return
        stopBgm()
        val path = when (type) {
            "dungeon" -> "bgm/dungeon.mp3"
            "battle" -> "bgm/battle_main.mp3"
            "guardian" -> "bgm/battle_guardian.mp3"
            "boss" -> "bgm/battle_boss.mp3"
            else -> null
        } ?: return
        try {
            val afd = context.assets.openFd(path)
            bgmPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = loop
                setVolume(0.5f, 0.5f)
                prepare()
                start()
            }
        } catch (_: Exception) {}
    }

    fun stopBgm() {
        bgmPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        bgmPlayer = null
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        if (isMuted.value) stopBgm() else bgmPlayer?.start()
    }

    fun release() {
        stopBgm()
        soundPool.release()
    }
}