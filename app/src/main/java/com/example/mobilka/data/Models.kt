package com.example.mobilka.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.time.LocalDate
import java.time.ZoneId

// Роли пользователей
enum class UserRole(val displayName: String) {
    CLIENT("Клиент"),
    TRAINER("Тренер"),
    ADMIN("Администратор");
    
    companion object {
        fun fromString(value: String): UserRole {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CLIENT
        }
    }
}

// Цели тренировок
enum class FitnessGoal(val displayName: String) {
    WEIGHT_LOSS("Похудение"),
    MUSCLE_GAIN("Набор массы"),
    MAINTENANCE("Поддержание формы");
    
    companion object {
        fun fromString(value: String): FitnessGoal {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MAINTENANCE
        }
    }
}

// Пол пользователя
enum class Gender(val displayName: String) {
    MALE("Мужской"),
    FEMALE("Женский");
    
    companion object {
        fun fromString(value: String): Gender {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MALE
        }
    }
}

// Модель пользователя для Firestore
data class User(
    val id: String = "",
    val email: String = "",
    val phone: String = "",
    val lastName: String = "",      // Фамилия
    val firstName: String = "",     // Имя
    val middleName: String = "",    // Отчество
    val birthDate: String = "",     // Дата рождения (формат: dd.MM.yyyy)
    val role: String = UserRole.CLIENT.name,
    val createdAt: Timestamp = Timestamp.now(),
    // Данные для БЖУ калькулятора
    val gender: String = Gender.MALE.name,
    val weight: Float = 0f,         // Вес в кг
    val height: Float = 0f,         // Рост в см
    val fitnessGoal: String = FitnessGoal.MAINTENANCE.name
) {
    // Пустой конструктор для Firestore
    constructor() : this("", "", "", "", "", "", "", UserRole.CLIENT.name, Timestamp.now(), Gender.MALE.name, 0f, 0f, FitnessGoal.MAINTENANCE.name)
    
    val userRole: UserRole
        get() = UserRole.fromString(role)
    
    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN
    
    val isTrainer: Boolean
        get() = userRole == UserRole.TRAINER
    
    val isClient: Boolean
        get() = userRole == UserRole.CLIENT
    
    // Полное имя (ФИО)
    val fullName: String
        get() = listOf(lastName, firstName, middleName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Без имени" }
    
    // Пол пользователя
    val userGender: Gender
        get() = Gender.fromString(gender)
    
    // Цель тренировок
    val userFitnessGoal: FitnessGoal
        get() = FitnessGoal.fromString(fitnessGoal)
    
    // Возраст на основе даты рождения (полных лет)
    val age: Int
        get() {
            if (birthDate.isBlank()) return 0
            return try {
                val parts = birthDate.split(".")
                if (parts.size != 3) return 0
                val birthDay = parts[0].toIntOrNull() ?: return 0
                val birthMonth = parts[1].toIntOrNull() ?: return 0
                val birthYear = parts[2].toIntOrNull() ?: return 0
                
                val today = java.util.Calendar.getInstance()
                val currentYear = today.get(java.util.Calendar.YEAR)
                val currentMonth = today.get(java.util.Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                val currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)
                
                var calculatedAge = currentYear - birthYear
                
                // Если день рождения ещё не наступил в этом году, уменьшаем возраст на 1
                if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                    calculatedAge--
                }
                
                if (calculatedAge < 0) 0 else calculatedAge
            } catch (e: Exception) {
                0
            }
        }
    
    // ИМТ (Индекс массы тела)
    val bmi: Float
        get() {
            if (weight <= 0 || height <= 0) return 0f
            val heightInMeters = height / 100
            return weight / (heightInMeters * heightInMeters)
        }
    
    // Категория ИМТ
    val bmiCategory: String
        get() = when {
            bmi <= 0 -> "Не указано"
            bmi < 18.5f -> "Недостаточный вес"
            bmi < 25f -> "Норма"
            bmi < 30f -> "Избыточный вес"
            else -> "Ожирение"
        }
}

// Модель абонемента (шаблон) для Firestore
data class Subscription(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val durationDays: Int = 0,
    val features: List<String> = emptyList(),
    val iconEmoji: String = "🏋️",
    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean = true
) {
    constructor() : this("", "", "", 0, 0, emptyList(), "🏋️", true)
}

// Модель купленного абонемента пользователя
data class UserSubscription(
    @DocumentId
    val id: String = "",
    val userId: String = "",  // ID пользователя
    val orderId: String = "",
    val subscriptionId: String = "",
    val subscriptionName: String = "",
    val subscriptionDescription: String = "",
    val subscriptionIconEmoji: String = "🏋️",
    val subscriptionFeatures: List<String> = emptyList(),
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean = true
) {
    constructor() : this("", "", "", "", "", "", "🏋️", emptyList(), Timestamp.now(), Timestamp.now(), true)
    
    val startLocalDate: LocalDate
        get() = startDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    
    val endLocalDate: LocalDate
        get() = endDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    
    val remainingDays: Long
        get() {
            val now = LocalDate.now()
            val end = endLocalDate
            return java.time.temporal.ChronoUnit.DAYS.between(now, end).coerceAtLeast(0)
        }
    
    val isExpired: Boolean
        get() = remainingDays <= 0
}

