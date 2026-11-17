package com.kartak.autosms.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import com.kartak.autosms.ui.SimulationOutbox

@Composable
fun SimulatorScreen(
    defaultSender: String,
    outbox: SimulationOutbox?,
    onSimulate: (sender: String, body: String) -> Unit
) {
    var sender by remember(defaultSender) { mutableStateOf(defaultSender) }
    var messageBody by remember { mutableStateOf("تم اضيف 100 ر.ي الى حسابك 780861945") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("محاكاة استقبال رسالة SMS", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = sender,
            onValueChange = { sender = it },
            label = { Text("اسم المرسل") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = messageBody,
            onValueChange = { messageBody = it },
            label = { Text("نص الرسالة") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // أزرار سريعة للأمثلة
        Text("أمثلة سريعة للاختبار:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = {
                sender = defaultSender
                messageBody = "تم اضيف 100 ر.ي الى حسابك 780861945"
            }) { Text("رسالة صحيحة (100)") }

            OutlinedButton(onClick = {
                sender = defaultSender
                messageBody = "تم اضيف 500 ر.ي الى حسابك 771234567"
            }) { Text("فئة نفدت (500)") }

            OutlinedButton(onClick = {
                sender = defaultSender
                messageBody = "تم اضيف 350 ر.ي الى حسابك 733001122"
            }) { Text("مبلغ غير مطابق (350)") }

            OutlinedButton(onClick = {
                sender = "UnknownBank"
                messageBody = "تم اضيف 100 ر.ي الى حسابك 780861945"
            }) { Text("مرسل مختلف") }
        }

        Button(
            onClick = { onSimulate(sender, messageBody) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("محاكاة استقبال رسالة", fontWeight = FontWeight.Bold)
        }

        // صندوق الرد
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("صندوق الرد", style = MaterialTheme.typography.titleMedium)

                if (outbox == null) {
                    Text(
                        "لم يتم تنفيذ أي محاكاة بعد. اضغط على زر المحاكاة لعرض الردود المرسلة للزبون والمالك.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "الحالة: ${outbox.status.arabicLabel}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    Text(
                        "رد الزبون (${outbox.customerPhone ?: "—"}):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        outbox.customerReply ?: "لم يُرسل رد للزبون",
                        fontSize = 14.sp
                    )
                    HorizontalDivider()
                    Text(
                        "تنبيه المالك:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        outbox.ownerAlert ?: "لا يوجد تنبيه للمالك في هذه العملية",
                        fontSize = 14.sp,
                        color = if (outbox.ownerAlert != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
