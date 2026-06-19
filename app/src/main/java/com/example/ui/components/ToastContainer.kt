package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ToastMessage
import com.example.ui.ToastType
import com.example.ui.theme.*

@Composable
fun ToastContainer(
    toast: ToastMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (toast.type) {
        ToastType.SUCCESS -> BentoGreenContainer
        ToastType.ERROR -> BentoPinkContainer
        ToastType.WARNING -> BentoAmberContainer
        ToastType.INFO -> BentoPurpleBg
    }
    val borderColor = when (toast.type) {
        ToastType.SUCCESS -> BentoGreenText
        ToastType.ERROR -> Color(0xFFFF8A80)
        ToastType.WARNING -> BentoAmberText
        ToastType.INFO -> BentoPurpleAccent
    }
    val textColor = when (toast.type) {
        ToastType.SUCCESS -> BentoGreenText
        ToastType.ERROR -> Color(0xFFC62828) // High visibility dark red
        ToastType.WARNING -> BentoAmberText
        ToastType.INFO -> BentoTextDeep
    }
    val icon = when (toast.type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle
        ToastType.ERROR -> Icons.Default.Error
        ToastType.WARNING -> Icons.Default.Warning
        ToastType.INFO -> Icons.Default.Info
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "${toast.type} icon",
                tint = textColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = toast.message,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close notifications",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
