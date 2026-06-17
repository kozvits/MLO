package com.mlo.app.data.local

import androidx.room.*

/**
 * Saved view/filter configuration — advanced filtering and grouping
 */
@Entity(tableName = "saved_views")
data class ViewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contextFilter: String = "",          // Comma-separated context IDs
    val flagFilter: String = "",             // Comma-separated flag IDs
    val statusFilter: String = "",           // "" = all, "ACTIVE", "COMPLETED", etc.
    val searchQuery: String = "",
    val hasDueDate: Boolean = false,         // filter: has due date
    val isOverdue: Boolean = false,          // filter: overdue only
    val groupBy: String = "NONE",            // NONE, CONTEXT, GOAL, PROJECT, STATUS, FLAG, MONTH
    val sortBy: String = "PRIORITY",         // PRIORITY, DUE_DATE, CREATED_DATE, NAME, CUSTOM
    val isAscending: Boolean = false,
    val showCompleted: Boolean = false,
    val showArchived: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ViewDao {
    @Query("SELECT * FROM saved_views ORDER BY isDefault DESC, name ASC")
    fun getAllViews(): kotlinx.coroutines.flow.Flow<List<ViewEntity>>

    @Query("SELECT * FROM saved_views ORDER BY isDefault DESC, name ASC")
    suspend fun getAllViewsSync(): List<ViewEntity>

    @Query("SELECT * FROM saved_views WHERE id = :id")
    suspend fun getViewById(id: Long): ViewEntity?

    @Query("SELECT * FROM saved_views WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultView(): ViewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(view: ViewEntity): Long

    @Update
    suspend fun update(view: ViewEntity)

    @Delete
    suspend fun delete(view: ViewEntity)

    @Query("UPDATE saved_views SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE saved_views SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}
