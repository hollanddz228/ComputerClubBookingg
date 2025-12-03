package com.example.computerclubbooking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.computerclubbooking.data.models.TimePackage

// -----------------------
// Выбор пакета
// -----------------------
@Composable
fun PackageItem(
    timePackage: TimePackage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    categoryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) categoryColor.copy(alpha = 0.3f) else Color(0xFF0D1527),
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp)
            .clickable { onSelect() }
    ) {
        Column {
            Text(timePackage.name, color = Color.White, fontSize = 16.sp)
            Text("${timePackage.hours} часов", color = Color.LightGray, fontSize = 12.sp)
            Text("${timePackage.price} ₸", color = categoryColor, fontSize = 16.sp)
        }
    }
}

// -----------------------
// SMS окно
// -----------------------
@Composable
fun SmsVerificationDialog(
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF081020)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("Введите SMS-код", color = Color.White, fontSize = 18.sp)

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = smsCode,
                    onValueChange = onSmsCodeChange,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A2332),
                        unfocusedContainerColor = Color(0xFF1A2332),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onCancel) {
                        Text("Отмена", color = Color.Gray)
                    }
                    Button(
                        onClick = onVerify,
                        colors = ButtonDefaults.buttonColors(Color(0xFF00C6FF))
                    ) {
                        Text("Подтвердить")
                    }
                }
            }
        }
    }
}
