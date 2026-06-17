package com.mlo.app.data.local

import androidx.room.*

/**
 * Reminder entity — time-based or location-based reminders for tasks
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("taskId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val type: String = "TIME",           // "TIME" or "LOCATION"
    val triggerTime: Long? = null,       // Millis for TIME reminders
    val locationLat: Double? = null,     // Latitude for LOCATION reminders
    val locationLon: Double? = null,     // Longitude
    val locationRadiusMeters: Int = 100, // Geofence radius
    val isEnabled: Boolean = true,
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    fun getAllEnabledReminders(): kotlinx.coroutines.flow.Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabledRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getRemindersForTask(taskId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE type = 'LOCATION' AND isEnabled = 1")
    suspend fun getEnabledLocationReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE type = 'TIME' AND isEnabled = 1 AND triggerTime <= :now")
    suspend fun getOverdueTimeReminders(now: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("UPDATE reminders SET isEnabled = 0 WHERE id = :id")
    suspend fun disable(id: Long)

    @Query("UPDATE reminders SET snoozedUntil = :until WHERE id = :id")
    suspend fun snooze(id: Long, until: Long)
}
