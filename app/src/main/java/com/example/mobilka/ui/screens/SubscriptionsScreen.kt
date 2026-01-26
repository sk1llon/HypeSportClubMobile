package com.example.mobilka.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.FitnessGoal
import com.example.mobilka.data.Gender
import com.example.mobilka.data.GroupWorkout
import com.example.mobilka.data.SettingsManager
import com.example.mobilka.data.Strings
import com.example.mobilka.data.Subscription
import com.example.mobilka.data.Trainer
import com.example.mobilka.data.User
import com.example.mobilka.data.UserRole
import com.example.mobilka.data.UserSubscription
import com.example.mobilka.ui.theme.EnergyGreen
import com.example.mobilka.ui.theme.SportOrange
import com.example.mobilka.ui.theme.SportOrangeDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

// Элементы навигации для клиента
enum class ClientNavItem(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    CALCULATOR(Icons.Default.List),
    WORKOUTS(Icons.Default.Star),
    PROFILE(Icons.Default.Person);
    
    fun getLabel(lang: AppLanguage): String = when (this) {
        HOME -> Strings.home(lang)
        CALCULATOR -> Strings.calculator(lang)
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

// Типы тренировок
enum class WorkoutType {
    INDIVIDUAL,
    GROUP;
    
    fun getTitle(lang: AppLanguage): String = when (this) {
        INDIVIDUAL -> Strings.individual(lang)
        GROUP -> Strings.group(lang)
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNavItem by remember { mutableStateOf(ClientNavItem.HOME) }
    var trainersList by remember { mutableStateOf<List<Trainer>>(emptyList()) }
    var groupWorkouts by remember { mutableStateOf<List<GroupWorkout>>(emptyList()) }
    
    // Текущий раздел профиля (null = главное меню профиля)
    var currentProfileSection by remember { mutableStateOf<ProfileSection?>(null) }
    
    // Тип тренировок (для раздела Тренировки) - по умолчанию индивидуальные
    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.INDIVIDUAL) }
    
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
                                    IndividualWorkoutsScreen(trainers = trainersList)
                                }
                                WorkoutType.GROUP -> {
                                    GroupWorkoutsScreen(workouts = groupWorkouts)
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
                                    }
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
            
            // Сообщение об успешной покупке
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
                            text = "Абонемент успешно оформлен!",
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
    onLogoutClick: () -> Unit
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
                        text = user?.fullName ?: "Пользователь",
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
                title = "Личные данные",
                subtitle = "Телефон, почта, пароль",
                onClick = onPersonalInfoClick
            )
        }
        
        // Данные для БЖУ
        item {
            ProfileMenuButton(
                icon = "📊",
                title = "Данные для БЖУ",
                subtitle = "Пол, вес, рост, цель",
                onClick = onHealthDataClick
            )
        }
        
        // Абонементы
        item {
            ProfileMenuButton(
                icon = "🎫",
                title = "Абонементы",
                subtitle = "Текущие и доступные",
                onClick = onSubscriptionsClick
            )
        }
        
        // Тренеры
        item {
            ProfileMenuButton(
                icon = "🏃",
                title = "Тренеры",
                subtitle = "Список тренеров клуба",
                onClick = onTrainersClick
            )
        }
        
        // Настройки
        item {
            ProfileMenuButton(
                icon = "⚙️",
                title = "Настройки",
                subtitle = "Тема, язык",
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
                        text = "Выйти из аккаунта",
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
                            if (trainer.phone.isNotBlank()) {
                                Text(
                                    text = "📱 ${trainer.phone}",
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkoutType.entries.forEach { type ->
                val isSelected = selectedType == type
                val backgroundColor = if (isSelected) SportOrange else Color.Transparent
                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = backgroundColor,
                    onClick = { onTypeSelected(type) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (type == WorkoutType.INDIVIDUAL) "🏋️" else "👥",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type.getTitle(lang),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = contentColor
                        )
                    }
                }
            }
        }
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

@Composable
private fun GroupWorkoutsScreen(workouts: List<GroupWorkout>) {
    if (workouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "👥", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Групповых тренировок пока нет",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Следите за расписанием",
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
                    modifier = Modifier.fillMaxWidth(),
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
                                if (workout.description.isNotBlank()) {
                                    Text(
                                        text = workout.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (workout.isFull) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Мест нет",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📅", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = workout.formattedDateTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⏱️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${workout.durationMinutes} мин",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👤", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Тренер: ${workout.trainerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Участников: ${workout.currentParticipants}/${workout.maxParticipants}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
