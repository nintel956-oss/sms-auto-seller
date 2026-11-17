package com.kartak.autosms.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartak.autosms.data.local.MessageLogEntity
import com.kartak.autosms.data.local.SoldCardEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(
    soldCards: List<SoldCardEntity>,
    messageLogs: List<MessageLogEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("جدول المبيعات (${soldCards.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل الرسائل (${messageLogs.size})") }
            )
        }

        if (selectedTab == 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("بحث برقم الزبون") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(soldCards, key = { it.id }) { sale ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("كرت: ${sale.cardNumber}", fontWeight = FontWeight.Bold)
                                Text("${sale.denomination} ر.ي", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text("الزبون: ${sale.customerPhone}", fontSize = 13.sp)
                            Text(
                                "الوقت: ${dateFormat.format(Date(sale.soldAt))}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messageLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المرسل: ${log.sender}", fontWeight = FontWeight.Bold)
                                Text(log.status.arabicLabel, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(log.body, fontSize = 13.sp)
                            if (log.customerReply != null) {
                                Text("الرد: ${log.customerReply}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                dateFormat.format(Date(log.receivedAt)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
