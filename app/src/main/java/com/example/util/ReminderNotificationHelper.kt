package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Product
import com.example.data.model.TaskItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderNotificationHelper {
    private const val CHANNEL_ID = "inventapro_reminders_channel"
    private const val CHANNEL_NAME = "Alertas de Stock y Recordatorios"
    private const val CHANNEL_DESC =
        "Notificaciones automáticas de reabastecimiento y recordatorios de tareas operativas"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showLowStockNotification(context: Context, product: Product) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            product.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = if (product.isOutOfStock) {
            "¡Producto Agotado!: ${product.name}"
        } else {
            "Alerta de Stock Bajo: ${product.name}"
        }

        val body = "Disponible: ${product.quantity} ${product.unit} (Mínimo: ${product.minStock} ${product.unit}). " +
                "Código: ${product.code} • Se programó recordatorio de reabastecimiento."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(statusTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(product.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun showTaskReminderNotification(context: Context, task: TaskItem) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueStr = formatDateTime(task.dueDateMillis)
        val body = buildString {
            if (task.description.isNotBlank()) {
                append(task.description).append(" • ")
            }
            append("Prioridad: ${task.priority} • Programado: $dueStr")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Recordatorio: ${task.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(task.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun sendLowStockEmailAlert(
        context: Context,
        lowStockProducts: List<Product>,
        recipientEmail: String?
    ) {
        val dateStr = formatDateTime(System.currentTimeMillis())
        val subject = "[InventaPro] Alerta de Abastecimiento (${lowStockProducts.size} productos en nivel bajo)"
        val body = buildString {
            appendLine("Reporte Automático de InventaPro - Control de Abastecimiento")
            appendLine("Fecha de corte: $dateStr")
            appendLine("------------------------------------------------------------")
            if (lowStockProducts.isEmpty()) {
                appendLine("Todos los productos del catálogo se encuentran por encima del stock mínimo.")
            } else {
                appendLine("Los siguientes productos han alcanzado o están por debajo de su nivel mínimo de existencias:")
                appendLine()
                lowStockProducts.forEachIndexed { index, p ->
                    val estado = if (p.isOutOfStock) "AGOTADO" else "STOCK BAJO"
                    val sugerido = ((p.minStock * 2) - p.quantity).coerceAtLeast(p.minStock)
                    appendLine("${index + 1}. [${p.code}] ${p.name} ($estado)")
                    appendLine("   • Categoría: ${p.category}")
                    appendLine("   • Existencia actual: ${p.quantity} ${p.unit} | Mínimo requerido: ${p.minStock} ${p.unit}")
                    appendLine("   • Pedido sugerido: +$sugerido ${p.unit}")
                    if (p.supplier.isNotBlank()) {
                        appendLine("   • Proveedor: ${p.supplier}")
                    }
                    appendLine()
                }
            }
            appendLine("------------------------------------------------------------")
            appendLine("Generado desde InventaPro Android • Sincronizado en Firebase Firestore")
        }

        val mailtoUri = Uri.parse("mailto:${recipientEmail.orEmpty()}")
        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val shareFallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Enviar alerta de stock bajo por correo"))
        } catch (_: Exception) {
            try {
                context.startActivity(Intent.createChooser(shareFallback, "Compartir reporte de stock bajo"))
            } catch (_: Exception) {
                // No handler available
            }
        }
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "Sin fecha"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))
        return sdf.format(Date(millis))
    }
}
