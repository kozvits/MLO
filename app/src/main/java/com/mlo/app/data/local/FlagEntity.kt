package com.mlo.app.data.local

import androidx.room.*

/**
 * Flag/color label entity — for task tagging with colors/icons
 */
@Entity(tableName = "flags")
data class FlagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // "STARRED", "DELEGATED", "WAITING", etc.
    val label: String = "",              // Display name (e.g. "Избранное")
    val color: Int? = null,              // ARGB color
    val iconName: String? = null,        // Material icon name
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FlagDao {
    @Query("SELECT * FROM flags ORDER BY sortOrder, name")
    fun getAllFlags(): kotlinx.coroutines.flow.Flow<List<FlagEntity>>

    @Query("SELECT * FROM flags ORDER BY sortOrder, name")
    suspend fun getAllFlagsSync(): List<FlagEntity>

    @Query("SELECT * FROM flags WHERE id = :id")
    suspend fun getFlagById(id: Long): FlagEntity?

    @Query("SELECT * FROM flags WHERE name = :name LIMIT 1")
    suspend fun getFlagByName(name: String): FlagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flag: FlagEntity): Long

    @Update
    suspend fun update(flag: FlagEntity)

    @Delete
    suspend fun delete(flag: FlagEntity)

    @Query("DELETE FROM flags WHERE id = :id")
    suspend fun deleteById(id: Long)
}
