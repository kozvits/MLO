package com.mlo.app.data.local

import androidx.room.*

@Entity(tableName = "contexts")
data class ContextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val label: String = "",
    val icon: String? = null,
    val iconName: String? = null,
    val color: Int? = null,
    val includeIds: String = "",
    val sortOrder: Int = 0,
    val locationLat: Double? = null,
    val locationLon: Double? = null,
    val locationRadiusMeters: Int = 0,
    val isLocationOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts ORDER BY sortOrder, name")
    fun getAllContexts(): kotlinx.coroutines.flow.Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts ORDER BY sortOrder, name")
    suspend fun getAllContextsSync(): List<ContextEntity>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getContextById(id: Long): ContextEntity?

    @Query("SELECT * FROM contexts WHERE id IN (:ids)")
    suspend fun getContextsByIds(ids: List<Long>): List<ContextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(context: ContextEntity): Long

    @Update
    suspend fun update(context: ContextEntity)

    @Delete
    suspend fun delete(context: ContextEntity)
}
