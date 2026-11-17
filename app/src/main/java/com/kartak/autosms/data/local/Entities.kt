package com.kartak.autosms.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * حالة معالجة الرسالة الواردة
 */
enum class MessageStatus(val arabicLabel: String) {
    SOLD("مباعة"),
    OUT_OF_STOCK("نفد المخزون"),
    AMOUNT_MISMATCH("غير مطابق"),
    IGNORED("متجاهلة"),
    DUPLICATE("مكررة")
}

/**
 * جدول كروت الشحن المتاحة في المخزون
 */
@Entity(
    tableName = "available_cards",
    indices = [Index(value = ["cardNumber"], unique = true)]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardNumber: String,
    val denomination: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * جدول الكروت المباعة (يتم نقل الكرت إليه قبل إرسال الرد لضمان عدم السحب المزدوج)
 */
@Entity(tableName = "sold_cards")
data class SoldCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardNumber: String,
    val denomination: Int,
    val customerPhone: String,
    val soldAt: Long = System.currentTimeMillis(),
    val rawMessage: String,
    val isSimulated: Boolean = false
)

/**
 * سجل جميع الرسائل الواردة (الحقيقية والمحاكاة) مع النتيجة النهائية
 */
@Entity(tableName = "message_logs")
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val parsedAmount: Int?,
    val customerPhone: String?,
    val status: MessageStatus,
    val customerReply: String? = null,
    val ownerAlert: String? = null,
    val isSimulated: Boolean = false,
    val receivedAt: Long = System.currentTimeMillis()
)

/**
 * جدول الإعدادات العامة للنظام (صف واحد بمعرف ثابت = 1)
 */
@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isServiceEnabled: Boolean = true,
    val ownerPhone: String = "770000000",
    val allowedSender: String = "Jaib",
    val keyword: String = "اضيف",
    val allowedDenominationsCsv: String = "100,200,500,1000",
    val templateSuccess: String = "رقم كرتك: {رقم_الكرت} فئة {الفئة} ر.ي",
    val templateOutOfStock: String = "سيصلك الكرت خلال دقائق",
    val templateMismatch: String = "المبلغ غير مطابق",
    val templateOwnerOutOfStock: String = "نفد مخزون فئة {الفئة}، الزبون {رقم_الزبون}",
    val templateOwnerMismatch: String = "مبلغ غير مطابق {المبلغ} من {رقم_الزبون}"
) {
    fun getDenominationsList(): List<Int> =
        allowedDenominationsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sorted()
}
