package com.jianzhang.app.ui.lock

import com.jianzhang.app.ui.components.appShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.model.AuthStatus
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.HudBackground
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch

@Composable
fun LockScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val setup = vm.authStatus == AuthStatus.SETUP

    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resetArm by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        HudBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(appShape(16.dp))
                    .shadow(14.dp, appShape(16.dp), ambientColor = palette.accent.copy(alpha = 0.45f))
                    .background(Brush.linearGradient(listOf(palette.accent, Color(0xFF00859C)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFF00171D),
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                if (setup) "设置解锁密码" else "简账已锁定",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                letterSpacing = 1.5.sp,
                style = TextStyle(shadow = hudGlow(palette.accent, 0.35f, 16f)),
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                if (setup) "首次使用请设置密码（至少 4 位），密码只存在本机、无法找回"
                else "输入密码解锁，账目数据才会显示",
                fontSize = 13.sp,
                color = palette.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 26.dp),
            )

            PasswordField(
                value = pwd,
                onValueChange = { pwd = it },
                show = showPwd,
                onToggleShow = { showPwd = !showPwd },
                placeholder = if (setup) "设置密码（至少 4 位）" else "请输入密码",
            )
            if (setup) {
                PasswordField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    show = showPwd,
                    onToggleShow = { showPwd = !showPwd },
                    placeholder = "再次输入密码确认",
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            error?.let {
                Text(
                    it,
                    fontSize = 12.5.sp,
                    color = palette.danger,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(50.dp)
                    .clip(appShape(10.dp))
                    .shadow(10.dp, appShape(10.dp), ambientColor = palette.accent.copy(alpha = 0.4f))
                    .background(Brush.linearGradient(listOf(palette.accent, palette.accentHover)))
                    .clickable {
                        when {
                            setup -> {
                                if (pwd.length < 4) {
                                    error = "密码至少需要 4 位"
                                } else if (pwd != confirm) {
                                    error = "两次输入的密码不一致"
                                } else {
                                    vm.setPassword(pwd)
                                }
                            }
                            else -> {
                                if (pwd.isEmpty()) {
                                    error = "请输入密码"
                                } else if (!vm.unlock(pwd)) {
                                    error = "密码不正确"
                                    pwd = ""
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("确定", color = Color(0xFF00171D), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }

            if (!setup) {
                Column(
                    Modifier.padding(top = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!resetArm) {
                        Text(
                            "忘记密码？",
                            fontSize = 13.sp,
                            color = palette.text3,
                            modifier = Modifier
                                .clip(appShape(6.dp))
                                .clickable { resetArm = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    } else {
                        Text(
                            "将清空本机全部账目数据并重新设置密码，确定继续？",
                            fontSize = 12.5.sp,
                            color = palette.danger,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        Row(
                            Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Text(
                                "取消",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.text2,
                                modifier = Modifier
                                    .clip(appShape(6.dp))
                                    .clickable { resetArm = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                            Text(
                                "确认清空并重置",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.danger,
                                modifier = Modifier
                                    .clip(appShape(6.dp))
                                    .border(1.dp, palette.danger.copy(alpha = 0.4f), appShape(6.dp))
                                    .clickable {
                                        scope.launch {
                                            try {
                                                vm.resetAll()
                                            } catch (e: Exception) {
                                                error = "重置失败：${e.message ?: "未知错误"}"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    show: Boolean,
    onToggleShow: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val shape = appShape(8.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.bg1)
            .border(1.dp, palette.border, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            textStyle = TextStyle(color = palette.text, fontSize = 15.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = palette.text3, fontSize = 15.sp)
                    inner()
                }
            },
        )
        Box(
            Modifier.size(42.dp).clip(appShape(8.dp)).clickable(onClick = onToggleShow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (show) "隐藏密码" else "显示密码",
                tint = palette.text3,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}