package com.antispam.blocker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antispam.blocker.data.BlockRuleManager
import com.antispam.blocker.data.BlockedCallsRepository
import com.antispam.blocker.ui.theme.*
import com.antispam.blocker.util.PhoneNumberHelper

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    ruleManager: BlockRuleManager,
    repository: BlockedCallsRepository,
    isRoleGranted: Boolean,
    onRequestRole: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(ruleManager.isProtectionEnabled) }
    var prefixes by remember { mutableStateOf(ruleManager.getBlockedPrefixes()) }
    var history by remember { mutableStateOf(repository.getBlockedCalls()) }
    var totalBlocked by remember { mutableStateOf(repository.getTotalBlockedCount()) }

    var newPrefixText by remember { mutableStateOf("") }
    var testNumberText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var rejectImmediately by remember { mutableStateOf(ruleManager.rejectCallImmediately) }
    var keepInLog by remember { mutableStateOf(ruleManager.keepInSystemCallLog) }

    fun refreshState() {
        prefixes = ruleManager.getBlockedPrefixes()
        history = repository.getBlockedCalls()
        totalBlocked = repository.getTotalBlockedCount()
    }

    // Actualizar automáticamente cuando la app vuelve al primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Actualizar en tiempo real cuando el servicio en segundo plano guarde una llamada cortada
    DisposableEffect(repository) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            refreshState()
        }
        repository.registerListener(listener)
        onDispose {
            repository.unregisterListener(listener)
        }
    }

    val subtleTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBgColor = MaterialTheme.colorScheme.surface
    val chipBgColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isEnabled && isRoleGranted) Emerald500 else Red500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEnabled && isRoleGranted) Icons.Default.Shield else Icons.Default.GppBad,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "anti-spam",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnabled && isRoleGranted) "Protección activa" else "Requiere permiso de llamadas",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEnabled && isRoleGranted) Emerald500 else Red500
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Descarga anti-spam v1.1.0 para bloquear llamadas molestas (prefijos 600 y 80):\nhttps://github.com/Pablo-Millones/AntiSpam-600-80/releases/download/v1.1.0/anti-spam-v1.1.0.apk"
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir anti-spam"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir enlace de descarga del APK",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Tarjeta de Estado del Rol en Android
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRoleGranted) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRoleGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isRoleGranted) Emerald500 else Red500,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRoleGranted) "Filtro Oficial Activo" else "Permiso de Android Requerido",
                                fontWeight = FontWeight.Bold,
                                color = if (isRoleGranted) Emerald500 else Red500,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRoleGranted)
                                "Tu teléfono ha asignado a anti-spam como filtro oficial. Las llamadas de números 600 y 80 se colgarán en segundo plano."
                            else
                                "Para que Android permita colgar llamadas no deseadas antes de que suenen, presiona el botón inferior y selecciona anti-spam como 'Filtro de llamadas y spam'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!isRoleGranted) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onRequestRole,
                                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Activar Filtro en el Sistema", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Interruptor Principal y Estadísticas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Bloqueador Automático",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEnabled) "Cortando llamadas de spam" else "Pausado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isEnabled) Emerald500 else subtleTextColor
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    isEnabled = checked
                                    ruleManager.isProtectionEnabled = checked
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalBlocked",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    color = Red500
                                )
                                Text("Llamadas cortadas", style = MaterialTheme.typography.labelSmall, color = subtleTextColor)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${prefixes.size}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    color = Indigo500
                                )
                                Text("Prefijos activos", style = MaterialTheme.typography.labelSmall, color = subtleTextColor)
                            }
                        }
                    }
                }
            }

            // 3. Configuración de Prefijos Bloqueados
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prefijos Bloqueados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = {
                                    ruleManager.restoreDefaults()
                                    refreshState()
                                }
                            ) {
                                Text("Por defecto (600, 80)", fontSize = 12.sp, color = Emerald500)
                            }
                        }

                        Text(
                            text = "Toda llamada cuyo número comience con estos dígitos será rechazada (cubre +56 600, 600, +56 80, 800, 809, etc.):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtleTextColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chips con prefijos activos con fondo de buen contraste
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            prefixes.forEach { prefix ->
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = if (prefix == "600") "600 (Servicios/Promo)" else if (prefix == "80") "80 (800, 809, 80x...)" else "Prefijo: $prefix",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = null,
                                            tint = Red500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                ruleManager.removePrefix(prefix)
                                                refreshState()
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = chipBgColor
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Input para agregar un nuevo prefijo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPrefixText,
                                onValueChange = { if (it.length <= 8) newPrefixText = it },
                                label = { Text("Nuevo prefijo (ej: 800, 44)") },
                                placeholder = { Text("Ej: 44") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newPrefixText.isNotBlank()) {
                                            ruleManager.addPrefix(newPrefixText)
                                            newPrefixText = ""
                                            focusManager.clearFocus()
                                            refreshState()
                                        }
                                    }
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPrefixText.isNotBlank()) {
                                        ruleManager.addPrefix(newPrefixText)
                                        newPrefixText = ""
                                        focusManager.clearFocus()
                                        refreshState()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // 4. Simulador / Probador de Números
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null, tint = Indigo500)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Probar Número",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Escribe un número para probar en tiempo real si sería colgado:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtleTextColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = testNumberText,
                            onValueChange = { testNumberText = it },
                            label = { Text("Número telefónico") },
                            placeholder = { Text("Ej: +56 600 300 4000 o 80234567") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (testNumberText.isNotEmpty()) {
                                    IconButton(onClick = { testNumberText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            }
                        )

                        if (testNumberText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val testResult = PhoneNumberHelper.checkIsBlocked(testNumberText, prefixes)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (testResult.isBlocked) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFF064E3B).copy(alpha = 0.6f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (testResult.isBlocked) Icons.Default.CallEnd else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (testResult.isBlocked) Red500 else Emerald500,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (testResult.isBlocked) "¡SERÍA BLOQUEADO Y COLGADO!" else "Llamada Permitida",
                                            fontWeight = FontWeight.Bold,
                                            color = if (testResult.isBlocked) Red500 else Emerald500,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = testResult.reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botón de prueba para verificar en vivo que el contador y el historial funcionan
                        OutlinedButton(
                            onClick = {
                                val targetNumber = if (testNumberText.isNotBlank()) testNumberText.trim() else "+56 600 300 4000"
                                val check = PhoneNumberHelper.checkIsBlocked(targetNumber, prefixes)
                                val detectedPrefix = check.matchedPrefix ?: (if (targetNumber.contains("600")) "600" else "80")
                                repository.addBlockedCall(targetNumber, detectedPrefix)
                                refreshState()
                                Toast.makeText(
                                    context,
                                    "¡Llamada simulada ($targetNumber)! Contador sumó +1",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = Indigo500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simular llamada spam de prueba (+1 al contador)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 5. Historial de Llamadas Bloqueadas Recientemente
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Historial de Bloqueos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (history.isNotEmpty() || totalBlocked > 0) {
                                TextButton(
                                    onClick = {
                                        repository.clearHistory()
                                        repository.resetCounter()
                                        refreshState()
                                        Toast.makeText(context, "Historial y contador restablecidos a 0", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Vaciar (0)", color = Red500, fontSize = 12.sp)
                                }
                            }
                        }

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PhoneDisabled,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aún no se han recibido llamadas de 600 u 80",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = subtleTextColor
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                history.take(15).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(chipBgColor)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Red500.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.CallEnd,
                                                    contentDescription = null,
                                                    tint = Red500,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = item.phoneNumber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Prefijo detectado: ${item.matchedPrefix}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = subtleTextColor
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.formattedTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = subtleTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Opciones y Ajustes de Comportamiento
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ajustes de Interceptación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Colgar llamada de inmediato", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "Corta la llamada al primer milisegundo para evitar timbrado.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subtleTextColor
                                )
                            }
                            Checkbox(
                                checked = rejectImmediately,
                                onCheckedChange = {
                                    rejectImmediately = it
                                    ruleManager.rejectCallImmediately = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Registrar en Historial de Teléfono", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "Permite ver en el registro de llamadas que intentaron llamar.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subtleTextColor
                                )
                            }
                            Checkbox(
                                checked = keepInLog,
                                onCheckedChange = {
                                    keepInLog = it
                                    ruleManager.keepInSystemCallLog = it
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}
