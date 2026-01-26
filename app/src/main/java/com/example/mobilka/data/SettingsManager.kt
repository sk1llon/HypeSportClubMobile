package com.example.mobilka.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Поддерживаемые темы
enum class AppTheme(val displayName: String, val displayNameEn: String) {
    LIGHT("Светлая", "Light"),
    DARK("Тёмная", "Dark");
    
    fun getDisplayName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.RUSSIAN -> displayName
            AppLanguage.ENGLISH -> displayNameEn
        }
    }
}

// Поддерживаемые языки
enum class AppLanguage(val displayName: String, val code: String) {
    RUSSIAN("Русский", "ru"),
    ENGLISH("English", "en")
}

// Менеджер настроек (Singleton)
class SettingsManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()
    
    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()
    
    private fun loadTheme(): AppTheme {
        val themeName = prefs.getString(KEY_THEME, AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.LIGHT
        }
    }
    
    private fun loadLanguage(): AppLanguage {
        val langName = prefs.getString(KEY_LANGUAGE, AppLanguage.RUSSIAN.name) ?: AppLanguage.RUSSIAN.name
        return try {
            AppLanguage.valueOf(langName)
        } catch (e: Exception) {
            AppLanguage.RUSSIAN
        }
    }
    
    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }
    
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _language.value = language
    }
    
    val isDarkTheme: Boolean
        get() = _theme.value == AppTheme.DARK
    
    val isEnglish: Boolean
        get() = _language.value == AppLanguage.ENGLISH
    
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Для использования в Composable
        val instance: SettingsManager
            get() = INSTANCE ?: throw IllegalStateException("SettingsManager not initialized. Call getInstance(context) first.")
    }
}

