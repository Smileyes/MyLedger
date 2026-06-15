package com.utopia.finance

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.utopia.finance.ui.FinanceApp
import com.utopia.finance.ui.theme.FinanceTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var isAuthenticated by mutableStateOf(false)
    private var biometricEnabled by mutableStateOf<Boolean?>(null)
    private var authMessage by mutableStateOf("请先验证指纹")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as FinanceApplication).container.settingsRepository

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.biometricEnabled.collect { enabled ->
                    biometricEnabled = enabled
                    if (!enabled) {
                        isAuthenticated = true
                    } else if (!isAuthenticated) {
                        authenticate()
                    }
                }
            }
        }

        setContent {
            FinanceTheme {
                when {
                    biometricEnabled == null -> SecurityLoadingScreen()
                    biometricEnabled == false || isAuthenticated -> FinanceApp()
                    else -> LockedFinanceScreen(authMessage = authMessage, onAuthenticate = ::authenticate)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        isAuthenticated = false
    }

    private fun authenticate() {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        val canAuthenticate = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            authMessage = "当前设备未启用可用的指纹或锁屏凭据"
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticated = true
                    authMessage = "已验证"
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    authMessage = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    authMessage = "验证失败，请重试"
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("验证身份")
                .setSubtitle("解锁个人财务数据")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}

@Composable
private fun SecurityLoadingScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("个人财务", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("正在读取安全设置", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LockedFinanceScreen(authMessage: String, onAuthenticate: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("个人财务", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(authMessage, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAuthenticate, modifier = Modifier.padding(top = 24.dp)) {
                Text("验证指纹")
            }
        }
    }
}
