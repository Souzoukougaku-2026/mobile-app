package com.example.keyframeplayer.data

import android.content.Context

class AppDatabase private constructor() {
    private val cropImageDao = CropImageDao()

    fun cropImageDao(): CropImageDao {
        return cropImageDao
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase().also { INSTANCE = it }
            }
        }
    }
}
