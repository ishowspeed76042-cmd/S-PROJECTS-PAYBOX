package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UpiAppCatalog
import com.example.data.UpiAppCategory
import com.example.data.UpiAppDefinition
import com.example.ui.theme.*

@Composable
fun UpiSelectionDialog(
    enableAll: Boolean,
    selectedAppIds: Set<String>,
    onToggleEnableAll: (Boolean) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            UpiAppCatalog.ALL_APPS
        } else {
            val q = searchQuery.trim().lowercase()
            UpiAppCatalog.ALL_APPS.filter {
                it.displayName.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.officialPackages.any { pkg -> pkg.contains(q) }
            }
        }
    }

    val groupedApps = remember(filteredApps) {
        UpiAppCategory.entries.mapNotNull { category ->
            val items = filteredApps.filter { it.category == category }
            if (items.isEmpty()) null else category to items
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DarkBackground,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select UPI Apps",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${selectedAppIds.size} of ${UpiAppCatalog.ALL_APPS.size} verified apps monitored",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_upi_picker_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Master Enable All Toggle
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (enableAll) SuccessGreen.copy(alpha = 0.6f) else NeonBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleEnableAll(!enableAll) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable All UPI Apps",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Monitor PhonePe, GPay, Paytm, FamPay, BHIM, Navi & more",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = enableAll,
                            onCheckedChange = onToggleEnableAll,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = SuccessGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("dialog_enable_all_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search PhonePe, GPay, Navi, SBI...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("search_upi_apps_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedApps.forEach { (category, apps) ->
                        item(key = "header_${category.name}") {
                            Text(
                                text = category.label.uppercase(),
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(apps, key = { it.id }) { app ->
                            val isChecked = enableAll || app.id in selectedAppIds
                            UpiAppCheckboxRow(
                                app = app,
                                checked = isChecked,
                                onCheckedChange = { checked -> onToggleApp(app.id, checked) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().testTag("done_upi_picker_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Selection", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UpiAppCheckboxRow(
    app: UpiAppDefinition,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (checked) DarkSurface else DarkSurface.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (checked) NeonCyan.copy(alpha = 0.4f) else DarkSurfaceElevated
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonCyan,
                    checkmarkColor = DarkBackground,
                    uncheckedColor = TextSecondary
                ),
                modifier = Modifier.testTag("upi_checkbox_${app.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${app.description} • ${app.officialPackages.firstOrNull().orEmpty()}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
