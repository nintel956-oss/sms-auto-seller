package com.kartak.autosms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartak.autosms.data.local.SettingsEntity

@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    onSaveSettings: (SettingsEntity) -> Unit
) {
    var ownerPhone by remember(settings) { mutableStateOf(settings.ownerPhone) }
    var allowedSender by remember(settings) { mutableStateOf(settings.allowedSender) }
    var keyword by remember(settings) { mutableStateOf(settings.keyword) }
    var denominationsList by remember(settings) { mutableStateOf(settings.getDenominationsList()) }
    var newDenomText by remember { mutableStateOf("") }
    var templateSuccess by remember(settings) { mutableStateOf(settings.templateSuccess) }
    var templateOutOfStock by remember(settings) { mutableStateOf(settings.templateOutOfStock) }
    var templateMismatch by remember(settings) { mutableStateOf(settings.templateMismatch) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("إعدادات النظام الآلي", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = ownerPhone,
            onValueChange = { ownerPhone = it },
            label = { Text("رقم المالك (للتنبيهات)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = allowedSender,
            onValueChange = { allowedSender = it },
            label = { Text("المرسل المسموح (افتراضياً Jaib)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("الكلمة المفتاحية (افتراضياً اضيف)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("الفئات المسموحة", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newDenomText,
                onValueChange = { newDenomText = it },
                label = { Text("إضافة فئة") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val value = newDenomText.trim().toIntOrNull()
                    if (value != null && value > 0 && !denominationsList.contains(value)) {
                        denominationsList = (denominationsList + value).sorted()
                        newDenomText = ""
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("إضافة")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            denominationsList.forEach { denom ->
                InputChip(
                    selected = false,
                    onClick = {
                        if (denominationsList.size > 1) {
                            denominationsList = denominationsList.filterNot { it == denom }
                        }
                    },
                    label = { Text("$denom ×") }
                )
            }
        }

        Text("نصوص الردود التلقائية", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            "المتغيرات المتاحة: {رقم_الكرت} و {الفئة}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = templateSuccess,
            onValueChange = { templateSuccess = it },
            label = { Text("رد توفر الكرت") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = templateOutOfStock,
            onValueChange = { templateOutOfStock = it },
            label = { Text("رد نفاد المخزون") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = templateMismatch,
            onValueChange = { templateMismatch = it },
            label = { Text("رد عدم مطابقة المبلغ") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSaveSettings(
                    settings.copy(
                        ownerPhone = ownerPhone.trim(),
                        allowedSender = allowedSender.trim(),
                        keyword = keyword.trim(),
                        allowedDenominationsCsv = denominationsList.joinToString(","),
                        templateSuccess = templateSuccess,
                        templateOutOfStock = templateOutOfStock,
                        templateMismatch = templateMismatch
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("حفظ الإعدادات", fontWeight = FontWeight.Bold)
        }
    }
}
