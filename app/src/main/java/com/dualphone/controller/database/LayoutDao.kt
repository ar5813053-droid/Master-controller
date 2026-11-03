package com.dualphone.controller.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutDao {
    @Query("SELECT * FROM controller_layouts ORDER BY updatedAt DESC")
    fun getAllLayouts(): Flow<List<LayoutEntity>>

    @Query("SELECT * FROM controller_layouts WHERE layoutId = :id LIMIT 1")
    suspend fun getLayoutById(id: String): LayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(layout: LayoutEntity)

    @Query("DELETE FROM controller_layouts WHERE layoutId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM controller_layouts")
    suspend fun deleteAll()
}
