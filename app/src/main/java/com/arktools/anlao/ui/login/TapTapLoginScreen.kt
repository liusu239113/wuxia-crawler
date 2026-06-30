package com.arktools.anlao.ui.login

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taptap.sdk.login.TapTapLogin
import com.taptap.sdk.login.TapTapAccount
import com.taptap.sdk.login.Scopes
import com.taptap.sdk.kit.internal.callback.TapTapCallback
import com.taptap.sdk.kit.internal.exception.TapTapException
import kotlinx.coroutines.delay

// 武侠风格配色
private val WuxiaGold = Color(0xFFD4A853)
private val WuxiaBgDark = Color(0xFF1A120C)

/**
 * TapTap 登录界面
 * 在进入游戏前要求用户登录 TapTap 账号
 */
@Composable
fun TapTapLoginScreen(
    onLoginSuccess: (TapTapAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var logoVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    // 进入动画
    LaunchedEffect(Unit) {
        delay(200)
        logoVisible = true
        delay(400)
        buttonVisible = true
    }

    // 自动检查登录状态
    LaunchedEffect(Unit) {
        val currentAccount = TapTapLogin.getCurrentTapAccount()
        if (currentAccount != null) {
            onLoginSuccess(currentAccount)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 深色背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(WuxiaBgDark)
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo 动画
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -100 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚔",
                        fontSize = 72.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暗牢江湖行",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = WuxiaGold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "登录 TapTap 账号踏入江湖",
                        fontSize = 14.sp,
                        color = Color(0xFF8B7355),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // 登录按钮
            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 50 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoggingIn) {
                        Box(
                            modifier = Modifier.size(48.dp)
                                .border(3.dp, WuxiaGold, CircleShape)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .background(WuxiaGold.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在踏入江湖...",
                            fontSize = 14.sp,
                            color = Color(0xFF8B7355)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (activity == null) return@Button
                                isLoggingIn = true
                                loginError = null

                                val scopes = arrayOf(Scopes.SCOPE_PUBLIC_PROFILE)
                                try {
                                    TapTapLogin.loginWithScopes(
                                        activity,
                                        scopes,
                                        object : TapTapCallback<TapTapAccount> {
                                            override fun onSuccess(result: TapTapAccount) {
                                                isLoggingIn = false
                                                onLoginSuccess(result)
                                            }

                                            override fun onCancel() {
                                                isLoggingIn = false
                                                loginError = "登录已取消"
                                            }

                                            override fun onFail(exception: TapTapException) {
                                                isLoggingIn = false
                                                loginError = "登录失败: ${exception.message}"
                                            }
                                        }
                                    )
                                } catch (e: Exception) {
                                    isLoggingIn = false
                                    loginError = "登录服务不可用，请安装或更新 TapTap 客户端"
                                    android.util.Log.e("TapTapLogin", "loginWithScopes crashed", e)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WuxiaGold
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "TapTap 登录",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A120C)
                            )
                        }

                        // 错误提示
                        val capturedLoginError = loginError
                        if (capturedLoginError != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = capturedLoginError,
                                fontSize = 13.sp,
                                color = Color(0xFFE53935),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
