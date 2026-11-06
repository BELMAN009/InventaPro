package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Product
import com.example.data.model.StockEntry
import com.example.data.model.TaskItem
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InventaProRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google antes de acceder a Firestore.")
    }

    // --- PRODUCTS ---

    fun observeProducts(userId: String = requireUserId()): Flow<List<Product>> = flow {
        val path = "products"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserProducts(): Result<List<Product>> = runCatching {
        val uid = requireUserId()
        val path = "products"
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                Product::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getProductById(productId: String): Result<Product> = runCatching {
        val path = "products/$productId"
        try {
            val doc = db.collection("products").document(productId).get().await()
            doc.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalArgumentException("Producto no encontrado: $productId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createProduct(
        name: String,
        code: String,
        category: String,
        unit: String,
        supplier: String,
        quantity: Long,
        minStock: Long
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("products").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim(),
            "code" to code.trim(),
            "category" to category.trim(),
            "unit" to unit.trim(),
            "supplier" to supplier.trim(),
            "quantity" to quantity.coerceAtLeast(0L),
            "minStock" to minStock.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProduct(
        productId: String,
        name: String,
        code: String,
        category: String,
        unit: String,
        supplier: String,
        quantity: Long,
        minStock: Long
    ): Result<Unit> = runCatching {
        requireUserId()
        require(productId.isNotBlank()) { "El ID del producto no puede estar vacío" }
        val docRef = db.collection("products").document(productId)
        val updates = mapOf(
            "name" to name.trim(),
            "code" to code.trim(),
            "category" to category.trim(),
            "unit" to unit.trim(),
            "supplier" to supplier.trim(),
            "quantity" to quantity.coerceAtLeast(0L),
            "minStock" to minStock.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- STOCK ENTRIES ---

    fun observeStockEntries(userId: String = requireUserId()): Flow<List<StockEntry>> = flow {
        val path = "stock_entries"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StockEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun registerStockEntry(
        product: Product,
        addedQuantity: Long,
        supplier: String,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(product.id.isNotBlank()) { "Producto inválido" }
        require(addedQuantity > 0L) { "La cantidad de entrada debe ser mayor a 0" }

        val previousQty = product.quantity.coerceAtLeast(0L)
        val newQty = previousQty + addedQuantity
        val productRef = db.collection("products").document(product.id)
        val entryRef = db.collection("stock_entries").document()

        val entryPayload = mapOf(
            "userId" to uid,
            "productId" to product.id,
            "productName" to product.name,
            "productCode" to product.code,
            "addedQuantity" to addedQuantity,
            "previousQuantity" to previousQty,
            "newQuantity" to newQty,
            "supplier" to supplier.trim().ifBlank { product.supplier },
            "notes" to notes.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val productUpdate = mapOf(
            "quantity" to newQty,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(entryRef, entryPayload)
            batch.update(productRef, productUpdate)
            batch.commit().await()
            entryRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, entryRef.path)
            throw e
        }
    }

    // --- TASKS & AUTOMATIC REMINDERS ---

    fun observeTasks(userId: String = requireUserId()): Flow<List<TaskItem>> = flow {
        val path = "tasks"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TaskItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<TaskItem> { it.completed }
                            .thenBy { if (it.dueDateMillis > 0L) it.dueDateMillis else Long.MAX_VALUE }
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserTasks(): Result<List<TaskItem>> = runCatching {
        val uid = requireUserId()
        val path = "tasks"
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                TaskItem::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getTaskById(taskId: String): Result<TaskItem> = runCatching {
        val path = "tasks/$taskId"
        try {
            val doc = db.collection("tasks").document(taskId).get().await()
            doc.toObject(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalArgumentException("Tarea no encontrada: $taskId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createTask(
        title: String,
        description: String = "",
        category: String = "Reabastecimiento",
        priority: String = "ALTA",
        dueDateMillis: Long = System.currentTimeMillis() + 3600_000L * 4,
        reminderEnabled: Boolean = true,
        linkedProductId: String = "",
        linkedProductName: String = "",
        autoGenerated: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("tasks").document()
        val validPriority = if (priority in listOf("ALTA", "MEDIA", "BAJA")) priority else "ALTA"
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to category.trim().ifBlank { "Operativa" },
            "priority" to validPriority,
            "completed" to false,
            "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
            "reminderEnabled" to reminderEnabled,
            "linkedProductId" to linkedProductId.trim(),
            "linkedProductName" to linkedProductName.trim(),
            "autoGenerated" to autoGenerated,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleTaskCompleted(taskId: String, completed: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        val updates = mapOf(
            "completed" to completed,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        category: String,
        priority: String,
        completed: Boolean,
        dueDateMillis: Long,
        reminderEnabled: Boolean,
        linkedProductId: String = "",
        linkedProductName: String = "",
        autoGenerated: Boolean = false
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        val validPriority = if (priority in listOf("ALTA", "MEDIA", "BAJA")) priority else "MEDIA"
        val updates = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to category.trim(),
            "priority" to validPriority,
            "completed" to completed,
            "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
            "reminderEnabled" to reminderEnabled,
            "linkedProductId" to linkedProductId.trim(),
            "linkedProductName" to linkedProductName.trim(),
            "autoGenerated" to autoGenerated,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
