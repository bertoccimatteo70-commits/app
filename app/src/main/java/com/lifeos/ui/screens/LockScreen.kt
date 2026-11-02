package com.lifeos.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lifeos.security.BiometricUnlock
import com.lifeos.ui.LifeOSViewModel
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    viewModel: LifeOSViewModel,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    var biometricAvailable by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        biometricAvailable = biometricEnabled && BiometricUnlock.isAvailable(context)
        if (biometricAvailable) {
            delay(250)
            context.findFragmentActivity()?.let { activity ->
                BiometricUnlock.prompt(
                    activity = activity,
                    onSuccess = { onUnlocked() },
                    onError = { message -> error = message },
                    onCancel = { }
                )
            }
        }
    }

    fun submit() {
        if (pin.length < 4 || busy) return
        busy = true
        if (viewModel.verifyPin(pin)) {
            onUnlocked()
        } else {
            attempts++
            error = "PIN errato ($attempts)"
            pin = ""
            busy = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("LifeOS è bloccata", style = MaterialTheme.typography.titleLarge)
            Text(
                "Inserisci il PIN per continuare",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (index < pin.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error ?: " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))
            keypad(
                onDigit = { digit ->
                    if (pin.length < 8) {
                        pin += digit
                        error = null
                    }
                },
                onDelete = { pin = pin.dropLast(1) },
                onConfirm = { submit() },
                canConfirm = pin.length >= 4
            )

            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    context.findFragmentActivity()?.let { activity ->
                        BiometricUnlock.prompt(
                            activity = activity,
                            onSuccess = { onUnlocked() },
                            onError = { message -> error = message },
                            onCancel = { }
                        )
                    }
                }) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Usa impronta o volto")
                }
            }
        }
    }
}

@Composable
private fun keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    canConfirm: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit ->
                    Button(
                        onClick = { onDigit(digit) },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape
                    ) { Text(digit, style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDelete, modifier = Modifier.size(72.dp)) {
                Icon(Icons.Default.Backspace, contentDescription = "Cancella cifra")
            }
            Button(
                onClick = { onDigit("0") },
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) { Text("0", style = MaterialTheme.typography.titleLarge) }
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) { Text("OK") }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