// Демо-данные для начальной загрузки в Firestore
object SubscriptionTemplates {
    val defaultSubscriptions = listOf(
        Subscription(
            id = "basic",
            name = "Базовый",
            description = "Доступ в тренажёрный зал",
            price = 2500,
            durationDays = 30,
            features = listOf(
                "Тренажёрный зал",
                "Раздевалки с душем",
                "Шкафчик для вещей"
            ),
            iconEmoji = "💪",
            active = true
        ),
        Subscription(
            id = "standard",
            name = "Стандарт",
            description = "Тренажёрный зал + групповые занятия",
            price = 4000,
            durationDays = 30,
            features = listOf(
                "Тренажёрный зал",
                "Групповые программы",
                "Бассейн",
                "Сауна",
                "Раздевалки с душем"
            ),
            iconEmoji = "🏊",
            active = true
        ),
        Subscription(
            id = "premium",
            name = "Премиум",
            description = "Полный доступ ко всем услугам",
            price = 7000,
            durationDays = 30,
            features = listOf(
                "Тренажёрный зал 24/7",
                "Все групповые программы",
                "Бассейн и СПА",
                "Персональный тренер (2 занятия)",
                "Полотенца и напитки",
                "Парковка"
            ),
            iconEmoji = "⭐",
            active = true
        ),
        Subscription(
            id = "vip_yearly",
            name = "Годовой VIP",
            description = "Максимум возможностей на год",
            price = 60000,
            durationDays = 365,
            features = listOf(
                "Всё из Премиум",
                "Персональный тренер (8 занятий/мес)",
                "Заморозка до 30 дней",
                "Гостевые визиты",
                "Приоритетная запись",
                "Скидки на доп. услуги 20%"
            ),
            iconEmoji = "👑",
            active = true
        )
    )
}

// Специализации тренеров
enum class TrainerSpecialization(val displayName: String) {
    FITNESS("Фитнес"),
    BODYBUILDING("Бодибилдинг"),
    CROSSFIT("Кроссфит"),
    YOGA("Йога"),
    PILATES("Пилатес"),
    BOXING("Бокс"),
    SWIMMING("Плавание"),
    CARDIO("Кардио");
    
    companion object {
        fun fromString(value: String): TrainerSpecialization {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: FITNESS
        }
    }
}

