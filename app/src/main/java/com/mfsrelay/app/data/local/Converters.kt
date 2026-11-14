package com.mfsrelay.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: UploadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): UploadStatus =
        runCatching { UploadStatus.valueOf(value) }.getOrDefault(UploadStatus.PENDING_UPLOAD)
}
