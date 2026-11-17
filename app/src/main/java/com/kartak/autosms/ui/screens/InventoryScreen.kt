package com.kartak.autosms.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartak.autosms.data.repository.CsvImportSummary

@Composable
fun InventoryScreen(
    denominations: List<Int>,
    stockCounts: Map<Int, Int>,
    lastSummary: CsvImportSummary?,
    onImportCsv: (denomination: Int, csvContent: String) -> Unit
) {
    val context = LocalContext.current
    var selectedDenom by remember(denominations) {
        mutableStateOf(denominations.firstOrNull() ?: 100)
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                onImportCsv(selectedDenom, content)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("إدارة المخزون واستيراد CSV", style = MaterialTheme.typography.titleMedium)

        Text(
            "اختر الفئة قبل استيراد ملف CSV (سطر لكل كرت، رقم الكرت فقط):",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            denominations.forEach { denom ->
                FilterChip(
                    selected = selectedDenom == denom,
                    onClick = { selectedDenom = denom },
                    label = { Text("$denom ر.ي (${stockCounts[denom] ?: 0})") }
                )
            }
        }

        Button(
            onClick = { csvPickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("استيراد ملف CSV لفئة $selectedDenom ر.ي", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                val sampleCsv = "849201948271\n849201948272\n849201948273\n849201948274\n849201948275"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "قالب_كروت_$selectedDenom.csv")
                    putExtra(Intent.EXTRA_TEXT, sampleCsv)
                }
                context.startActivity(Intent.createChooser(shareIntent, "تصدير قالب CSV تجريبي"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("تصدير / مشاركة قالب CSV تجريبي")
        }

        if (lastSummary != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("ملخص آخر عملية استيراد (فئة ${lastSummary.denomination} ر.ي)", fontWeight = FontWeight.Bold)
                    Text("الكروت المضافة بنجاح: ${lastSummary.addedCount}", color = MaterialTheme.colorScheme.primary)
                    Text("الأسطر المكررة المتجاهلة: ${lastSummary.duplicateCount}")
                    Text("الأسطر الفارغة المتجاهلة: ${lastSummary.ignoredEmptyCount}")
                }
            }
        }
    }
}
