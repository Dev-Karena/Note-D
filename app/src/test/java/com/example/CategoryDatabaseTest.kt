package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.CategoryDao
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetCategory() = runBlocking {
        val category = Category(name = "Personal")
        categoryDao.insertCategory(category)
        
        val allCategories = categoryDao.getAllCategories().first()
        assertEquals(1, allCategories.size)
        assertEquals("Personal", allCategories[0].name)
    }

    @Test
    fun deleteCategory() = runBlocking {
        val category = Category(name = "Work")
        categoryDao.insertCategory(category)
        
        var allCategories = categoryDao.getAllCategories().first()
        assertEquals(1, allCategories.size)

        categoryDao.deleteCategory(category)
        allCategories = categoryDao.getAllCategories().first()
        assertTrue(allCategories.isEmpty())
    }
}
