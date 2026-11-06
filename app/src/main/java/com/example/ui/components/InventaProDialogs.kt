package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.ProductCategories
import com.example.data.model.ProductUnits
import com.example.data.model.TaskCategories
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriorities
import com.example.util.ReminderNotificationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductFormDialog(
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        code: String,
        category: String,
        unit: String,
        supplier: String,
        quantity: Long,
        minStock: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var code by remember {
        mutableStateOf(
            initialProduct?.code ?: "SKU-${(100..999).random()}"
        )
    }
    var category by remember {
        mutableStateOf(initialProduct?.category ?: ProductCategories.ALL.first())
    }
    var unit by remember {
        mutableStateOf(initialProduct?.unit ?: ProductUnits.ALL.first())
    }
    var supplier by remember { mutableStateOf(initialProduct?.supplier ?: "") }
    var quantityText by remember {
        mutableStateOf((initialProduct?.quantity ?: 10L).toString())
    }
    var minStockText by remember {
        mutableStateOf((initialProduct?.minStock ?: 5L).toString())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "Registrar Producto" else "Editar Producto",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Nombre del producto *") },
                    placeholder = { Text("Ej. Aceite Vegetal 1L") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it
                            validationError = null
                        },
                        label = { Text("Código / SKU *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_code_input")
                    )

                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Proveedor") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_supplier_input")
                    )
                }

                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductCategories.ALL.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text(
                    text = "Unidad de medida",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductUnits.ALL.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Existencia actual *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_quantity_input")
                    )

                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = {
                            minStockText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Stock mínimo *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_min_stock_input")
                    )
                }

                val parsedQty = quantityText.toLongOrNull() ?: 0L
                val parsedMin = minStockText.toLongOrNull() ?: 0L
                if (parsedQty <= parsedMin) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nota: La cantidad actual ($parsedQty $unit) está en o por debajo del stock mínimo ($parsedMin $unit). Se generará una alerta y un recordatorio automático de reabastecimiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedCode = code.trim()
                    val qty = quantityText.toLongOrNull()
                    val min = minStockText.toLongOrNull()
                    when {
                        trimmedName.isEmpty() -> validationError = "Ingresa el nombre del producto"
                        trimmedCode.isEmpty() -> validationError = "Ingresa el código del producto"
                        qty == null || qty < 0L -> validationError = "Ingresa una cantidad válida"
                        min == null || min < 0L -> validationError = "Ingresa un stock mínimo válido"
                        else -> onConfirm(
                            trimmedName,
                            trimmedCode,
                            category,
                            unit,
                            supplier.trim(),
                            qty,
                            min
                        )
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun StockEntryDialog(
    products: List<Product>,
    preselectedProduct: Product?,
    onDismiss: () -> Unit,
    onConfirm: (product: Product, addedQuantity: Long, supplier: String, notes: String) -> Unit
) {
    var selectedProduct by remember {
        mutableStateOf(preselectedProduct ?: products.firstOrNull())
    }
    var addedQuantityText by remember { mutableStateOf("10") }
    var supplier by remember {
        mutableStateOf(selectedProduct?.supplier.orEmpty())
    }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Entrada de Mercancía",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (products.isEmpty()) {
                    Text(
                        text = "Primero registra al menos un producto en el inventario para poder ingresar entradas de mercancía.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Selecciona el producto",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        products.forEach { prod ->
                            FilterChip(
                                selected = selectedProduct?.id == prod.id,
                                onClick = {
                                    selectedProduct = prod
                                    if (supplier.isBlank()) supplier = prod.supplier
                                },
                                label = { Text("${prod.name} (${prod.quantity} ${prod.unit})") }
                            )
                        }
                    }

                    selectedProduct?.let { prod ->
                        val added = addedQuantityText.toLongOrNull() ?: 0L
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Código: ${prod.code} • Stock mínimo: ${prod.minStock} ${prod.unit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Existencia: ${prod.quantity} ➔ ${prod.quantity + added} ${prod.unit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = addedQuantityText,
                        onValueChange = {
                            addedQuantityText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Cantidad que ingresa *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("entry_quantity_input")
                    )

                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Proveedor / Remisión") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("entry_supplier_input")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observaciones de entrega") },
                        placeholder = { Text("Ej. Pedido quincenal completo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("entry_notes_input")
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (products.isNotEmpty()) {
                Button(
                    onClick = {
                        val prod = selectedProduct
                        val qty = addedQuantityText.toLongOrNull()
                        when {
                            prod == null -> validationError = "Selecciona un producto"
                            qty == null || qty <= 0L -> validationError = "Ingresa una cantidad mayor a 0"
                            else -> onConfirm(prod, qty, supplier.trim(), notes.trim())
                        }
                    },
                    modifier = Modifier.testTag("save_entry_button")
                ) {
                    Text("Registrar Entrada")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TaskFormDialog(
    initialTask: TaskItem?,
    products: List<Product>,
    preselectedProduct: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDateMillis: Long,
        reminderEnabled: Boolean,
        linkedProductId: String,
        linkedProductName: String
    ) -> Unit
) {
    var title by remember {
        mutableStateOf(
            initialTask?.title
                ?: preselectedProduct?.let { "Reabastecer ${it.name} (${it.code})" }
                ?: ""
        )
    }
    var description by remember {
        mutableStateOf(
            initialTask?.description
                ?: preselectedProduct?.let {
                    "Quedan ${it.quantity} ${it.unit} (mínimo ${it.minStock}). Contactar a ${it.supplier.ifBlank { "proveedor" }}."
                }
                ?: ""
        )
    }
    var category by remember {
        mutableStateOf(
            initialTask?.category
                ?: if (preselectedProduct != null) "Reabastecimiento" else TaskCategories.ALL.first()
        )
    }
    var priority by remember {
        mutableStateOf(initialTask?.priority ?: "ALTA")
    }
    var reminderEnabled by remember {
        mutableStateOf(initialTask?.reminderEnabled ?: true)
    }
    var dueDateMillis by remember {
        mutableLongStateOf(
            initialTask?.dueDateMillis?.takeIf { it > 0L }
                ?: (System.currentTimeMillis() + 3600_000L * 2)
        )
    }
    var linkedProduct by remember {
        mutableStateOf(
            preselectedProduct
                ?: products.find { it.id == initialTask?.linkedProductId }
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "Nueva Tarea con Recordatorio" else "Editar Tarea y Recordatorio",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Título de la tarea *") },
                    placeholder = { Text("Ej. Solicitar pedido de abarrotes") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Instrucciones") },
                    placeholder = { Text("Detalles o notas para el recordatorio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input")
                )

                Text(
                    text = "Prioridad",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriorities.ALL.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                Text(
                    text = "Categoría de tarea",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategories.ALL.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                if (products.isNotEmpty()) {
                    Text(
                        text = "Vincular con producto (opcional)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = linkedProduct == null,
                            onClick = { linkedProduct = null },
                            label = { Text("Ninguno") }
                        )
                        products.forEach { prod ->
                            FilterChip(
                                selected = linkedProduct?.id == prod.id,
                                onClick = { linkedProduct = prod },
                                label = { Text(prod.name) }
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recordatorio Automático",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = ReminderNotificationHelper.formatDateTime(dueDateMillis),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                modifier = Modifier.testTag("task_reminder_switch")
                            )
                        }

                        if (reminderEnabled) {
                            Text(
                                text = "Programar recordatorio rápido:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        dueDateMillis = System.currentTimeMillis() + 3600_000L
                                    }
                                ) {
                                    Text("En 1 h")
                                }
                                OutlinedButton(
                                    onClick = {
                                        dueDateMillis = System.currentTimeMillis() + 3600_000L * 4
                                    }
                                ) {
                                    Text("En 4 h")
                                }
                                OutlinedButton(
                                    onClick = {
                                        dueDateMillis = System.currentTimeMillis() + 3600_000L * 24
                                    }
                                ) {
                                    Text("Mañana")
                                }
                                OutlinedButton(
                                    onClick = {
                                        dueDateMillis = System.currentTimeMillis() + 3600_000L * 72
                                    }
                                ) {
                                    Text("En 3 días")
                                }
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        validationError = "Ingresa el título de la tarea"
                    } else {
                        onConfirm(
                            trimmedTitle,
                            description.trim(),
                            category,
                            priority,
                            dueDateMillis,
                            reminderEnabled,
                            linkedProduct?.id.orEmpty(),
                            linkedProduct?.name.orEmpty()
                        )
                    }
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Guardar Tarea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
