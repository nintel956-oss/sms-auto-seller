package com.kartak.autosms

import android.app.Application
import com.kartak.autosms.data.local.AppDatabase
import com.kartak.autosms.data.repository.AutoSmsRepository
import com.kartak.autosms.service.NotificationHelper
import com.kartak.autosms.service.SmsSender

class KartakApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val smsSender by lazy { SmsSender(this) }
    val notificationHelper by lazy { NotificationHelper(this) }
    val repository by lazy { AutoSmsRepository(database, smsSender, notificationHelper) }
}
