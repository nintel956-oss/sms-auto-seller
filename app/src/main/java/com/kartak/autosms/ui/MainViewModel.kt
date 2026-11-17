package com.kartak.autosms.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kartak.autosms.KartakApplication
import com.kartak.autosms.data.local.*
import com.kartak.autosms.data.repository.CsvImportSummary
import com.kartak.autosms.data.repository.ProcessResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class SimulationOutbox(
    val status: MessageStatus,
    val customerPhone: String?,
    val customerReply: String?,
    val ownerAlert: String?,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KartakApplication).repository

    private val startOfTodayMillis: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    val settings: StateFlow<SettingsEntity> = repository.observeSettings()
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    val stockCountsMap: StateFlow<Map<Int, Int>> = repository.observeStockCounts()
        .map { list -> list.associate { it.denomination to it.remainingCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val todaySalesTotal: StateFlow<Int> = repository.observeTodaySalesTotal(startOfTodayMillis)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todaySalesCount: StateFlow<Int> = repository.observeTodaySalesCount(startOfTodayMillis)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val soldCards: StateFlow<List<SoldCardEntity>> = _customerSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.observeAllSold()
            else repository.searchSoldByCustomer(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messageLogs: StateFlow<List<MessageLogEntity>> = repository.observeAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _simulationOutbox = MutableStateFlow<SimulationOutbox?>(null)
    val simulationOutbox: StateFlow<SimulationOutbox?> = _simulationOutbox.asStateFlow()

    private val _lastImportSummary = MutableStateFlow<CsvImportSummary?>(null)
    val lastImportSummary: StateFlow<CsvImportSummary?> = _lastImportSummary.asStateFlow()

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(isServiceEnabled = enabled))
        }
    }

    fun saveSettings(updated: SettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(updated)
        }
    }

    fun updateSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun simulateIncomingSms(sender: String, body: String) {
        viewModelScope.launch {
            val result: ProcessResult = repository.enqueueMessage(
                sender = sender,
                body = body,
                isSimulation = true
            )
            _simulationOutbox.value = SimulationOutbox(
                status = result.status,
                customerPhone = result.customerPhone,
                customerReply = result.customerReply,
                ownerAlert = result.ownerAlert
            )
        }
    }

    fun importCsv(denomination: Int, csvContent: String) {
        viewModelScope.launch {
            val summary = repository.importCsvCards(denomination, csvContent)
            _lastImportSummary.value = summary
        }
    }
}
