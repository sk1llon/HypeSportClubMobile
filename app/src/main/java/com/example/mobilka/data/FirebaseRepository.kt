package com.example.mobilka.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Коллекции
    private val usersCollection = firestore.collection("users")
    private val subscriptionsCollection = firestore.collection("subscriptions")
    private val userSubscriptionsCollection = firestore.collection("user_subscriptions")
    private val trainersCollection = firestore.collection("trainers")
    private val groupWorkoutsCollection = firestore.collection("group_workouts")
    private val trainerAvailabilityCollection = firestore.collection("trainer_availability")
    
    // Текущий пользователь Firebase
    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser
    
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // ==================== АУТЕНТИФИКАЦИЯ ====================
    
    suspend fun register(
        email: String, 
        password: String,
        lastName: String = "",
        firstName: String = "",
        middleName: String = "",
        birthDate: String = "",
        gender: String = Gender.MALE.name,
        height: Float = 0f,
        weight: Float = 0f,
        fitnessGoal: String = FitnessGoal.MAINTENANCE.name
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Не удалось создать пользователя")
            
            // Создаём документ пользователя в Firestore (всегда как CLIENT)
            // Не сохраняем id, так как @DocumentId автоматически заполняется из document ID
            val userData = hashMapOf(
                "email" to email,
                "phone" to "",
                "lastName" to lastName,
                "firstName" to firstName,
                "middleName" to middleName,
                "birthDate" to birthDate,
                "role" to UserRole.CLIENT.name,
                "gender" to gender,
                "height" to height,
                "weight" to weight,
                "fitnessGoal" to fitnessGoal,
                "createdAt" to Timestamp.now()
            )
            usersCollection.document(user.uid).set(userData).await()
            
            // Инициализируем абонементы если их нет
            initializeSubscriptionsIfNeeded()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }
    
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Не удалось войти")
            
            // Инициализируем абонементы при входе
            initializeSubscriptionsIfNeeded()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }
    
    fun logout() {
        auth.signOut()
    }
    
    // ==================== ПОЛЬЗОВАТЕЛЬ ====================
    
    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = usersCollection.document(uid).get().await()
            val user = doc.toObject(User::class.java)
            // Устанавливаем id из document ID, если его нет в данных
            user?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }
    
    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                // Устанавливаем id из document ID, если его нет в данных
                trySend(user?.copy(id = snapshot?.id ?: ""))
            }
        awaitClose { listener.remove() }
    }
    
    // Обновление данных пользователя (для администратора)
    suspend fun updateUserData(
        userId: String,
        email: String,
        phone: String,
        lastName: String,
        firstName: String,
        middleName: String,
        birthDate: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf(
                "email" to email,
                "phone" to phone,
                "lastName" to lastName,
                "firstName" to firstName,
                "middleName" to middleName,
                "birthDate" to birthDate
            )
            usersCollection.document(userId).update(updates as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Обновление базовых данных пользователя (ФИО, дата рождения, пол) - для админа
    suspend fun updateUserBasicData(
        userId: String,
        lastName: String,
        firstName: String,
        middleName: String,
        birthDate: String,
        gender: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf(
                "lastName" to lastName,
                "firstName" to firstName,
                "middleName" to middleName,
                "birthDate" to birthDate,
                "gender" to gender
            )
            usersCollection.document(userId).update(updates as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Удаление пользователя
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // Удаляем документ пользователя из Firestore
            usersCollection.document(userId).delete().await()
            
            // Также удаляем подписки пользователя
            val userSubs = userSubscriptionsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            for (doc in userSubs.documents) {
                doc.reference.delete().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Обновление контактных данных пользователя (телефон, email)
    suspend fun updateUserContactInfo(
        phone: String,
        email: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            val updates = hashMapOf(
                "phone" to phone,
                "email" to email
            )
            usersCollection.document(uid).update(updates as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Обновление данных для БЖУ калькулятора
    suspend fun updateUserHealthData(
        gender: String,
        weight: Float,
        height: Float,
        fitnessGoal: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            val updates = hashMapOf(
                "gender" to gender,
                "weight" to weight,
                "height" to height,
                "fitnessGoal" to fitnessGoal
            )
            usersCollection.document(uid).update(updates as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Получить всех тренеров (из таблицы users с ролью TRAINER)
    suspend fun getAllTrainersUsers(): List<User> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", UserRole.TRAINER.name)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                user?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Получить всех тренеров (из таблицы trainers)
    suspend fun getAllTrainers(): List<Trainer> {
        return try {
            val snapshot = trainersCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Trainer::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Добавить тренера
    suspend fun addTrainer(trainer: Trainer): Result<String> {
        return try {
            val data = hashMapOf(
                "userId" to trainer.userId,
                "lastName" to trainer.lastName,
                "firstName" to trainer.firstName,
                "middleName" to trainer.middleName,
                "birthDate" to trainer.birthDate,
                "email" to trainer.email,
                "phone" to trainer.phone,
                "experience" to trainer.experience,
                "specialization" to trainer.specialization,
                "achievements" to trainer.achievements,
                "pricePerTraining" to trainer.pricePerTraining,
                "photoUrl" to trainer.photoUrl,
                "createdAt" to Timestamp.now()
            )
            val docRef = trainersCollection.add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Получить все групповые тренировки
    suspend fun getAllGroupWorkouts(): List<GroupWorkout> {
        return try {
            // Простой запрос без составного индекса
            val snapshot = groupWorkoutsCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(GroupWorkout::class.java)?.copy(id = doc.id)
            }.sortedBy { it.dateTime } // Сортировка в памяти
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Добавить групповую тренировку
    suspend fun addGroupWorkout(workout: GroupWorkout): Result<String> {
        return try {
            val data = hashMapOf(
                "name" to workout.name,
                "description" to workout.description,
                "trainerId" to workout.trainerId,
                "trainerName" to workout.trainerName,
                "clientId" to workout.clientId,
                "clientName" to workout.clientName,
                "dateTime" to workout.dateTime,
                "durationMinutes" to workout.durationMinutes,
                "maxParticipants" to workout.maxParticipants,
                "currentParticipants" to workout.currentParticipants,
                "participantIds" to workout.participantIds,
                "isIndividual" to workout.isIndividual,
                "active" to workout.active
            )
            val docRef = groupWorkoutsCollection.add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Удалить групповую тренировку
    suspend fun deleteGroupWorkout(workoutId: String): Result<Unit> {
        return try {
            groupWorkoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Обновить групповую тренировку
    suspend fun updateGroupWorkout(workout: GroupWorkout): Result<Unit> {
        return try {
            val data = hashMapOf(
                "name" to workout.name,
                "description" to workout.description,
                "trainerId" to workout.trainerId,
                "trainerName" to workout.trainerName,
                "clientId" to workout.clientId,
                "clientName" to workout.clientName,
                "dateTime" to workout.dateTime,
                "durationMinutes" to workout.durationMinutes,
                "maxParticipants" to workout.maxParticipants,
                "currentParticipants" to workout.currentParticipants,
                "participantIds" to workout.participantIds,
                "isIndividual" to workout.isIndividual,
                "active" to workout.active
            )
            groupWorkoutsCollection.document(workout.id).update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Записаться на групповую тренировку
    suspend fun signUpForWorkout(workoutId: String, userId: String): Result<Unit> {
        return try {
            // Получаем текущие данные тренировки
            val workoutDoc = groupWorkoutsCollection.document(workoutId).get().await()
            val workout = workoutDoc.toObject(GroupWorkout::class.java)
                ?: return Result.failure(Exception("Тренировка не найдена"))
            
            // Проверяем, не записан ли уже пользователь
            if (workout.participantIds.contains(userId)) {
                return Result.failure(Exception("Вы уже записаны на эту тренировку"))
            }
            
            // Проверяем, есть ли свободные места
            if (workout.isFull) {
                return Result.failure(Exception("На тренировке нет свободных мест"))
            }
            
            // Добавляем пользователя в список участников
            val updatedParticipants = workout.participantIds + userId
            val updatedCount = workout.currentParticipants + 1
            
            groupWorkoutsCollection.document(workoutId).update(
                mapOf(
                    "participantIds" to updatedParticipants,
                    "currentParticipants" to updatedCount
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Отменить запись на групповую тренировку
    suspend fun cancelWorkoutSignUp(workoutId: String, userId: String): Result<Unit> {
        return try {
            // Получаем текущие данные тренировки
            val workoutDoc = groupWorkoutsCollection.document(workoutId).get().await()
            val workout = workoutDoc.toObject(GroupWorkout::class.java)
                ?: return Result.failure(Exception("Тренировка не найдена"))
            
            // Проверяем, записан ли пользователь
            if (!workout.participantIds.contains(userId)) {
                return Result.failure(Exception("Вы не записаны на эту тренировку"))
            }
            
            // Удаляем пользователя из списка участников
            val updatedParticipants = workout.participantIds - userId
            val updatedCount = (workout.currentParticipants - 1).coerceAtLeast(0)
            
            groupWorkoutsCollection.document(workoutId).update(
                mapOf(
                    "participantIds" to updatedParticipants,
                    "currentParticipants" to updatedCount
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== РАСПИСАНИЕ ТРЕНЕРА ====================
    
    // Получить расписание тренера
    suspend fun getTrainerAvailability(trainerId: String): List<TrainerAvailability> {
        return try {
            val snapshot = trainerAvailabilityCollection
                .whereEqualTo("trainerId", trainerId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(TrainerAvailability::class.java) }
                .sortedBy { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Получить расписание тренера на определённую дату
    suspend fun getTrainerAvailabilityByDate(trainerId: String, date: Timestamp): List<TrainerAvailability> {
        return try {
            // Создаём диапазон для дня
            val calendar = Calendar.getInstance().apply {
                time = date.toDate()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = Timestamp(calendar.time)
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = Timestamp(calendar.time)
            
            val snapshot = trainerAvailabilityCollection
                .whereEqualTo("trainerId", trainerId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(TrainerAvailability::class.java) }
                .sortedBy { it.startTime }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Добавить слот расписания
    suspend fun addTrainerAvailability(availability: TrainerAvailability): Result<String> {
        return try {
            val data = hashMapOf(
                "trainerId" to availability.trainerId,
                "trainerName" to availability.trainerName,
                "date" to availability.date,
                "startTime" to availability.startTime,
                "endTime" to availability.endTime,
                "isAvailable" to availability.isAvailable
            )
            val docRef = trainerAvailabilityCollection.add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Обновить слот расписания
    suspend fun updateTrainerAvailability(availability: TrainerAvailability): Result<Unit> {
        return try {
            val data = hashMapOf(
                "trainerId" to availability.trainerId,
                "trainerName" to availability.trainerName,
                "date" to availability.date,
                "startTime" to availability.startTime,
                "endTime" to availability.endTime,
                "isAvailable" to availability.isAvailable
            )
            trainerAvailabilityCollection.document(availability.id).update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Удалить слот расписания
    suspend fun deleteTrainerAvailability(availabilityId: String): Result<Unit> {
        return try {
            trainerAvailabilityCollection.document(availabilityId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== ЧАТЫ ====================
    
    private val chatMessagesCollection = firestore.collection("chats")
    
    // Отправить сообщение
    suspend fun sendMessage(chatId: String, senderId: String, senderName: String, text: String): Result<String> {
        return try {
            val data = hashMapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "senderName" to senderName,
                "text" to text,
                "timestamp" to Timestamp.now(),
                "isRead" to false
            )
            val docRef = chatMessagesCollection.add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Получить сообщения чата
    suspend fun getChatMessages(chatId: String): List<ChatMessage> {
        return try {
            val snapshot = chatMessagesCollection
                .whereEqualTo("chatId", chatId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Слушать сообщения чата в реальном времени
    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatMessagesCollection
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    // Явно читаем isRead из документа, т.к. Kotlin val не всегда корректно
                    // десериализуется через toObject() при обновлении поля
                    val base = doc.toObject(ChatMessage::class.java) ?: return@mapNotNull null
                    base.copy(
                        id = doc.id,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                }?.sortedBy { it.timestamp } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Пометить сообщения чата как прочитанные
    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        try {
            val snapshot = chatMessagesCollection
                .whereEqualTo("chatId", chatId)
                .get()
                .await()
            for (doc in snapshot.documents) {
                val isAlreadyRead = doc.getBoolean("isRead") ?: false
                val senderId = doc.getString("senderId") ?: ""
                if (!isAlreadyRead && senderId != userId) {
                    doc.reference.update("isRead", true).await()
                }
            }
        } catch (_: Exception) { }
    }

    // Количество непрочитанных входящих сообщений
    suspend fun getUnreadMessageCount(chatId: String, userId: String): Int {
        return try {
            val snapshot = chatMessagesCollection
                .whereEqualTo("chatId", chatId)
                .get()
                .await()
            snapshot.documents.count { doc ->
                val isRead = doc.getBoolean("isRead") ?: false
                val senderId = doc.getString("senderId") ?: ""
                !isRead && senderId != userId
            }
        } catch (e: Exception) {
            0
        }
    }
    
    // Обновить профиль тренера (редактируемые поля)
    suspend fun updateTrainerProfile(
        userId: String,
        email: String,
        phone: String,
        birthDate: String,
        height: Float,
        weight: Float
    ): Result<Unit> {
        return try {
            val updates = hashMapOf(
                "email" to email,
                "phone" to phone,
                "birthDate" to birthDate,
                "height" to height,
                "weight" to weight
            )
            usersCollection.document(userId).update(updates as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Изменить пароль (требует ре-аутентификации)
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Не авторизован"))
            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Получить доступные слоты расписания тренера (только будущие и незанятые)
    suspend fun getAvailableTrainerSlots(trainerId: String): List<TrainerAvailability> {
        return try {
            val now = Calendar.getInstance().time
            val snapshot = trainerAvailabilityCollection
                .whereEqualTo("trainerId", trainerId)
                .get()
                .await()
            snapshot.documents
                .mapNotNull { it.toObject(TrainerAvailability::class.java)?.copy(id = it.id) }
                .filter { slot ->
                    slot.startTime.isNotBlank() &&
                    slot.endTime.isNotBlank() &&
                    slot.date.toDate().after(now)
                }
                .sortedWith(compareBy({ it.date }, { it.startTime }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBookedIndividualHours(trainerId: String): Set<String> {
        return try {
            val snapshot = groupWorkoutsCollection
                .whereEqualTo("trainerId", trainerId)
                .whereEqualTo("isIndividual", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val ts = doc.getTimestamp("dateTime") ?: return@mapNotNull null
                val cal = Calendar.getInstance().apply { time = ts.toDate() }
                "%04d-%02d-%02d-%02d".format(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY)
                )
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
    
    // Записаться на индивидуальную тренировку: создать тренировку + занять слот
    suspend fun bookIndividualTrainerSlot(
        slot: TrainerAvailability,
        currentUser: User,
        selectedHour: Int = -1  // -1 = использовать startTime слота
    ): Result<String> {
        return try {
            // Определяем час начала: выбранный или из startTime слота
            val startHour = if (selectedHour >= 0) selectedHour
            else slot.startTime.split(":").firstOrNull()?.toIntOrNull() ?: 0

            val startTimeStr = "%02d:00".format(startHour)
            val endTimeStr = "%02d:00".format(startHour + 1)

            // Составляем дату и время тренировки
            val cal = Calendar.getInstance().apply {
                time = slot.date.toDate()
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val workoutTimestamp = Timestamp(cal.time)
            val durationMinutes = 60
            
            // Создаём индивидуальную тренировку
            val workoutData = hashMapOf(
                "name" to "Индивидуальная тренировка",
                "description" to "",
                "trainerId" to slot.trainerId,
                "trainerName" to slot.trainerName,
                "clientId" to currentUser.id,
                "clientName" to currentUser.fullName,
                "dateTime" to workoutTimestamp,
                "durationMinutes" to durationMinutes,
                "maxParticipants" to 1,
                "currentParticipants" to 1,
                "participantIds" to listOf(currentUser.id),
                "isIndividual" to true,
                "availabilitySlotId" to slot.id,
                "active" to true
            )
            val docRef = groupWorkoutsCollection.add(workoutData).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Отменить индивидуальную тренировку: удалить запись и восстановить слот тренера
    suspend fun cancelIndividualWorkout(workoutId: String): Result<Unit> {
        return try {
            groupWorkoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Обновить пол пользователя (перенос в личные данные)
    suspend fun updateUserGender(gender: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            usersCollection.document(uid).update("gender", gender).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Регистрация пользователя администратором (без выхода из аккаунта админа)
    suspend fun registerUserByAdmin(
        context: Context,
        email: String,
        password: String,
        lastName: String,
        firstName: String,
        middleName: String,
        birthDate: String,
        role: UserRole
    ): Result<String> {
        return try {
            // Создаём вторичный экземпляр Firebase для регистрации без влияния на текущую сессию
            val secondaryAppName = "secondaryApp"
            
            // Удаляем старый экземпляр если существует
            try {
                FirebaseApp.getInstance(secondaryAppName).delete()
            } catch (e: Exception) {
                // Игнорируем - приложение не существует
            }
            
            // Получаем настройки из основного приложения
            val defaultApp = FirebaseApp.getInstance()
            val options = FirebaseOptions.Builder()
                .setProjectId(defaultApp.options.projectId)
                .setApplicationId(defaultApp.options.applicationId)
                .setApiKey(defaultApp.options.apiKey ?: "")
                .build()
            
            // Создаём вторичный экземпляр Firebase
            val secondaryApp = FirebaseApp.initializeApp(context, options, secondaryAppName)
                ?: throw Exception("Не удалось создать вторичное приложение Firebase")
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            
            try {
                // Создаём пользователя через вторичный Auth (не влияет на текущую сессию)
                val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
                val newUser = result.user ?: throw Exception("Не удалось создать пользователя")
                val newUserId = newUser.uid
            
                // Создаём документ пользователя в Firestore
            val userData = hashMapOf(
                "email" to email,
                "phone" to "",
                "lastName" to lastName,
                "firstName" to firstName,
                "middleName" to middleName,
                "birthDate" to birthDate,
                "role" to role.name,
                "createdAt" to Timestamp.now()
            )
                usersCollection.document(newUserId).set(userData).await()
            
                // Выходим из вторичного Auth и удаляем приложение
                secondaryAuth.signOut()
                secondaryApp.delete()
                
                Result.success(newUserId)
            } catch (e: Exception) {
                // Очищаем вторичное приложение в случае ошибки
                try {
                    secondaryAuth.signOut()
                    secondaryApp.delete()
                } catch (cleanupError: Exception) {
                    // Игнорируем ошибки очистки
                }
                throw e
            }
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }
    
    suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> {
        return try {
            usersCollection.document(userId).update("role", role.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                user?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    user?.copy(id = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }
    
    // ==================== АБОНЕМЕНТЫ (ШАБЛОНЫ) ====================
    
    private suspend fun initializeSubscriptionsIfNeeded() {
        try {
            val snapshot = subscriptionsCollection.get().await()
            val existingIds = snapshot.documents.map { it.id }.toSet()
            
            // Загружаем дефолтные абонементы, если их нет или если какой-то был удалён
            SubscriptionTemplates.defaultSubscriptions.forEach { subscription ->
                if (!existingIds.contains(subscription.id)) {
                    // Создаём отсутствующий абонемент
                    val data = hashMapOf(
                        "name" to subscription.name,
                        "description" to subscription.description,
                        "price" to subscription.price,
                        "durationDays" to subscription.durationDays,
                        "features" to subscription.features,
                        "iconEmoji" to subscription.iconEmoji,
                        "active" to subscription.active
                    )
                    subscriptionsCollection.document(subscription.id).set(data).await()
                }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки инициализации
            e.printStackTrace()
        }
    }
    
    // Метод для восстановления конкретного абонемента
    suspend fun restoreSubscription(subscriptionId: String): Result<Unit> {
        return try {
            val template = SubscriptionTemplates.defaultSubscriptions.find { it.id == subscriptionId }
            if (template == null) {
                return Result.failure(Exception("Абонемент с ID '$subscriptionId' не найден в шаблонах"))
            }
            
            val data = hashMapOf(
                "name" to template.name,
                "description" to template.description,
                "price" to template.price,
                "durationDays" to template.durationDays,
                "features" to template.features,
                "iconEmoji" to template.iconEmoji,
                "active" to template.active
            )
            subscriptionsCollection.document(template.id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableSubscriptions(): List<Subscription> {
        return try {
            // Сначала попробуем инициализировать
            initializeSubscriptionsIfNeeded()
            
            // Мигрируем старые абонементы (выполняется один раз)
            try {
                migrateOldSubscriptions()
            } catch (e: Exception) {
                // Игнорируем ошибки миграции
                e.printStackTrace()
            }
            
            // Загружаем все абонементы без фильтра по active
            val snapshot = subscriptionsCollection.get().await()
            val subscriptions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Subscription::class.java)?.copy(id = doc.id)
            }
            
            // Фильтруем активные локально
            val activeSubscriptions = subscriptions.filter { it.active }
            
            // Если нет активных, возвращаем дефолтные
            activeSubscriptions.ifEmpty { SubscriptionTemplates.defaultSubscriptions }
        } catch (e: Exception) {
            e.printStackTrace()
            // Возвращаем дефолтные если не удалось загрузить
            SubscriptionTemplates.defaultSubscriptions
        }
    }
    
    fun observeAvailableSubscriptions(): Flow<List<Subscription>> = callbackFlow {
        val listener = subscriptionsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(SubscriptionTemplates.defaultSubscriptions)
                    return@addSnapshotListener
                }
                val subscriptions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Subscription::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                val activeSubscriptions = subscriptions.filter { it.active }
                trySend(activeSubscriptions.ifEmpty { SubscriptionTemplates.defaultSubscriptions })
            }
        awaitClose { listener.remove() }
    }
    
    // Для администратора - получить все абонементы (включая неактивные)
    suspend fun getAllSubscriptions(): List<Subscription> {
        return try {
            val snapshot = subscriptionsCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Subscription::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Для администратора - добавить абонемент
    suspend fun addSubscription(subscription: Subscription): Result<String> {
        return try {
            val data = hashMapOf(
                "name" to subscription.name,
                "description" to subscription.description,
                "price" to subscription.price,
                "durationDays" to subscription.durationDays,
                "features" to subscription.features,
                "iconEmoji" to subscription.iconEmoji,
                "active" to subscription.active
            )
            val docRef = subscriptionsCollection.add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Для администратора - обновить абонемент
    suspend fun updateSubscription(subscriptionId: String, subscription: Subscription): Result<Unit> {
        return try {
            val data = hashMapOf(
                "name" to subscription.name,
                "description" to subscription.description,
                "price" to subscription.price,
                "durationDays" to subscription.durationDays,
                "features" to subscription.features,
                "iconEmoji" to subscription.iconEmoji,
                "active" to subscription.active
            )
            subscriptionsCollection.document(subscriptionId).update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Для администратора - удалить абонемент
    suspend fun deleteSubscription(subscriptionId: String): Result<Unit> {
        return try {
            subscriptionsCollection.document(subscriptionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== АБОНЕМЕНТЫ ПОЛЬЗОВАТЕЛЯ ====================
    
    suspend fun getUserSubscriptions(): List<UserSubscription> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = userSubscriptionsCollection
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserSubscription::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun observeUserSubscriptions(): Flow<List<UserSubscription>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = userSubscriptionsCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val subscriptions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserSubscription::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(subscriptions)
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun purchaseSubscription(subscription: Subscription): Result<UserSubscription> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Не авторизован"))
        
        return try {
            // Проверяем, есть ли уже такой же активный абонемент
            val existingSubscriptions = userSubscriptionsCollection
                .whereEqualTo("userId", uid)
                .whereEqualTo("subscriptionId", subscription.id)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            val now = Calendar.getInstance()
            
            if (existingSubscriptions.documents.isNotEmpty()) {
                // Найден существующий активный абонемент - продлеваем его
                val existingDoc = existingSubscriptions.documents.first()
                val existingSub = existingDoc.toObject(UserSubscription::class.java)
                
                if (existingSub != null) {
                    // Вычисляем новую дату окончания: к текущей дате окончания добавляем дни нового абонемента
                    val existingEndDate = existingSub.endDate.toDate()
                    val newEndCalendar = Calendar.getInstance().apply {
                        time = existingEndDate
                        add(Calendar.DAY_OF_YEAR, subscription.durationDays)
                    }
                    
                    // Обновляем существующий абонемент
                    val updateData = hashMapOf(
                        "endDate" to Timestamp(newEndCalendar.time)
                    )
                    existingDoc.reference.update(updateData as Map<String, Any>).await()
                    
                    // Возвращаем обновлённый абонемент
                    val updatedSub = existingSub.copy(
                        id = existingDoc.id,
                        userId = uid,
                        endDate = Timestamp(newEndCalendar.time)
                    )
                    return Result.success(updatedSub)
                }
            }
            
            // Создаём новый абонемент
            val endCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, subscription.durationDays)
            }
            
            val data = hashMapOf(
                "userId" to uid,
                "orderId" to "ORD-${System.currentTimeMillis()}",
                "subscriptionId" to subscription.id,
                "subscriptionName" to subscription.name,
                "subscriptionDescription" to subscription.description,
                "subscriptionIconEmoji" to subscription.iconEmoji,
                "subscriptionFeatures" to subscription.features,
                "startDate" to Timestamp(now.time),
                "endDate" to Timestamp(endCalendar.time),
                "active" to true
            )
            
            val docRef = userSubscriptionsCollection.add(data).await()
            
            val userSubscription = UserSubscription(
                id = docRef.id,
                userId = uid,
                orderId = data["orderId"] as String,
                subscriptionId = subscription.id,
                subscriptionName = subscription.name,
                subscriptionDescription = subscription.description,
                subscriptionIconEmoji = subscription.iconEmoji,
                subscriptionFeatures = subscription.features,
                startDate = data["startDate"] as Timestamp,
                endDate = data["endDate"] as Timestamp,
                active = true
            )
            
            Result.success(userSubscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Миграция старых абонементов из подколлекций в отдельную коллекцию
    suspend fun migrateOldSubscriptions(): Result<Int> {
        return try {
            var migratedCount = 0
            
            // Получаем всех пользователей
            val usersSnapshot = usersCollection.get().await()
            
            for (userDoc in usersSnapshot.documents) {
                val userId = userDoc.id
                val oldSubscriptionsCollection = usersCollection.document(userId).collection("subscriptions")
                val oldSubscriptions = oldSubscriptionsCollection.get().await()
                
                // Переносим каждый абонемент в новую коллекцию
                for (oldSubDoc in oldSubscriptions.documents) {
                    val oldSub = oldSubDoc.toObject(UserSubscription::class.java)
                    if (oldSub != null) {
                        // Проверяем, не существует ли уже такой абонемент
                        val existing = userSubscriptionsCollection
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("subscriptionId", oldSub.subscriptionId)
                            .whereEqualTo("orderId", oldSub.orderId)
                            .get()
                            .await()
                        
                        if (existing.isEmpty) {
                            // Добавляем userId и переносим в новую коллекцию
                            val data = hashMapOf(
                                "userId" to userId,
                                "orderId" to oldSub.orderId,
                                "subscriptionId" to oldSub.subscriptionId,
                                "subscriptionName" to oldSub.subscriptionName,
                                "subscriptionDescription" to oldSub.subscriptionDescription,
                                "subscriptionIconEmoji" to oldSub.subscriptionIconEmoji,
                                "subscriptionFeatures" to oldSub.subscriptionFeatures,
                                "startDate" to oldSub.startDate,
                                "endDate" to oldSub.endDate,
                                "active" to oldSub.active
                            )
                            userSubscriptionsCollection.add(data).await()
                            migratedCount++
                        }
                    }
                }
            }
            
            Result.success(migratedCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // ==================== УТИЛИТЫ ====================
    
    private fun mapFirebaseException(e: Exception): Exception {
        val message = when {
            e.message?.contains("email address is badly formatted") == true -> 
                "Неверный формат email"
            e.message?.contains("password is invalid") == true || 
            e.message?.contains("wrong-password") == true -> 
                "Неверный пароль"
            e.message?.contains("no user record") == true ||
            e.message?.contains("user-not-found") == true -> 
                "Пользователь не найден"
            e.message?.contains("email address is already in use") == true -> 
                "Этот email уже зарегистрирован"
            e.message?.contains("weak-password") == true -> 
                "Пароль слишком слабый (минимум 6 символов)"
            e.message?.contains("network") == true -> 
                "Ошибка сети. Проверьте подключение к интернету"
            else -> e.message ?: "Неизвестная ошибка"
        }
        return Exception(message)
    }
}

// Синглтон для доступа к репозиторию
object FirebaseRepo {
    val instance: FirebaseRepository by lazy { FirebaseRepository() }
}
