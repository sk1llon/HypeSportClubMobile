package com.example.mobilka.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.FitnessGoal
import com.example.mobilka.data.Gender
import com.example.mobilka.ui.theme.SportOrange
import com.example.mobilka.ui.theme.SportOrangeDark
import com.example.mobilka.ui.theme.SportOrangeLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var birthDateValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedFitnessGoal by remember { mutableStateOf(FitnessGoal.MAINTENANCE) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val repository = remember { FirebaseRepo.instance }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Логотип
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SportOrange, SportOrangeDark, SportOrangeLight)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 56.sp
                    )
                    Text(
                        text = "HSC",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "HypeSportClub",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Переключатель режима
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "Вход",
                        selected = isLoginMode,
                        onClick = { 
                            isLoginMode = true
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Регистрация",
                        selected = !isLoginMode,
                        onClick = { 
                            isLoginMode = false
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Поля для регистрации (ФИО и дата рождения)
            AnimatedVisibility(
                visible = !isLoginMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column {
                    // Фамилия
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { 
                            lastName = it
                            errorMessage = null
                        },
                        label = { Text("Фамилия") },
                        placeholder = { Text("Иванов") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Имя
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { 
                            firstName = it
                            errorMessage = null
                        },
                        label = { Text("Имя") },
                        placeholder = { Text("Иван") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Отчество
                    OutlinedTextField(
                        value = middleName,
                        onValueChange = { 
                            middleName = it
                            errorMessage = null
                        },
                        label = { Text("Отчество") },
                        placeholder = { Text("Иванович") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Дата рождения
                    OutlinedTextField(
                        value = birthDateValue,
                        onValueChange = { newValue ->
                            // Автоформатирование даты с сохранением позиции курсора
                            val digits = newValue.text.filter { it.isDigit() }
                            val formatted = when {
                                digits.length <= 2 -> digits
                                digits.length <= 4 -> "${digits.take(2)}.${digits.drop(2)}"
                                else -> "${digits.take(2)}.${digits.substring(2, 4)}.${digits.drop(4).take(4)}"
                            }
                            birthDateValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                            errorMessage = null
                        },
                        label = { Text("Дата рождения") },
                        placeholder = { Text("ДД.ММ.ГГГГ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Пол
                    Text(
                        text = "Пол",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.entries.forEach { gender ->
                            FilterChip(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender },
                                label = { 
                                    Text(
                                        text = gender.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Рост и вес в одной строке
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { 
                                if (it.all { c -> c.isDigit() } && it.length <= 3) {
                                    height = it
                                }
                                errorMessage = null
                            },
                            label = { Text("Рост (см)") },
                            placeholder = { Text("170") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { 
                                if (it.all { c -> c.isDigit() || c == '.' } && it.count { c -> c == '.' } <= 1 && it.length <= 5) {
                                    weight = it
                                }
                                errorMessage = null
                            },
                            label = { Text("Вес (кг)") },
                            placeholder = { Text("70") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Цель занятий
                    Text(
                        text = "Цель занятий",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FitnessGoal.entries.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (row.size == 1) Arrangement.Center else Arrangement.spacedBy(8.dp)
                            ) {
                                if (row.size == 1) {
                                    // Центрируем одиночный элемент
                                    row.forEach { goal ->
                                        FilterChip(
                                            selected = selectedFitnessGoal == goal,
                                            onClick = { selectedFitnessGoal = goal },
                                            label = { 
                                                Text(
                                                    text = goal.displayName,
                                                    style = MaterialTheme.typography.bodySmall
                                                ) 
                                            }
                                        )
                                    }
                                } else {
                                    row.forEach { goal ->
                                        FilterChip(
                                            selected = selectedFitnessGoal == goal,
                                            onClick = { selectedFitnessGoal = goal },
                                            label = { 
                                                Text(
                                                    text = goal.displayName,
                                                    style = MaterialTheme.typography.bodySmall
                                                ) 
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Поле email
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it.trim()
                    errorMessage = null
                },
                label = { Text("Email") },
                placeholder = { Text("example@mail.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Поле пароля
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = { Text("Пароль") },
                placeholder = { Text("Минимум 6 символов") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Скрыть" else "Показать",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            // Подтверждение пароля для регистрации
            AnimatedVisibility(
                visible = !isLoginMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Подтвердите пароль") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Сообщение об ошибке
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопка действия
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        
                        // Валидация
                        if (email.isBlank() || !email.contains("@")) {
                            errorMessage = "Введите корректный email"
                            isLoading = false
                            return@launch
                        }
                        
                        if (password.length < 6) {
                            errorMessage = "Пароль должен содержать минимум 6 символов"
                            isLoading = false
                            return@launch
                        }
                        
                        val result = if (isLoginMode) {
                            repository.login(email, password)
                        } else {
                            // Валидация для регистрации
                            if (lastName.isBlank()) {
                                isLoading = false
                                errorMessage = "Введите фамилию"
                                return@launch
                            }
                            if (firstName.isBlank()) {
                                isLoading = false
                                errorMessage = "Введите имя"
                                return@launch
                            }
                            if (!isValidBirthDate(birthDateValue.text)) {
                                isLoading = false
                                errorMessage = "Введите корректную дату рождения в формате ДД.ММ.ГГГГ"
                                return@launch
                            }
                            if (password != confirmPassword) {
                                isLoading = false
                                errorMessage = "Пароли не совпадают"
                                return@launch
                            }
                            repository.register(
                                email = email,
                                password = password,
                                lastName = lastName,
                                firstName = firstName,
                                middleName = middleName,
                                birthDate = birthDateValue.text,
                                gender = selectedGender.name,
                                height = height.toFloatOrNull() ?: 0f,
                                weight = weight.toFloatOrNull() ?: 0f,
                                fitnessGoal = selectedFitnessGoal.name
                            )
                        }
                        
                        isLoading = false
                        result.fold(
                            onSuccess = { onAuthSuccess() },
                            onFailure = { e ->
                                errorMessage = when {
                                    e.message?.contains("password is invalid", ignoreCase = true) == true ||
                                    e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                                        "Неверный email или пароль"
                                    e.message?.contains("no user record", ignoreCase = true) == true ->
                                        "Пользователь с таким email не найден"
                                    e.message?.contains("email address is already", ignoreCase = true) == true ->
                                        "Пользователь с таким email уже зарегистрирован"
                                    e.message?.contains("network", ignoreCase = true) == true ->
                                        "Ошибка сети. Проверьте подключение к интернету"
                                    e.message?.contains("badly formatted", ignoreCase = true) == true ->
                                        "Введите корректный email-адрес"
                                    else -> "Ошибка: ${e.message}"
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Войти" else "Зарегистрироваться",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

private fun isValidBirthDate(dateStr: String): Boolean {
    if (dateStr.length != 10) return false
    val parts = dateStr.split(".")
    if (parts.size != 3) return false
    val day = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    if (year < 1900 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) return false
    if (month < 1 || month > 12) return false
    val maxDay = when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return day in 1..maxDay
}
