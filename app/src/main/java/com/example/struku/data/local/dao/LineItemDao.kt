package com.example.struku.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.struku.data.local.entity.LineItemEntity

@Dao
interface LineItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(lineItem: LineItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(lineItems: List<LineItemEntity>)
    
    @Update
    suspend fun updateLineItem(lineItem: LineItemEntity)
    
    @Delete
    suspend fun deleteLineItem(lineItem: LineItemEntity)
    
    @Query("SELECT * FROM line_items WHERE receiptId = :receiptId")
    suspend fun getLineItemsForReceipt(receiptId: Int): List<LineItemEntity>
    
    @Query("DELETE FROM line_items WHERE receiptId = :receiptId")
    suspend fun deleteLineItemsForReceipt(receiptId: Int)
}
