package com.example.mobilka.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka.data.AppLanguage
import com.example.mobilka.data.AppTheme
import com.example.mobilka.data.ChatMessage
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.FitnessGoal
import com.example.mobilka.data.Gender
import com.example.mobilka.data.GroupWorkout
import com.example.mobilka.data.SettingsManager
import com.example.mobilka.data.Strings
import com.example.mobilka.data.Subscription
import com.example.mobilka.data.Trainer
import com.example.mobilka.data.TrainerAvailability
import com.example.mobilka.data.User
import com.example.mobilka.data.UserRole
import com.example.mobilka.data.UserSubscription
import com.example.mobilka.data.FoodEntry
import com.example.mobilka.data.FoodProduct
import com.example.mobilka.data.DailyNorm
import com.example.mobilka.data.NutritionCalculator
import com.example.mobilka.ui.theme.EnergyGreen
import com.example.mobilka.ui.theme.SportOrange
import com.example.mobilka.ui.theme.SportOrangeDark
import com.example.mobilka.ui.theme.SportOrangeLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

// Элементы навигации для клиента
enum class ClientNavItem(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    CALCULATOR(Icons.Default.List),
    CHATS(Icons.Default.Email),
    WORKOUTS(Icons.Default.Star),
    PROFILE(Icons.Default.Person);
    
    fun getLabel(lang: AppLanguage): String = when (this) {
        HOME -> Strings.home(lang)
        CALCULATOR -> Strings.calculator(lang)
        CHATS -> Strings.chats(lang)
        WORKOUTS -> Strings.workouts(lang)
        PROFILE -> Strings.profile(lang)
    }
}

// Разделы профиля
enum class ProfileSection {
    PERSONAL_INFO,
    HEALTH_DATA,
    SUBSCRIPTIONS,
    TRAINERS,
    SETTINGS;
    
    fun getTitle(lang: AppLanguage): String = when (this) {
        PERSONAL_INFO -> Strings.personalData(lang)
        HEALTH_DATA -> Strings.healthData(lang)
        SUBSCRIPTIONS -> Strings.subscriptions(lang)
        TRAINERS -> Strings.trainers(lang)
        SETTINGS -> Strings.settings(lang)
    }
}

// Типы тренировок (для клиента)
enum class WorkoutType {
    MY_WORKOUTS,
    INDIVIDUAL,
    GROUP;
    
    fun getTitle(lang: AppLanguage): String = when (this) {
        MY_WORKOUTS -> if (lang == AppLanguage.RUSSIAN) "Мои тренировки" else "My Workouts"
        INDIVIDUAL -> if (lang == AppLanguage.RUSSIAN) "Индивидуальные" else "Individual"
        GROUP -> if (lang == AppLanguage.RUSSIAN) "Групповые тренировки" else "Group Workouts"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToTrainer: () -> Unit = {}
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Настройки приложения
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentLanguage by settingsManager.language.collectAsState()
    val lang = currentLanguage
    
    var currentUser by remember { mutableStateOf<User?>(null) }
    var userSubscriptions by remember { mutableStateOf<List<UserSubscription>>(emptyList()) }
    var availableSubscriptions by remember { mutableStateOf<List<Subscription>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPurchaseDialog by remember { mutableStateOf<Subscription?>(null) }
    var isPurchasing by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNavItem by remember { mutableStateOf(ClientNavItem.HOME) }
    var chatResetSignal by remember { mutableIntStateOf(0) }
    var trainersList by remember { mutableStateOf<List<Trainer>>(emptyList()) }
    var groupWorkouts by remember { mutableStateOf<List<GroupWorkout>>(emptyList()) }
    
    // Текущий раздел профиля (null = главное меню профиля)
    var currentProfileSection by remember { mutableStateOf<ProfileSection?>(null) }
    
    // Тип тренировок (для раздела Тренировки) - по умолчанию мои тренировки
    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.MY_WORKOUTS) }
    
    // Загрузка данных
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Загружаем пользователя
            currentUser = repository.getCurrentUser()
            
            // Если пользователь админ - сразу открываем панель администратора
            if (currentUser?.isAdmin == true) {
                onNavigateToAdmin()
                return@LaunchedEffect
            }
            
            // Если пользователь тренер - открываем панель тренера
            if (currentUser?.isTrainer == true) {
                onNavigateToTrainer()
                return@LaunchedEffect
            }
            
