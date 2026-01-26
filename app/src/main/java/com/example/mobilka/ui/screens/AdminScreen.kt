package com.example.mobilka.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka.data.AppLanguage
import com.example.mobilka.data.AppTheme
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.Gender
import com.example.mobilka.data.GroupWorkout
import com.example.mobilka.data.SettingsManager
import com.example.mobilka.data.Strings
import com.example.mobilka.data.Subscription
import com.example.mobilka.data.Trainer
import com.example.mobilka.data.TrainerSpecialization
import com.example.mobilka.data.User
import com.example.mobilka.data.UserRole
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Варианты сортировки
enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC;
    
    fun getDisplayName(lang: AppLanguage): String = when (this) {
        NAME_ASC -> Strings.sortAZ(lang)
        NAME_DESC -> Strings.sortZA(lang)
        DATE_ASC -> Strings.sortDateAsc(lang)
        DATE_DESC -> Strings.sortDateDesc(lang)
    }
}

// Данные тренера для создания
data class TrainerFormData(
    val phone: String = "",
    val experience: Int = 0,
    val specialization: TrainerSpecialization = TrainerSpecialization.FITNESS,
    val pricePerTraining: Int = 0,
    val photoUrl: String = ""
)

enum class AdminViewMode {
    USERS,
    SUBSCRIPTIONS,
    GROUP_WORKOUTS,
    ADD_USER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Настройки приложения
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme by settingsManager.theme.collectAsState()
    val currentLanguage by settingsManager.language.collectAsState()
    val lang = currentLanguage
    val isDarkTheme = currentTheme == AppTheme.DARK
    
    var currentView by remember { mutableStateOf(AdminViewMode.USERS) }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var subscriptions by remember { mutableStateOf<List<Subscription>>(emptyList()) }
    var groupWorkouts by remember { mutableStateOf<List<GroupWorkout>>(emptyList()) }
    var trainers by remember { mutableStateOf<List<Trainer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var showAddGroupWorkoutDialog by remember { mutableStateOf(false) }
    var showEditSubscriptionDialog by remember { mutableStateOf<Subscription?>(null) }
    var showEditUserDialog by remember { mutableStateOf<User?>(null) }
    var showDeleteSubscriptionDialog by remember { mutableStateOf<Subscription?>(null) }
    var showDeleteUserDialog by remember { mutableStateOf<User?>(null) }
    var showDeleteGroupWorkoutDialog by remember { mutableStateOf<GroupWorkout?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Для добавления пользователя
    var addUserSelectedRole by remember { mutableStateOf<UserRole?>(null) }
    var previousView by remember { mutableStateOf(AdminViewMode.USERS) }
    
    // Поиск и сортировка
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    
    // Функция парсинга даты DD.MM.YYYY в сортируемый формат YYYYMMDD
    fun parseDateForSorting(dateStr: String): String {
        return try {
            val parts = dateStr.split(".")
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2].padStart(4, '0')
                "$year$month$day"
            } else {
                "00000000"
            }
        } catch (e: Exception) {
            "00000000"
        }
    }
    
    // Отфильтрованный и отсортированный список пользователей
    val filteredUsers = remember(users, searchQuery, selectedSortOption) {
        var filtered = users
        
        // Фильтрация по поиску
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            filtered = filtered.filter { user ->
                user.fullName.lowercase().contains(query) ||
                user.email.lowercase().contains(query) ||
                user.birthDate.contains(query)
            }
        }
        
