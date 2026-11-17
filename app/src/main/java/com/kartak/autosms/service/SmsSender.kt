package com.kartak.autosms.service

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

/**
 * مسؤول عن إرسال رسائل SMS التلقائية عبر SmsManager
 */
class SmsSender(private val context: Context) {

    fun sendSms(phoneNumber: String, message: String): Boolean {
        if (phoneNumber.isBlank() || message.isBlank()) return false
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // تقسيم الرسالة تلقائياً إذا كانت طويلة أو تحتوي على أحرف عربية UTF-16
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e("SmsSender", "فشل إرسال SMS إلى $phoneNumber: ${e.message}", e)
            false
        }
    }
}
