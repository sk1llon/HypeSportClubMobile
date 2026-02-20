package com.example.mobilka.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.mobilka.ui.theme.EnergyGreen
import com.example.mobilka.ui.theme.SportOrange
import com.example.mobilka.ui.theme.SportOrangeDark
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
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showWorkoutSignUpSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNavItem by remember { mutableStateOf(ClientNavItem.HOME) }
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
            userSubscriptions = subs
        }
    }
    
    LaunchedEffect(Unit) {
        repository.observeAvailableSubscriptions().collect { subs ->
            if (subs.isNotEmpty()) {
                availableSubscriptions = subs
            }
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
                )
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
                        onClick = { selectedNavItem = item },
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
                        // Главная страница
                        HomeWelcomeContent()
                    }
                    ClientNavItem.CALCULATOR -> {
                        // Заглушка для БЖУ калькулятора
                        PlaceholderContent(
                            icon = "🧮",
                            title = "БЖУ Калькулятор",
                            subtitle = "Раздел в разработке"
                        )
                    }
                    ClientNavItem.CHATS -> {
                        ClientChatsScreen(
                            currentUser = currentUser,
                            groupWorkouts = groupWorkouts,
                            lang = lang
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
                                                        showWorkoutSignUpSuccess = true
                                                        delay(3000)
                                                        showWorkoutSignUpSuccess = false
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
                                    onHealthDataClick = { currentProfileSection = ProfileSection.HEALTH_DATA },
                                    onSubscriptionsClick = { currentProfileSection = ProfileSection.SUBSCRIPTIONS },
                                    onTrainersClick = { currentProfileSection = ProfileSection.TRAINERS },
                                    onSettingsClick = { currentProfileSection = ProfileSection.SETTINGS },
                                    onLogoutClick = {
                                        repository.logout()
                                        onLogout()
                                    },
                                    lang = lang
                                )
                            }
                            ProfileSection.PERSONAL_INFO -> {
                                PersonalInfoScreen(
                                    user = currentUser,
                                    onSave = { phone, email ->
                                        scope.launch {
                                            repository.updateUserContactInfo(phone, email)
                                        }
                                    }
                                )
                            }
                            ProfileSection.HEALTH_DATA -> {
                                HealthDataScreen(
                                    user = currentUser,
                                    onSave = { gender, weight, height, goal ->
                                        scope.launch {
                                            repository.updateUserHealthData(gender, weight, height, goal)
                                        }
                                    }
                                )
                            }
                            ProfileSection.SUBSCRIPTIONS -> {
                                SubscriptionsFullScreen(
                                    userSubscriptions = activeSubscriptions,
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
            
            // Сообщение об успешной покупке абонемента
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = EnergyGreen
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Абонемент успешно оформлен!" else "Subscription purchased successfully!",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Сообщение об успешной записи на тренировку
            AnimatedVisibility(
                visible = showWorkoutSignUpSuccess,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SportOrange
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Вы записаны на тренировку!" else "You signed up for workout!",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
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
                                    showSuccessMessage = true
                                    // Обновляем список абонементов
                                    userSubscriptions = repository.getUserSubscriptions()
                                    delay(3000)
                                    showSuccessMessage = false
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
                            SportOrangeDark
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
private fun HomeWelcomeContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🏋️",
                fontSize = 80.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Добро пожаловать!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HypeSportClub",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Используйте меню навигации для доступа к функциям приложения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlaceholderContent(
    icon: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileContent(
    user: User?,
    onPersonalInfoClick: () -> Unit,
    onHealthDataClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onTrainersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
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
        
        // Данные для БЖУ
        item {
            ProfileMenuButton(
                icon = "📊",
                title = Strings.healthData(lang),
                subtitle = Strings.genderWeightHeightGoal(lang),
                onClick = onHealthDataClick
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
        
        // Выход из аккаунта
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogoutClick() },
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
                        text = "🚪",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = Strings.logout(lang),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
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
    onSave: (phone: String, email: String) -> Unit
) {
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileInfoCard(label = "ФИО", value = user?.fullName ?: "—", icon = "👤")
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Телефон",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; isSaved = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("+7 (___) ___-__-__") }
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📧 Email",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); isSaved = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("example@mail.com") }
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPasswordDialog = true },
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
                    Text(text = "🔒", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Пароль", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "••••••••", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(text = "Изменить", color = SportOrange, fontWeight = FontWeight.Medium)
                }
            }
        }
        
        item {
            Button(
                onClick = { 
                    onSave(phone, email)
                    isSaved = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                Text(if (isSaved) "✓ Сохранено" else "Сохранить изменения")
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
    
    // Диалог изменения пароля
    if (showPasswordDialog) {
        ChangePasswordDialog(onDismiss = { showPasswordDialog = false })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthDataScreen(
    user: User?,
    onSave: (gender: String, weight: Float, height: Float, goal: String) -> Unit
) {
    var weight by remember { mutableStateOf(if ((user?.weight ?: 0f) > 0) user?.weight.toString() else "") }
    var height by remember { mutableStateOf(if ((user?.height ?: 0f) > 0) user?.height.toString() else "") }
    var selectedGoal by remember { mutableStateOf(user?.userFitnessGoal ?: FitnessGoal.MAINTENANCE) }
    var isSaved by remember { mutableStateOf(false) }
    
    val bmi = remember(weight, height) {
        val w = weight.toFloatOrNull() ?: 0f
        val h = height.toFloatOrNull() ?: 0f
        if (w > 0 && h > 0) {
            val hMeters = h / 100
            w / (hMeters * hMeters)
        } else 0f
    }
    
    val bmiCategory = when {
        bmi <= 0 -> ""
        bmi < 18.5f -> "Недостаточный вес"
        bmi < 25f -> "Норма"
        bmi < 30f -> "Избыточный вес"
        else -> "Ожирение"
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Возраст
        item {
            ProfileInfoCard(
                label = "Возраст",
                value = if ((user?.age ?: 0) > 0) "${user?.age} лет" else "Не указан",
                icon = "🎂"
            )
        }
        
        // Вес
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "⚖️ Вес (кг)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) { weight = it; isSaved = false } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("70") }
                    )
                }
            }
        }
        
        // Рост
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📏 Рост (см)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) { height = it; isSaved = false } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("175") }
                    )
                }
            }
        }
        
        // ИМТ
        if (bmi > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            bmi < 18.5f || bmi >= 30f -> MaterialTheme.colorScheme.errorContainer
                            bmi < 25f -> EnergyGreen.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📊", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "ИМТ (Индекс массы тела)", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "%.1f — $bmiCategory".format(bmi),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
        
        // Цель тренировок
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎯 Цель занятий", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    FitnessGoal.entries.forEach { goal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGoal = goal; isSaved = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal; isSaved = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = goal.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = {
                    onSave(user?.gender ?: Gender.MALE.name, weight.toFloatOrNull() ?: 0f, height.toFloatOrNull() ?: 0f, selectedGoal.name)
                    isSaved = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                Text(if (isSaved) "✓ Сохранено" else "Сохранить изменения")
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// Диалог изменения пароля
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить пароль", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; errorMessage = null },
                    label = { Text("Текущий пароль") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text("Новый пароль") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Подтвердите пароль") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPassword.length < 6 -> errorMessage = "Пароль должен быть не менее 6 символов"
                        newPassword != confirmPassword -> errorMessage = "Пароли не совпадают"
                        else -> {
                            // TODO: Реализовать смену пароля через Firebase Auth
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SubscriptionsFullScreen(
    userSubscriptions: List<UserSubscription>,
    availableSubscriptions: List<Subscription>,
    onPurchase: (Subscription) -> Unit
) {
    val hasActiveSubscriptions = userSubscriptions.isNotEmpty()
    
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
                    "Ваши активные подписки (${userSubscriptions.size})" 
                else 
                    "У вас пока нет активных абонементов"
            )
        }

        if (hasActiveSubscriptions) {
            // Активные абонементы
            items(userSubscriptions, key = { it.id }) { subscription ->
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                if (trainer.experience > 0) {
                                    Text(
                                        text = "📅 ${trainer.experienceText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                if (trainer.pricePerTraining > 0) {
                                    Text(
                                        text = "💰 ${trainer.pricePerTraining} ₽",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SportOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            val specText = trainer.specializationsText
                            if (specText.isNotBlank()) {
                                Text(
                                    text = "🏅 $specText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isDarkTheme,
                            onClick = { settingsManager.setTheme(AppTheme.DARK) },
                            label = { Text(Strings.darkTheme(lang)) },
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = lang == AppLanguage.ENGLISH,
                            onClick = { settingsManager.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("🇺🇸 English") },
                            modifier = Modifier.weight(1f)
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
    var showSuccess by remember { mutableStateOf(false) }
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
        availableSlots = repository.getAvailableTrainerSlots(trainer.userId)
        bookedHours = repository.getBookedIndividualHours(trainer.userId)
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
                                text = if (lang == AppLanguage.RUSSIAN) "У тренера нет свободного времени"
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
                                        if (hasSlots && !isPast)
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
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN)
                                    "Нет свободного времени на эту дату"
                                else "No available time slots on this date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                            showSuccess = true
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

        if (showSuccess) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✅", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN)
                                "Вы успешно записаны на тренировку! Она отображается в разделе «Мои тренировки»."
                            else "You have successfully booked a workout! It appears in 'My Workouts'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
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
                                    text = "👤 ${workout.trainerName}",
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

@Composable
private fun ClientChatsScreen(
    currentUser: User?,
    groupWorkouts: List<GroupWorkout>,
    lang: AppLanguage
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    
    var selectedChatTrainer by remember { mutableStateOf<Trainer?>(null) }
    var chatTrainers by remember { mutableStateOf<List<Trainer>>(emptyList()) }
    
    // Загружаем тренеров из индивидуальных тренировок клиента
    // Используем коллекцию trainers (доступна клиентам), а не users (закрыта для клиентов)
    LaunchedEffect(groupWorkouts, currentUser) {
        val allTrainers = repository.getAllTrainers()
        
        val trainerIdsFromWorkouts = groupWorkouts
            .filter { it.isIndividualWorkout && it.clientId == currentUser?.id && it.trainerId.isNotBlank() }
            .map { it.trainerId }
            .distinct()
        
        chatTrainers = allTrainers
            .filter { it.id in trainerIdsFromWorkouts && it.userId.isNotBlank() }
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SportOrange
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(SportOrange)
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
                        color = SportOrange
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
