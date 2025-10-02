package islam.adhanalarm.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import islam.adhanalarm.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            CategoryHeader(title = stringResource(id = R.string.settings))
        }
        item {
            LocationSettings(viewModel)
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.calculation),
                items = stringArrayResource(R.array.calculation_methods).toList(),
                selectedValue = viewModel.calculationMethod.collectAsState().value,
                onValueChange = { viewModel.updateCalculationMethod(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.time_format),
                items = stringArrayResource(R.array.time_format).toList(),
                selectedValue = viewModel.timeFormat.collectAsState().value,
                onValueChange = { viewModel.updateTimeFormat(it) }
            )
        }

        item {
            CategoryHeader(title = stringResource(id = R.string.notification))
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.fajr),
                items = stringArrayResource(R.array.notification_methods).toList(),
                selectedValue = viewModel.notificationFajr.collectAsState().value,
                onValueChange = { viewModel.updateNotificationFajr(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.sunrise),
                items = stringArrayResource(R.array.notification_methods_sunrise).toList(),
                selectedValue = viewModel.notificationSunrise.collectAsState().value,
                onValueChange = { viewModel.updateNotificationSunrise(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.dhuhr),
                items = stringArrayResource(R.array.notification_methods).toList(),
                selectedValue = viewModel.notificationDhuhr.collectAsState().value,
                onValueChange = { viewModel.updateNotificationDhuhr(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.asr),
                items = stringArrayResource(R.array.notification_methods).toList(),
                selectedValue = viewModel.notificationAsr.collectAsState().value,
                onValueChange = { viewModel.updateNotificationAsr(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.maghrib),
                items = stringArrayResource(R.array.notification_methods).toList(),
                selectedValue = viewModel.notificationMaghrib.collectAsState().value,
                onValueChange = { viewModel.updateNotificationMaghrib(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.ishaa),
                items = stringArrayResource(R.array.notification_methods).toList(),
                selectedValue = viewModel.notificationIshaa.collectAsState().value,
                onValueChange = { viewModel.updateNotificationIshaa(it) }
            )
        }

        item {
            CategoryHeader(title = stringResource(id = R.string.advanced))
        }
        item {
            PreferenceTextField(
                title = stringResource(id = R.string.altitude),
                value = viewModel.altitude.collectAsState().value,
                onValueChange = { viewModel.updateAltitude(it) }
            )
        }
        item {
            PreferenceTextField(
                title = stringResource(id = R.string.pressure),
                value = viewModel.pressure.collectAsState().value,
                onValueChange = { viewModel.updatePressure(it) }
            )
        }
        item {
            PreferenceTextField(
                title = stringResource(id = R.string.temperature),
                value = viewModel.temperature.collectAsState().value,
                onValueChange = { viewModel.updateTemperature(it) }
            )
        }
        item {
            PreferenceDropDown(
                title = stringResource(id = R.string.rounding),
                items = stringArrayResource(R.array.rounding_types).toList(),
                selectedValue = viewModel.roundingType.collectAsState().value,
                onValueChange = { viewModel.updateRoundingType(it) }
            )
        }
        item {
            PreferenceTextField(
                title = stringResource(id = R.string.offset),
                value = viewModel.offsetMinutes.collectAsState().value,
                onValueChange = { viewModel.updateOffsetMinutes(it) }
            )
        }

        item {
            PreferenceItem(
                title = stringResource(id = R.string.information),
                summary = stringResource(id = R.string.information_text).replace("#", "3.0"),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.spiritofislam.com"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun LocationSettings(viewModel: SettingsViewModel) {
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()

    PreferenceTextField(
        title = stringResource(id = R.string.latitude),
        value = latitude,
        onValueChange = { viewModel.updateLatitude(it) }
    )
    PreferenceTextField(
        title = stringResource(id = R.string.longitude),
        value = longitude,
        onValueChange = { viewModel.updateLongitude(it) }
    )
    PreferenceItem(
        title = stringResource(id = R.string.lookup_gps),
        summary = "",
        onClick = { viewModel.lookupGps() }
    )
}

@Composable
fun PreferenceTextField(title: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(title) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceDropDown(
    title: String,
    items: List<String>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = items.getOrElse(selectedValue) { "" },
            onValueChange = {},
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onValueChange(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PreferenceItem(title: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.Medium)
        if (summary.isNotEmpty()) {
            Text(text = summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun stringArrayResource(id: Int): Array<String> {
    return LocalContext.current.resources.getStringArray(id)
}