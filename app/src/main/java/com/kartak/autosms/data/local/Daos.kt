package com.kartak.autosms.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class DenominationStockCount(
    val denomination: Int,
    val remainingCount: Int
)

@Dao
interface CardDao {
    @Query("SELECT * FROM available_cards ORDER BY id ASC")
    fun observeAllAvailable(): Flow<List<CardEntity>>

    @Query("SELECT denomination, COUNT(*) AS remainingCount FROM available_cards GROUP BY denomination")
    fun observeStockCounts(): Flow<List<DenominationStockCount>>

    @Query("SELECT * FROM available_cards WHERE denomination = :denomination ORDER BY id ASC LIMIT 1")
    suspend fun getFirstAvailableCard(denomination: Int): CardEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM available_cards WHERE cardNumber = :cardNumber) OR EXISTS(SELECT 1 FROM sold_cards WHERE cardNumber = :cardNumber)")
    suspend fun cardExistsAnywhere(cardNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: CardEntity): Long

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM available_cards WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SoldCardDao {
    @Query("SELECT * FROM sold_cards ORDER BY soldAt DESC")
    fun observeAllSold(): Flow<List<SoldCardEntity>>

    @Query("SELECT * FROM sold_cards WHERE customerPhone LIKE '%' || :query || '%' ORDER BY soldAt DESC")
    fun searchSoldByCustomer(query: String): Flow<List<SoldCardEntity>>

    @Query("SELECT COALESCE(SUM(denomination), 0) FROM sold_cards WHERE soldAt >= :startOfDayMillis")
    fun observeTodaySalesTotal(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sold_cards WHERE soldAt >= :startOfDayMillis")
    fun observeTodaySalesCount(startOfDayMillis: Long): Flow<Int>

    @Insert
    suspend fun insertSoldCard(soldCard: SoldCardEntity): Long
}

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_logs ORDER BY receivedAt DESC")
    fun observeAllLogs(): Flow<List<MessageLogEntity>>

    @Insert
    suspend fun insertLog(log: MessageLogEntity): Long

    /**
     * التحقق من وجود رسالة مطابقة (نفس النص، المبلغ، ورقم الزبون) خلال آخر 60 ثانية
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM message_logs 
            WHERE body = :body 
              AND parsedAmount = :amount 
              AND customerPhone = :customerPhone 
              AND status != 'DUPLICATE'
              AND receivedAt >= :sinceMillis
        )
    """)
    suspend fun existsRecentDuplicate(
        body: String,
        amount: Int?,
        customerPhone: String?,
        sinceMillis: Long
    ): Boolean
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)
}

/**
 * DAO معاملات قاعدة البيانات لضمان السحب الذري (Atomic) للكرت قبل إرسال SMS
 */
@Dao
interface CardTransactionDao {
    @Transaction
    suspend fun claimAndSellFirstCard(
        cardDao: CardDao,
        soldCardDao: SoldCardDao,
        denomination: Int,
        customerPhone: String,
        rawMessage: String,
        isSimulated: Boolean
    ): SoldCardEntity? {
        val firstCard = cardDao.getFirstAvailableCard(denomination) ?: return null
        cardDao.deleteById(firstCard.id)
        val sold = SoldCardEntity(
            cardNumber = firstCard.cardNumber,
            denomination = firstCard.denomination,
            customerPhone = customerPhone,
            rawMessage = rawMessage,
            isSimulated = isSimulated
        )
        val insertedId = soldCardDao.insertSoldCard(sold)
        return sold.copy(id = insertedId)
    }
}
