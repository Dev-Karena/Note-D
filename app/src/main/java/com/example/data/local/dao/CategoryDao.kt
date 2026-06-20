package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE notes SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateNotesCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE notes SET category = 'Uncategorized' WHERE category = :categoryName")
    suspend fun resetNotesCategoryToUncategorized(categoryName: String)

    @Transaction
    suspend fun editCategoryAndMigration(oldCategory: String, newCategory: String) {
        insertCategory(Category(name = newCategory))
        updateNotesCategory(oldCategory, newCategory)
        deleteCategory(Category(name = oldCategory))
    }

    @Transaction
    suspend fun deleteCategoryAndMigration(category: Category) {
        deleteCategory(category)
        resetNotesCategoryToUncategorized(category.name)
    }
}