// Локализованные строки
object Strings {
    // Profile section
    fun profileTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Профиль"
        AppLanguage.ENGLISH -> "Profile"
    }
    
    fun personalData(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Личные данные"
        AppLanguage.ENGLISH -> "Personal Data"
    }
    
    fun healthData(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Данные для БЖУ"
        AppLanguage.ENGLISH -> "Health Data"
    }
    
    fun subscriptions(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Абонементы"
        AppLanguage.ENGLISH -> "Subscriptions"
    }
    
    fun trainers(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренеры"
        AppLanguage.ENGLISH -> "Trainers"
    }
    
    fun settings(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Настройки"
        AppLanguage.ENGLISH -> "Settings"
    }
    
    fun logout(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выйти из аккаунта"
        AppLanguage.ENGLISH -> "Logout"
    }
    
    // Navigation
    fun home(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Главная"
        AppLanguage.ENGLISH -> "Home"
    }
    
    fun calculator(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "БЖУ"
        AppLanguage.ENGLISH -> "Macros"
    }
    
    fun workouts(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренировки"
        AppLanguage.ENGLISH -> "Workouts"
    }
    
    fun profile(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Профиль"
        AppLanguage.ENGLISH -> "Profile"
    }
    
    // Settings
    fun appTheme(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тема приложения"
        AppLanguage.ENGLISH -> "App Theme"
    }
    
    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Язык"
        AppLanguage.ENGLISH -> "Language"
    }
    
    fun lightTheme(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "☀️ Светлая"
        AppLanguage.ENGLISH -> "☀️ Light"
    }
    
    fun darkTheme(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "🌙 Тёмная"
        AppLanguage.ENGLISH -> "🌙 Dark"
    }
    
    // Workouts
    fun individual(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Индивидуальные"
        AppLanguage.ENGLISH -> "Individual"
    }
    
    fun group(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Групповые"
        AppLanguage.ENGLISH -> "Group"
    }
    
    fun noTrainers(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренеры пока не добавлены"
        AppLanguage.ENGLISH -> "No trainers yet"
    }
    
    fun noGroupWorkouts(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Групповых тренировок пока нет"
        AppLanguage.ENGLISH -> "No group workouts yet"
    }
    
    fun followSchedule(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Следите за расписанием"
        AppLanguage.ENGLISH -> "Check the schedule"
    }
    
    // Admin panel
    fun usersManagement(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Управление пользователями"
        AppLanguage.ENGLISH -> "Users Management"
    }
    
    fun subscriptionsManagement(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Управление абонементами"
        AppLanguage.ENGLISH -> "Subscriptions Management"
    }
    
    fun groupWorkoutsManagement(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Групповые тренировки"
        AppLanguage.ENGLISH -> "Group Workouts"
    }
    
    fun users(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пользователи"
        AppLanguage.ENGLISH -> "Users"
    }
    
    fun newUser(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Новый пользователь"
        AppLanguage.ENGLISH -> "New User"
    }
    
    fun newSubscription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Новый абонемент"
        AppLanguage.ENGLISH -> "New Subscription"
    }
    
    fun newGroupWorkout(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Новая групповая тренировка"
        AppLanguage.ENGLISH -> "New Group Workout"
    }
    
    fun create(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Создать"
        AppLanguage.ENGLISH -> "Create"
    }
    
    fun save(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сохранить"
        AppLanguage.ENGLISH -> "Save"
    }
    
    fun cancel(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Отмена"
        AppLanguage.ENGLISH -> "Cancel"
    }
    
    fun delete(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить"
        AppLanguage.ENGLISH -> "Delete"
    }
    
    fun edit(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Редактировать"
        AppLanguage.ENGLISH -> "Edit"
    }
    
    fun back(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Назад"
        AppLanguage.ENGLISH -> "Back"
    }
    
    fun search(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск"
        AppLanguage.ENGLISH -> "Search"
    }
    
    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск по ФИО, email или дате рождения"
        AppLanguage.ENGLISH -> "Search by name, email or birth date"
    }
    
    fun statistics(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Статистика"
        AppLanguage.ENGLISH -> "Statistics"
    }
    
    fun clients(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Клиентов"
        AppLanguage.ENGLISH -> "Clients"
    }
    
    fun trainersCount(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренеров"
        AppLanguage.ENGLISH -> "Trainers"
    }
    
    fun admins(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Админов"
        AppLanguage.ENGLISH -> "Admins"
    }
    
    fun allUsers(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все пользователи"
        AppLanguage.ENGLISH -> "All users"
    }
    
    fun allSubscriptions(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все абонементы"
        AppLanguage.ENGLISH -> "All subscriptions"
    }
    
    fun allGroupWorkouts(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все групповые тренировки"
        AppLanguage.ENGLISH -> "All group workouts"
    }
    
    // Form fields
    fun email(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Email"
        AppLanguage.ENGLISH -> "Email"
    }
    
    fun password(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пароль"
        AppLanguage.ENGLISH -> "Password"
    }
    
    fun lastName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фамилия"
        AppLanguage.ENGLISH -> "Last Name"
    }
    
    fun firstName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Имя"
        AppLanguage.ENGLISH -> "First Name"
    }
    
    fun middleName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Отчество"
        AppLanguage.ENGLISH -> "Middle Name"
    }
    
    fun birthDate(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дата рождения"
        AppLanguage.ENGLISH -> "Birth Date"
    }
    
    fun gender(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пол"
        AppLanguage.ENGLISH -> "Gender"
    }
    
    fun male(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Мужской"
        AppLanguage.ENGLISH -> "Male"
    }
    
    fun female(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Женский"
        AppLanguage.ENGLISH -> "Female"
    }
    
    fun phone(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Телефон"
        AppLanguage.ENGLISH -> "Phone"
    }
    
    fun role(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Роль"
        AppLanguage.ENGLISH -> "Role"
    }
    
    fun client(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Клиент"
        AppLanguage.ENGLISH -> "Client"
    }
    
    fun trainer(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренер"
        AppLanguage.ENGLISH -> "Trainer"
    }
    
    fun admin(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Администратор"
        AppLanguage.ENGLISH -> "Administrator"
    }
    
    fun photoUrl(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "URL фото (необязательно)"
        AppLanguage.ENGLISH -> "Photo URL (optional)"
    }
    
    // Trainer fields
    fun experience(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Стаж (лет)"
        AppLanguage.ENGLISH -> "Experience (years)"
    }
    
    fun specialization(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Специализация"
        AppLanguage.ENGLISH -> "Specialization"
    }
    
    fun pricePerTraining(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Цена (₽)"
        AppLanguage.ENGLISH -> "Price (₽)"
    }
    
    fun trainerData(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Данные тренера"
        AppLanguage.ENGLISH -> "Trainer Data"
    }
    
    // Subscription fields
    fun name(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Название"
        AppLanguage.ENGLISH -> "Name"
    }
    
    fun description(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Описание"
        AppLanguage.ENGLISH -> "Description"
    }
    
    fun price(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Цена (₽)"
        AppLanguage.ENGLISH -> "Price (₽)"
    }
    
    fun durationDays(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дней"
        AppLanguage.ENGLISH -> "Days"
    }
    
    fun iconEmoji(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Эмодзи иконки"
        AppLanguage.ENGLISH -> "Icon Emoji"
    }
    
    fun features(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Возможности (через запятую)"
        AppLanguage.ENGLISH -> "Features (comma-separated)"
    }
    
    // Group workout fields
    fun workoutName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Название тренировки"
        AppLanguage.ENGLISH -> "Workout Name"
    }
    
    fun selectTrainer(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите тренера"
        AppLanguage.ENGLISH -> "Select Trainer"
    }
    
    fun dateAndTime(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дата и время"
        AppLanguage.ENGLISH -> "Date and Time"
    }
    
    fun duration(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Длительность (мин)"
        AppLanguage.ENGLISH -> "Duration (min)"
    }
    
    fun maxParticipants(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Макс. участников"
        AppLanguage.ENGLISH -> "Max Participants"
    }
    
    // Confirmation dialogs
    fun confirmDelete(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Подтверждение удаления"
        AppLanguage.ENGLISH -> "Confirm Deletion"
    }
    
    fun deleteUserConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Вы уверены, что хотите удалить пользователя?"
        AppLanguage.ENGLISH -> "Are you sure you want to delete this user?"
    }
    
    fun deleteSubscriptionConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Вы уверены, что хотите удалить абонемент"
        AppLanguage.ENGLISH -> "Are you sure you want to delete subscription"
    }
    
    fun deleteWorkoutConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Вы уверены, что хотите удалить тренировку?"
        AppLanguage.ENGLISH -> "Are you sure you want to delete this workout?"
    }
    
    fun actionCannotBeUndone(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "⚠️ Это действие нельзя отменить!"
        AppLanguage.ENGLISH -> "⚠️ This action cannot be undone!"
    }
    
    // Messages
    fun userCreated(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пользователь успешно создан!"
        AppLanguage.ENGLISH -> "User created successfully!"
    }
    
    fun userDeleted(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пользователь удалён"
        AppLanguage.ENGLISH -> "User deleted"
    }
    
    fun userUpdated(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Данные пользователя обновлены"
        AppLanguage.ENGLISH -> "User data updated"
    }
    
    fun subscriptionCreated(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Абонемент успешно создан!"
        AppLanguage.ENGLISH -> "Subscription created successfully!"
    }
    
    fun subscriptionUpdated(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Абонемент обновлён"
        AppLanguage.ENGLISH -> "Subscription updated"
    }
    
    fun subscriptionDeleted(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Абонемент удалён"
        AppLanguage.ENGLISH -> "Subscription deleted"
    }
    
    fun workoutCreated(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренировка успешно создана!"
        AppLanguage.ENGLISH -> "Workout created successfully!"
    }
    
    fun workoutDeleted(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренировка удалена"
        AppLanguage.ENGLISH -> "Workout deleted"
    }
    
    fun error(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ошибка"
        AppLanguage.ENGLISH -> "Error"
    }
    
    fun loadingError(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ошибка загрузки"
        AppLanguage.ENGLISH -> "Loading error"
    }
    
    fun nothingFound(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ничего не найдено"
        AppLanguage.ENGLISH -> "Nothing found"
    }
    
    fun tryDifferentSearch(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Попробуйте изменить поисковый запрос"
        AppLanguage.ENGLISH -> "Try a different search query"
    }
    
    // Role descriptions
    fun clientDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Посетитель спортивного клуба. Может просматривать и покупать абонементы, записываться на тренировки."
        AppLanguage.ENGLISH -> "Gym visitor. Can view and purchase subscriptions, sign up for workouts."
    }
    
    fun trainerDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тренер клуба. Проводит индивидуальные и групповые тренировки. Видит список своих клиентов."
        AppLanguage.ENGLISH -> "Club trainer. Conducts individual and group workouts. Can see client list."
    }
    
    fun adminDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Администратор системы. Полный доступ к управлению пользователями, абонементами и тренировками."
        AppLanguage.ENGLISH -> "System administrator. Full access to users, subscriptions and workouts management."
    }
    
    fun createNewUser(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Создание нового пользователя"
        AppLanguage.ENGLISH -> "Create New User"
    }
    
    fun selectRole(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите роль:"
        AppLanguage.ENGLISH -> "Select role:"
    }
    
    fun selectRoleDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите роль для нового пользователя. От выбранной роли зависят доступные функции и данные, которые нужно будет заполнить."
        AppLanguage.ENGLISH -> "Select a role for the new user. The role determines available functions and required data."
    }
    
    fun basicData(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Основные данные"
        AppLanguage.ENGLISH -> "Basic Data"
    }
    
    fun minSixChars(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Минимум 6 символов"
        AppLanguage.ENGLISH -> "At least 6 characters"
    }
    
    // Sort options
    fun sortAZ(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "А-Я"
        AppLanguage.ENGLISH -> "A-Z"
    }
    
    fun sortZA(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Я-А"
        AppLanguage.ENGLISH -> "Z-A"
    }
    
    fun sortDateAsc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дата ↑"
        AppLanguage.ENGLISH -> "Date ↑"
    }
    
    fun sortDateDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дата ↓"
        AppLanguage.ENGLISH -> "Date ↓"
    }
    
    // Fitness goals
    fun weightLoss(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Похудение"
        AppLanguage.ENGLISH -> "Weight Loss"
    }
    
    fun muscleGain(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Набор массы"
        AppLanguage.ENGLISH -> "Muscle Gain"
    }
    
    fun maintenance(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поддержание формы"
        AppLanguage.ENGLISH -> "Maintenance"
    }
    
    // Trainer specializations
    fun fitness(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фитнес"
        AppLanguage.ENGLISH -> "Fitness"
    }
    
    fun bodybuilding(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Бодибилдинг"
        AppLanguage.ENGLISH -> "Bodybuilding"
    }
    
    fun crossfit(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Кроссфит"
        AppLanguage.ENGLISH -> "CrossFit"
    }
    
    fun yoga(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Йога"
        AppLanguage.ENGLISH -> "Yoga"
    }
    
    fun pilates(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пилатес"
        AppLanguage.ENGLISH -> "Pilates"
    }
    
    fun boxing(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Бокс"
        AppLanguage.ENGLISH -> "Boxing"
    }
    
    fun swimming(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Плавание"
        AppLanguage.ENGLISH -> "Swimming"
    }
    
    fun cardio(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Кардио"
        AppLanguage.ENGLISH -> "Cardio"
    }
    
    // Additional common strings
    fun loading(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Загрузка..."
        AppLanguage.ENGLISH -> "Loading..."
    }
    
    fun noName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Без имени"
        AppLanguage.ENGLISH -> "No name"
    }
    
    fun notSpecified(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не указано"
        AppLanguage.ENGLISH -> "Not specified"
    }
    
    fun editUser(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Редактировать пользователя"
        AppLanguage.ENGLISH -> "Edit User"
    }
    
    fun editSubscription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Редактировать абонемент"
        AppLanguage.ENGLISH -> "Edit Subscription"
    }
    
    fun age(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Возраст"
        AppLanguage.ENGLISH -> "Age"
    }
    
    fun weight(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Вес"
        AppLanguage.ENGLISH -> "Weight"
    }
    
    fun height(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Рост"
        AppLanguage.ENGLISH -> "Height"
    }
    
    fun bmi(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "ИМТ"
        AppLanguage.ENGLISH -> "BMI"
    }
    
    fun fitnessGoal(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Цель занятий"
        AppLanguage.ENGLISH -> "Fitness Goal"
    }
    
    fun mySubscriptions(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Мои абонементы"
        AppLanguage.ENGLISH -> "My Subscriptions"
    }
    
    fun availableSubscriptions(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Доступные абонементы"
        AppLanguage.ENGLISH -> "Available Subscriptions"
    }
    
    fun buy(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Купить"
        AppLanguage.ENGLISH -> "Buy"
    }
    
    fun extend(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Продлить"
        AppLanguage.ENGLISH -> "Extend"
    }
    
    fun daysLeft(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Осталось дней"
        AppLanguage.ENGLISH -> "Days left"
    }
    
    fun expired(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Истёк"
        AppLanguage.ENGLISH -> "Expired"
    }
    
    fun appName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "HypeSportClub"
        AppLanguage.ENGLISH -> "HypeSportClub"
    }
    
    fun participants(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "участников"
        AppLanguage.ENGLISH -> "participants"
    }
    
    fun minutes(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "мин"
        AppLanguage.ENGLISH -> "min"
    }
    
    fun years(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "лет"
        AppLanguage.ENGLISH -> "years"
    }
    
    fun rubles(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "₽"
        AppLanguage.ENGLISH -> "₽"
    }
    
    fun perTraining(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "/тренировка"
        AppLanguage.ENGLISH -> "/training"
    }
}

