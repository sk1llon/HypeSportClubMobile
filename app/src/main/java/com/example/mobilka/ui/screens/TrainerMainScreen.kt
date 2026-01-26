package com.example.mobilka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.GroupWorkout
import com.example.mobilka.data.User
import com.example.mobilka.ui.theme.SportOrange
import kotlinx.coroutines.launch

// Навигация для тренера
enum class TrainerNavItem(
    val label: String,
    val icon: ImageVector
) {
    SCHEDULE("Расписание", Icons.Default.Home),
    CLIENTS("Клиенты", Icons.Default.Star),
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
    var isLoading by remember { mutableStateOf(true) }
    var clients by remember { mutableStateOf<List<User>>(emptyList()) }
    var myWorkouts by remember { mutableStateOf<List<GroupWorkout>>(emptyList()) }
    
    // Загрузка данных
    LaunchedEffect(Unit) {
        isLoading = true
        currentUser = repository.getCurrentUser()
        clients = repository.getAllUsers().filter { it.isClient }
        isLoading = false
    }
    
    val displayName = currentUser?.let { user ->
        val name = "${user.firstName} ${user.lastName}".trim()
        name.ifBlank { "Тренер" }
    } ?: "Тренер"
    
    val topBarTitle = when (selectedNavItem) {
        TrainerNavItem.SCHEDULE -> "Моё расписание"
        TrainerNavItem.CLIENTS -> "Мои клиенты"
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
                )
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SportOrange)
                }
            } else {
                when (selectedNavItem) {
                    TrainerNavItem.SCHEDULE -> {
                        TrainerScheduleScreen(workouts = myWorkouts)
                    }
                    TrainerNavItem.CLIENTS -> {
                        TrainerClientsScreen(clients = clients)
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
private fun TrainerScheduleScreen(workouts: List<GroupWorkout>) {
    if (workouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📅", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "У вас пока нет тренировок",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Администратор добавит вам расписание",
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = workout.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text(text = "📅 ${workout.formattedDateTime}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "⏱️ ${workout.durationMinutes} мин", style = MaterialTheme.typography.bodyMedium)
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
        }
    }
}

@Composable
private fun TrainerClientsScreen(clients: List<User>) {
    if (clients.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "👥", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Клиентов пока нет",
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
            item {
                Text(
                    text = "Всего клиентов: ${clients.size}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(clients) { client ->
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = client.firstName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = client.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            if (client.phone.isNotBlank()) {
                                Text(
                                    text = "📱 ${client.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.fullName ?: "Тренер",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Тренер",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Информация профиля
        item {
            ProfileInfoCardTrainer(label = "Email", value = user?.email ?: "—", icon = "📧")
        }
        item {
            ProfileInfoCardTrainer(label = "Телефон", value = user?.phone?.ifBlank { "—" } ?: "—", icon = "📱")
        }
        item {
            ProfileInfoCardTrainer(label = "Дата рождения", value = user?.birthDate?.ifBlank { "—" } ?: "—", icon = "📅")
        }
        
        // Кнопка выхода
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚪", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Выйти из аккаунта",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ProfileInfoCardTrainer(label: String, value: String, icon: String) {
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
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

