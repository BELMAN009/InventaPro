package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.ProductCategories
import com.example.data.model.StockEntry
import com.example.data.model.TaskItem
import com.example.ui.components.ProductFormDialog
import com.example.ui.components.StockEntryDialog
import com.example.ui.components.TaskFormDialog
import com.example.ui.theme.StatusCriticalRed
import com.example.ui.theme.StatusOptimalGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.viewmodel.InventaProViewModel
import com.example.ui.viewmodel.StockFilter
import com.example.ui.viewmodel.TaskFilter
import com.example.ui.viewmodel.UiState
import com.example.util.ReminderNotificationHelper

enum class InventaTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Alertas", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive, "nav_dashboard"),
    INVENTORY("Inventario", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_inventory"),
    TASKS("Tareas", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    ENTRIES("Entradas", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "nav_entries")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventaProMainScreen(
    viewModel: InventaProViewModel,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val stockEntriesState by viewModel.stockEntriesState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val stockFilter by viewModel.stockFilter.collectAsStateWithLifecycle()
    val taskFilter by viewModel.taskFilter.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(InventaTab.DASHBOARD) }
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var preselectedEntryProduct by remember { mutableStateOf<Product?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var preselectedTaskProduct by remember { mutableStateOf<Product?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* handled gracefully */ }
    )

    LaunchedEffect(Unit) {
        ReminderNotificationHelper.ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (currentTab != InventaTab.DASHBOARD) {
        BackHandler {
            currentTab = InventaTab.DASHBOARD
        }
    }

    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val entries = (stockEntriesState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val lowStockProducts = remember(products) { products.filter { it.isLowStock } }
    val pendingTasksCount = remember(tasks) { tasks.count { !it.completed } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "InventaPro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail ?: "Sesión activa en Firebase",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.syncAutomaticLowStockReminders { lowList ->
                                    lowList.firstOrNull()?.let {
                                        ReminderNotificationHelper.showLowStockNotification(context, it)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("sync_reminders_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sincronizar recordatorios automáticos"
                            )
                        }
                        IconButton(
                            onClick = {
                                ReminderNotificationHelper.sendLowStockEmailAlert(
                                    context = context,
                                    lowStockProducts = lowStockProducts,
                                    recipientEmail = userEmail
                                )
                            },
                            modifier = Modifier.testTag("send_email_alert_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Enviar alerta de stock bajo por correo"
                            )
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Cerrar sesión"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar {
                        InventaTab.entries.forEach { tab ->
                            val badgeCount = when (tab) {
                                InventaTab.DASHBOARD -> lowStockProducts.size
                                InventaTab.TASKS -> pendingTasksCount
                                else -> 0
                            }
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text(badgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    }
                                },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (currentTab) {
                    InventaTab.DASHBOARD, InventaTab.TASKS -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingTask = null
                                preselectedTaskProduct = null
                                showTaskDialog = true
                            },
                            icon = { Icon(Icons.Default.AlarmOn, contentDescription = null) },
                            text = { Text("Nueva Tarea") },
                            modifier = Modifier.testTag("fab_add_task")
                        )
                    }
                    InventaTab.INVENTORY -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingProduct = null
                                showProductDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Nuevo Producto") },
                            modifier = Modifier.testTag("fab_add_product")
                        )
                    }
                    InventaTab.ENTRIES -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                preselectedEntryProduct = null
                                showEntryDialog = true
                            },
                            icon = { Icon(Icons.Default.AddBox, contentDescription = null) },
                            text = { Text("Registrar Entrada") },
                            modifier = Modifier.testTag("fab_add_entry")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InventaTab.entries.forEach { tab ->
                            val badgeCount = when (tab) {
                                InventaTab.DASHBOARD -> lowStockProducts.size
                                InventaTab.TASKS -> pendingTasksCount
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text(badgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    }
                                },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentTab) {
                        InventaTab.DASHBOARD -> DashboardTabContent(
                            productsState = productsState,
                            tasksState = tasksState,
                            entriesCount = entries.size,
                            onSeedSampleData = {
                                viewModel.seedSampleCatalogAndTasks { seededLow ->
                                    seededLow.firstOrNull()?.let {
                                        ReminderNotificationHelper.showLowStockNotification(context, it)
                                    }
                                }
                            },
                            onRegisterEntryForProduct = { prod ->
                                preselectedEntryProduct = prod
                                showEntryDialog = true
                            },
                            onCreateReminderForProduct = { prod ->
                                editingTask = null
                                preselectedTaskProduct = prod
                                showTaskDialog = true
                            },
                            onSendEmailReport = {
                                ReminderNotificationHelper.sendLowStockEmailAlert(
                                    context = context,
                                    lowStockProducts = lowStockProducts,
                                    recipientEmail = userEmail
                                )
                            },
                            onToggleTask = { viewModel.toggleTaskCompleted(it) },
                            onTriggerTaskNotification = { task ->
                                ReminderNotificationHelper.showTaskReminderNotification(context, task)
                            },
                            onAddProductClick = {
                                editingProduct = null
                                showProductDialog = true
                            }
                        )

                        InventaTab.INVENTORY -> InventoryTabContent(
                            productsState = productsState,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            stockFilter = stockFilter,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onCategoryChange = viewModel::updateSelectedCategory,
                            onStockFilterChange = viewModel::updateStockFilter,
                            onEditProduct = { prod ->
                                editingProduct = prod
                                showProductDialog = true
                            },
                            onDeleteProduct = viewModel::deleteProduct,
                            onRegisterEntry = { prod ->
                                preselectedEntryProduct = prod
                                showEntryDialog = true
                            },
                            onCreateReminder = { prod ->
                                editingTask = null
                                preselectedTaskProduct = prod
                                showTaskDialog = true
                            },
                            onSeedSampleData = {
                                viewModel.seedSampleCatalogAndTasks { seededLow ->
                                    seededLow.firstOrNull()?.let {
                                        ReminderNotificationHelper.showLowStockNotification(context, it)
                                    }
                                }
                            }
                        )

                        InventaTab.TASKS -> TasksTabContent(
                            tasksState = tasksState,
                            taskFilter = taskFilter,
                            onTaskFilterChange = viewModel::updateTaskFilter,
                            onToggleTask = viewModel::toggleTaskCompleted,
                            onEditTask = { task ->
                                editingTask = task
                                preselectedTaskProduct = null
                                showTaskDialog = true
                            },
                            onDeleteTask = viewModel::deleteTask,
                            onTriggerNotification = { task ->
                                ReminderNotificationHelper.showTaskReminderNotification(context, task)
                            },
                            onSyncLowStockReminders = {
                                viewModel.syncAutomaticLowStockReminders { lowList ->
                                    lowList.firstOrNull()?.let {
                                        ReminderNotificationHelper.showLowStockNotification(context, it)
                                    }
                                }
                            }
                        )

                        InventaTab.ENTRIES -> EntriesTabContent(
                            entriesState = stockEntriesState,
                            onNewEntryClick = {
                                preselectedEntryProduct = null
                                showEntryDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showProductDialog) {
        ProductFormDialog(
            initialProduct = editingProduct,
            onDismiss = { showProductDialog = false },
            onConfirm = { name, code, category, unit, supplier, quantity, minStock ->
                showProductDialog = false
                viewModel.saveProduct(
                    existingProductId = editingProduct?.id,
                    name = name,
                    code = code,
                    category = category,
                    unit = unit,
                    supplier = supplier,
                    quantity = quantity,
                    minStock = minStock,
                    onLowStockDetected = { lowProduct ->
                        ReminderNotificationHelper.showLowStockNotification(context, lowProduct)
                    }
                )
            }
        )
    }

    if (showEntryDialog) {
        StockEntryDialog(
            products = products,
            preselectedProduct = preselectedEntryProduct,
            onDismiss = { showEntryDialog = false },
            onConfirm = { product, addedQty, supplier, notes ->
                showEntryDialog = false
                viewModel.registerStockEntry(
                    product = product,
                    addedQuantity = addedQty,
                    supplier = supplier,
                    notes = notes
                )
            }
        )
    }

    if (showTaskDialog) {
        TaskFormDialog(
            initialTask = editingTask,
            products = products,
            preselectedProduct = preselectedTaskProduct,
            onDismiss = { showTaskDialog = false },
            onConfirm = { title, description, category, priority, dueDateMillis, reminderEnabled, linkedProductId, linkedProductName ->
                showTaskDialog = false
                viewModel.saveTask(
                    existingTaskId = editingTask?.id,
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    completed = editingTask?.completed ?: false,
                    dueDateMillis = dueDateMillis,
                    reminderEnabled = reminderEnabled,
                    linkedProductId = linkedProductId,
                    linkedProductName = linkedProductName,
                    autoGenerated = editingTask?.autoGenerated ?: false,
                    onReminderScheduled = { createdTask ->
                        ReminderNotificationHelper.showTaskReminderNotification(context, createdTask)
                    }
                )
            }
        )
    }
}

@Composable
private fun DashboardTabContent(
    productsState: UiState<List<Product>>,
    tasksState: UiState<List<TaskItem>>,
    entriesCount: Int,
    onSeedSampleData: () -> Unit,
    onRegisterEntryForProduct: (Product) -> Unit,
    onCreateReminderForProduct: (Product) -> Unit,
    onSendEmailReport: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onTriggerTaskNotification: (TaskItem) -> Unit,
    onAddProductClick: () -> Unit
) {
    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val lowStockProducts = remember(products) { products.filter { it.isLowStock } }
    val pendingTasks = remember(tasks) { tasks.filter { !it.completed } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF072A3A),
                                    Color(0xFF0B4F6C),
                                    Color(0xFF0D9488)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.07f),
                            radius = size.minDimension * 0.55f,
                            center = Offset(size.width * 0.9f, size.height * 0.2f)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Panel de Control y Recordatorios",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Monitorea existencias en tiempo real, recibe alertas de stock bajo y gestiona tareas operativas con recordatorios automáticos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAddProductClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF072A3A)
                                ),
                                modifier = Modifier.testTag("hero_add_product_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Registrar Producto")
                            }
                            OutlinedButton(
                                onClick = onSeedSampleData,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("seed_sample_data_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catálogo de Ejemplo")
                            }
                        }
                    }
                }
            }
        }

        // KPI Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiSummaryCard(
                    title = "Productos",
                    value = products.size.toString(),
                    subtitle = "En catálogo",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiSummaryCard(
                    title = "Stock Bajo",
                    value = lowStockProducts.size.toString(),
                    subtitle = "En alerta",
                    accentColor = if (lowStockProducts.isNotEmpty()) StatusCriticalRed else StatusOptimalGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiSummaryCard(
                    title = "Tareas Activas",
                    value = pendingTasks.size.toString(),
                    subtitle = "${pendingTasks.count { it.reminderEnabled }} con recordatorio",
                    accentColor = StatusWarningAmber,
                    modifier = Modifier.weight(1f)
                )
                KpiSummaryCard(
                    title = "Entradas",
                    value = entriesCount.toString(),
                    subtitle = "Movimientos",
                    accentColor = StatusOptimalGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Low-Stock Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (lowStockProducts.isNotEmpty()) StatusCriticalRed else StatusOptimalGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alertas de Stock Bajo (${lowStockProducts.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (lowStockProducts.isNotEmpty()) {
                    AssistChip(
                        onClick = onSendEmailReport,
                        label = { Text("Alerta por Correo") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        if (productsState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (lowStockProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusOptimalGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (products.isEmpty()) "Catálogo vacío" else "Sin alertas de faltantes",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (products.isEmpty()) {
                                    "Toca \"Catálogo de Ejemplo\" arriba o agrega tu primer producto para activar las alertas automáticas."
                                } else {
                                    "Todos tus productos superan el nivel mínimo de existencias establecido."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else {
            items(lowStockProducts, key = { "alert_${it.id}" }) { product ->
                LowStockAlertCard(
                    product = product,
                    onRegisterEntry = { onRegisterEntryForProduct(product) },
                    onCreateReminder = { onCreateReminderForProduct(product) }
                )
            }
        }

        // Upcoming Automatic Reminders & Tasks Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AlarmOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tareas y Recordatorios Automáticos (${pendingTasks.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No tienes tareas pendientes",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Las tareas de reabastecimiento se crean automáticamente cuando un producto cae en stock bajo, o puedes programar recordatorios personalizados con el botón \"Nueva Tarea\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingTasks.take(6), key = { "dash_task_${it.id}" }) { task ->
                TaskCardItem(
                    task = task,
                    onToggleCompleted = { onToggleTask(task) },
                    onEdit = {},
                    onDelete = {},
                    onTriggerNotification = { onTriggerTaskNotification(task) },
                    compact = true
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun InventoryTabContent(
    productsState: UiState<List<Product>>,
    searchQuery: String,
    selectedCategory: String,
    stockFilter: StockFilter,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStockFilterChange: (StockFilter) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onRegisterEntry: (Product) -> Unit,
    onCreateReminder: (Product) -> Unit,
    onSeedSampleData: () -> Unit
) {
    val allProducts = (productsState as? UiState.Success)?.data.orEmpty()
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory, stockFilter) {
        allProducts.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.code.contains(searchQuery, ignoreCase = true) ||
                    p.supplier.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "Todas" || p.category == selectedCategory
            val matchesStock = when (stockFilter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> p.isLowStock
                StockFilter.OUT_OF_STOCK -> p.isOutOfStock
                StockFilter.OPTIMAL -> !p.isLowStock
            }
            matchesQuery && matchesCat && matchesStock
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Buscar por nombre, código SKU o proveedor") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_products_input")
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = stockFilter == filter,
                        onClick = { onStockFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todas").plus(ProductCategories.ALL).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (allProducts.isEmpty()) {
                                "Aún no hay productos registrados"
                            } else {
                                "Sin coincidencias para tu filtro actual"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (allProducts.isEmpty()) {
                            Button(onClick = onSeedSampleData) {
                                Text("Cargar Catálogo de Ejemplo")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                ProductCatalogCard(
                    product = product,
                    onEdit = { onEditProduct(product) },
                    onDelete = { onDeleteProduct(product) },
                    onRegisterEntry = { onRegisterEntry(product) },
                    onCreateReminder = { onCreateReminder(product) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TasksTabContent(
    tasksState: UiState<List<TaskItem>>,
    taskFilter: TaskFilter,
    onTaskFilterChange: (TaskFilter) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onTriggerNotification: (TaskItem) -> Unit,
    onSyncLowStockReminders: () -> Unit
) {
    val allTasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val filteredTasks = remember(allTasks, taskFilter) {
        allTasks.filter { t ->
            when (taskFilter) {
                TaskFilter.PENDING -> !t.completed
                TaskFilter.REMINDERS -> t.reminderEnabled && !t.completed
                TaskFilter.AUTO -> t.autoGenerated
                TaskFilter.COMPLETED -> t.completed
                TaskFilter.ALL -> true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Motor de Recordatorios Automáticos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Genera tareas de reabastecimiento automáticamente para todos los productos con stock bajo y recibe notificaciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onSyncLowStockReminders,
                        modifier = Modifier.testTag("sync_auto_tasks_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sincronizar")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = taskFilter == filter,
                        onClick = { onTaskFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No hay tareas en esta vista",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Usa el botón \"Nueva Tarea\" o \"Sincronizar\" para programar recordatorios automáticos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskCardItem(
                    task = task,
                    onToggleCompleted = { onToggleTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) },
                    onTriggerNotification = { onTriggerNotification(task) },
                    compact = false
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun EntriesTabContent(
    entriesState: UiState<List<StockEntry>>,
    onNewEntryClick: () -> Unit
) {
    val entries = (entriesState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bitácora de Entradas de Mercancía",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Cada recepción incrementa automáticamente las existencias del producto en tiempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sin entradas de mercancía registradas",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = onNewEntryClick) {
                            Text("Registrar Primera Entrada")
                        }
                    }
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.productName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = StatusOptimalGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "+${entry.addedQuantity}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StatusOptimalGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "SKU: ${entry.productCode} • Stock: ${entry.previousQuantity} ➔ ${entry.newQuantity}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (entry.supplier.isNotBlank()) {
                            Text(
                                text = "Proveedor: ${entry.supplier}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (entry.notes.isNotBlank()) {
                            Text(
                                text = "Nota: ${entry.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val timeMillis = (entry.createdAt?.seconds ?: 0L) * 1000L
                        if (timeMillis > 0L) {
                            Text(
                                text = ReminderNotificationHelper.formatDateTime(timeMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun KpiSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun LowStockAlertCard(
    product: Product,
    onRegisterEntry: () -> Unit,
    onCreateReminder: () -> Unit
) {
    val badgeColor = if (product.isOutOfStock) StatusCriticalRed else StatusWarningAmber
    val badgeText = if (product.isOutOfStock) "SIN STOCK" else "POR AGOTARSE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("low_stock_card_${product.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Código: ${product.code} • ${product.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Disponible: ${product.quantity} ${product.unit} (Nivel mínimo: ${product.minStock} ${product.unit})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRegisterEntry,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Entrada")
                }
                OutlinedButton(
                    onClick = onCreateReminder,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AlarmOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recordatorio")
                }
            }
        }
    }
}

@Composable
private fun ProductCatalogCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegisterEntry: () -> Unit,
    onCreateReminder: () -> Unit
) {
    val statusColor = when {
        product.isOutOfStock -> StatusCriticalRed
        product.isLowStock -> StatusWarningAmber
        else -> StatusOptimalGreen
    }
    val statusLabel = when {
        product.isOutOfStock -> "AGOTADO"
        product.isLowStock -> "STOCK BAJO"
        else -> "ÓPTIMO"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_item_card_${product.code}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SKU: ${product.code} • ${product.category}" +
                                if (product.supplier.isNotBlank()) " • Prov: ${product.supplier}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Existencias: ${product.quantity} ${product.unit}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Mínimo: ${product.minStock} ${product.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { product.stockRatio },
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onRegisterEntry,
                        label = { Text("+ Entrada") },
                        leadingIcon = {
                            Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    AssistChip(
                        onClick = onCreateReminder,
                        label = { Text("Recordatorio") },
                        leadingIcon = {
                            Icon(Icons.Default.AlarmOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar producto"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar producto",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCardItem(
    task: TaskItem,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTriggerNotification: () -> Unit,
    compact: Boolean
) {
    val priorityColor = when (task.priority) {
        "ALTA" -> StatusCriticalRed
        "MEDIA" -> StatusWarningAmber
        else -> StatusOptimalGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCompleted() }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggleCompleted() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (task.autoGenerated) {
                            Text(
                                text = "• Auto-alerta",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusWarningAmber
                            )
                        }
                    }
                }
                Surface(
                    color = priorityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = task.priority,
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (task.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (task.reminderEnabled) {
                            ReminderNotificationHelper.formatDateTime(task.dueDateMillis)
                        } else {
                            "Sin recordatorio"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isOverdue) StatusCriticalRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onTriggerNotification) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Probar notificación de recordatorio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!compact) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar tarea"
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar tarea",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
