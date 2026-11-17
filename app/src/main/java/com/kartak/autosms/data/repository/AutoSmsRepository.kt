package com.kartak.autosms.data.repository

import com.kartak.autosms.data.local.*
import com.kartak.autosms.service.NotificationHelper
import com.kartak.autosms.service.SmsSender
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ParsedSms(
    val amount: Int,
    val customerPhone: String,
    val normalizedBody: String
)

data class ProcessResult(
    val status: MessageStatus,
    val customerReply: String? = null,
    val ownerAlert: String? = null,
    val cardNumber: String? = null,
    val amount: Int? = null,
    val customerPhone: String? = null
)

data class CsvImportSummary(
    val denomination: Int,
    val totalLines: Int,
    val addedCount: Int,
    val ignoredEmptyCount: Int,
    val duplicateCount: Int
)

private data class QueuedSmsJob(
    val sender: String,
    val body: String,
    val isSimulation: Boolean,
    val deferred: CompletableDeferred<ProcessResult>
)

class AutoSmsRepository(
    private val database: AppDatabase,
    private val smsSender: SmsSender,
    private val notificationHelper: NotificationHelper
) {
    private val cardDao = database.cardDao()
    private val soldCardDao = database.soldCardDao()
    private val messageLogDao = database.messageLogDao()
    private val settingsDao = database.settingsDao()
    private val transactionDao = database.cardTransactionDao()

    // طابور المعالجة المتسلسلة مع Mutex لمنع سحب نفس الكرت مرتين نهائياً
    private val processMutex = Mutex()
    private val messageQueue = Channel<QueuedSmsJob>(capacity = Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            for (job in messageQueue) {
                val result = processMutex.withLock {
                    processMessage(job.sender, job.body, job.isSimulation)
                }
                job.deferred.complete(result)
            }
        }
    }

    fun observeStockCounts(): Flow<List<DenominationStockCount>> = cardDao.observeStockCounts()
    fun observeAllSold(): Flow<List<SoldCardEntity>> = soldCardDao.observeAllSold()
    fun searchSoldByCustomer(query: String): Flow<List<SoldCardEntity>> = soldCardDao.searchSoldByCustomer(query)
    fun observeTodaySalesTotal(startOfDayMillis: Long): Flow<Int> = soldCardDao.observeTodaySalesTotal(startOfDayMillis)
    fun observeTodaySalesCount(startOfDayMillis: Long): Flow<Int> = soldCardDao.observeTodaySalesCount(startOfDayMillis)
    fun observeAllLogs(): Flow<List<MessageLogEntity>> = messageLogDao.observeAllLogs()
    fun observeSettings(): Flow<SettingsEntity?> = settingsDao.observeSettings()

    suspend fun updateSettings(settings: SettingsEntity) {
        settingsDao.upsertSettings(settings)
    }

    /**
     * إضافة الرسالة إلى الطابور وانتظار نتيجة معالجتها بالترتيب
     */
    suspend fun enqueueMessage(
        sender: String,
        body: String,
        isSimulation: Boolean = false
    ): ProcessResult {
        val deferred = CompletableDeferred<ProcessResult>()
        messageQueue.send(QueuedSmsJob(sender, body, isSimulation, deferred))
        return deferred.await()
    }

    /**
     * تحويل الأرقام العربية (٠-٩) إلى إنجليزية (0-9) لتوحيد التحليل
     */
    private fun normalizeDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '٠'..'٩' -> builder.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> builder.append(('0'.code + (ch.code - '۰'.code)).toChar())
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * تحليل نص الرسالة الواردة لاستخراج المبلغ ورقم الزبون.
     * - المبلغ: أول رقم يأتي قبل كلمة "ر.ي" مباشرة.
     * - رقم الزبون: آخر رقم يظهر في نص الرسالة.
     * يدعم الأرقام العربية والإنجليزية.
     */
    fun parseMessage(rawText: String): ParsedSms? {
        val normalized = normalizeDigits(rawText).trim()

        // استخراج المبلغ: أول رقم يسبق "ر.ي"
        val amountRegex = Regex("""(\d+)\s*ر\.ي""")
        val amountMatch = amountRegex.find(normalized)
        val amount = amountMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null

        // استخراج رقم الزبون: آخر سلسلة أرقام في الرسالة
        val allNumbers = Regex("""\d+""").findAll(normalized).map { it.value }.toList()
        val customerPhone = allNumbers.lastOrNull() ?: return null

        return ParsedSms(
            amount = amount,
            customerPhone = customerPhone,
            normalizedBody = normalized
        )
    }

    /**
     * معالجة الرسالة الواردة بشكل متسلسل وآمن:
     * 1. التحقق من مطابقة المرسل والكلمة المفتاحية.
     * 2. تحليل المبلغ ورقم الزبون ومنع التكرار خلال 60 ثانية.
     * 3. سحب أول كرت وحفظه في جدول المباع أولاً، ثم إرسال الرد عبر SmsManager.
     */
    suspend fun processMessage(
        sender: String,
        body: String,
        isSimulation: Boolean
    ): ProcessResult {
        val settings = settingsDao.getSettingsOnce() ?: SettingsEntity()

        // إذا كانت الخدمة متوقفة والرسالة حقيقية يتم تجاهلها
        if (!isSimulation && !settings.isServiceEnabled) {
            messageLogDao.insertLog(
                MessageLogEntity(
                    sender = sender,
                    body = body,
                    parsedAmount = null,
                    customerPhone = null,
                    status = MessageStatus.IGNORED,
                    isSimulated = false
                )
            )
            return ProcessResult(status = MessageStatus.IGNORED)
        }

        // 1. فحص المرسل (بدون حساسية لحالة الأحرف) والكلمة المفتاحية
        val senderMatches = sender.trim().equals(settings.allowedSender.trim(), ignoreCase = true)
        val keywordMatches = body.contains(settings.keyword.trim(), ignoreCase = true)

        if (!senderMatches || !keywordMatches) {
            messageLogDao.insertLog(
                MessageLogEntity(
                    sender = sender,
                    body = body,
                    parsedAmount = null,
                    customerPhone = null,
                    status = MessageStatus.IGNORED,
                    isSimulated = isSimulation
                )
            )
            return ProcessResult(status = MessageStatus.IGNORED)
        }

        // 2. تحليل نص الرسالة لاستخراج المبلغ ورقم الزبون
        val parsed = parseMessage(body)
        if (parsed == null) {
            messageLogDao.insertLog(
                MessageLogEntity(
                    sender = sender,
                    body = body,
                    parsedAmount = null,
                    customerPhone = null,
                    status = MessageStatus.IGNORED,
                    isSimulated = isSimulation
                )
            )
            return ProcessResult(status = MessageStatus.IGNORED)
        }

        // 3. منع التكرار خلال 60 ثانية (نفس النص، المبلغ، والزبون)
        val sixtySecondsAgo = System.currentTimeMillis() - 60_000L
        val isDuplicate = messageLogDao.existsRecentDuplicate(
            body = body,
            amount = parsed.amount,
            customerPhone = parsed.customerPhone,
            sinceMillis = sixtySecondsAgo
        )

        if (isDuplicate) {
            messageLogDao.insertLog(
                MessageLogEntity(
                    sender = sender,
                    body = body,
                    parsedAmount = parsed.amount,
                    customerPhone = parsed.customerPhone,
                    status = MessageStatus.DUPLICATE,
                    isSimulated = isSimulation
                )
            )
            return ProcessResult(
                status = MessageStatus.DUPLICATE,
                amount = parsed.amount,
                customerPhone = parsed.customerPhone
            )
        }

        val allowedDenominations = settings.getDenominationsList()

        // السيناريو الثالث: المبلغ لا يطابق أي فئة مسموحة
        if (!allowedDenominations.contains(parsed.amount)) {
            val customerReply = settings.templateMismatch
                .replace("{المبلغ}", parsed.amount.toString())
                .replace("{رقم_الزبون}", parsed.customerPhone)

            val ownerAlert = settings.templateOwnerMismatch
                .replace("{المبلغ}", parsed.amount.toString())
                .replace("{رقم_الزبون}", parsed.customerPhone)

            messageLogDao.insertLog(
                MessageLogEntity(
                    sender = sender,
                    body = body,
                    parsedAmount = parsed.amount,
                    customerPhone = parsed.customerPhone,
                    status = MessageStatus.AMOUNT_MISMATCH,
                    customerReply = customerReply,
                    ownerAlert = ownerAlert,
                    isSimulated = isSimulation
                )
            )

            if (!isSimulation) {
                smsSender.sendSms(parsed.customerPhone, customerReply)
                if (settings.ownerPhone.isNotBlank()) {
                    smsSender.sendSms(settings.ownerPhone, ownerAlert)
                }
            }
            notificationHelper.showOwnerAlert("مبلغ غير مطابق", ownerAlert)

            return ProcessResult(
                status = MessageStatus.AMOUNT_MISMATCH,
                customerReply = customerReply,
                ownerAlert = ownerAlert,
                amount = parsed.amount,
                customerPhone = parsed.customerPhone
            )
        }

        // الترتيب الحاسم: نقل الكرت إلى جدول "المباع" وحفظه في قاعدة البيانات أولاً
        val soldCard = transactionDao.claimAndSellFirstCard(
            cardDao = cardDao,
            soldCardDao = soldCardDao,
            denomination = parsed.amount,
            customerPhone = parsed.customerPhone,
            rawMessage = body,
            isSimulated = isSimulation
        )

        // السيناريو الثاني: الفئة موجودة ولكن المخزون نفد
        if (soldCard == null) {
            val customerReply = settings.templateOutOfStock
                .replace("{الفئة}", parsed.amount.toString())
                .replace("{رقم_الزبون}", parsed.customerPhone)

            val ownerAlert = settings.templateOwnerOutOfStock
                .replace("{الفئة}", parsed.amount.toString())
                .replace("{رقم_الزبون}", parsed.customerPhone)

            messageLogDao.insertLog(
                MessageLogEntity(
                    sender = sender,
                    body = body,
                    parsedAmount = parsed.amount,
                    customerPhone = parsed.customerPhone,
                    status = MessageStatus.OUT_OF_STOCK,
                    customerReply = customerReply,
                    ownerAlert = ownerAlert,
                    isSimulated = isSimulation
                )
            )

            if (!isSimulation) {
                smsSender.sendSms(parsed.customerPhone, customerReply)
                if (settings.ownerPhone.isNotBlank()) {
                    smsSender.sendSms(settings.ownerPhone, ownerAlert)
                }
            }
            notificationHelper.showOwnerAlert("نفاد مخزون فئة ${parsed.amount}", ownerAlert)

            return ProcessResult(
                status = MessageStatus.OUT_OF_STOCK,
                customerReply = customerReply,
                ownerAlert = ownerAlert,
                amount = parsed.amount,
                customerPhone = parsed.customerPhone
            )
        }

        // السيناريو الأول: توفر الكرت وتم حفظه في جدول المباع، الآن نرسل الرد للزبون
        val customerReply = settings.templateSuccess
            .replace("{رقم_الكرت}", soldCard.cardNumber)
            .replace("{الفئة}", soldCard.denomination.toString())
            .replace("{رقم_الزبون}", parsed.customerPhone)

        messageLogDao.insertLog(
            MessageLogEntity(
                sender = sender,
                body = body,
                parsedAmount = parsed.amount,
                customerPhone = parsed.customerPhone,
                status = MessageStatus.SOLD,
                customerReply = customerReply,
                isSimulated = isSimulation
            )
        )

        if (!isSimulation) {
            smsSender.sendSms(parsed.customerPhone, customerReply)
        }

        return ProcessResult(
            status = MessageStatus.SOLD,
            customerReply = customerReply,
            cardNumber = soldCard.cardNumber,
            amount = soldCard.denomination,
            customerPhone = parsed.customerPhone
        )
    }

    /**
     * استيراد كروت فئة محددة من نص CSV (سطر لكل كرت) مع تجاهل الأسطر الفارغة والمكررة
     */
    suspend fun importCsvCards(denomination: Int, csvContent: String): CsvImportSummary {
        val lines = csvContent.lines()
        var added = 0
        var empty = 0
        var duplicates = 0
        val seenInBatch = HashSet<String>()

        for (rawLine in lines) {
            val cleaned = rawLine.trim().removePrefix("\uFEFF")
            if (cleaned.isEmpty()) {
                empty++
                continue
            }
            // تجاهل رأس الملف إن وجد
            if (cleaned.equals("card_number", ignoreCase = true) || cleaned == "رقم_الكرت") {
                empty++
                continue
            }
            val cardCode = cleaned.split(",").first().trim()
            if (cardCode.isEmpty()) {
                empty++
                continue
            }
            if (!seenInBatch.add(cardCode) || cardDao.cardExistsAnywhere(cardCode)) {
                duplicates++
                continue
            }
            val inserted = cardDao.insertCard(
                CardEntity(cardNumber = cardCode, denomination = denomination)
            )
            if (inserted > 0L) added++ else duplicates++
        }

        return CsvImportSummary(
            denomination = denomination,
            totalLines = lines.size,
            addedCount = added,
            ignoredEmptyCount = empty,
            duplicateCount = duplicates
        )
    }
}