        // Сортировка
        when (selectedSortOption) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.fullName.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.fullName.lowercase() }
            SortOption.DATE_ASC -> filtered.sortedBy { parseDateForSorting(it.birthDate) }
            SortOption.DATE_DESC -> filtered.sortedByDescending { parseDateForSorting(it.birthDate) }
        }
    }
    
    // Загрузка данных
    LaunchedEffect(currentView) {
        if (currentView == AdminViewMode.ADD_USER) return@LaunchedEffect
        
        isLoading = true
        scope.launch {
            try {
                when (currentView) {
                    AdminViewMode.USERS -> {
                        users = repository.getAllUsers()
                    }
                    AdminViewMode.SUBSCRIPTIONS -> {
                        subscriptions = repository.getAllSubscriptions()
                    }
                    AdminViewMode.GROUP_WORKOUTS -> {
                        groupWorkouts = repository.getAllGroupWorkouts()
                        trainers = repository.getAllTrainers()
                    }
                    AdminViewMode.ADD_USER -> { /* Не загружаем */ }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "${Strings.loadingError(lang)}: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Подписка на изменения пользователей
    LaunchedEffect(Unit) {
        repository.observeAllUsers().collect { usersList ->
            if (currentView == AdminViewMode.USERS) {
                users = usersList
            }
        }
    }
    
    // Обновление данных при переключении на вкладку
    LaunchedEffect(currentView) {
        if (currentView == AdminViewMode.SUBSCRIPTIONS) {
            subscriptions = repository.getAllSubscriptions()
        }
        if (currentView == AdminViewMode.USERS) {
            users = repository.getAllUsers()
        }
        if (currentView == AdminViewMode.GROUP_WORKOUTS) {
            groupWorkouts = repository.getAllGroupWorkouts()
            trainers = repository.getAllTrainers()
        }
    }
    
    // Автоскрытие сообщений
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
    }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (currentView == AdminViewMode.ADD_USER) {
                        IconButton(
                            onClick = {
                                addUserSelectedRole = null
                                currentView = previousView
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Strings.back(lang)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = when (currentView) {
                            AdminViewMode.USERS -> Strings.usersManagement(lang)
                            AdminViewMode.SUBSCRIPTIONS -> Strings.subscriptionsManagement(lang)
                            AdminViewMode.GROUP_WORKOUTS -> Strings.groupWorkoutsManagement(lang)
                            AdminViewMode.ADD_USER -> if (addUserSelectedRole == null) 
                                Strings.newUser(lang)
                            else when (addUserSelectedRole!!) {
                                UserRole.CLIENT -> Strings.client(lang)
                                UserRole.TRAINER -> Strings.trainer(lang)
                                UserRole.ADMIN -> Strings.admin(lang)
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    if (currentView != AdminViewMode.ADD_USER) {
                        // Кнопка смены темы
                        IconButton(
                            onClick = {
                                val newTheme = if (isDarkTheme) AppTheme.LIGHT else AppTheme.DARK
                                settingsManager.setTheme(newTheme)
                            }
                        ) {
                            // Используем Text с эмодзи вместо недоступных иконок
                            Text(
                                text = if (isDarkTheme) "☀️" else "🌙",
                                fontSize = 20.sp
                            )
                        }
                        // Кнопка выхода
                        IconButton(
                            onClick = {
                                repository.logout()
                                onLogout()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = Strings.logout(lang),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            when (currentView) {
                AdminViewMode.USERS -> {
                    FloatingActionButton(
                        onClick = { 
                            previousView = currentView
                            addUserSelectedRole = null
                            currentView = AdminViewMode.ADD_USER 
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = Strings.newUser(lang)
                        )
                    }
                }
                AdminViewMode.SUBSCRIPTIONS -> {
                    FloatingActionButton(
                        onClick = { showAddSubscriptionDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = Strings.newSubscription(lang)
                        )
                    }
                }
                AdminViewMode.GROUP_WORKOUTS -> {
                    FloatingActionButton(
                        onClick = { showAddGroupWorkoutDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = Strings.newGroupWorkout(lang)
                        )
                    }
                }
                AdminViewMode.ADD_USER -> { /* Нет FAB на экране добавления */ }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Кнопки переключения режимов (скрываем при добавлении пользователя)
            if (currentView != AdminViewMode.ADD_USER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMenuButton(
                        icon = Icons.Default.Person,
                        label = Strings.users(lang),
                        isSelected = currentView == AdminViewMode.USERS,
                        onClick = { currentView = AdminViewMode.USERS },
                        modifier = Modifier.weight(1f)
                    )
                    AdminMenuButton(
                        icon = Icons.Default.Star,
                        label = Strings.subscriptions(lang),
                        isSelected = currentView == AdminViewMode.SUBSCRIPTIONS,
                        onClick = { currentView = AdminViewMode.SUBSCRIPTIONS },
                        modifier = Modifier.weight(1f)
                    )
                    AdminMenuButton(
                        icon = Icons.Default.DateRange,
                        label = Strings.group(lang),
                        isSelected = currentView == AdminViewMode.GROUP_WORKOUTS,
                        onClick = { currentView = AdminViewMode.GROUP_WORKOUTS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Контент
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && currentView != AdminViewMode.ADD_USER) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (currentView) {
                        AdminViewMode.USERS -> {
                            UsersListView(
                                users = filteredUsers,
                                allUsers = users,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedSortOption = selectedSortOption,
                                onSortOptionChange = { selectedSortOption = it },
                                onUserEdit = { user -> showEditUserDialog = user },
                                onUserDelete = { user -> showDeleteUserDialog = user },
                                lang = lang
                            )
                        }
                        AdminViewMode.SUBSCRIPTIONS -> {
                            SubscriptionsListView(
                                subscriptions = subscriptions,
                                onSubscriptionEdit = { subscription ->
                                    showEditSubscriptionDialog = subscription
                                },
                                onSubscriptionDelete = { subscription ->
                                    showDeleteSubscriptionDialog = subscription
                                },
                                lang = lang
                            )
                        }
                        AdminViewMode.GROUP_WORKOUTS -> {
                            GroupWorkoutsListView(
                                groupWorkouts = groupWorkouts,
                                onWorkoutDelete = { workout ->
                                    showDeleteGroupWorkoutDialog = workout
                                },
                                lang = lang
                            )
                        }
                        AdminViewMode.ADD_USER -> {
                            AddUserFullScreen(
                                selectedRole = addUserSelectedRole,
                                onRoleSelected = { role -> addUserSelectedRole = role },
                                onBack = { 
                                    if (addUserSelectedRole != null) {
                                        addUserSelectedRole = null
                                    } else {
                                        currentView = previousView
                                    }
                                },
                                onUserCreated = { email, password, lastName, firstName, middleName, birthDate, gender, role, trainerData, photoUrl ->
                                    scope.launch {
                                        val result = repository.registerUserByAdmin(
                                            context = context,
                                            email = email,
                                            password = password,
                                            lastName = lastName,
                                            firstName = firstName,
                                            middleName = middleName,
                                            birthDate = birthDate,
                                            role = role
                                        )
                                        result.fold(
                                            onSuccess = { newUserId ->
                                                // Если это тренер, сохраняем дополнительные данные в таблицу trainers
                                                if (role == UserRole.TRAINER && trainerData != null) {
                                                    val trainer = Trainer(
                                                        userId = newUserId,
                                                        lastName = lastName,
                                                        firstName = firstName,
                                                        middleName = middleName,
                                                        birthDate = birthDate,
                                                        email = email,
                                                        phone = trainerData.phone,
                                                        experience = trainerData.experience,
                                                        specialization = trainerData.specialization.name,
                                                        pricePerTraining = trainerData.pricePerTraining,
                                                        photoUrl = trainerData.photoUrl
                                                    )
                                                    val trainerResult = repository.addTrainer(trainer)
                                                    if (trainerResult.isFailure) {
                                                        errorMessage = "${Strings.error(lang)}: trainer not saved"
                                                    }
                                                    
                                                    repository.updateUserData(
                                                        userId = newUserId,
                                                        email = email,
                                                        phone = trainerData.phone,
                                                        lastName = lastName,
                                                        firstName = firstName,
                                                        middleName = middleName,
                                                        birthDate = birthDate
                                                    )
                                                }
                                                
                                                repository.updateUserBasicData(
                                                    userId = newUserId,
                                                    lastName = lastName,
                                                    firstName = firstName,
                                                    middleName = middleName,
                                                    birthDate = birthDate,
                                                    gender = gender
                                                )
                                                
                                                successMessage = Strings.userCreated(lang)
                                                addUserSelectedRole = null
                                                currentView = AdminViewMode.USERS
                                                users = repository.getAllUsers()
                                            },
                                            onFailure = { e ->
                                                errorMessage = "${Strings.error(lang)}: ${e.message}"
                                            }
                                        )
                                    }
                                },
                                lang = lang
                            )
                        }
                    }
                }
                
                // Сообщения
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = successMessage ?: "",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог добавления абонемента
    if (showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddSubscriptionDialog = false },
            onAdd = { subscription ->
                scope.launch {
                    val result = repository.addSubscription(subscription)
                    result.fold(
                        onSuccess = {
                            successMessage = Strings.subscriptionCreated(lang)
                            showAddSubscriptionDialog = false
                            subscriptions = repository.getAllSubscriptions()
                        },
                        onFailure = { e ->
                            errorMessage = "${Strings.error(lang)}: ${e.message}"
                        }
                    )
                }
            },
            lang = lang
        )
    }
    
    // Диалог добавления групповой тренировки
    if (showAddGroupWorkoutDialog) {
        AddGroupWorkoutDialog(
            trainers = trainers,
            onDismiss = { showAddGroupWorkoutDialog = false },
            onAdd = { workout ->
                scope.launch {
                    val result = repository.addGroupWorkout(workout)
                    result.fold(
                        onSuccess = {
                            successMessage = Strings.workoutCreated(lang)
                            showAddGroupWorkoutDialog = false
                            groupWorkouts = repository.getAllGroupWorkouts()
                        },
                        onFailure = { e ->
                            errorMessage = "${Strings.error(lang)}: ${e.message}"
                        }
                    )
                }
            },
            lang = lang
        )
    }
    
    // Диалог редактирования пользователя
    if (showEditUserDialog != null) {
        EditUserDialog(
            user = showEditUserDialog!!,
            onDismiss = { showEditUserDialog = null },
            onSave = { lastName, firstName, middleName, birthDate, gender ->
                scope.launch {
                    val result = repository.updateUserBasicData(
                        userId = showEditUserDialog!!.id,
                        lastName = lastName,
                        firstName = firstName,
                        middleName = middleName,
                        birthDate = birthDate,
                        gender = gender
                    )
                    result.fold(
                        onSuccess = {
                            successMessage = Strings.userUpdated(lang)
                            showEditUserDialog = null
                            users = repository.getAllUsers()
                        },
                        onFailure = { e ->
                            errorMessage = "${Strings.error(lang)}: ${e.message}"
                        }
                    )
                }
            },
            lang = lang
        )
    }
    
    // Диалог редактирования абонемента
    if (showEditSubscriptionDialog != null) {
        EditSubscriptionDialog(
            subscription = showEditSubscriptionDialog!!,
            onDismiss = { showEditSubscriptionDialog = null },
            onSave = { subscription ->
                scope.launch {
                    val result = repository.updateSubscription(showEditSubscriptionDialog!!.id, subscription)
                    result.fold(
                        onSuccess = {
                            successMessage = Strings.subscriptionUpdated(lang)
                            showEditSubscriptionDialog = null
                            subscriptions = repository.getAllSubscriptions()
                        },
                        onFailure = { e ->
                            errorMessage = "${Strings.error(lang)}: ${e.message}"
                        }
                    )
                }
            },
            lang = lang
        )
    }
    
    // Диалог подтверждения удаления абонемента
    if (showDeleteSubscriptionDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSubscriptionDialog = null },
            title = {
                Text(
                    text = Strings.confirmDelete(lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("${Strings.deleteSubscriptionConfirm(lang)} \"${showDeleteSubscriptionDialog!!.name}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val subscription = showDeleteSubscriptionDialog!!
                            val result = repository.deleteSubscription(subscription.id)
                            result.fold(
                                onSuccess = {
                                    successMessage = Strings.subscriptionDeleted(lang)
                                    showDeleteSubscriptionDialog = null
                                    subscriptions = repository.getAllSubscriptions()
                                },
                                onFailure = { e ->
                                    errorMessage = "${Strings.error(lang)}: ${e.message}"
                                    showDeleteSubscriptionDialog = null
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.delete(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSubscriptionDialog = null }) {
                    Text(Strings.cancel(lang))
                }
            }
        )
    }
    
    // Диалог подтверждения удаления пользователя
    if (showDeleteUserDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteUserDialog = null },
            title = {
                Text(
                    text = Strings.confirmDelete(lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(Strings.deleteUserConfirm(lang))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = showDeleteUserDialog!!.fullName,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = showDeleteUserDialog!!.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.actionCannotBeUndone(lang),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val user = showDeleteUserDialog!!
                            val result = repository.deleteUser(user.id)
                            result.fold(
                                onSuccess = {
                                    successMessage = Strings.userDeleted(lang)
                                    showDeleteUserDialog = null
                                    users = repository.getAllUsers()
                                },
                                onFailure = { e ->
                                    errorMessage = "${Strings.error(lang)}: ${e.message}"
                                    showDeleteUserDialog = null
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.delete(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteUserDialog = null }) {
                    Text(Strings.cancel(lang))
                }
            }
        )
    }
    
    // Диалог подтверждения удаления групповой тренировки
    if (showDeleteGroupWorkoutDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupWorkoutDialog = null },
            title = {
                Text(
                    text = Strings.confirmDelete(lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(Strings.deleteWorkoutConfirm(lang))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = showDeleteGroupWorkoutDialog!!.name,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = showDeleteGroupWorkoutDialog!!.formattedDateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val workout = showDeleteGroupWorkoutDialog!!
                            try {
                                FirebaseRepo.instance.deleteGroupWorkout(workout.id)
                                successMessage = Strings.workoutDeleted(lang)
                                showDeleteGroupWorkoutDialog = null
                                groupWorkouts = repository.getAllGroupWorkouts()
                            } catch (e: Exception) {
                                errorMessage = "${Strings.error(lang)}: ${e.message}"
                                showDeleteGroupWorkoutDialog = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.delete(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupWorkoutDialog = null }) {
                    Text(Strings.cancel(lang))
                }
            }
        )
    }
    
}

@Composable
private fun AdminMenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersListView(
    users: List<User>,
    allUsers: List<User>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    onUserEdit: (User) -> Unit,
    onUserDelete: (User) -> Unit,
    lang: AppLanguage
) {
    var showSortMenu by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Статистика
        item {
            AdminStatsCard(users = allUsers, lang = lang)
        }
        
        // Поиск
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(Strings.searchPlaceholder(lang)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = Strings.search(lang))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        // Заголовок с сортировкой
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Strings.allUsers(lang)} (${users.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showSortMenu = true },
                        label = { Text(selectedSortOption.getDisplayName(lang)) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.getDisplayName(lang)) },
                                onClick = {
                                    onSortOptionChange(option)
                                    showSortMenu = false
                                },
                                leadingIcon = if (selectedSortOption == option) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
        
        if (users.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.nothingFound(lang),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = Strings.tryDifferentSearch(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        items(users, key = { it.id }) { user ->
            UserCard(
                user = user,
                onEdit = { onUserEdit(user) },
                onDelete = { onUserDelete(user) },
                lang = lang
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SubscriptionsListView(
    subscriptions: List<Subscription>,
    onSubscriptionEdit: (Subscription) -> Unit,
    onSubscriptionDelete: (Subscription) -> Unit,
    lang: AppLanguage
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${Strings.allSubscriptions(lang)} (${subscriptions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(subscriptions, key = { it.id }) { subscription ->
            SubscriptionCard(
                subscription = subscription,
                onEdit = { onSubscriptionEdit(subscription) },
                onDelete = { onSubscriptionDelete(subscription) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// Список групповых тренировок
@Composable
private fun GroupWorkoutsListView(
    groupWorkouts: List<GroupWorkout>,
    onWorkoutDelete: (GroupWorkout) -> Unit,
    lang: AppLanguage
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${Strings.allGroupWorkouts(lang)} (${groupWorkouts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        if (groupWorkouts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏃", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.noGroupWorkouts(lang),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = Strings.followSchedule(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        items(groupWorkouts, key = { it.id }) { workout ->
            GroupWorkoutCard(
                workout = workout,
                onDelete = { onWorkoutDelete(workout) },
                lang = lang
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// Карточка групповой тренировки
@Composable
private fun GroupWorkoutCard(
    workout: GroupWorkout,
    onDelete: () -> Unit,
    lang: AppLanguage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏃", fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Информация
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = workout.trainerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "📅 ${workout.formattedDateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "⏱ ${workout.durationMinutes} ${Strings.minutes(lang)} | 👥 ${workout.currentParticipants}/${workout.maxParticipants}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Кнопка удаления
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = Strings.delete(lang),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Text(
                text = subscription.iconEmoji,
                fontSize = 40.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            // Информация
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subscription.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${subscription.price} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${subscription.durationDays} дней",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (subscription.features.isNotEmpty()) {
                    Text(
                        text = subscription.features.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Кнопки действий
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatsCard(users: List<User>, lang: AppLanguage) {
    val clients = users.count { it.userRole == UserRole.CLIENT }
    val trainersCount = users.count { it.userRole == UserRole.TRAINER }
    val adminsCount = users.count { it.userRole == UserRole.ADMIN }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = Strings.statistics(lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(emoji = "👤", count = clients, label = Strings.clients(lang))
                StatItem(emoji = "🏃", count = trainersCount, label = Strings.trainersCount(lang))
                StatItem(emoji = "👑", count = adminsCount, label = Strings.admins(lang))
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    lang: AppLanguage
) {
    val roleColor = when (user.userRole) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.TRAINER -> MaterialTheme.colorScheme.tertiary
        UserRole.CLIENT -> MaterialTheme.colorScheme.secondary
    }
    
    val roleEmoji = when (user.userRole) {
        UserRole.ADMIN -> "👑"
        UserRole.TRAINER -> "🏃"
        UserRole.CLIENT -> "👤"
    }
    
    val roleDisplayName = when (user.userRole) {
        UserRole.ADMIN -> Strings.admin(lang)
        UserRole.TRAINER -> Strings.trainer(lang)
        UserRole.CLIENT -> Strings.client(lang)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = roleEmoji, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Информация
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (user.email.isNotBlank()) {
                    Text(
                        text = "📧 ${user.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (user.phone.isNotBlank()) {
                    Text(
                        text = "📱 ${user.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (user.birthDate.isNotBlank()) {
                    Text(
                        text = "🎂 ${user.birthDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = roleDisplayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = roleColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Кнопки действий
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = Strings.edit(lang),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = Strings.delete(lang),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Функция форматирования даты с сохранением позиции курсора
private fun formatDateInput(input: TextFieldValue): TextFieldValue {
    val digits = input.text.filter { it.isDigit() }
    val formatted = when {
        digits.length <= 2 -> digits
        digits.length <= 4 -> "${digits.take(2)}.${digits.drop(2)}"
        else -> "${digits.take(2)}.${digits.substring(2, 4)}.${digits.drop(4).take(4)}"
    }
    
    // Вычисляем новую позицию курсора
    val newCursorPos = formatted.length
    return TextFieldValue(
        text = formatted,
        selection = TextRange(newCursorPos)
    )
}

// Полноэкранная форма добавления пользователя
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserFullScreen(
    selectedRole: UserRole?,
    onRoleSelected: (UserRole) -> Unit,
    onBack: () -> Unit,
    onUserCreated: (
        email: String, 
        password: String, 
        lastName: String, 
        firstName: String, 
        middleName: String, 
        birthDate: String,
        gender: String,
        role: UserRole, 
        trainerData: TrainerFormData?,
        photoUrl: String
    ) -> Unit,
    lang: AppLanguage
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var birthDateValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var photoUrl by remember { mutableStateOf("") }
    
    // Дополнительные поля для тренера
    var experience by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf(TrainerSpecialization.FITNESS) }
    var pricePerTraining by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    // Сбрасываем форму при смене роли
    LaunchedEffect(selectedRole) {
        email = ""
        password = ""
        lastName = ""
        firstName = ""
        middleName = ""
        birthDateValue = TextFieldValue("")
        selectedGender = Gender.MALE
        photoUrl = ""
        experience = ""
        specialization = TrainerSpecialization.FITNESS
        pricePerTraining = ""
        phone = ""
    }
    
    if (selectedRole == null) {
        // Шаг 1: Выбор роли
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Объяснительный текст
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "ℹ️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Strings.createNewUser(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Strings.selectRoleDescription(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = Strings.selectRole(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Карточки ролей
            items(UserRole.entries.toList()) { role ->
                val roleEmoji = when (role) {
                    UserRole.CLIENT -> "👤"
                    UserRole.TRAINER -> "🏃"
                    UserRole.ADMIN -> "👑"
                }
                val roleDescription = when (role) {
                    UserRole.CLIENT -> Strings.clientDescription(lang)
                    UserRole.TRAINER -> Strings.trainerDescription(lang)
                    UserRole.ADMIN -> Strings.adminDescription(lang)
                }
                val roleName = when (role) {
                    UserRole.CLIENT -> Strings.client(lang)
                    UserRole.TRAINER -> Strings.trainer(lang)
                    UserRole.ADMIN -> Strings.admin(lang)
                }
                val roleColor = when (role) {
                    UserRole.CLIENT -> MaterialTheme.colorScheme.secondary
                    UserRole.TRAINER -> MaterialTheme.colorScheme.tertiary
                    UserRole.ADMIN -> MaterialTheme.colorScheme.error
                }
                
                Card(
                    onClick = { onRoleSelected(role) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = roleColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(roleColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = roleEmoji, fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = roleName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = roleDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Шаг 2: Заполнение формы
        val isFormValid = email.isNotBlank() && 
                         password.length >= 6 && 
                         lastName.isNotBlank() && 
                         firstName.isNotBlank() && 
                         birthDateValue.text.length == 10
        
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = Strings.basicData(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("${Strings.email(lang)} *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("${Strings.password(lang)} * (${Strings.minSixChars(lang)})") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = if (password.isNotBlank() && password.length < 6) {
                            { Text(Strings.minSixChars(lang), color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("${Strings.lastName(lang)} *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("${Strings.firstName(lang)} *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        label = { Text(Strings.middleName(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = birthDateValue,
                        onValueChange = { newValue ->
                            birthDateValue = formatDateInput(newValue)
                        },
                        label = { Text("${Strings.birthDate(lang)} *") },
                        placeholder = { Text("ДД.ММ.ГГГГ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    Text(
                        text = "${Strings.gender(lang)}:",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.entries.forEach { gender ->
                            val genderName = when (gender) {
                                Gender.MALE -> Strings.male(lang)
                                Gender.FEMALE -> Strings.female(lang)
                            }
                            FilterChip(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender },
                                label = { 
                                    Text(
                                        text = genderName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Поле для фото URL
                item {
                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it },
                        label = { Text(Strings.photoUrl(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                // Дополнительные поля для тренера
                if (selectedRole == UserRole.TRAINER) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = Strings.trainerData(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(Strings.phone(lang)) },
                            placeholder = { Text("+7XXXXXXXXXX") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = experience,
                                onValueChange = { if (it.all { c -> c.isDigit() }) experience = it },
                                label = { Text(Strings.experience(lang)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            OutlinedTextField(
                                value = pricePerTraining,
                                onValueChange = { if (it.all { c -> c.isDigit() }) pricePerTraining = it },
                                label = { Text(Strings.pricePerTraining(lang)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    
                    item {
                        Text(
                            text = "${Strings.specialization(lang)}:",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    item {
                        // Специализация в виде сетки
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrainerSpecialization.entries.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { spec ->
                                        val specName = getSpecializationName(spec, lang)
                                        FilterChip(
                                            selected = specialization == spec,
                                            onClick = { specialization = spec },
                                            label = { 
                                                Text(
                                                    text = specName,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            
            // Кнопки внизу
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Strings.back(lang))
                    }
                    
                    Button(
                        onClick = { 
                            val trainerData = if (selectedRole == UserRole.TRAINER) {
                                TrainerFormData(
                                    phone = phone,
                                    experience = experience.toIntOrNull() ?: 0,
                                    specialization = specialization,
                                    pricePerTraining = pricePerTraining.toIntOrNull() ?: 0,
                                    photoUrl = photoUrl
                                )
                            } else null
                            
                            onUserCreated(
                                email, 
                                password, 
                                lastName, 
                                firstName, 
                                middleName, 
                                birthDateValue.text,
                                selectedGender.name,
                                selectedRole, 
                                trainerData,
                                photoUrl
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Strings.create(lang))
                    }
                }
            }
        }
    }
}

// Вспомогательная функция для получения локализованного названия специализации
private fun getSpecializationName(spec: TrainerSpecialization, lang: AppLanguage): String {
    return when (spec) {
        TrainerSpecialization.FITNESS -> Strings.fitness(lang)
        TrainerSpecialization.BODYBUILDING -> Strings.bodybuilding(lang)
        TrainerSpecialization.CROSSFIT -> Strings.crossfit(lang)
        TrainerSpecialization.YOGA -> Strings.yoga(lang)
        TrainerSpecialization.PILATES -> Strings.pilates(lang)
        TrainerSpecialization.BOXING -> Strings.boxing(lang)
        TrainerSpecialization.SWIMMING -> Strings.swimming(lang)
        TrainerSpecialization.CARDIO -> Strings.cardio(lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onAdd: (Subscription) -> Unit,
    lang: AppLanguage
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("🏋️") }
    var features by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.newSubscription(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.name(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.description(lang)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                        label = { Text(Strings.price(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationDays = it },
                        label = { Text(Strings.durationDays(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                OutlinedTextField(
                    value = iconEmoji,
                    onValueChange = { if (it.length <= 2) iconEmoji = it },
                    label = { Text(Strings.iconEmoji(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = features,
                    onValueChange = { features = it },
                    label = { Text(Strings.features(lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val featuresList = features.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val subscription = Subscription(
                        name = name,
                        description = description,
                        price = price.toIntOrNull() ?: 0,
                        durationDays = durationDays.toIntOrNull() ?: 0,
                        features = featuresList,
                        iconEmoji = iconEmoji.ifBlank { "🏋️" },
                        active = true
                    )
                    onAdd(subscription)
                },
                enabled = name.isNotBlank() && description.isNotBlank() && 
                         price.toIntOrNull() != null && price.toIntOrNull()!! > 0 &&
                         durationDays.toIntOrNull() != null && durationDays.toIntOrNull()!! > 0
            ) {
                Text(Strings.create(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel(lang))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Диалог добавления групповой тренировки
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupWorkoutDialog(
    trainers: List<Trainer>,
    onDismiss: () -> Unit,
    onAdd: (GroupWorkout) -> Unit,
    lang: AppLanguage
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTrainer by remember { mutableStateOf<Trainer?>(null) }
    var dateTimeStr by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("60") }
    var maxParticipants by remember { mutableStateOf("20") }
    var showTrainerDropdown by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.newGroupWorkout(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.workoutName(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.description(lang)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Выбор тренера
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedTrainer?.fullName ?: "",
                        onValueChange = {},
                        label = { Text(Strings.selectTrainer(lang)) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showTrainerDropdown = !showTrainerDropdown }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    DropdownMenu(
                        expanded = showTrainerDropdown,
                        onDismissRequest = { showTrainerDropdown = false }
                    ) {
                        trainers.forEach { trainer ->
                            DropdownMenuItem(
                                text = { Text(trainer.fullName) },
                                onClick = {
                                    selectedTrainer = trainer
                                    showTrainerDropdown = false
                                }
                            )
                        }
                        if (trainers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(Strings.noTrainers(lang)) },
                                onClick = { showTrainerDropdown = false }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = dateTimeStr,
                    onValueChange = { dateTimeStr = it },
                    label = { Text(Strings.dateAndTime(lang)) },
                    placeholder = { Text("дд.мм.гггг чч:мм") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { if (it.all { c -> c.isDigit() }) durationMinutes = it },
                        label = { Text(Strings.duration(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = maxParticipants,
                        onValueChange = { if (it.all { c -> c.isDigit() }) maxParticipants = it },
                        label = { Text(Strings.maxParticipants(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    // Парсим дату и время
                    val dateTime = try {
                        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        val date = format.parse(dateTimeStr)
                        if (date != null) Timestamp(date) else Timestamp.now()
                    } catch (e: Exception) {
                        Timestamp.now()
                    }
                    
                    val workout = GroupWorkout(
                        name = name,
                        description = description,
                        trainerId = selectedTrainer?.id ?: "",
                        trainerName = selectedTrainer?.fullName ?: "",
                        dateTime = dateTime,
                        durationMinutes = durationMinutes.toIntOrNull() ?: 60,
                        maxParticipants = maxParticipants.toIntOrNull() ?: 20,
                        currentParticipants = 0,
                        active = true
                    )
                    onAdd(workout)
                },
                enabled = name.isNotBlank() && selectedTrainer != null && dateTimeStr.isNotBlank()
            ) {
                Text(Strings.create(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel(lang))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (lastName: String, firstName: String, middleName: String, birthDate: String, gender: String) -> Unit,
    lang: AppLanguage
) {
    var lastName by remember { mutableStateOf(user.lastName) }
    var firstName by remember { mutableStateOf(user.firstName) }
    var middleName by remember { mutableStateOf(user.middleName) }
    var birthDateValue by remember { mutableStateOf(TextFieldValue(user.birthDate)) }
    var selectedGender by remember { mutableStateOf(user.userGender) }
    
    val roleDisplayName = when (user.userRole) {
        UserRole.ADMIN -> Strings.admin(lang)
        UserRole.TRAINER -> Strings.trainer(lang)
        UserRole.CLIENT -> Strings.client(lang)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.editUser(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Отображение нередактируемых данных
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${Strings.email(lang)}: ${user.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (user.phone.isNotBlank()) {
                            Text(
                                text = "${Strings.phone(lang)}: ${user.phone}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${Strings.role(lang)}: $roleDisplayName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("${Strings.lastName(lang)} *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("${Strings.firstName(lang)} *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = middleName,
                    onValueChange = { middleName = it },
                    label = { Text(Strings.middleName(lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = birthDateValue,
                    onValueChange = { newValue ->
                        birthDateValue = formatDateInput(newValue)
                    },
                    label = { Text("${Strings.birthDate(lang)} *") },
                    placeholder = { Text("ДД.ММ.ГГГГ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text(
                    text = "${Strings.gender(lang)}:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Gender.entries.forEach { gender ->
                        val genderName = when (gender) {
                            Gender.MALE -> Strings.male(lang)
                            Gender.FEMALE -> Strings.female(lang)
                        }
                        FilterChip(
                            selected = selectedGender == gender,
                            onClick = { selectedGender = gender },
                            label = { 
                                Text(
                                    text = genderName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(lastName, firstName, middleName, birthDateValue.text, selectedGender.name)
                },
                enabled = lastName.isNotBlank() && firstName.isNotBlank() && birthDateValue.text.length == 10
            ) {
                Text(Strings.save(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel(lang))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSubscriptionDialog(
    subscription: Subscription,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit,
    lang: AppLanguage
) {
    var name by remember { mutableStateOf(subscription.name) }
    var description by remember { mutableStateOf(subscription.description) }
    var price by remember { mutableStateOf(subscription.price.toString()) }
    var durationDays by remember { mutableStateOf(subscription.durationDays.toString()) }
    var iconEmoji by remember { mutableStateOf(subscription.iconEmoji) }
    var features by remember { mutableStateOf(subscription.features.joinToString(", ")) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.editSubscription(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.name(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.description(lang)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                        label = { Text(Strings.price(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationDays = it },
                        label = { Text(Strings.durationDays(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                OutlinedTextField(
                    value = iconEmoji,
                    onValueChange = { if (it.length <= 2) iconEmoji = it },
                    label = { Text(Strings.iconEmoji(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = features,
                    onValueChange = { features = it },
                    label = { Text(Strings.features(lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val featuresList = features.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val updatedSubscription = subscription.copy(
                        name = name,
                        description = description,
                        price = price.toIntOrNull() ?: 0,
                        durationDays = durationDays.toIntOrNull() ?: 0,
                        features = featuresList,
                        iconEmoji = iconEmoji.ifBlank { "🏋️" }
                    )
                    onSave(updatedSubscription)
                },
                enabled = name.isNotBlank() && description.isNotBlank() && 
                         price.toIntOrNull() != null && price.toIntOrNull()!! > 0 &&
                         durationDays.toIntOrNull() != null && durationDays.toIntOrNull()!! > 0
            ) {
                Text(Strings.save(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel(lang))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

