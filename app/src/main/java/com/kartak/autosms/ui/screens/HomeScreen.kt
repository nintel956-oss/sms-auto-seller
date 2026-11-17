package com.kartak.autosms.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    isServiceEnabled: Boolean,
    onToggleService: (Boolean) -> Unit,
    todaySalesTotal: Int,
    todaySalesCount: Int,
    denominations: List<Int>,
    stockCounts: Map<Int, Int>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة تفعيل/إيقاف الخدمة الآلية
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) Color(0xFF0F766E) else Color(0xFF334155)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isServiceEnabled) "الخدمة الآلية مفعّلة" else "الخدمة الآلية متوقفة",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isServiceEnabled) "يتم استقبال رسائل SMS والرد تلقائياً" else "لن يتم الرد على الرسائل الواردة حالياً",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = isServiceEnabled,
                    onCheckedChange = onToggleService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0F766E),
                        checkedTrackColor = Color.White
                    )
                )
            }
        }

        // ملخص مبيعات اليوم
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مبيعات اليوم", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$todaySalesTotal ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("عدد العمليات", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$todaySalesCount عملية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Text(
            text = "مخزون الفئات المتاح",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // شبكة بطاقات الفئات (يتحول اللون للأحمر إذا قل المخزون عن 5)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(denominations) { denom ->
                val count = stockCounts[denom] ?: 0
                val isLowStock = count < 5

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "فئة $denom ر.ي",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isLowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$count",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when {
                                count == 0 -> "نفد المخزون"
                                isLowStock -> "مخزون منخفض (أقل من 5)"
                                else -> "كرت متبقي"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
