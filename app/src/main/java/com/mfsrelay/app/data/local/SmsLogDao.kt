package com.mfsrelay.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SmsLogEntry)

    @Update
    suspend fun update(entry: SmsLogEntry)

    @Query("SELECT * FROM sms_log_entries ORDER BY receivedAtEpochMillis DESC")
    fun observeAll(): Flow<List<SmsLogEntry>>

    @Query("SELECT * FROM sms_log_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SmsLogEntry?

    @Query("SELECT * FROM sms_log_entries WHERE status = :status")
    suspend fun getByStatus(status: UploadStatus): List<SmsLogEntry>

    @Query("SELECT * FROM sms_log_entries WHERE method = :method AND trxId = :trxId LIMIT 1")
    suspend fun findByMethodAndTrxId(method: String, trxId: String): SmsLogEntry?
}