// Модель тренера для Firestore
data class Trainer(
    @DocumentId
    val id: String = "",
    val userId: String = "",      // ID пользователя-тренера (связь с User)
    val lastName: String = "",      // Фамилия
    val firstName: String = "",     // Имя  
    val middleName: String = "",    // Отчество
    val birthDate: String = "",     // Дата рождения
    val email: String = "",
    val phone: String = "",
    val experience: Int = 0,        // Стаж в годах
    val specialization: String = TrainerSpecialization.FITNESS.name, // Основная специализация (для обратной совместимости)
    val specializations: List<String> = emptyList(), // Список всех специализаций
    val achievements: List<String> = emptyList(),
    val pricePerTraining: Int = 0,  // Цена за тренировку
    val photoUrl: String = "",      // URL фото
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", "", "", "", "", 0, TrainerSpecialization.FITNESS.name, emptyList(), emptyList(), 0, "", Timestamp.now())
    
    val fullName: String
        get() = listOf(lastName, firstName, middleName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Без имени" }
    
    val trainerSpecialization: TrainerSpecialization
        get() = TrainerSpecialization.fromString(specialization)
    
    // Получить все специализации (учитывая обратную совместимость)
    val allSpecializations: List<TrainerSpecialization>
        get() {
            val specs = if (specializations.isNotEmpty()) {
                specializations.mapNotNull { 
                    try { TrainerSpecialization.fromString(it) } catch (e: Exception) { null }
                }
            } else {
                listOf(trainerSpecialization)
            }
            return specs.ifEmpty { listOf(TrainerSpecialization.FITNESS) }
        }
    
    // Текст для отображения специализаций
    val specializationsText: String
        get() = allSpecializations.joinToString(", ") { it.displayName }
    
    val experienceText: String
        get() {
            val lastDigit = experience % 10
            val lastTwoDigits = experience % 100
            val word = when {
                lastTwoDigits in 11..14 -> "лет"
                lastDigit == 1 -> "год"
                lastDigit in 2..4 -> "года"
                else -> "лет"
            }
            return "$experience $word"
        }
}

// Модель групповой тренировки (также используется для индивидуальных)
data class GroupWorkout(
    @DocumentId
    val id: String = "",
    val name: String = "",          // Название тренировки
    val description: String = "",
    val trainerId: String = "",     // ID тренера
    val trainerName: String = "",   // ФИО тренера (для отображения)
    val clientId: String = "",      // ID клиента (для индивидуальных тренировок)
    val clientName: String = "",    // ФИО клиента (для индивидуальных тренировок)
    val dateTime: Timestamp = Timestamp.now(),
    val durationMinutes: Int = 60,
    val maxParticipants: Int = 20,
    val currentParticipants: Int = 0,
    val participantIds: List<String> = emptyList(), // Список ID участников групповой тренировки
    val isIndividual: Boolean = false, // Флаг индивидуальной тренировки
    val availabilitySlotId: String = "", // ID слота расписания (для индивидуальных тренировок)
    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean = true
) {
    constructor() : this("", "", "", "", "", "", "", Timestamp.now(), 60, 20, 0, emptyList(), false, "", true)
    
    val formattedDateTime: String
        get() {
            val date = dateTime.toDate()
            val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            return format.format(date)
        }
    
    val isFull: Boolean
        get() = currentParticipants >= maxParticipants
    
    // Вычисляемое свойство: определяет индивидуальную тренировку по флагу или по признакам
    val isIndividualWorkout: Boolean
        get() = isIndividual || (maxParticipants == 1 && clientName.isNotBlank())
    
    // Проверка, записан ли пользователь на тренировку
    fun isUserSignedUp(userId: String): Boolean = participantIds.contains(userId)
}

// Модель расписания тренера для индивидуальных тренировок
data class TrainerAvailability(
    @DocumentId
    val id: String = "",
    val trainerId: String = "",        // ID тренера (из коллекции users)
    val trainerName: String = "",      // ФИО тренера
    val date: Timestamp = Timestamp.now(), // Дата доступности
    val startTime: String = "",        // Время начала (формат "HH:mm")
    val endTime: String = "",          // Время окончания (формат "HH:mm")
    val isAvailable: Boolean = true,   // Доступен ли в это время
    val notes: String = ""             // Примечания
) {
    constructor() : this("", "", "", Timestamp.now(), "", "", true, "")
    
    val formattedDate: String
        get() {
            val date = date.toDate()
            val format = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            return format.format(date)
        }
    
    val timeRange: String
        get() = "$startTime - $endTime"
}

// Модель сообщения в чате
data class ChatMessage(
    @DocumentId
    val id: String = "",
    val chatId: String = "",         // ID чата (trainerId_clientId)
    val senderId: String = "",       // ID отправителя
    val senderName: String = "",     // ФИО отправителя
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
) {
    constructor() : this("", "", "", "", "", Timestamp.now(), false)
    
    val formattedTime: String
        get() {
            val date = timestamp.toDate()
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return format.format(date)
        }
    
    val formattedDate: String
        get() {
            val date = timestamp.toDate()
            val format = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            return format.format(date)
        }
}

// ==================== БЖУ КАЛЬКУЛЯТОР ====================

// Продукт из встроенной базы (JSON)
data class FoodProduct(
    val name: String = "",
    val calories: Float = 0f,
    val proteins: Float = 0f,
    val fats: Float = 0f,
    val carbs: Float = 0f,
    val category: String = ""
)

// Запись о съеденном продукте (Firestore)
data class FoodEntry(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val weightGrams: Float = 0f,
    val calories: Float = 0f,
    val proteins: Float = 0f,
    val fats: Float = 0f,
    val carbs: Float = 0f,
    val date: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", 0f, 0f, 0f, 0f, 0f, "", Timestamp.now())
}

// Дневная норма КБЖУ
data class DailyNorm(
    val calories: Float,
    val proteins: Float,
    val fats: Float,
    val carbs: Float
)

object NutritionCalculator {
    /**
     * Формула Миффлина-Сан Жеора с коэф. активности 1.55 (умеренная).
     * Макронутриенты: белки 30%, жиры 25%, углеводы 45%.
     */
    fun calculateDailyNorm(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        goal: FitnessGoal
    ): DailyNorm {
        if (weightKg <= 0f || heightCm <= 0f || age <= 0) {
            return DailyNorm(2000f, 150f, 56f, 225f)
        }
        val bmr = when (gender) {
            Gender.MALE -> 10f * weightKg + 6.25f * heightCm - 5f * age + 5f
            Gender.FEMALE -> 10f * weightKg + 6.25f * heightCm - 5f * age - 161f
        }
        val tdee = bmr * 1.55f
        val adjustedCalories = when (goal) {
            FitnessGoal.WEIGHT_LOSS -> tdee * 0.80f
            FitnessGoal.MUSCLE_GAIN -> tdee * 1.15f
            FitnessGoal.MAINTENANCE -> tdee
        }
        return DailyNorm(
            calories = adjustedCalories,
            proteins = adjustedCalories * 0.30f / 4f,
            fats = adjustedCalories * 0.25f / 9f,
            carbs = adjustedCalories * 0.45f / 4f
        )
    }
}
