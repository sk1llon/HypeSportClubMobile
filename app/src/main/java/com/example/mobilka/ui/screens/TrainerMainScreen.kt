package com.example.mobilka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka.data.ChatMessage
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.GroupWorkout
import com.example.mobilka.data.Trainer
import com.example.mobilka.data.TrainerAvailability
import com.example.mobilka.data.User
import com.example.mobilka.ui.theme.SportOrange
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Навигация для тренера
enum class TrainerNavItem(
    val label: String,
    val icon: ImageVector
) {
    SCHEDULE("Расписание", Icons.Default.Home),
    CHATS("Чаты", Icons.Default.Email),
    PROFILE("Профиль", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerMainScreen(
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    
    var currentUser by remember { mutableStateOf<User?>(null) }
    var selectedNavItem by remember { mutableStateOf(TrainerNavItem.SCHEDULE) }
    var chatResetSignal by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var clients by remember { mutableStateOf<List<User>>(emptyList()) }
    var myWorkouts by remember { mutableStateOf<List<GroupWorkout>>(emptyList()) }
    var myAvailability by remember { mutableStateOf<List<TrainerAvailability>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var myTrainerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Функция для обновления расписания
    fun refreshAvailability() {
        scope.launch {
            currentUser?.id?.let { userId ->
                val ids = if (myTrainerIds.isNotEmpty()) myTrainerIds else setOf(userId)
                myAvailability = repository.getTrainerAvailabilityForTrainer(
                    trainerIds = ids,
                    trainerName = currentUser?.fullName ?: ""
                )
            }
        }
    }
    
    // Функция для обновления тренировок
    fun refreshWorkouts() {
        scope.launch {
            val allWorkouts = repository.getAllGroupWorkouts()
            myWorkouts = allWorkouts.filter { it.trainerId in myTrainerIds }
        }
    }
    
    // Обновление currentUser в реальном времени (чтобы изменения профиля сразу отображались)
    LaunchedEffect(Unit) {
        repository.observeCurrentUser().collect { user ->
            if (user != null) currentUser = user
        }
    }
    
    // Загрузка данных
    LaunchedEffect(Unit) {
        isLoading = true
        currentUser = repository.getCurrentUser()
        allUsers = repository.getAllUsers()
        clients = allUsers.filter { it.isClient }
        
        currentUser?.id?.let { userId ->
            // Находим все ID тренеров, связанные с этим пользователем
            val allTrainers = repository.getAllTrainers()
            val matchingTrainer = allTrainers.find { it.userId == userId }
            val trainerName = currentUser?.fullName ?: ""
            
            // Собираем все возможные ID для фильтрации тренировок
            val ids = mutableSetOf(userId)
            matchingTrainer?.let { ids.add(it.id) }
            myTrainerIds = ids

            myAvailability = repository.getTrainerAvailabilityForTrainer(
                trainerIds = ids,
                trainerName = trainerName
            )
            
            val allWorkouts = repository.getAllGroupWorkouts()
            // Фильтруем по ID тренера (из trainers коллекции) или по имени тренера
            myWorkouts = allWorkouts.filter { workout ->
                workout.trainerId in ids || workout.trainerName == trainerName
            }
        }
        isLoading = false
    }
    
    val displayName = currentUser?.let { user ->
        val name = "${user.firstName} ${user.lastName}".trim()
        name.ifBlank { "Тренер" }
    } ?: "Тренер"
    
    val topBarTitle = when (selectedNavItem) {
        TrainerNavItem.SCHEDULE -> "Моё расписание"
        TrainerNavItem.CHATS -> "Чаты"
        TrainerNavItem.PROFILE -> "Профиль"
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                        if (selectedNavItem == TrainerNavItem.SCHEDULE) {
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
                    if (selectedNavItem == TrainerNavItem.PROFILE) {
                        IconButton(
                            onClick = {
                                repository.logout()
                                onLogout()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Выйти из аккаунта",
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
                TrainerNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { 
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        selected = selectedNavItem == item,
                        onClick = {
                            if (item == TrainerNavItem.CHATS && selectedNavItem == TrainerNavItem.CHATS) {
                                chatResetSignal++
                            } else {
                                selectedNavItem = item
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SportOrange)
                }
            } else {
                when (selectedNavItem) {
                    TrainerNavItem.SCHEDULE -> {
                        TrainerScheduleScreen(
                            currentUser = currentUser,
                            availability = myAvailability,
                            workouts = myWorkouts,
                            onAddAvailability = { availability ->
                                scope.launch {
                                    repository.addTrainerAvailability(availability)
                                    refreshAvailability()
                                }
                            },
                            onDeleteAvailability = { availabilityId ->
                                scope.launch {
                                    repository.deleteTrainerAvailability(availabilityId)
                                    refreshAvailability()
                                }
                            }
                        )
                    }
                    TrainerNavItem.CHATS -> {
                        TrainerChatsScreen(
                            currentUser = currentUser,
                            workouts = myWorkouts,
                            allUsers = allUsers,
                            resetSignal = chatResetSignal
                        )
                    }
                    TrainerNavItem.PROFILE -> {
                        TrainerProfileScreen(
                            user = currentUser,
                            onLogout = {
                                repository.logout()
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerScheduleScreen(
    currentUser: User?,
    availability: List<TrainerAvailability>,
    workouts: List<GroupWorkout>,
    onAddAvailability: (TrainerAvailability) -> Unit,
    onDeleteAvailability: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var currentWeekStart by remember { 
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEE", Locale("ru")) }
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    
    val monthNames = remember { mapOf(
        0 to "Январь", 1 to "Февраль", 2 to "Март", 3 to "Апрель",
        4 to "Май", 5 to "Июнь", 6 to "Июль", 7 to "Август",
        8 to "Сентябрь", 9 to "Октябрь", 10 to "Ноябрь", 11 to "Декабрь"
    ) }
    
    // Получаем дни текущей недели
    val weekDays = remember(currentWeekStart) {
        (0..6).map { dayOffset ->
            Calendar.getInstance().apply {
                timeInMillis = currentWeekStart.timeInMillis
                add(Calendar.DAY_OF_MONTH, dayOffset)
            }
        }
    }
    
    // Фильтруем расписание для выбранной даты (исключаем пустые записи)
    val selectedDateAvailability = remember(selectedDate, availability) {
        selectedDate?.let { date ->
            availability.filter { slot ->
                val slotDate = Calendar.getInstance().apply {
                    time = slot.date.toDate()
                }
                slotDate.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                slotDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) &&
                slot.startTime.isNotBlank() && slot.endTime.isNotBlank()
            }.sortedBy { it.startTime }
        } ?: emptyList()
    }
    
    // Фильтруем групповые тренировки для выбранной даты
    val selectedDateWorkouts = remember(selectedDate, workouts) {
        selectedDate?.let { date ->
            workouts.filter { workout ->
                val workoutDate = Calendar.getInstance().apply {
                    time = workout.dateTime.toDate()
                }
                workoutDate.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                workoutDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
            }.sortedBy { it.dateTime }
        } ?: emptyList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок с месяцем и навигацией
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SportOrange.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentWeekStart = Calendar.getInstance().apply {
                            timeInMillis = currentWeekStart.timeInMillis
                            add(Calendar.WEEK_OF_YEAR, -1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Предыдущая неделя",
                            tint = SportOrange
                        )
                    }
                    
                    // Корректное отображение месяца с учётом перехода между месяцами
                    val firstDay = weekDays.first()
                    val lastDay = weekDays.last()
                    val headerText = if (firstDay.get(Calendar.MONTH) != lastDay.get(Calendar.MONTH)) {
                        if (firstDay.get(Calendar.YEAR) != lastDay.get(Calendar.YEAR)) {
                            "${monthNames[firstDay.get(Calendar.MONTH)]} ${firstDay.get(Calendar.YEAR)} - ${monthNames[lastDay.get(Calendar.MONTH)]} ${lastDay.get(Calendar.YEAR)}"
                        } else {
                            "${monthNames[firstDay.get(Calendar.MONTH)]} - ${monthNames[lastDay.get(Calendar.MONTH)]} ${lastDay.get(Calendar.YEAR)}"
                        }
                    } else {
                        "${monthNames[firstDay.get(Calendar.MONTH)]} ${firstDay.get(Calendar.YEAR)}"
                    }
                    
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SportOrange
                    )
                    
                    IconButton(onClick = {
                        currentWeekStart = Calendar.getInstance().apply {
                            timeInMillis = currentWeekStart.timeInMillis
                            add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Следующая неделя",
                            tint = SportOrange
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Дни недели
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val today = Calendar.getInstance()
                    
                    weekDays.forEach { day ->
                        val isSelected = selectedDate?.let {
                            it.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            it.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                        } ?: false
                        
                        val isToday = today.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            today.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                        
                        // Проверяем, является ли день прошедшим (до сегодняшнего дня)
                        val isPastDay = day.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
                            (day.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
                             day.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR))
                        
                        // Проверяем есть ли расписание на этот день (с непустым временем)
                        val hasAvailability = availability.any { slot ->
                            val slotDate = Calendar.getInstance().apply { time = slot.date.toDate() }
                            slotDate.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            slotDate.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                            slot.startTime.isNotBlank() && slot.endTime.isNotBlank()
                        }
                        
                        // Проверяем есть ли тренировки на этот день
                        val hasWorkouts = workouts.any { workout ->
                            val workoutDate = Calendar.getInstance().apply { time = workout.dateTime.toDate() }
                            workoutDate.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            workoutDate.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                        }
                        
                        val hasSchedule = hasAvailability || hasWorkouts
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedDate = day }
                                .background(
                                    when {
                                        isSelected -> SportOrange
                                        isToday -> SportOrange.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = dayOfWeekFormat.format(day.time).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isSelected -> Color.White
                                    isPastDay -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayFormat.format(day.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isSelected -> Color.White
                                    isPastDay -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasSchedule) {
                                            if (isSelected) Color.White else SportOrange
                                        } else Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Проверяем, можно ли добавлять расписание на выбранную дату
        val canAddAvailability = selectedDate?.let { date ->
            val today = Calendar.getInstance()
            date.get(Calendar.YEAR) > today.get(Calendar.YEAR) ||
            (date.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
             date.get(Calendar.DAY_OF_YEAR) > today.get(Calendar.DAY_OF_YEAR))
        } ?: false
        
        // Расписание на выбранную дату
        if (selectedDate != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Расписание на ${dateFormat.format(selectedDate!!.time)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                // Показываем кнопку добавления только для будущих дат
                if (canAddAvailability) {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SportOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить слот",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (selectedDateAvailability.isEmpty() && selectedDateWorkouts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нет расписания на этот день",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Добавьте время для индивидуальных тренировок",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Групповые тренировки
                    if (selectedDateWorkouts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Тренировки",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(selectedDateWorkouts) { workout ->
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (workout.isIndividualWorkout) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                    else 
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (workout.isIndividualWorkout) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.secondary
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (workout.isIndividualWorkout) "🏋️" else "👥",
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (workout.isIndividualWorkout) 
                                                "Индивидуальная" 
                                            else 
                                                "Групповая - ${workout.name}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${timeFormat.format(workout.dateTime.toDate())} • ${workout.durationMinutes} мин",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (workout.isIndividualWorkout && workout.clientName.isNotBlank()) {
                                            Text(
                                                text = "Клиент: ${workout.clientName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else if (!workout.isIndividualWorkout) {
                                            Text(
                                                text = "Участников: ${workout.currentParticipants}/${workout.maxParticipants}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Слоты для индивидуальных тренировок
                    if (selectedDateAvailability.isNotEmpty()) {
                        item {
                            if (selectedDateWorkouts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = "Слоты для записи",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SportOrange,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(selectedDateAvailability) { slot ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (slot.isAvailable) 
                                        SportOrange.copy(alpha = 0.15f) 
                                    else 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (slot.isAvailable) SportOrange else MaterialTheme.colorScheme.error),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "⏰",
                                                fontSize = 20.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = slot.timeRange,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = if (slot.isAvailable) "Доступно" else "Занято",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (slot.isAvailable) SportOrange else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    IconButton(onClick = { onDeleteAvailability(slot.id) }) {
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
                }
            }
        } else {
            // Когда дата не выбрана
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "👆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Выберите дату",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "чтобы настроить расписание для индивидуальных тренировок",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Диалог добавления слота
    if (showAddDialog && selectedDate != null) {
        AddAvailabilityDialog(
            selectedDate = selectedDate!!,
            currentUser = currentUser,
            onDismiss = { showAddDialog = false },
            onConfirm = { availability ->
                onAddAvailability(availability)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddAvailabilityDialog(
    selectedDate: Calendar,
    currentUser: User?,
    onDismiss: () -> Unit,
    onConfirm: (TrainerAvailability) -> Unit
) {
    var startHour by remember { mutableStateOf("09") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("10") }
    var endMinute by remember { mutableStateOf("00") }
    
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Добавить слот",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Дата
                OutlinedTextField(
                    value = dateFormat.format(selectedDate.time),
                    onValueChange = {},
                    label = { Text("Дата") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Время начала
                Text(
                    text = "Время начала",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startHour = it },
                        label = { Text("Часы") },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    OutlinedTextField(
                        value = startMinute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startMinute = it },
                        label = { Text("Минуты") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Время окончания
                Text(
                    text = "Время окончания",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endHour = it },
                        label = { Text("Часы") },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    OutlinedTextField(
                        value = endMinute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endMinute = it },
                        label = { Text("Минуты") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedDate = Calendar.getInstance().apply {
                        timeInMillis = selectedDate.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dateTimestamp = Timestamp(normalizedDate.time)
                    val startTime = "${startHour.padStart(2, '0')}:${startMinute.padStart(2, '0')}"
                    val endTime = "${endHour.padStart(2, '0')}:${endMinute.padStart(2, '0')}"
                    
                    val availability = TrainerAvailability(
                        trainerId = currentUser?.id ?: "",
                        trainerName = currentUser?.fullName ?: "",
                        date = dateTimestamp,
                        startTime = startTime,
                        endTime = endTime,
                        isAvailable = true,
                        notes = ""
                    )
                    onConfirm(availability)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun TrainerChatsScreen(
    currentUser: User?,
    workouts: List<GroupWorkout>,
    allUsers: List<User>,
    resetSignal: Int
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    
    var selectedChatClient by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(resetSignal) {
        selectedChatClient = null
    }
    
    // Получаем уникальных клиентов из индивидуальных тренировок
    val clientsWithChats = remember(workouts, allUsers) {
        val clientIds = workouts
            .filter { it.isIndividualWorkout && it.clientId.isNotBlank() }
            .map { it.clientId }
            .distinct()
        
        allUsers.filter { it.id in clientIds }
    }
    
    if (selectedChatClient != null) {
        // Экран чата
        ChatScreen(
            currentUser = currentUser,
            otherUser = selectedChatClient!!,
            onBack = { selectedChatClient = null }
        )
    } else {
        // Список чатов
        if (clientsWithChats.isEmpty()) {
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
                        text = "Чатов пока нет",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Здесь будут отображаться чаты с клиентами, которые записаны на индивидуальные тренировки",
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
                items(clientsWithChats) { client ->
                    val lastWorkout = workouts
                        .filter { it.isIndividualWorkout && it.clientId == client.id }
                        .maxByOrNull { it.dateTime }
                    
                    // Непрочитанные сообщения
                    var unreadCount by remember { mutableIntStateOf(0) }
                    LaunchedEffect(client.id) {
                        val chatId = getChatId(currentUser?.id ?: "", client.id)
                        unreadCount = repository.getUnreadMessageCount(chatId, currentUser?.id ?: "")
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedChatClient = client },
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
                                    text = client.firstName.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                lastWorkout?.let { workout ->
                                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    Text(
                                        text = "Тренировка: ${dateFormat.format(workout.dateTime.toDate())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

// Формируем chatId из двух ID (всегда в одинаковом порядке)
private fun getChatId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
}

@Composable
private fun ChatScreen(
    currentUser: User?,
    otherUser: User,
    onBack: () -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()
    
    val chatId = remember { getChatId(currentUser?.id ?: "", otherUser.id) }
    var messageText by remember { mutableStateOf("") }
    val messages by repository.observeChatMessages(chatId).collectAsState(initial = emptyList())
    
    // Помечаем сообщения как прочитанные
    // Помечаем только входящие непрочитанные сообщения; ключ — количество таких сообщений,
    // чтобы LaunchedEffect не перезапускался при обновлении isRead (избегаем отмены корутины)
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
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Назад"
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
                        text = otherUser.firstName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = otherUser.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
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
                    placeholder = { Text("Сообщение...") },
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
                        contentDescription = "Отправить",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainerProfileScreen(
    user: User?,
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepo.instance }
    val scope = rememberCoroutineScope()

    var email by remember(user?.email) { mutableStateOf(user?.email ?: "") }
    var phone by remember(user?.phone) { mutableStateOf(user?.phone ?: "") }
    var birthDate by remember(user?.birthDate) { mutableStateOf(user?.birthDate ?: "") }
    var height by remember(user?.height) {
        mutableStateOf(if ((user?.height ?: 0f) > 0f) user!!.height.toInt().toString() else "")
    }
    var weight by remember(user?.weight) {
        mutableStateOf(if ((user?.weight ?: 0f) > 0f) user!!.weight.toInt().toString() else "")
    }

    val originalEmail = user?.email ?: ""
    val originalPhone = user?.phone ?: ""
    val originalBirthDate = user?.birthDate ?: ""
    val originalHeight = if ((user?.height ?: 0f) > 0f) user!!.height.toInt().toString() else ""
    val originalWeight = if ((user?.weight ?: 0f) > 0f) user!!.weight.toInt().toString() else ""

    val hasChanges = email != originalEmail || phone != originalPhone ||
        birthDate != originalBirthDate || height != originalHeight || weight != originalWeight

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
        // Аватар
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(SportOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.firstName?.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Нередактируемые поля: Фамилия, Имя, Отчество
        item {
            Text(
                text = "Личные данные",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        item {
            ProfileReadonlyField(label = "Фамилия", value = user?.lastName ?: "—", icon = "👤")
        }
        item {
            ProfileReadonlyField(label = "Имя", value = user?.firstName ?: "—", icon = "👤")
        }
        item {
            ProfileReadonlyField(label = "Отчество", value = user?.middleName ?: "—", icon = "👤")
        }

        // Редактируемые поля
        item {
            ProfileEditField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                icon = "📧",
                keyboardType = KeyboardType.Email
            )
        }
        item {
            ProfileEditField(
                label = "Телефон",
                value = phone,
                onValueChange = { phone = it },
                icon = "📱",
                keyboardType = KeyboardType.Phone
            )
        }
        item {
            ProfileEditField(
                label = "Дата рождения (ДД.ММ.ГГГГ)",
                value = birthDate,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() || c == '.' }) birthDate = it },
                icon = "📅"
            )
        }
        item {
            ProfileEditField(
                label = "Рост (см)",
                value = height,
                onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                icon = "📏",
                keyboardType = KeyboardType.Number
            )
        }
        item {
            ProfileEditField(
                label = "Вес (кг)",
                value = weight,
                onValueChange = { if (it.all { c -> c.isDigit() }) weight = it },
                icon = "⚖️",
                keyboardType = KeyboardType.Number
            )
        }

        // Кнопки Сохранить/Отмена появляются при изменениях
        if (hasChanges) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            email = originalEmail; phone = originalPhone
                            birthDate = originalBirthDate; height = originalHeight; weight = originalWeight
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            scope.launch {
                                errorMessage = null
                                val emailVal = email.trim()
                                if (emailVal.isNotBlank() && !emailVal.contains("@")) {
                                    errorMessage = "Введите корректный email-адрес"
                                    return@launch
                                }
                                val bdVal = birthDate.trim()
                                if (bdVal.isNotBlank() && bdVal.length != 10) {
                                    errorMessage = "Дата рождения должна быть в формате ДД.ММ.ГГГГ"
                                    return@launch
                                }
                                val h = height.toIntOrNull() ?: 0
                                if (height.isNotBlank() && (h < 50 || h > 300)) {
                                    errorMessage = "Рост должен быть от 50 до 300 см"
                                    return@launch
                                }
                                val w = weight.toIntOrNull() ?: 0
                                if (weight.isNotBlank() && (w < 20 || w > 500)) {
                                    errorMessage = "Вес должен быть от 20 до 500 кг"
                                    return@launch
                                }
                                isSaving = true
                                val userId = user?.id ?: return@launch
                                val result = repository.updateTrainerProfile(
                                    userId = userId,
                                    email = emailVal,
                                    phone = phone.trim(),
                                    birthDate = bdVal,
                                    height = height.toFloatOrNull() ?: 0f,
                                    weight = weight.toFloatOrNull() ?: 0f
                                )
                                if (result.isSuccess) {
                                    successMessage = "Данные сохранены"
                                } else {
                                    errorMessage = "Не удалось сохранить данные. Попробуйте позже"
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                    ) {
                        if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Сохранить", color = Color.White)
                    }
                }
            }
        }

        if (successMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✓ $successMessage",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Текущий пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Новый пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Подтвердите новый пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SportOrange)
                        ) { Text("Сохранить пароль", color = Color.White) }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ProfileReadonlyField(label: String, value: String, icon: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Text(text = icon, fontSize = 20.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

