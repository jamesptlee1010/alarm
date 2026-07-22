package com.james.mathwakealarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessAlarm
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.james.mathwakealarm.alarm.AlarmScheduler
import com.james.mathwakealarm.data.AlarmPreferences
import com.james.mathwakealarm.data.AlarmSettings
import com.james.mathwakealarm.ui.theme.MathWakeAlarmTheme
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var alarmPreferences: AlarmPreferences
    private lateinit var alarmScheduler: AlarmScheduler
    private var refreshPermissions by mutableStateOf(0)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshPermissions++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmPreferences = AlarmPreferences(this)
        alarmScheduler = AlarmScheduler(this)

        setContent {
            MathWakeAlarmTheme {
                AlarmSettingsScreen(
                    initialSettings = alarmPreferences.load(),
                    permissionRefresh = refreshPermissions,
                    onSave = ::saveAndSchedule,
                    onDisable = ::disableAlarm,
                    onTest = ::scheduleTest,
                    onRequestNotifications = ::requestNotificationPermission,
                    onRequestExactAlarm = ::requestExactAlarmPermission,
                    onRequestFullScreen = ::requestFullScreenPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions++
    }

    private fun saveAndSchedule(settings: AlarmSettings): Boolean {
        val enabledSettings = settings.copy(enabled = true)
        return alarmScheduler.schedule(enabledSettings).fold(
            onSuccess = { next ->
                alarmPreferences.save(enabledSettings)
                Toast.makeText(
                    this,
                    "Alarm set for ${formatNext(next)}",
                    Toast.LENGTH_LONG,
                ).show()
                true
            },
            onFailure = { error ->
                Toast.makeText(this, error.message ?: "Could not schedule alarm", Toast.LENGTH_LONG).show()
                false
            },
        )
    }

    private fun disableAlarm(settings: AlarmSettings) {
        alarmScheduler.cancelRegular()
        alarmPreferences.save(settings.copy(enabled = false))
        Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleTest() {
        alarmScheduler.scheduleTest(10)
            .onSuccess {
                Toast.makeText(this, "Test alarm will ring in 10 seconds", Toast.LENGTH_LONG).show()
            }
            .onFailure { error ->
                Toast.makeText(this, error.message ?: "Could not schedule test", Toast.LENGTH_LONG).show()
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(alarmScheduler.exactAlarmSettingsIntent())
        }
    }

    private fun requestFullScreenPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }

    private fun formatNext(dateTime: ZonedDateTime): String =
        dateTime.format(DateTimeFormatter.ofPattern("EEE d MMM, h:mm a"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSettingsScreen(
    initialSettings: AlarmSettings,
    permissionRefresh: Int,
    onSave: (AlarmSettings) -> Boolean,
    onDisable: (AlarmSettings) -> Unit,
    onTest: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestFullScreen: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var settings by remember { mutableStateOf(initialSettings) }
    var exactPermission by remember { mutableStateOf(false) }
    var notificationPermission by remember { mutableStateOf(true) }
    var fullScreenPermission by remember { mutableStateOf(true) }

    LaunchedEffect(permissionRefresh) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        exactPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        notificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        fullScreenPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Math Wake Alarm", fontWeight = FontWeight.Bold)
                        Text(
                            "Maths first. Then silence.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AccessAlarm,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermissionCard(
                exactPermission = exactPermission,
                notificationPermission = notificationPermission,
                fullScreenPermission = fullScreenPermission,
                onRequestExactAlarm = onRequestExactAlarm,
                onRequestNotifications = onRequestNotifications,
                onRequestFullScreen = onRequestFullScreen,
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.Schedule, null, modifier = Modifier.height(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Alarm time", style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> settings = settings.copy(hour = hour, minute = minute) },
                                settings.hour,
                                settings.minute,
                                false,
                            ).show()
                        },
                    ) {
                        Text(
                            text = formatTime(settings.hour, settings.minute),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        if (settings.enabled) "Alarm currently enabled" else "Alarm currently disabled",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }

            SettingCard(title = "Active days", icon = Icons.Rounded.Schedule) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in settings.activeDays
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val newDays = settings.activeDays.toMutableSet()
                                if (selected && newDays.size > 1) newDays.remove(day) else newDays.add(day)
                                settings = settings.copy(activeDays = newDays)
                            },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            },
                        )
                    }
                }
            }

            SettingCard(title = "Maths challenge", icon = Icons.Rounded.Calculate) {
                Text(
                    "Correct answers required: ${settings.requiredCorrectAnswers}",
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = settings.requiredCorrectAnswers.toFloat(),
                    onValueChange = { settings = settings.copy(requiredCorrectAnswers = it.roundToInt()) },
                    valueRange = 3f..10f,
                    steps = 6,
                )
                Text(
                    "Questions mix addition, subtraction, multiplication and exact division. Skipping never counts as a correct answer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingCard(title = "Gentle volume ramp", icon = Icons.Rounded.Notifications) {
                Text(
                    "Starting volume: ${settings.startingVolumePercent}%",
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = settings.startingVolumePercent.toFloat(),
                    onValueChange = {
                        settings = settings.copy(startingVolumePercent = (it / 5).roundToInt() * 5)
                    },
                    valueRange = 5f..40f,
                    steps = 6,
                )
                Text(
                    "The alarm increases by 10 percentage points every 10 seconds until it reaches full volume.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Vibrate while ringing")
                    Switch(
                        checked = settings.vibrate,
                        onCheckedChange = { settings = settings.copy(vibrate = it) },
                    )
                }
            }

            Button(
                onClick = {
                    val enabledSettings = settings.copy(enabled = true)
                    if (onSave(enabledSettings)) settings = enabledSettings
                },
                enabled = exactPermission && notificationPermission && fullScreenPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.padding(5.dp))
                Text("SAVE AND ENABLE ALARM", fontWeight = FontWeight.Bold)
            }

            if (settings.enabled) {
                TextButton(
                    onClick = {
                        onDisable(settings)
                        settings = settings.copy(enabled = false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disable alarm")
                }
            }

            Button(
                onClick = onTest,
                enabled = exactPermission && notificationPermission && fullScreenPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.padding(4.dp))
                Text("TEST IN 10 SECONDS")
            }

            Text(
                "Important: Android still allows the phone owner to force-stop or uninstall any app. Inside this app, however, there is no dismiss button—the alarm service stops only after the required number of correct answers.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    exactPermission: Boolean,
    notificationPermission: Boolean,
    fullScreenPermission: Boolean,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
) {
    val allGranted = exactPermission && notificationPermission && fullScreenPermission
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (allGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                )
                Spacer(Modifier.padding(5.dp))
                Text(
                    if (allGranted) "Alarm permissions ready" else "Complete alarm permissions",
                    fontWeight = FontWeight.Bold,
                )
            }
            PermissionRow("Exact alarms", exactPermission, onRequestExactAlarm)
            PermissionRow("Notifications", notificationPermission, onRequestNotifications)
            PermissionRow("Full-screen alarm", fullScreenPermission, onRequestFullScreen)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        if (granted) {
            Text("Granted", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        } else {
            TextButton(onClick = onGrant) { Text("Grant") }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(5.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val twelveHour = when (val converted = hour % 12) {
        0 -> 12
        else -> converted
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(twelveHour, minute, suffix)
}
