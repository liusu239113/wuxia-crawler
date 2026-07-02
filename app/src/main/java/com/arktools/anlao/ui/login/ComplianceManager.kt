package com.arktools.anlao.ui.login

import android.app.Activity
import android.util.Log
import com.taptap.sdk.compliance.TapTapCompliance
import com.taptap.sdk.compliance.TapTapComplianceCallback
import com.taptap.sdk.compliance.constants.ComplianceMessage
import com.taptap.sdk.kit.internal.callback.TapTapCallback
import com.taptap.sdk.kit.internal.exception.TapTapException
import com.taptap.sdk.compliance.bean.CheckPaymentResult
import kotlinx.serialization.json.JsonElement

/**
 * TapTap 防沉迷管理
 */
object ComplianceManager {

    private const val TAG = "ComplianceManager"

    /** 防沉迷认证结果回调 */
    interface ComplianceListener {
        /** 认证成功（code=500），可进入游戏 */
        fun onLoginSuccess()
        /** 退出认证（code=1000），游戏应返回登录页 */
        fun onExited()
        /** 切换账号（code=1001），游戏应返回登录页 */
        fun onSwitchAccount()
        /** 宵禁限制（code=1030），不可进入游戏 */
        fun onPeriodRestrict()
        /** 无可玩时长（code=1050），不可进入游戏 */
        fun onDurationLimit()
        /** 年龄限制（code=1100），不可进入游戏 */
        fun onAgeLimit()
        /** 实名过程中关闭窗口（code=9002） */
        fun onRealNameStop()
        /** 网络错误（code=1200） */
        fun onError(message: String)
    }

    private var listener: ComplianceListener? = null

    /**
     * 注册回调（在登录前调用）
     */
    fun register(listener: ComplianceListener) {
        this.listener = listener
        TapTapCompliance.registerComplianceCallback(
            callback = object : TapTapComplianceCallback {
                override fun onComplianceResult(code: Int, extra: Map<String, Any>?) {
                    Log.d(TAG, "onComplianceResult: code=$code, extra=$extra")
                    when (code) {
                        ComplianceMessage.LOGIN_SUCCESS -> {
                            listener.onLoginSuccess()
                        }
                        ComplianceMessage.EXITED -> {
                            listener.onExited()
                        }
                        ComplianceMessage.SWITCH_ACCOUNT -> {
                            listener.onSwitchAccount()
                        }
                        ComplianceMessage.PERIOD_RESTRICT -> {
                            listener.onPeriodRestrict()
                        }
                        ComplianceMessage.DURATION_LIMIT -> {
                            listener.onDurationLimit()
                        }
                        ComplianceMessage.INVALID_CLIENT_OR_NETWORK_ERROR -> {
                            listener.onError("数据请求失败，请检查网络连接")
                        }
                        ComplianceMessage.REAL_NAME_STOP -> {
                            listener.onRealNameStop()
                        }
                        else -> {
                            if (code == 1100) {
                                listener.onAgeLimit()
                            } else {
                                Log.w(TAG, "Unknown compliance code: $code")
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * 开始防沉迷认证
     */
    fun startup(activity: Activity, userId: String) {
        Log.d(TAG, "startup: userId=$userId")
        TapTapCompliance.startup(activity, userId)
    }

    /**
     * 充值前检查
     */
    fun checkPaymentLimit(
        activity: Activity,
        amount: Int,
        onAllowed: () -> Unit,
        onError: (String) -> Unit
    ) {
        TapTapCompliance.checkPaymentLimit(
            activity,
            amount,
            object : TapTapCallback<CheckPaymentResult> {
                override fun onSuccess(result: CheckPaymentResult) {
                    if (result.status) {
                        onAllowed()
                    }
                }
                override fun onFail(exception: TapTapException) {
                    onError(exception.message ?: "检查失败")
                }
            }
        )
    }

    /**
     * 上报充值金额
     */
    fun submitPayment(amount: Int) {
        TapTapCompliance.submitPayment(
            amount,
            object : TapTapCallback<JsonElement> {
                override fun onSuccess(result: JsonElement) {
                    Log.d(TAG, "submitPayment success: amount=$amount")
                }
                override fun onFail(exception: TapTapException) {
                    Log.e(TAG, "submitPayment failed: ${exception.message}")
                }
            }
        )
    }

    /**
     * 获取玩家年龄段
     * @return -1未知, 0(0-7), 8(8-15), 16(16-17), 18(成年)
     */
    fun getAgeRange(): Int = TapTapCompliance.getAgeRange()

    /**
     * 获取剩余可玩时长（秒）
     */
    fun getRemainingTime(): Int = TapTapCompliance.getRemainingTime()

    /**
     * 退出登录时调用，重置防沉迷状态
     */
    fun exit() {
        TapTapCompliance.exit()
    }
}