            // Загружаем абонементы
            availableSubscriptions = repository.getAvailableSubscriptions()
            userSubscriptions = repository.getUserSubscriptions()
            // Предзагружаем тренеров и тренировки, чтобы разделы Тренировки и Чаты
            // были готовы до перехода в них
            trainersList = repository.getAllTrainers()
            groupWorkouts = repository.getAllGroupWorkouts()
        } catch (e: Exception) {
            errorMessage = "${Strings.loadingError(lang)}: ${e.message}"
        }
        isLoading = false
    }
    
    // Подписка на изменения в реальном времени
    LaunchedEffect(Unit) {
        repository.observeCurrentUser().collect { user ->
            currentUser = user
            // Если пользователь стал админом - открываем панель
            if (user?.isAdmin == true) {
                onNavigateToAdmin()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        repository.observeUserSubscriptions().collect { subs ->
            if (subs.isNotEmpty() || userSubscriptions.isEmpty()) {
                userSubscriptions = subs
            }
        }
    }
    
    LaunchedEffect(Unit) {
        repository.observeAvailableSubscriptions().collect { subs ->
            availableSubscriptions = subs
        }
    }
    
    // Загрузка тренеров и групповых тренировок
    LaunchedEffect(selectedNavItem, currentProfileSection) {
        if (selectedNavItem == ClientNavItem.PROFILE && currentProfileSection == ProfileSection.TRAINERS) {
            // Загружаем тренеров из таблицы trainers для профиля
            trainersList = repository.getAllTrainers()
        }
        if (selectedNavItem == ClientNavItem.WORKOUTS) {
            // Загружаем тренеров из таблицы trainers для тренировок
            trainersList = repository.getAllTrainers()
            groupWorkouts = repository.getAllGroupWorkouts()
        }
    }
    
    val activeSubscriptions = userSubscriptions.filter { !it.isExpired && it.active }
    val hasActiveSubscriptions = activeSubscriptions.isNotEmpty()
    
    // Имя пользователя для отображения
    val displayName = currentUser?.let { user ->
        val name = "${user.firstName} ${user.lastName}".trim()
        name.ifBlank { Strings.noName(lang) }
    } ?: Strings.loading(lang)

    // Определяем заголовок верхней панели
    val topBarTitle = when {
        currentProfileSection != null -> currentProfileSection!!.getTitle(lang)
        selectedNavItem == ClientNavItem.PROFILE -> Strings.profile(lang)
        selectedNavItem == ClientNavItem.CALCULATOR -> Strings.calculator(lang)
        selectedNavItem == ClientNavItem.WORKOUTS -> Strings.workouts(lang)
        else -> Strings.appName(lang)
    }
    
    // Показываем ФИО только на главной
    val showSubtitle = selectedNavItem == ClientNavItem.HOME && currentProfileSection == null
    
    // Показываем кнопку "Назад" только в разделах профиля
    val showBackButton = currentProfileSection != null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    // Кнопка "Назад" в разделах профиля
                    if (showBackButton) {
                        IconButton(onClick = { currentProfileSection = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Назад",
                                tint = Color.White,
                                modifier = Modifier.graphicsLayer(rotationZ = 180f)
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                        if (showSubtitle) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SportOrange
                ),
                actions = {
                    if (selectedNavItem == ClientNavItem.PROFILE) {
                        IconButton(
                            onClick = {
                                repository.logout()
                                onLogout()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = Strings.logout(lang),
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SportOrange,
                contentColor = Color.White
            ) {
                ClientNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.getLabel(lang)
                            )
                        },
                        label = { 
                            Text(
                                text = item.getLabel(lang),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedNavItem == item,
                        onClick = {
                            when {
                                item == ClientNavItem.PROFILE && selectedNavItem == ClientNavItem.PROFILE && currentProfileSection != null -> {
                                    currentProfileSection = null
                                }
                                item == ClientNavItem.CHATS && selectedNavItem == ClientNavItem.CHATS -> {
                                    chatResetSignal++
                                }
                                item == ClientNavItem.WORKOUTS && selectedNavItem == ClientNavItem.WORKOUTS && selectedWorkoutType != WorkoutType.MY_WORKOUTS -> {
                                    selectedWorkoutType = WorkoutType.MY_WORKOUTS
                                }
                                else -> {
                                    if (item != ClientNavItem.PROFILE) currentProfileSection = null
                                    selectedNavItem = item
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SportOrange,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f),
                            indicatorColor = Color.White
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Индикатор загрузки
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Загрузка данных...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Контент в зависимости от выбранного пункта навигации
                when (selectedNavItem) {
                    ClientNavItem.HOME -> {
                        HomeWelcomeContent(
                            activeSubscriptions = activeSubscriptions,
                            trainers = trainersList,
                            groupWorkouts = groupWorkouts,
                            currentUser = currentUser,
                            onNavigateToSubscriptions = {
                                currentProfileSection = ProfileSection.SUBSCRIPTIONS
                                selectedNavItem = ClientNavItem.PROFILE
                            },
                            onNavigateToTrainers = {
                                currentProfileSection = ProfileSection.TRAINERS
                                selectedNavItem = ClientNavItem.PROFILE
                            }
                        )
                    }
                    ClientNavItem.CALCULATOR -> {
                        NutritionScreen(
                            currentUser = currentUser,
                            lang = lang
                        )
                    }
                    ClientNavItem.CHATS -> {
                        ClientChatsScreen(
                            currentUser = currentUser,
                            groupWorkouts = groupWorkouts,
                            lang = lang,
                            resetSignal = chatResetSignal
                        )
                    }
                    ClientNavItem.WORKOUTS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Переключатель типа тренировок
                            WorkoutTypeToggle(
                                selectedType = selectedWorkoutType,
                                onTypeSelected = { selectedWorkoutType = it },
                                lang = lang
                            )
                            
                            // Контент в зависимости от выбранного типа
                            when (selectedWorkoutType) {
                                WorkoutType.INDIVIDUAL -> {
                                    IndividualBookingScreen(
                                        currentUser = currentUser,
                                        lang = lang,
                                        onWorkoutBooked = {
                                            scope.launch {
                                                groupWorkouts = repository.getAllGroupWorkouts()
                                                trainersList = repository.getAllTrainers()
                                                successDialogMessage = if (lang == AppLanguage.RUSSIAN)
                                                    "Вы успешно записались на индивидуальную тренировку. Информация о ней будет доступна в разделе «Мои тренировки»."
                                                else
                                                    "You have successfully booked an individual workout. Details will be available in My Workouts."
                                            }
                                        }
                                    )
                                }
                                WorkoutType.MY_WORKOUTS -> {
                                    MyWorkoutsScreen(
                                        currentUserId = currentUser?.id ?: "",
                                        allWorkouts = groupWorkouts,
                                        lang = lang,
                                        onCancelSignUp = { workout ->
                                            scope.launch {
                                                val result = repository.cancelWorkoutSignUp(workout.id, currentUser?.id ?: "")
                                                result.fold(
                                                    onSuccess = {
                                                        groupWorkouts = repository.getAllGroupWorkouts()
                                                        successDialogMessage = if (lang == AppLanguage.RUSSIAN)
                                                            "Запись на тренировку отменена"
                                                        else
                                                            "Workout sign-up canceled"
                                                    },
                                                    onFailure = { e ->
                                                        errorMessage = e.message
                                                    }
                                                )
                                            }
                                        },
                                        onCancelIndividual = { workout ->
                                            scope.launch {
                                                val result = repository.cancelIndividualWorkout(
                                                    workout.id
                                                )
                                                result.fold(
                                                    onSuccess = {
                                                        groupWorkouts = repository.getAllGroupWorkouts()
                                                        successDialogMessage = if (lang == AppLanguage.RUSSIAN)
                                                            "Индивидуальная тренировка отменена"
                                                        else
                                                            "Individual workout canceled"
                                                    },
                                                    onFailure = { e ->
                                                        errorMessage = e.message
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                                WorkoutType.GROUP -> {
                                    // Показываем только групповые тренировки на ближайшие 2 недели
                                    val now = System.currentTimeMillis()
                                    val twoWeeksLater = now + 14 * 24 * 60 * 60 * 1000L
                                    val onlyGroupWorkouts = groupWorkouts.filter { workout ->
                                        !workout.isIndividualWorkout &&
                                        workout.dateTime.toDate().time >= now &&
                                        workout.dateTime.toDate().time <= twoWeeksLater
                                    }.sortedBy { it.dateTime }
                                    GroupWorkoutsScreen(
                                        workouts = onlyGroupWorkouts,
                                        currentUserId = currentUser?.id ?: "",
                                        lang = lang,
                                        onSignUp = { workout ->
                                            scope.launch {
                                                val result = repository.signUpForWorkout(workout.id, currentUser?.id ?: "")
                                                result.fold(
                                                    onSuccess = {
                                                        // Перезагружаем список тренировок
                                                        groupWorkouts = repository.getAllGroupWorkouts()
                                                        successDialogMessage = if (lang == AppLanguage.RUSSIAN)
                                                            "Вы успешно записаны на групповую тренировку"
                                                        else
                                                            "You have successfully signed up for the group workout"
                                                    },
                                                    onFailure = { e ->
                                                        errorMessage = e.message
                                                    }
                                                )
                                            }
                                        },
                                        onCancelSignUp = { workout ->
                                            scope.launch {
                                                val result = repository.cancelWorkoutSignUp(workout.id, currentUser?.id ?: "")
                                                result.fold(
                                                    onSuccess = {
                                                        // Перезагружаем список тренировок
                                                        groupWorkouts = repository.getAllGroupWorkouts()
                                                        successDialogMessage = if (lang == AppLanguage.RUSSIAN)
                                                            "Запись на групповую тренировку отменена"
                                                        else
                                                            "Group workout sign-up canceled"
                                                    },
                                                    onFailure = { e ->
                                                        errorMessage = e.message
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    ClientNavItem.PROFILE -> {
                        // Профиль пользователя
                        when (currentProfileSection) {
                            null -> {
                                // Главное меню профиля
                                ProfileContent(
                                    user = currentUser,
                                    onPersonalInfoClick = { currentProfileSection = ProfileSection.PERSONAL_INFO },
                                    onSubscriptionsClick = { currentProfileSection = ProfileSection.SUBSCRIPTIONS },
                                    onTrainersClick = { currentProfileSection = ProfileSection.TRAINERS },
                                    onSettingsClick = { currentProfileSection = ProfileSection.SETTINGS },
                                    lang = lang
                                )
                            }
                            ProfileSection.PERSONAL_INFO -> {
                                PersonalInfoScreen(
                                    user = currentUser,
                                    onSave = { phone, email, weight, height, goal ->
                                        scope.launch {
                                            repository.updateUserContactInfo(phone, email)
                                            repository.updateUserHealthData(
                                                currentUser?.gender ?: "MALE", weight, height, goal
                                            )
                                        }
                                    }
                                )
                            }
                            ProfileSection.HEALTH_DATA -> {
                                currentProfileSection = ProfileSection.PERSONAL_INFO
                            }
                            ProfileSection.SUBSCRIPTIONS -> {
                                SubscriptionsFullScreen(
                                    userSubscriptions = userSubscriptions,
                                    availableSubscriptions = availableSubscriptions,
                                    onPurchase = { subscription ->
                                        currentProfileSection = null
                                        showPurchaseDialog = subscription
                                    }
                                )
                            }
                            ProfileSection.TRAINERS -> {
                                TrainersScreen(trainers = trainersList)
                            }
                            ProfileSection.SETTINGS -> {
                                SettingsScreen()
                            }
                        }
                    }
                }
            }
            
        }
    }

    successDialogMessage?.let { message ->
        ActionResultDialog(
            message = message,
            lang = lang,
            onDismiss = { successDialogMessage = null }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Действие не выполнено" else "Action failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(if (lang == AppLanguage.RUSSIAN) "ОК" else "OK")
                }
            }
        )
    }

    // Диалог подтверждения покупки
    showPurchaseDialog?.let { subscription ->
        AlertDialog(
            onDismissRequest = { if (!isPurchasing) showPurchaseDialog = null },
            title = {
                Text(
                    text = "Оформить абонемент?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subscription.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Стоимость: ${formatPrice(subscription.price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Срок действия: ${subscription.durationDays} дней",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isPurchasing = true
                            val result = repository.purchaseSubscription(subscription)
                            isPurchasing = false
                            
                            result.fold(
                                onSuccess = {
                                    showPurchaseDialog = null
                                    // Обновляем список абонементов
                                    userSubscriptions = repository.getUserSubscriptions()
                                    availableSubscriptions = repository.getAvailableSubscriptions()
                                    successDialogMessage = if (lang == AppLanguage.RUSSIAN)
                                        "Абонемент успешно оформлен"
                                    else
                                        "Subscription purchased successfully"
                                },
                                onFailure = { e ->
                                    errorMessage = "Ошибка: ${e.message}"
                                    showPurchaseDialog = null
                                }
                            )
                        }
                    },
                    enabled = !isPurchasing
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Оформить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPurchaseDialog = null },
                    enabled = !isPurchasing
                ) {
                    Text("Отмена")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun RoleBadge(role: UserRole) {
    val (backgroundColor, text) = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.error to "Админ"
        UserRole.TRAINER -> MaterialTheme.colorScheme.tertiary to "Тренер"
        UserRole.CLIENT -> MaterialTheme.colorScheme.secondary to "Клиент"
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleInfoCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (user.userRole) {
                UserRole.ADMIN -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                UserRole.TRAINER -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (user.userRole) {
                    UserRole.ADMIN -> "👑"
                    UserRole.TRAINER -> "🏃"
                    else -> "👤"
                },
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.userRole.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (user.userRole) {
                        UserRole.ADMIN -> "Полный доступ к управлению клубом"
                        UserRole.TRAINER -> "Доступ к расписанию и клиентам"
                        else -> "Доступ к абонементам и услугам"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionResultDialog(
    message: String,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Готово" else "Done",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (lang == AppLanguage.RUSSIAN) "ОК" else "OK")
            }
        }
    )
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActiveSubscriptionCard(
    subscription: UserSubscription
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SportOrange.copy(alpha = 0.8f),
                            SportOrangeDark,
                            SportOrangeLight
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subscription.subscriptionIconEmoji,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = subscription.subscriptionName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = subscription.subscriptionDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Счётчик дней
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = subscription.remainingDays.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color.White
                            )
                            Text(
                                text = getDaysText(subscription.remainingDays.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "С ${subscription.startLocalDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "До ${subscription.endLocalDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailableSubscriptionCard(
    subscription: Subscription,
    onPurchase: () -> Unit,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subscription.iconEmoji,
                                fontSize = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = subscription.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${subscription.durationDays} дней",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatPrice(subscription.price),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = subscription.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Преимущества
                subscription.features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = EnergyGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Оформить",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private fun formatPrice(price: Int): String {
    return "${price.toString().reversed().chunked(3).joinToString(" ").reversed()} ₽"
}

private fun getDaysText(days: Int): String {
    val lastDigit = days % 10
    val lastTwoDigits = days % 100
    
    return when {
        lastTwoDigits in 11..14 -> "дней"
        lastDigit == 1 -> "день"
        lastDigit in 2..4 -> "дня"
        else -> "дней"
    }
}

@Composable
private fun HomeContent(
    errorMessage: String?,
    hasActiveSubscriptions: Boolean,
    activeSubscriptions: List<UserSubscription>,
    availableSubscriptions: List<Subscription>,
    onPurchase: (Subscription) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ошибка загрузки
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Заголовок секции
        item {
            SectionHeader(
                title = if (hasActiveSubscriptions) "Мои абонементы" else "Выберите абонемент",
                subtitle = if (hasActiveSubscriptions) 
                    "Ваши активные подписки (${activeSubscriptions.size})" 
                else 
                    "У вас пока нет активных абонементов"
            )
        }

        if (hasActiveSubscriptions) {
            // Активные абонементы
            items(activeSubscriptions, key = { it.id }) { subscription ->
                ActiveSubscriptionCard(subscription = subscription)
            }
            
            // Разделитель
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Доступные абонементы",
                    subtitle = "Продлите или добавьте новые"
                )
            }
        }

        // Все доступные абонементы
        itemsIndexed(availableSubscriptions, key = { _, sub -> sub.id }) { index, subscription ->
            AvailableSubscriptionCard(
                subscription = subscription,
                onPurchase = { onPurchase(subscription) },
                animationDelay = index * 100
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HomeWelcomeContent(
    activeSubscriptions: List<UserSubscription>,
    trainers: List<Trainer>,
    groupWorkouts: List<GroupWorkout>,
    currentUser: User?,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToTrainers: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    val repository = remember { FirebaseRepo.instance }
    val todayStr = remember { java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) }
    var todayEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    LaunchedEffect(todayStr) {
        repository.observeFoodEntries(todayStr).collect { todayEntries = it }
    }

    val totalCalories = todayEntries.sumOf { it.calories.toDouble() }.toFloat()
    val totalProteins = todayEntries.sumOf { it.proteins.toDouble() }.toFloat()
    val totalFats     = todayEntries.sumOf { it.fats.toDouble() }.toFloat()
    val totalCarbs    = todayEntries.sumOf { it.carbs.toDouble() }.toFloat()
    val macroSum      = totalProteins + totalFats + totalCarbs

    val now = remember { java.util.Date() }
    val sevenDaysLater = remember { java.util.Date(now.time + 7L * 24 * 60 * 60 * 1000) }
    val uid = currentUser?.id ?: ""
    val upcomingWorkouts = remember(groupWorkouts, uid) {
        groupWorkouts.filter { w ->
            val wDate = w.dateTime.toDate()
            wDate.after(now) && wDate.before(sevenDaysLater) &&
            ((!w.isIndividual && w.participantIds.contains(uid)) ||
             (w.isIndividual && w.clientId == uid))
        }.sortedBy { it.dateTime.toDate() }.take(3)
    }

    val proteinColor = Color(0xFF4CAF50)
    val fatsColor    = Color(0xFFFFA726)
    val carbsColor   = Color(0xFF42A5F5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Секция «БЖУ сегодня»
        item {
            Text(
                text = "БЖУ сегодня",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val bgColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        Canvas(modifier = Modifier.size(110.dp)) {
                            val strokeWidth = size.minDimension * 0.18f
                            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            val inset = strokeWidth / 2f
                            val arcSize = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset)
                            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                            if (macroSum > 0f) {
                                val pAngle = (totalProteins / macroSum) * 360f
                                val fAngle = (totalFats     / macroSum) * 360f
                                val cAngle = (totalCarbs    / macroSum) * 360f
                                drawArc(color = bgColor, startAngle = -90f, sweepAngle = 360f,
                                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                                var start = -90f
                                drawArc(color = proteinColor, startAngle = start, sweepAngle = pAngle,
                                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                                start += pAngle
                                drawArc(color = fatsColor, startAngle = start, sweepAngle = fAngle,
                                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                                start += fAngle
                                drawArc(color = carbsColor, startAngle = start, sweepAngle = cAngle,
                                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                            } else {
                                drawArc(color = bgColor, startAngle = -90f, sweepAngle = 360f,
                                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.0f".format(totalCalories),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = proteinColor
                            )
                            Text(
                                text = "ккал",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        listOf(
                            Triple("Белки",    "%.0f г".format(totalProteins), proteinColor),
                            Triple("Жиры",     "%.0f г".format(totalFats),    fatsColor),
                            Triple("Углеводы", "%.0f г".format(totalCarbs),   carbsColor)
                        ).forEach { (label, value, color) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
                            }
                        }
                    }
                }
            }
        }

        // Секция «Ближайшие тренировки»
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ближайшие тренировки",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        if (upcomingWorkouts.isNotEmpty()) {
            items(upcomingWorkouts, key = { it.id }) { workout ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (workout.isIndividual) "🧍" else "👥", fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workout.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = workout.formattedDateTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (workout.trainerName.isNotBlank()) {
                                Text(
                                    text = workout.trainerName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${workout.durationMinutes} мин",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📅", fontSize = 28.sp)
                        Text(
                            text = "Нет ближайших тренировок на неделю",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Секция абонементов
        item {
            Text(
                text = if (activeSubscriptions.size > 1) "Мои абонементы" else "Мой абонемент",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (activeSubscriptions.isNotEmpty()) {
            items(activeSubscriptions, key = { it.id }) { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SportOrange.copy(alpha = 0.85f), SportOrangeDark, SportOrangeLight)
                                )
                            )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.subscriptionIconEmoji,
                                        fontSize = 32.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = sub.subscriptionName,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = sub.remainingDays.toString(),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                            color = Color.White
                                        )
                                        Text(
                                            text = getDaysText(sub.remainingDays.toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Куплен: ${sub.startLocalDate.format(dateFormatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "До: ${sub.endLocalDate.format(dateFormatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onNavigateToSubscriptions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SportOrange)
                ) {
                    Text(
                        text = "Просмотреть все абонементы",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏋️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "У вас ещё нет купленных абонементов",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToSubscriptions,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                        ) {
                            Text(
                                text = "Посмотреть абонементы",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Секция «Тренеры клуба»
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Тренеры клуба",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (trainers.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trainers) { trainer ->
                        Card(
                            modifier = Modifier.width(160.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(SportOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = trainer.firstName.firstOrNull()?.uppercase() ?: "?",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = trainer.fullName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                val specText = trainer.specializationsText
                                if (specText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = specText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF4CAF50),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onNavigateToTrainers,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SportOrange)
                ) {
                    Text(
                        text = "Просмотреть всех тренеров",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Тренеры пока не добавлены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==================== БЖУ КАЛЬКУЛЯТОР ====================

private fun loadFoodProducts(context: android.content.Context): List<FoodProduct> {
    return try {
        val json = context.assets.open("food_products.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            FoodProduct(
                name = obj.getString("name"),
                calories = obj.getDouble("calories").toFloat(),
                proteins = obj.getDouble("proteins").toFloat(),
                fats = obj.getDouble("fats").toFloat(),
                carbs = obj.getDouble("carbs").toFloat(),
                category = obj.optString("category", "")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NutritionScreen(
    currentUser: User?,
    lang: AppLanguage
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()

    val allProducts = remember { loadFoodProducts(context) }
    val categories = remember(allProducts) { allProducts.map { it.category }.distinct().sorted() }

    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    val dateStr = remember(selectedDate) { selectedDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) }

    var foodEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<FoodEntry?>(null) }

    val dailyNorm = remember(currentUser) {
        if (currentUser != null && currentUser.weight > 0f && currentUser.height > 0f && currentUser.age > 0) {
            NutritionCalculator.calculateDailyNorm(
                currentUser.userGender,
                currentUser.weight,
                currentUser.height,
                currentUser.age,
                currentUser.userFitnessGoal
            )
        } else {
            DailyNorm(2000f, 150f, 56f, 225f)
        }
    }

    LaunchedEffect(dateStr) {
        isLoading = true
        repository.observeFoodEntries(dateStr).collect { entries ->
            foodEntries = entries
            isLoading = false
        }
    }

    val totalCalories = foodEntries.sumOf { it.calories.toDouble() }.toFloat()
    val totalProteins = foodEntries.sumOf { it.proteins.toDouble() }.toFloat()
    val totalFats = foodEntries.sumOf { it.fats.toDouble() }.toFloat()
    val totalCarbs = foodEntries.sumOf { it.carbs.toDouble() }.toFloat()

    val today = java.time.LocalDate.now()
    val dayLabel = when {
        selectedDate == today -> if (lang == AppLanguage.RUSSIAN) "Сегодня" else "Today"
        selectedDate == today.minusDays(1) -> if (lang == AppLanguage.RUSSIAN) "Вчера" else "Yesterday"
        selectedDate == today.plusDays(1) -> if (lang == AppLanguage.RUSSIAN) "Завтра" else "Tomorrow"
        else -> selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Навигация по дням
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Предыдущий день"
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (selectedDate != today) {
                            TextButton(onClick = { selectedDate = today }) {
                                Text(
                                    text = if (lang == AppLanguage.RUSSIAN) "Сегодня" else "Today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SportOrange
                                )
                            }
                        }
                    }
                    IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Следующий день"
                        )
                    }
                }
            }

            // Сводка по калориям
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Калории" else "Calories",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%.0f".format(totalCalories),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = " / %.0f".format(dailyNorm.calories),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "ккал" else "kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val calProgress = if (dailyNorm.calories > 0) (totalCalories / dailyNorm.calories).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { calProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = SportOrange,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Прогресс-бары БЖУ
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = if (lang == AppLanguage.RUSSIAN) "Белки" else "Protein",
                        current = totalProteins,
                        norm = dailyNorm.proteins,
                        unit = if (lang == AppLanguage.RUSSIAN) "г" else "g",
                        color = Color(0xFF4CAF50)
                    )
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = if (lang == AppLanguage.RUSSIAN) "Жиры" else "Fats",
                        current = totalFats,
                        norm = dailyNorm.fats,
                        unit = if (lang == AppLanguage.RUSSIAN) "г" else "g",
                        color = Color(0xFFFFA726)
                    )
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = if (lang == AppLanguage.RUSSIAN) "Углеводы" else "Carbs",
                        current = totalCarbs,
                        norm = dailyNorm.carbs,
                        unit = if (lang == AppLanguage.RUSSIAN) "г" else "g",
                        color = Color(0xFF42A5F5)
                    )
                }
            }

            // Список продуктов за день
            item {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Приёмы пищи" else "Meals",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SportOrange)
                    }
                }
            } else if (foodEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🍽️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "Нет записей за этот день" else "No entries for this day",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN)
                                    "Нажмите «+» чтобы добавить продукт"
                                else "Tap «+» to add a product",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(foodEntries.size) { index ->
                    val entry = foodEntries[index]
                    FoodEntryCard(
                        entry = entry,
                        lang = lang,
                        onDelete = { entryToDelete = entry }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = SportOrange,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить")
        }
    }

    // Диалог добавления продукта
    if (showAddDialog) {
        AddFoodDialog(
            allProducts = allProducts,
            categories = categories,
            lang = lang,
            onDismiss = { showAddDialog = false },
            onAdd = { product, weightGrams ->
                val factor = weightGrams / 100f
                val entry = FoodEntry(
                    productName = product.name,
                    weightGrams = weightGrams,
                    calories = product.calories * factor,
                    proteins = product.proteins * factor,
                    fats = product.fats * factor,
                    carbs = product.carbs * factor,
                    date = dateStr
                )
                scope.launch {
                    repository.addFoodEntry(entry)
                }
                showAddDialog = false
            }
        )
    }

    // Диалог подтверждения удаления
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = {
                Text(if (lang == AppLanguage.RUSSIAN) "Удалить запись?" else "Delete entry?")
            },
            text = {
                Text(
                    if (lang == AppLanguage.RUSSIAN)
                        "Удалить «${entry.productName}» из дневника?"
                    else
                        "Remove «${entry.productName}» from the diary?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteFoodEntry(entry.id) }
                    entryToDelete = null
                }) {
                    Text(
                        if (lang == AppLanguage.RUSSIAN) "Удалить" else "Delete",
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Отмена" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun NutrientProgressCard(
    modifier: Modifier = Modifier,
    label: String,
    current: Float,
    norm: Float,
    unit: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.0f".format(current),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = "/ %.0f $unit".format(norm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            val progress = if (norm > 0) (current / norm).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun FoodEntryCard(
    entry: FoodEntry,
    lang: AppLanguage,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.productName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "%.0f ${if (lang == AppLanguage.RUSSIAN) "г" else "g"} · %.0f ${if (lang == AppLanguage.RUSSIAN) "ккал" else "kcal"}".format(
                        entry.weightGrams, entry.calories
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Б: %.1f".format(entry.proteins),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Ж: %.1f".format(entry.fats),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFA726)
                    )
                    Text(
                        text = "У: %.1f".format(entry.carbs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF42A5F5)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = if (lang == AppLanguage.RUSSIAN) "Удалить" else "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodDialog(
    allProducts: List<FoodProduct>,
    categories: List<String>,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onAdd: (FoodProduct, Float) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedProduct by remember { mutableStateOf<FoodProduct?>(null) }
    var weightText by remember { mutableStateOf("100") }

    val filteredProducts = remember(searchQuery, selectedCategory, allProducts) {
        allProducts.filter { product ->
            (selectedCategory == null || product.category == selectedCategory) &&
            (searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        title = {
            Text(
                if (lang == AppLanguage.RUSSIAN) "Добавить продукт" else "Add product",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedProduct == null) {
                    // Поиск
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(if (lang == AppLanguage.RUSSIAN) "Поиск продукта..." else "Search product...")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Фильтр по категориям
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text(if (lang == AppLanguage.RUSSIAN) "Все" else "All", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        items(categories.size) { i ->
                            FilterChip(
                                selected = selectedCategory == categories[i],
                                onClick = {
                                    selectedCategory = if (selectedCategory == categories[i]) null else categories[i]
                                },
                                label = { Text(categories[i], style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Список продуктов
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredProducts.size) { idx ->
                            val product = filteredProducts[idx]
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedProduct = product },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "%.0f ${if (lang == AppLanguage.RUSSIAN) "ккал" else "kcal"}".format(product.calories),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SportOrange
                                        )
                                        Text(
                                            text = "Б: %.1f".format(product.proteins),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "Ж: %.1f".format(product.fats),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFA726)
                                        )
                                        Text(
                                            text = "У: %.1f".format(product.carbs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF42A5F5)
                                        )
                                    }
                                    if (product.category.isNotBlank()) {
                                        Text(
                                            text = product.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Выбранный продукт — ввод веса
                    val product = selectedProduct!!
                    val weight = weightText.toFloatOrNull() ?: 0f
                    val factor = weight / 100f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SportOrange.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { selectedProduct = null }) {
                                    Text(
                                        if (lang == AppLanguage.RUSSIAN) "Изменить" else "Change",
                                        color = SportOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "На 100 г:" else "Per 100 g:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("%.0f ккал".format(product.calories), style = MaterialTheme.typography.bodySmall, color = SportOrange)
                                Text("Б: %.1f".format(product.proteins), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                                Text("Ж: %.1f".format(product.fats), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFA726))
                                Text("У: %.1f".format(product.carbs), style = MaterialTheme.typography.bodySmall, color = Color(0xFF42A5F5))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || newVal.all { c -> c.isDigit() || c == '.' }) {
                                weightText = newVal
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (lang == AppLanguage.RUSSIAN) "Вес (г)" else "Weight (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (weight > 0f) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (lang == AppLanguage.RUSSIAN)
                                        "Итого за %.0f г:".format(weight)
                                    else "Total for %.0f g:".format(weight),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("%.0f ккал".format(product.calories * factor), style = MaterialTheme.typography.bodySmall, color = SportOrange)
                                    Text("Б: %.1f г".format(product.proteins * factor), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                                    Text("Ж: %.1f г".format(product.fats * factor), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFA726))
                                    Text("У: %.1f г".format(product.carbs * factor), style = MaterialTheme.typography.bodySmall, color = Color(0xFF42A5F5))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedProduct != null) {
                val weight = weightText.toFloatOrNull() ?: 0f
                TextButton(
                    onClick = {
                        if (weight > 0f) {
                            onAdd(selectedProduct!!, weight)
                        }
                    },
                    enabled = weight > 0f
                ) {
                    Text(
                        if (lang == AppLanguage.RUSSIAN) "Добавить" else "Add",
                        color = if (weight > 0f) SportOrange else Color.Gray
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == AppLanguage.RUSSIAN) "Отмена" else "Cancel")
            }
        }
    )
}

@Composable
private fun ProfileContent(
    user: User?,
    onPersonalInfoClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onTrainersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    lang: AppLanguage
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Аватар и ФИО
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SportOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.firstName?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.fullName ?: Strings.user(lang),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        
        // Личные данные
        item {
            ProfileMenuButton(
                icon = "👤",
                title = Strings.personalData(lang),
                subtitle = Strings.phoneEmailPassword(lang),
                onClick = onPersonalInfoClick
            )
        }
        
        // Абонементы
        item {
            ProfileMenuButton(
                icon = "🎫",
                title = Strings.subscriptions(lang),
                subtitle = Strings.currentAndAvailable(lang),
                onClick = onSubscriptionsClick
            )
        }
        
        // Тренеры
        item {
            ProfileMenuButton(
                icon = "🏃",
                title = Strings.trainers(lang),
                subtitle = Strings.clubTrainersList(lang),
                onClick = onTrainersClick
            )
        }
        
        // Настройки
        item {
            ProfileMenuButton(
                icon = "⚙️",
                title = Strings.settings(lang),
                subtitle = Strings.themeLanguage(lang),
                onClick = onSettingsClick
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileMenuButton(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== ПОЛНОЭКРАННЫЕ РАЗДЕЛЫ ПРОФИЛЯ ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoScreen(
    user: User?,
    onSave: (phone: String, email: String, weight: Float, height: Float, goal: String) -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()

    var phone by remember(user?.phone) { mutableStateOf(user?.phone ?: "") }
    var email by remember(user?.email) { mutableStateOf(user?.email ?: "") }
    var weight by remember(user?.weight) {
        mutableStateOf(if ((user?.weight ?: 0f) > 0f) user!!.weight.toInt().toString() else "")
    }
    var height by remember(user?.height) {
        mutableStateOf(if ((user?.height ?: 0f) > 0f) user!!.height.toInt().toString() else "")
    }
    var selectedGoal by remember(user?.fitnessGoal) { mutableStateOf(user?.userFitnessGoal ?: FitnessGoal.MAINTENANCE) }

    val origPhone = user?.phone ?: ""
    val origEmail = user?.email ?: ""
    val origWeight = if ((user?.weight ?: 0f) > 0f) user!!.weight.toInt().toString() else ""
    val origHeight = if ((user?.height ?: 0f) > 0f) user!!.height.toInt().toString() else ""
    val origGoal = user?.userFitnessGoal ?: FitnessGoal.MAINTENANCE

    val hasChanges = phone != origPhone || email != origEmail ||
        weight != origWeight || height != origHeight || selectedGoal != origGoal

    var showPasswordSection by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProfileInfoCard(label = "Фамилия", value = user?.lastName ?: "—", icon = "👤")
        }
        item {
            ProfileInfoCard(label = "Имя", value = user?.firstName ?: "—", icon = "👤")
        }
        item {
            ProfileInfoCard(label = "Отчество", value = user?.middleName ?: "—", icon = "👤")
        }
        item {
            ProfileInfoCard(label = "Дата рождения", value = user?.birthDate ?: "—", icon = "📅")
        }
        item {
            ProfileInfoCard(label = "Пол", value = user?.userGender?.displayName ?: "Не указан", icon = "⚧")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📱 Телефон", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("+7 (___) ___-__-__") }
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📧 Email", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.trim() },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("example@mail.com") }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📏 Рост (см)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("175") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "⚖️ Вес (кг)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.all { c -> c.isDigit() }) weight = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("70") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎯 Цель занятий", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    FitnessGoal.entries.forEach { goal ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedGoal = goal }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedGoal == goal, onClick = { selectedGoal = goal })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = goal.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        if (hasChanges) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            phone = origPhone; email = origEmail
                            weight = origWeight; height = origHeight; selectedGoal = origGoal
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            errorMessage = null
                            val emailVal = email.trim()
                            if (emailVal.isNotBlank() && !emailVal.contains("@")) {
                                errorMessage = "Введите корректный email-адрес"
                                return@Button
                            }
                            val h = height.toIntOrNull() ?: 0
                            if (height.isNotBlank() && (h < 50 || h > 300)) {
                                errorMessage = "Рост должен быть от 50 до 300 см"
                                return@Button
                            }
                            val w = weight.toIntOrNull() ?: 0
                            if (weight.isNotBlank() && (w < 20 || w > 500)) {
                                errorMessage = "Вес должен быть от 20 до 500 кг"
                                return@Button
                            }
                            onSave(phone, emailVal, weight.toFloatOrNull() ?: 0f, height.toFloatOrNull() ?: 0f, selectedGoal.name)
                            successMessage = "Данные сохранены"
                        },
                        modifier = Modifier.weight(1f), enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                    ) { Text("Сохранить", color = Color.White) }
                }
            }
        }

        // Секция изменения пароля
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showPasswordSection = !showPasswordSection },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔒", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Изменить пароль",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer(rotationZ = if (showPasswordSection) -90f else 90f)
                    )
                }
            }
        }
        if (showPasswordSection) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentPassword, onValueChange = { currentPassword = it },
                            label = { Text("Текущий пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newPassword, onValueChange = { newPassword = it },
                            label = { Text("Новый пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = confirmNewPassword, onValueChange = { confirmNewPassword = it },
                            label = { Text("Подтвердите новый пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    errorMessage = null
                                    if (newPassword.length < 6) {
                                        errorMessage = "Новый пароль должен содержать минимум 6 символов"
                                        return@launch
                                    }
                                    if (newPassword != confirmNewPassword) {
                                        errorMessage = "Пароли не совпадают. Проверьте правильность ввода"
                                        return@launch
                                    }
                                    isSaving = true
                                    val result = repository.updatePassword(currentPassword, newPassword)
                                    isSaving = false
                                    if (result.isSuccess) {
                                        successMessage = "Пароль успешно изменён"
                                        currentPassword = ""; newPassword = ""; confirmNewPassword = ""
                                        showPasswordSection = false
                                    } else {
                                        val msg = result.exceptionOrNull()?.message ?: ""
                                        errorMessage = when {
                                            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                                            msg.contains("password is invalid", ignoreCase = true) ||
                                            msg.contains("wrong-password", ignoreCase = true) ->
                                                "Неверный текущий пароль"
                                            msg.contains("network", ignoreCase = true) ->
                                                "Ошибка сети. Проверьте подключение к интернету"
                                            msg.contains("requires-recent-login", ignoreCase = true) ->
                                                "Для смены пароля необходимо заново войти в аккаунт"
                                            else -> "Не удалось сменить пароль. Попробуйте позже"
                                        }
                                    }
                                }
                            },
                            enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmNewPassword.isNotBlank() && !isSaving,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                        ) { Text("Сохранить пароль", color = Color.White) }
                    }
                }
            }
        }

        if (successMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EnergyGreen.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✓ $successMessage",
                        modifier = Modifier.padding(16.dp),
                        color = EnergyGreen, fontWeight = FontWeight.Bold
                    )
                }
                LaunchedEffect(successMessage) { delay(3000); successMessage = null }
            }
        }
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✗ $errorMessage",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ProfileInfoCard(label: String, value: String, icon: String) {
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
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}


private enum class SubscriptionSort(val label: String) {
    PRICE_ASC("Цена ↑"),
    PRICE_DESC("Цена ↓"),
    DURATION_ASC("Срок ↑"),
    DURATION_DESC("Срок ↓")
}

@Composable
private fun SubscriptionsFullScreen(
    userSubscriptions: List<UserSubscription>,
    availableSubscriptions: List<Subscription>,
    onPurchase: (Subscription) -> Unit
) {
    val activeUserSubscriptions = userSubscriptions.filter { !it.isExpired && it.active }
    val hasActiveSubscriptions = activeUserSubscriptions.isNotEmpty()

    var selectedSort by remember { mutableStateOf(SubscriptionSort.PRICE_ASC) }

    val sortedSubscriptions = remember(availableSubscriptions, selectedSort) {
        when (selectedSort) {
            SubscriptionSort.PRICE_ASC     -> availableSubscriptions.sortedBy { it.price }
            SubscriptionSort.PRICE_DESC    -> availableSubscriptions.sortedByDescending { it.price }
            SubscriptionSort.DURATION_ASC  -> availableSubscriptions.sortedBy { it.durationDays }
            SubscriptionSort.DURATION_DESC -> availableSubscriptions.sortedByDescending { it.durationDays }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок секции
        item {
            SectionHeader(
                title = if (hasActiveSubscriptions) "Мои абонементы" else "Выберите абонемент",
                subtitle = if (hasActiveSubscriptions)
                    "Ваши активные подписки (${activeUserSubscriptions.size})"
                else
                    "У вас пока нет активных абонементов"
            )
        }

        if (hasActiveSubscriptions) {
            items(activeUserSubscriptions, key = { it.id }) { subscription ->
                ActiveSubscriptionCard(subscription = subscription)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Доступные абонементы",
                    subtitle = "Продлите или добавьте новые"
                )
            }
        } else {
            item {
                SectionHeader(
                    title = "Доступные абонементы",
                    subtitle = "Выберите подходящий тариф"
                )
            }
        }

        // Строка сортировки
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(SubscriptionSort.entries) { sort ->
                    val isSelected = sort == selectedSort
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSort = sort },
                        label = { Text(sort.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Отсортированные доступные абонементы
        itemsIndexed(sortedSubscriptions, key = { _, sub -> sub.id }) { index, subscription ->
            AvailableSubscriptionCard(
                subscription = subscription,
                onPurchase = { onPurchase(subscription) },
                animationDelay = index * 100
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TrainersScreen(trainers: List<Trainer>) {
    if (trainers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🏃", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Тренеры пока не добавлены",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trainers) { trainer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = trainer.firstName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trainer.fullName, 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = trainer.trainerSpecialization.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                if (trainer.experience > 0) {
                                    Text(
                                        text = "📅 ${trainer.experienceText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                if (trainer.pricePerTraining > 0) {
                                    Text(
                                        text = "💰 ${trainer.pricePerTraining} ₽",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFFA726)
                                    )
                                }
                            }
                            val specText = trainer.specializationsText
                            if (specText.isNotBlank()) {
                                Text(
                                    text = "🏅 $specText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    
    val currentTheme by settingsManager.theme.collectAsState()
    val currentLanguage by settingsManager.language.collectAsState()
    val lang = currentLanguage
    
    val isDarkTheme = currentTheme == AppTheme.DARK
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = Strings.appTheme(lang), 
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isDarkTheme,
                            onClick = { settingsManager.setTheme(AppTheme.LIGHT) },
                            label = { Text(Strings.lightTheme(lang)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = !isDarkTheme,
                                enabled = true,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        FilterChip(
                            selected = isDarkTheme,
                            onClick = { settingsManager.setTheme(AppTheme.DARK) },
                            label = { Text(Strings.darkTheme(lang)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = isDarkTheme,
                                enabled = true,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌍", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = Strings.language(lang), 
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = lang == AppLanguage.RUSSIAN,
                            onClick = { settingsManager.setLanguage(AppLanguage.RUSSIAN) },
                            label = { Text("🇷🇺 Русский") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = lang == AppLanguage.RUSSIAN,
                                enabled = true,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        FilterChip(
                            selected = lang == AppLanguage.ENGLISH,
                            onClick = { settingsManager.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("🇺🇸 English") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = lang == AppLanguage.ENGLISH,
                                enabled = true,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==================== ЭКРАНЫ ТРЕНИРОВОК ====================

// Переключатель типа тренировок (под верхней панелью)
@Composable
private fun WorkoutTypeToggle(
    selectedType: WorkoutType,
    onTypeSelected: (WorkoutType) -> Unit,
    lang: AppLanguage
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkoutType.entries.forEach { type ->
                val isSelected = selectedType == type
                val backgroundColor = if (isSelected) SportOrange else Color.Transparent
                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = backgroundColor,
                    onClick = { onTypeSelected(type) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (type) {
                                WorkoutType.MY_WORKOUTS -> "📋"
                                WorkoutType.INDIVIDUAL -> "🏋️"
                                WorkoutType.GROUP -> "👥"
                            },
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = type.getTitle(lang),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndividualBookingScreen(
    currentUser: User?,
    lang: AppLanguage,
    onWorkoutBooked: () -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()

    var allTrainers by remember { mutableStateOf<List<Trainer>>(emptyList()) }
    var showTrainerDropdown by remember { mutableStateOf(false) }
    var selectedTrainer by remember { mutableStateOf<Trainer?>(null) }
    var availableSlots by remember { mutableStateOf<List<TrainerAvailability>>(emptyList()) }
    var bookedHours by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingSlots by remember { mutableStateOf(false) }
    var selectedDateKey by remember { mutableStateOf<String?>(null) }
    var selectedHour by remember { mutableIntStateOf(-1) }
    var selectedSlotForHour by remember { mutableStateOf<TrainerAvailability?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var weekOffset by remember { mutableIntStateOf(0) }

    // Словарь названий месяцев (именительный падеж для заголовка)
    val monthNames = remember {
        mapOf(
            0 to "Январь", 1 to "Февраль", 2 to "Март", 3 to "Апрель",
            4 to "Май", 5 to "Июнь", 6 to "Июль", 7 to "Август",
            8 to "Сентябрь", 9 to "Октябрь", 10 to "Ноябрь", 11 to "Декабрь"
        )
    }
    val dayNameFmt = remember { SimpleDateFormat("EEE", Locale("ru")) }
    val dayNumFmt = remember { SimpleDateFormat("d", Locale.getDefault()) }
    fun calDateKey(cal: Calendar): String =
        "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    fun slotDateKey(slot: TrainerAvailability): String {
        val c = Calendar.getInstance().apply { time = slot.date.toDate() }
        return calDateKey(c)
    }

    // Дни отображаемой недели (пн–вс)
    val weekDays = remember(weekOffset) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        (0..6).map { i ->
            val d = cal.clone() as Calendar
            if (i > 0) d.add(Calendar.DAY_OF_MONTH, i)
            d
        }
    }

    // Заголовок недели (с учётом смены месяца/года)
    val weekHeader = remember(weekDays, monthNames) {
        val f = weekDays.first(); val l = weekDays.last()
        val m1 = monthNames[f.get(Calendar.MONTH)] ?: ""; val m2 = monthNames[l.get(Calendar.MONTH)] ?: ""
        val y1 = f.get(Calendar.YEAR); val y2 = l.get(Calendar.YEAR)
        when { y1 != y2 -> "$m1 $y1 — $m2 $y2"; m1 != m2 -> "$m1 — $m2 $y1"; else -> "$m1 $y1" }
    }

    val todayKey = remember { calDateKey(Calendar.getInstance()) }

    // Множество дат, где остались свободные часы (учитывая уже забронированные)
    val availableDateSet = remember(availableSlots, bookedHours) {
        availableSlots.filter { slot ->
            val sh = slot.startTime.split(":").firstOrNull()?.toIntOrNull() ?: return@filter false
            val eh = slot.endTime.split(":").firstOrNull()?.toIntOrNull() ?: return@filter false
            val c = Calendar.getInstance().apply { time = slot.date.toDate() }
            (sh until eh).any { h ->
                val key = "%04d-%02d-%02d-%02d".format(
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH), h
                )
                key !in bookedHours
            }
        }.map { slotDateKey(it) }.toSet()
    }

    // Слоты для выбранной даты
    val slotsForDate = remember(selectedDateKey, availableSlots) {
        val dk = selectedDateKey ?: return@remember emptyList<TrainerAvailability>()
        availableSlots.filter { slotDateKey(it) == dk }
    }

    // Почасовые слоты: Pair(час, TrainerAvailability), исключая уже забронированные
    val hourlySlots: List<Pair<Int, TrainerAvailability>> = remember(slotsForDate, bookedHours) {
        slotsForDate.flatMap { slot ->
            val sh = slot.startTime.split(":").firstOrNull()?.toIntOrNull() ?: return@flatMap emptyList()
            val eh = slot.endTime.split(":").firstOrNull()?.toIntOrNull() ?: return@flatMap emptyList()
            val c = Calendar.getInstance().apply { time = slot.date.toDate() }
            (sh until eh).mapNotNull { h ->
                val key = "%04d-%02d-%02d-%02d".format(
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH), h
                )
                if (key in bookedHours) null else Pair(h, slot)
            }
        }.distinctBy { it.first }.sortedBy { it.first }
    }

    LaunchedEffect(Unit) { allTrainers = repository.getAllTrainers() }

    LaunchedEffect(selectedTrainer) {
        val trainer = selectedTrainer ?: return@LaunchedEffect
        isLoadingSlots = true
        selectedHour = -1
        selectedSlotForHour = null
        selectedDateKey = null
        weekOffset = 0
        val trainerIds = listOf(trainer.userId, trainer.id).filter { it.isNotBlank() }.distinct()
        val now = Calendar.getInstance().time
        availableSlots = repository.getTrainerAvailabilityForTrainer(
            trainerIds = trainerIds,
            trainerName = trainer.fullName
        ).filter { slot ->
            val startHour = slot.startTime.split(":").firstOrNull()?.toIntOrNull()
            val startMinute = slot.startTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
            if (startHour == null || slot.endTime.isBlank()) {
                false
            } else {
                val slotStart = Calendar.getInstance().apply {
                    time = slot.date.toDate()
                    set(Calendar.HOUR_OF_DAY, startHour)
                    set(Calendar.MINUTE, startMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                slotStart.time.after(now)
            }
        }
        bookedHours = trainerIds
            .flatMap { repository.getBookedIndividualHours(it) }
            .toSet()
        isLoadingSlots = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (lang == AppLanguage.RUSSIAN) "Запись к тренеру" else "Book a Trainer",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Выпадающий список тренеров
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = { showTrainerDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "👤", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.RUSSIAN) "Выберите тренера" else "Select Trainer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedTrainer?.fullName
                                        ?: if (lang == AppLanguage.RUSSIAN) "Не выбран" else "Not selected",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer(rotationZ = 90f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showTrainerDropdown,
                        onDismissRequest = { showTrainerDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        allTrainers.forEach { trainer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(trainer.fullName, fontWeight = FontWeight.Medium)
                                        Text(
                                            trainer.specializationsText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SportOrange
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTrainer = trainer
                                    showTrainerDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isLoadingSlots) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = SportOrange) }
            }
        } else if (selectedTrainer != null) {
            if (availableSlots.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📅", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "У тренера нет свободных занятий"
                                else "No available slots",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Навигация по неделям
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            weekOffset--
                            selectedDateKey = null
                            selectedHour = -1
                            selectedSlotForHour = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                        Text(
                            text = weekHeader,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = {
                            weekOffset++
                            selectedDateKey = null
                            selectedHour = -1
                            selectedSlotForHour = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }

                // Ряд дней недели
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekDays.forEach { day ->
                            val dk = calDateKey(day)
                            val isToday = dk == todayKey
                            val hasSlots = dk in availableDateSet
                            val isPast = dk < todayKey
                            val isSelected = dk == selectedDateKey

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> SportOrange
                                            isToday -> SportOrange.copy(alpha = 0.18f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .then(
                                        if (!isPast)
                                            Modifier.clickable {
                                                selectedDateKey = dk
                                                selectedHour = -1
                                                selectedSlotForHour = null
                                            }
                                        else Modifier
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayNameFmt.format(day.time).take(2).uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dayNumFmt.format(day.time),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        isSelected -> Color.White
                                        isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasSlots && !isPast) {
                                                if (isSelected) Color.White else SportOrange
                                            } else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }

                // Почасовые слоты для выбранного дня
                if (selectedDateKey != null) {
                    if (hourlySlots.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "📅", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN)
                                            "У тренера нет свободных занятий в этот день"
                                        else "The trainer has no available sessions on this day",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "Доступное время:" else "Available times:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(hourlySlots) { (hour, _) ->
                                    val isSelected = selectedHour == hour
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) SportOrange
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (!isSelected) BorderStroke(
                                            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ) else null,
                                        onClick = {
                                            selectedHour = hour
                                            selectedSlotForHour = hourlySlots.find { it.first == hour }?.second
                                        }
                                    ) {
                                        Text(
                                            text = "%02d:00".format(hour),
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                            color = if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val slot = selectedSlotForHour ?: return@Button
                                    val user = currentUser ?: return@Button
                                    val hour = selectedHour
                                    scope.launch {
                                        isBooking = true
                                        errorMessage = null
                                        val result = repository.bookIndividualTrainerSlot(slot, user, hour)
                                        isBooking = false
                                        if (result.isSuccess) {
                                            selectedHour = -1
                                            selectedSlotForHour = null
                                            selectedTrainer = null
                                            availableSlots = emptyList()
                                            selectedDateKey = null
                                            weekOffset = 0
                                            onWorkoutBooked()
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "Ошибка записи"
                                        }
                                    }
                                },
                                enabled = selectedHour >= 0 && !isBooking,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                            ) {
                                if (isBooking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN) "Записаться" else "Book",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Действие не выполнено" else "Action failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(if (lang == AppLanguage.RUSSIAN) "ОК" else "OK")
                }
            }
        )
    }
}

@Composable
private fun IndividualWorkoutsScreen(trainers: List<Trainer>) {
    if (trainers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🏋️", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Тренеры пока не добавлены",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trainers) { trainer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Фото/аватар тренера
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SportOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = trainer.firstName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${trainer.lastName} ${trainer.firstName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = trainer.trainerSpecialization.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(
                                    text = "📅 ${trainer.experienceText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "💰 ${trainer.pricePerTraining} ₽",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SportOrange
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// Экран "Мои тренировки" - показывает тренировки пользователя (индивидуальные + записанные групповые)
@Composable
private fun MyWorkoutsScreen(
    currentUserId: String,
    allWorkouts: List<GroupWorkout>,
    lang: AppLanguage,
    onCancelSignUp: (GroupWorkout) -> Unit,
    onCancelIndividual: (GroupWorkout) -> Unit
) {
    var showCancelConfirmDialog by remember { mutableStateOf<GroupWorkout?>(null) }
    var showCancelIndividualDialog by remember { mutableStateOf<GroupWorkout?>(null) }
    
    // Фильтруем тренировки: индивидуальные для этого клиента + групповые, на которые записан
    // Показываем только текущие и на ближайшие 2 недели
    val myWorkouts = remember(allWorkouts, currentUserId) {
        val now = System.currentTimeMillis()
        val twoWeeksLater = now + 14 * 24 * 60 * 60 * 1000L
        allWorkouts.filter { workout ->
            val workoutTime = workout.dateTime.toDate().time
            // Только текущие и будущие тренировки (в пределах 2 недель)
            workoutTime >= now && workoutTime <= twoWeeksLater &&
            (
                // Индивидуальная тренировка для этого клиента
                (workout.isIndividualWorkout && workout.clientId == currentUserId) ||
                // Или групповая тренировка, на которую пользователь записан
                (!workout.isIndividualWorkout && workout.isUserSignedUp(currentUserId))
            )
        }.sortedBy { it.dateTime }
    }
    
    if (myWorkouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(text = "📋", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "У вас пока нет тренировок" else "You don't have workouts yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) 
                        "Запишитесь на групповую тренировку или обратитесь к тренеру для индивидуального занятия" 
                    else 
                        "Sign up for a group workout or contact a trainer for an individual session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(myWorkouts) { workout ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SportOrange.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Иконка типа тренировки
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SportOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (workout.isIndividualWorkout) "🏋️" else "👥",
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                // Заголовок: для индивидуальных "Индивидуальная тренировка", для групповых "Групповая - название"
                                val displayTitle = if (workout.isIndividualWorkout) {
                                    if (lang == AppLanguage.RUSSIAN) "Индивидуальная тренировка" else "Individual workout"
                                } else {
                                    "${if (lang == AppLanguage.RUSSIAN) "Групповая" else "Group"} - ${workout.name}"
                                }
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Text(
                                text = "📅 ${workout.formattedDateTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Text(
                                    text = "👤 ${shortFullName(workout.trainerName)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "⏱ ${workout.durationMinutes} ${if (lang == AppLanguage.RUSSIAN) "мин" else "min"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Кнопка отмены для групповых тренировок
                            if (!workout.isIndividualWorkout && workout.isUserSignedUp(currentUserId)) {
                                OutlinedButton(
                                    onClick = { showCancelConfirmDialog = workout },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN) "Отменить" else "Cancel",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            // Кнопка отмены для индивидуальных тренировок
                            if (workout.isIndividualWorkout && workout.clientId == currentUserId) {
                                OutlinedButton(
                                    onClick = { showCancelIndividualDialog = workout },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN) "Отменить" else "Cancel",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    // Диалог подтверждения отмены групповой тренировки
    showCancelConfirmDialog?.let { workout ->
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = null },
            title = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Отменить запись?" else "Cancel sign up?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN)
                        "Вы уверены, что хотите отменить запись на тренировку «${workout.name}»?"
                    else
                        "Are you sure you want to cancel your sign up for «${workout.name}»?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSignUp(workout)
                        showCancelConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Отменить запись" else "Cancel sign up")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = null }) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Назад" else "Back")
                }
            }
        )
    }

    // Диалог подтверждения отмены индивидуальной тренировки
    showCancelIndividualDialog?.let { workout ->
        AlertDialog(
            onDismissRequest = { showCancelIndividualDialog = null },
            title = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Отменить тренировку?" else "Cancel workout?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN)
                        "Вы уверены, что хотите отменить индивидуальную тренировку ${workout.formattedDateTime}? Это время станет снова доступным у тренера."
                    else
                        "Are you sure you want to cancel the individual workout on ${workout.formattedDateTime}? The trainer's slot will become available again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelIndividual(workout)
                        showCancelIndividualDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Отменить тренировку" else "Cancel workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelIndividualDialog = null }) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Назад" else "Back")
                }
            }
        )
    }
}

@Composable
private fun GroupWorkoutsScreen(
    workouts: List<GroupWorkout>,
    currentUserId: String,
    lang: AppLanguage,
    onSignUp: (GroupWorkout) -> Unit,
    onCancelSignUp: (GroupWorkout) -> Unit
) {
    var selectedWorkout by remember { mutableStateOf<GroupWorkout?>(null) }
    
    if (workouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "👥", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Групповых тренировок пока нет" else "No group workouts yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Следите за расписанием" else "Follow the schedule",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workouts) { workout ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedWorkout = workout },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (workout.isFull) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workout.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (workout.isFull) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN) "Мест нет" else "Full",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📅", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = workout.formattedDateTime,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "👤", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = workout.trainerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (lang == AppLanguage.RUSSIAN) "Участников" else "Participants"}: ${workout.currentParticipants}/${workout.maxParticipants}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            val isUserSignedUp = workout.isUserSignedUp(currentUserId)
                            
                            if (isUserSignedUp) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SportOrange.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN) "Вы записаны" else "Signed up",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SportOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    // Диалог с подробной информацией о тренировке
    selectedWorkout?.let { workout ->
        WorkoutDetailsDialog(
            workout = workout,
            currentUserId = currentUserId,
            lang = lang,
            onDismiss = { selectedWorkout = null },
            onSignUp = {
                onSignUp(workout)
                selectedWorkout = null
            },
            onCancelSignUp = {
                onCancelSignUp(workout)
                selectedWorkout = null
            }
        )
    }
}

@Composable
private fun WorkoutDetailsDialog(
    workout: GroupWorkout,
    currentUserId: String,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSignUp: () -> Unit,
    onCancelSignUp: () -> Unit
) {
    val isUserSignedUp = workout.isUserSignedUp(currentUserId)
    var showCancelConfirmation by remember { mutableStateOf(false) }
    
    // Диалог подтверждения отмены
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) "Отменить запись?" else "Cancel sign up?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (lang == AppLanguage.RUSSIAN) 
                        "Вы уверены, что хотите отменить запись на тренировку «${workout.name}»?" 
                    else 
                        "Are you sure you want to cancel your sign up for «${workout.name}»?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSignUp()
                        showCancelConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Отменить запись" else "Cancel sign up")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Назад" else "Back")
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "👥", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Описание
                if (workout.description.isNotBlank()) {
                    Column {
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Описание" else "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = workout.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                // Тренер
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SportOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👤", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Тренер" else "Trainer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = workout.trainerName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
                
                // Дата и время
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Дата и время" else "Date & Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📅", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = workout.formattedDateTime,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Длительность" else "Duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⏱️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${workout.durationMinutes} ${if (lang == AppLanguage.RUSSIAN) "мин" else "min"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
                
                // Участники
                Column {
                    Text(
                        text = if (lang == AppLanguage.RUSSIAN) "Участники" else "Participants",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { workout.currentParticipants.toFloat() / workout.maxParticipants },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (workout.isFull) MaterialTheme.colorScheme.error else SportOrange,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${workout.currentParticipants}/${workout.maxParticipants}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                // Статус записи
                if (isUserSignedUp) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SportOrange.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SportOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "Вы записаны на эту тренировку" else "You are signed up for this workout",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SportOrange
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isUserSignedUp) {
                OutlinedButton(
                    onClick = { showCancelConfirmation = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Отменить запись" else "Cancel sign up")
                }
            } else if (!workout.isFull) {
                Button(
                    onClick = onSignUp,
                    colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                ) {
                    Text(if (lang == AppLanguage.RUSSIAN) "Записаться" else "Sign up")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == AppLanguage.RUSSIAN) "Закрыть" else "Close")
            }
        }
    )
}

// ==================== ЧАТЫ КЛИЕНТА ====================

private fun getClientChatId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
}

private fun shortFullName(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return parts.take(2).joinToString(" ").ifBlank { fullName }
}

@Composable
private fun ClientChatsScreen(
    currentUser: User?,
    groupWorkouts: List<GroupWorkout>,
    lang: AppLanguage,
    resetSignal: Int
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    
    var selectedChatTrainer by remember { mutableStateOf<Trainer?>(null) }
    var chatTrainers by remember { mutableStateOf<List<Trainer>>(emptyList()) }

    LaunchedEffect(resetSignal) {
        selectedChatTrainer = null
    }
    
    // Загружаем тренеров из индивидуальных тренировок клиента
    // Используем коллекцию trainers (доступна клиентам), а не users (закрыта для клиентов)
    LaunchedEffect(groupWorkouts, currentUser) {
        val allTrainers = repository.getAllTrainers()
        
        val trainerIdsFromWorkouts = groupWorkouts
            .filter { it.isIndividualWorkout && it.clientId == currentUser?.id && it.trainerId.isNotBlank() }
            .map { it.trainerId }
            .distinct()
        
        chatTrainers = allTrainers
            .filter { (it.id in trainerIdsFromWorkouts || it.userId in trainerIdsFromWorkouts) && it.userId.isNotBlank() }
            .distinctBy { it.userId }
    }
    
    if (selectedChatTrainer != null) {
        val trainer = selectedChatTrainer!!
        ClientChatDetailScreen(
            currentUser = currentUser,
            otherUserId = trainer.userId,
            otherName = trainer.fullName,
            lang = lang,
            onBack = { selectedChatTrainer = null }
        )
    } else {
        if (chatTrainers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(text = "💬", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (lang == AppLanguage.RUSSIAN) "Чатов пока нет" else "No chats yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.RUSSIAN) 
                            "Здесь будут чаты с вашими тренерами по индивидуальным тренировкам" 
                        else 
                            "Your chats with personal trainers will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatTrainers) { trainer ->
                    var unreadCount by remember { mutableIntStateOf(0) }
                    LaunchedEffect(trainer.userId) {
                        val chatId = getClientChatId(currentUser?.id ?: "", trainer.userId)
                        unreadCount = repository.getUnreadMessageCount(chatId, currentUser?.id ?: "")
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChatTrainer = trainer },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(SportOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = trainer.firstName.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trainer.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = if (lang == AppLanguage.RUSSIAN) "Тренер" else "Trainer",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ClientChatDetailScreen(
    currentUser: User?,
    otherUserId: String,
    otherName: String,
    lang: AppLanguage,
    onBack: () -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    
    val chatId = remember(currentUser?.id, otherUserId) { getClientChatId(currentUser?.id ?: "", otherUserId) }
    var messageText by remember { mutableStateOf("") }
    val messages by repository.observeChatMessages(chatId).collectAsState(initial = emptyList())
    
    val incomingUnread = messages.count { !it.isRead && it.senderId != currentUser?.id }
    LaunchedEffect(chatId, incomingUnread) {
        if (incomingUnread > 0) {
            repository.markMessagesAsRead(chatId, currentUser?.id ?: "")
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Шапка чата
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SportOrange.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (lang == AppLanguage.RUSSIAN) "Назад" else "Back",
                        modifier = Modifier.graphicsLayer(rotationZ = 180f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SportOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = otherName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (lang == AppLanguage.RUSSIAN) "Тренер" else "Trainer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Сообщения
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages.reversed()) { message ->
                val isMyMessage = message.senderId == currentUser?.id
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMyMessage) 16.dp else 4.dp,
                            bottomEnd = if (isMyMessage) 4.dp else 16.dp
                        ),
                        color = if (isMyMessage) SportOrange else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = message.formattedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMyMessage) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (isMyMessage) {
                                    Text(
                                        text = if (message.isRead) "✓✓" else "✓",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (message.isRead)
                                            Color.White
                                        else
                                            Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Поле ввода
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(if (lang == AppLanguage.RUSSIAN) "Сообщение..." else "Message...") 
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val text = messageText.trim()
                            messageText = ""
                            scope.launch {
                                repository.sendMessage(
                                    chatId = chatId,
                                    senderId = currentUser?.id ?: "",
                                    senderName = currentUser?.fullName ?: "",
                                    text = text
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SportOrange)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (lang == AppLanguage.RUSSIAN) "Отправить" else "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
