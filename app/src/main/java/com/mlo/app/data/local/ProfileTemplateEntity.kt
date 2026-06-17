package com.mlo.app.data.local

import androidx.room.*

/**
 * Profile template — GTD, FranklinCovey, etc.
 * Stores template data as JSON for export/import of full profile structure.
 */
@Entity(tableName = "profile_templates")
data class ProfileTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // "GTD", "FranklinCovey", etc.
    val description: String? = null,
    val category: String = "PRODUCTIVITY",  // "PRODUCTIVITY", "EDUCATION", etc.
    val isBuiltIn: Boolean = false,         // true for pre-installed templates
    val templateJson: String,               // Full profile data as JSON
    val thumbnailName: String? = null,      // Reference to drawable resource
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ProfileTemplateDao {
    @Query("SELECT * FROM profile_templates ORDER BY sortOrder, name")
    fun getAllTemplates(): kotlinx.coroutines.flow.Flow<List<ProfileTemplateEntity>>

    @Query("SELECT * FROM profile_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): ProfileTemplateEntity?

    @Query("SELECT * FROM profile_templates WHERE category = :category ORDER BY sortOrder")
    fun getTemplatesByCategory(category: String): kotlinx.coroutines.flow.Flow<List<ProfileTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: ProfileTemplateEntity): Long

    @Update
    suspend fun update(template: ProfileTemplateEntity)

    @Delete
    suspend fun delete(template: ProfileTemplateEntity)

    @Query("DELETE FROM profile_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
