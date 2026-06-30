package com.arktools.anlao.adsdk

import android.app.Application
import com.tosin.sdk.initsdk.init.CustomController
import com.tosin.sdk.initsdk.init.InitListener
import com.tosin.sdk.initsdk.init.TosinInitConfig
import com.tosin.sdk.initsdk.init.TosinSDK

/**
 * Tosin 广告 SDK 初始化管理器
 * 在 Application.onCreate 或用户同意隐私政策后调用 init()
 */
class TosinAdInitializer private constructor() {

    companion object {
        @Volatile
        private var instance: TosinAdInitializer? = null

        fun getInstance(): TosinAdInitializer {
            return instance ?: synchronized(this) {
                instance ?: TosinAdInitializer().also { instance = it }
            }
        }

        @Volatile
        var isSdkInitialized: Boolean = false
            private set
    }

    /**
     * 初始化 Tosin SDK
     * @param application Application 实例
     * @param listener 初始化回调
     */
    fun init(application: Application, listener: InitListener? = null) {
        if (isSdkInitialized) {
            listener?.onInitSuccess()
            return
        }

        if (AdSdkConfig.appId == 0L) {
            throw IllegalStateException("AdSdkConfig.appId 未配置，请先调用 AdSdkConfig.configure()")
        }

        // 隐私合规：通过 CustomController 关闭广告 SDK 对 IMEI 等敏感个人信息的采集
        val config = TosinInitConfig.Builder()
            .appId(AdSdkConfig.appId)
            .isDebug(AdSdkConfig.isDebug)
            .customController(object : CustomController() {
                override fun canUsePhoneState(): Boolean = false
                override fun canUseMacAddress(): Boolean = false
                override fun canReadLocation(): Boolean = false
                override fun canGetInstallPackages(): Boolean = false
                override fun canUsePermissionRecordAudio(): Boolean = false
                override fun canUseOaid(): Boolean = true
                override fun canUseAndroidId(): Boolean = true
                override fun canUseWifiState(): Boolean = true
            })
            .build()

        TosinSDK.instance.init(application, config, object : InitListener {
            override fun onInitFail(fail: String?) {
                // Tosin SDK 1.1.2 内部会尝试读取一个不存在的 String resource（Resources$NotFoundException）
                // 这是 SDK 自身的 bug，不影响后续广告加载。
                // 标记为已初始化，避免反复重试。
                isSdkInitialized = true
                listener?.onInitFail(fail)
            }

            override fun onInitSuccess() {
                isSdkInitialized = true
                listener?.onInitSuccess()
            }
        })
    }
}
