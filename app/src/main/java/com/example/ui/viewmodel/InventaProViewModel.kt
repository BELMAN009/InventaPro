package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.StockEntry
import com.example.data.model.TaskItem
import com.example.data.repository.InventaProRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StockFilter(val label: String) {
    ALL("Todos"),
    LOW_STOCK("Stock Bajo"),
    OUT_OF_STOCK("Sin Stock"),
    OPTIMAL("Óptimo")
}

enum class TaskFilter(val label: String) {
    PENDING("Pendientes"),
    REMINDERS("Con Recordatorio"),
    AUTO("Auto-generadas"),
    COMPLETED("Completadas"),
    ALL("Todas")
}

class InventaProViewModel(
    private val repository: InventaProRepository,
    private val currentUserId: String
) : ViewModel() {

    val productsState: StateFlow<UiState<List<Product>>> =
        repository.observeProducts(currentUserId)
            .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing products", error)
                emit(UiState.Error(error.message ?: "Error al cargar productos"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val stockEntriesState: StateFlow<UiState<List<StockEntry>>> =
        repository.observeStockEntries(currentUserId)
            .map<List<StockEntry>, UiState<List<StockEntry>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing stock entries", error)
                emit(UiState.Error(error.message ?: "Error al cargar historial de entradas"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val tasksState: StateFlow<UiState<List<TaskItem>>> =
        repository.observeTasks(currentUserId)
            .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing tasks", error)
                emit(UiState.Error(error.message ?: "Error al cargar tareas y recordatorios"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilter.ALL)
    val stockFilter: StateFlow<StockFilter> = _stockFilter.asStateFlow()

    private val _taskFilter = MutableStateFlow(TaskFilter.PENDING)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateStockFilter(filter: StockFilter) {
        _stockFilter.value = filter
    }

    fun updateTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun saveProduct(
        existingProductId: String?,
        name: String,
        code: String,
        category: String,
        unit: String,
        supplier: String,
        quantity: Long,
        minStock: Long,
        onLowStockDetected: (Product) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val isLow = quantity <= minStock
            if (existingProductId.isNullOrBlank()) {
                val result = repository.createProduct(
                    name = name,
                    code = code,
                    category = category,
                    unit = unit,
                    supplier = supplier,
                    quantity = quantity,
                    minStock = minStock
                )
                result.onSuccess { newId ->
                    _snackbarMessage.value = "Producto \"$name\" registrado correctamente"
                    if (isLow) {
                        val created = Product(
                            id = newId,
                            userId = currentUserId,
                            name = name,
                            code = code,
                            category = category,
                            unit = unit,
                            supplier = supplier,
                            quantity = quantity,
                            minStock = minStock
                        )
                        ensureAutomaticRestockTask(created)
                        onLowStockDetected(created)
                    }
                }.onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Error al guardar producto"
                }
            } else {
                val result = repository.updateProduct(
                    productId = existingProductId,
                    name = name,
                    code = code,
                    category = category,
                    unit = unit,
                    supplier = supplier,
                    quantity = quantity,
                    minStock = minStock
                )
                result.onSuccess {
                    _snackbarMessage.value = "Producto \"$name\" actualizado"
                    if (isLow) {
                        val updated = Product(
                            id = existingProductId,
                            userId = currentUserId,
                            name = name,
                            code = code,
                            category = category,
                            unit = unit,
                            supplier = supplier,
                            quantity = quantity,
                            minStock = minStock
                        )
                        ensureAutomaticRestockTask(updated)
                        onLowStockDetected(updated)
                    }
                }.onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Error al actualizar producto"
                }
            }
            _isBusy.value = false
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.deleteProduct(product.id)
            result.onSuccess {
                _snackbarMessage.value = "Producto \"${product.name}\" eliminado"
            }.onFailure { e ->
                _snackbarMessage.value = e.localizedMessage ?: "No se pudo eliminar el producto"
            }
        }
    }

    fun registerStockEntry(
        product: Product,
        addedQuantity: Long,
        supplier: String,
        notes: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.registerStockEntry(
                product = product,
                addedQuantity = addedQuantity,
                supplier = supplier,
                notes = notes
            )
            result.onSuccess {
                val newQty = product.quantity + addedQuantity
                // If stock is now above minimum, auto-complete pending linked restock tasks
                if (newQty > product.minStock) {
                    val currentTasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
                    currentTasks.filter {
                        !it.completed && it.linkedProductId == product.id && it.autoGenerated
                    }.forEach { task ->
                        repository.toggleTaskCompleted(task.id, true)
                    }
                }
                _snackbarMessage.value =
                    "Entrada registrada: +$addedQuantity ${product.unit} de ${product.name} (Total: $newQty)"
            }.onFailure { e ->
                _snackbarMessage.value = e.localizedMessage ?: "Error al registrar entrada"
            }
            _isBusy.value = false
        }
    }

    fun saveTask(
        existingTaskId: String?,
        title: String,
        description: String,
        category: String,
        priority: String,
        completed: Boolean = false,
        dueDateMillis: Long,
        reminderEnabled: Boolean,
        linkedProductId: String = "",
        linkedProductName: String = "",
        autoGenerated: Boolean = false,
        onReminderScheduled: (TaskItem) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            if (existingTaskId.isNullOrBlank()) {
                val result = repository.createTask(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    dueDateMillis = dueDateMillis,
                    reminderEnabled = reminderEnabled,
                    linkedProductId = linkedProductId,
                    linkedProductName = linkedProductName,
                    autoGenerated = autoGenerated
                )
                result.onSuccess { newId ->
                    _snackbarMessage.value = "Tarea y recordatorio guardados"
                    if (reminderEnabled) {
                        onReminderScheduled(
                            TaskItem(
                                id = newId,
                                userId = currentUserId,
                                title = title,
                                description = description,
                                category = category,
                                priority = priority,
                                completed = false,
                                dueDateMillis = dueDateMillis,
                                reminderEnabled = true,
                                linkedProductId = linkedProductId,
                                linkedProductName = linkedProductName,
                                autoGenerated = autoGenerated
                            )
                        )
                    }
                }.onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Error al crear tarea"
                }
            } else {
                val result = repository.updateTask(
                    taskId = existingTaskId,
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    completed = completed,
                    dueDateMillis = dueDateMillis,
                    reminderEnabled = reminderEnabled,
                    linkedProductId = linkedProductId,
                    linkedProductName = linkedProductName,
                    autoGenerated = autoGenerated
                )
                result.onSuccess {
                    _snackbarMessage.value = "Tarea actualizada"
                }.onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Error al actualizar tarea"
                }
            }
            _isBusy.value = false
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        viewModelScope.launch {
            val newState = !task.completed
            repository.toggleTaskCompleted(task.id, newState).onSuccess {
                _snackbarMessage.value = if (newState) {
                    "Tarea completada: ${task.title}"
                } else {
                    "Tarea reabierta: ${task.title}"
                }
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task.id).onSuccess {
                _snackbarMessage.value = "Tarea eliminada"
            }
        }
    }

    private suspend fun ensureAutomaticRestockTask(product: Product) {
        val currentTasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val alreadyExists = currentTasks.any {
            !it.completed && (it.linkedProductId == product.id || it.title.contains(product.name, ignoreCase = true))
        }
        if (!alreadyExists) {
            val neededQty = ((product.minStock * 2L) - product.quantity).coerceAtLeast(product.minStock.coerceAtLeast(5L))
            val priority = if (product.isOutOfStock) "ALTA" else "MEDIA"
            repository.createTask(
                title = "Reabastecer ${product.name} (${product.code})",
                description = "Alerta automática de stock bajo: quedan ${product.quantity} ${product.unit} (mínimo ${product.minStock}). Solicitar +$neededQty ${product.unit} a ${product.supplier.ifBlank { "proveedor" }}.",
                category = "Reabastecimiento",
                priority = priority,
                dueDateMillis = System.currentTimeMillis() + 3600_000L * 2,
                reminderEnabled = true,
                linkedProductId = product.id,
                linkedProductName = product.name,
                autoGenerated = true
            )
        }
    }

    fun syncAutomaticLowStockReminders(onNotifyLowStock: (List<Product>) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val products = (productsState.value as? UiState.Success)?.data.orEmpty()
            val lowStockList = products.filter { it.isLowStock }
            lowStockList.forEach { product ->
                ensureAutomaticRestockTask(product)
            }
            if (lowStockList.isEmpty()) {
                _snackbarMessage.value = "Todo el inventario está en nivel óptimo"
            } else {
                onNotifyLowStock(lowStockList)
                _snackbarMessage.value =
                    "Se sincronizaron ${lowStockList.size} alertas y recordatorios automáticos"
            }
            _isBusy.value = false
        }
    }

    fun seedSampleCatalogAndTasks(onSeeded: (List<Product>) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val sampleProducts = listOf(
                    SampleProductSeed("Aceite Vegetal 1L", "AB-101", "Abarrotes", "pza", "Distribuidora Central", 4L, 12L),
                    SampleProductSeed("Arroz Súper Extra 1kg", "AB-102", "Abarrotes", "bolsa", "Granos del Norte", 25L, 10L),
                    SampleProductSeed("Cuaderno Profesional 100h", "PAP-201", "Papelería", "pza", "Papelera Nacional", 6L, 15L),
                    SampleProductSeed("Caja Bolígrafos Punto Medio", "PAP-204", "Papelería", "caja", "Papelera Nacional", 0L, 5L),
                    SampleProductSeed("Leche Entera Ultrapasteurizada 1L", "LAC-301", "Lácteos", "litro", "Lácteos del Valle", 18L, 10L),
                    SampleProductSeed("Detergente Multiusos 1kg", "LIM-401", "Limpieza", "bolsa", "Químicos Express", 3L, 8L)
                )

                val createdLowStock = mutableListOf<Product>()
                for (seed in sampleProducts) {
                    val res = repository.createProduct(
                        name = seed.name,
                        code = seed.code,
                        category = seed.category,
                        unit = seed.unit,
                        supplier = seed.supplier,
                        quantity = seed.quantity,
                        minStock = seed.minStock
                    )
                    val id = res.getOrNull() ?: continue
                    val prod = Product(
                        id = id,
                        userId = currentUserId,
                        name = seed.name,
                        code = seed.code,
                        category = seed.category,
                        unit = seed.unit,
                        supplier = seed.supplier,
                        quantity = seed.quantity,
                        minStock = seed.minStock
                    )
                    if (prod.isLowStock) {
                        createdLowStock.add(prod)
                        ensureAutomaticRestockTask(prod)
                    }
                }

                repository.createTask(
                    title = "Verificación semanal de caducidades en Lácteos",
                    description = "Revisar lotes de leche y derivados en refrigerador principal.",
                    category = "Inventario",
                    priority = "MEDIA",
                    dueDateMillis = System.currentTimeMillis() + 3600_000L * 6,
                    reminderEnabled = true,
                    autoGenerated = false
                )

                onSeeded(createdLowStock)
                _snackbarMessage.value =
                    "Catálogo de ejemplo y recordatorios automáticos creados en Firebase"
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Error al cargar datos de ejemplo"
            } finally {
                _isBusy.value = false
            }
        }
    }

    private data class SampleProductSeed(
        val name: String,
        val code: String,
        val category: String,
        val unit: String,
        val supplier: String,
        val quantity: Long,
        val minStock: Long
    )

    private companion object {
        const val TAG = "InventaProVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
