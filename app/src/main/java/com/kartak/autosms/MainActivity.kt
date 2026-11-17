package com.kartak.autosms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kartak.autosms.ui.MainViewModel
import com.kartak.autosms.ui.screens.*
import com.kartak.autosms.ui.theme.KartakTheme

enum class BottomNavTab(val label: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Rounded.Home),
    SIMULATOR("المحاكاة", Icons.Rounded.Sms),
    INVENTORY("المخزون", Icons.Rounded.Inventory2),
    LOGS("السجل", Icons.Rounded.ReceiptLong),
    SETTINGS("الإعدادات", Icons.Rounded.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // فرض الاتجاه من اليمين إلى اليسار (RTL) لجميع مكونات الواجهة
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KartakTheme {
                    KartakAppRoot()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartakAppRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }

    // طلب صلاحيات وقت التشغيل (Runtime Permissions) للرسائل والإشعارات
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val missing = requiredPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    val settings by viewModel.settings.collectAsState()
    val stockCounts by viewModel.stockCountsMap.collectAsState()
    val todaySalesTotal by viewModel.todaySalesTotal.collectAsState()
    val todaySalesCount by viewModel.todaySalesCount.collectAsState()
    val soldCards by viewModel.soldCards.collectAsState()
    val messageLogs by viewModel.messageLogs.collectAsState()
    val searchQuery by viewModel.customerSearchQuery.collectAsState()
    val simulationOutbox by viewModel.simulationOutbox.collectAsState()
    val lastImportSummary by viewModel.lastImportSummary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("كارتك الآلي — ${currentTab.label}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                BottomNavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                BottomNavTab.HOME -> HomeScreen(
                    isServiceEnabled = settings.isServiceEnabled,
                    onToggleService = viewModel::toggleService,
                    todaySalesTotal = todaySalesTotal,
                    todaySalesCount = todaySalesCount,
                    denominations = settings.getDenominationsList(),
                    stockCounts = stockCounts
                )
                BottomNavTab.SIMULATOR -> SimulatorScreen(
                    defaultSender = settings.allowedSender,
                    outbox = simulationOutbox,
                    onSimulate = viewModel::simulateIncomingSms
                )
                BottomNavTab.INVENTORY -> InventoryScreen(
                    denominations = settings.getDenominationsList(),
                    stockCounts = stockCounts,
                    lastSummary = lastImportSummary,
                    onImportCsv = viewModel::importCsv
                )
                BottomNavTab.LOGS -> LogsScreen(
                    soldCards = soldCards,
                    messageLogs = messageLogs,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery
                )
                BottomNavTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onSaveSettings = viewModel::saveSettings
                )
            }
        }
    }
}
