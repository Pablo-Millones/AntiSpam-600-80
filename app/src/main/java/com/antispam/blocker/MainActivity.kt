package com.antispam.blocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.antispam.blocker.data.BlockRuleManager
import com.antispam.blocker.data.BlockedCallsRepository
import com.antispam.blocker.ui.screens.MainScreen
import com.antispam.blocker.ui.theme.SpamBlockerTheme

class MainActivity : ComponentActivity() {

    private lateinit var ruleManager: BlockRuleManager
    private lateinit var repository: BlockedCallsRepository
    private var isRoleGranted by mutableStateOf(false)

    // Lanzador para solicitar el rol de Call Screening en Android
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkRoleStatus()
        if (isRoleGranted) {
            Toast.makeText(this, "¡Filtro de llamadas activado con éxito!", Toast.LENGTH_SHORT).show()
        } else {
            // Si el cuadro del sistema no lo asignó o se canceló, redirigir a los Ajustes de Aplicaciones Predeterminadas
            openDefaultAppsSettings()
        }
    }

    // Permiso de notificaciones para Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ruleManager = BlockRuleManager(this)
        repository = BlockedCallsRepository(this)

        checkRoleStatus()
        requestNotificationPermissionIfNeeded()

        setContent {
            SpamBlockerTheme {
                MainScreen(
                    ruleManager = ruleManager,
                    repository = repository,
                    isRoleGranted = isRoleGranted,
                    onRequestRole = { requestCallScreeningRole() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkRoleStatus()
    }

    private fun checkRoleStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            isRoleGranted = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            isRoleGranted = true
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    try {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleRequestLauncher.launch(intent)
                        return
                    } catch (e: Exception) {
                        openDefaultAppsSettings()
                        return
                    }
                } else {
                    isRoleGranted = true
                    Toast.makeText(this, "El filtro ya está activo en el sistema.", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        openDefaultAppsSettings()
    }

    private fun openDefaultAppsSettings() {
        try {
            // Redirigir a Ajustes de Aplicaciones Predeterminadas
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Selecciona 'Identificador de llamadas y spam' y elige AntiSpam 600 y 80",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            try {
                // Fallback a los ajustes propios de la aplicación
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Abre Ajustes > Aplicaciones > Predeterminadas", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
