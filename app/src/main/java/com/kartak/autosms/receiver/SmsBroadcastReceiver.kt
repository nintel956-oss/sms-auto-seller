package com.kartak.autosms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.kartak.autosms.KartakApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبل البث لالتقاط رسائل SMS الواردة فور وصولها إلى الجهاز
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // تجميع أجزاء الرسالة الطويلة حسب المرسل
        val groupedBySender = LinkedHashMap<String, StringBuilder>()
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: continue
            val bodyPart = sms.messageBody ?: continue
            groupedBySender.getOrPut(sender) { StringBuilder() }.append(bodyPart)
        }

        val pendingResult = goAsync()
        val repository = (context.applicationContext as KartakApplication).repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, bodyBuilder) in groupedBySender) {
                    repository.enqueueMessage(
                        sender = sender,
                        body = bodyBuilder.toString(),
                        isSimulation = false
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
