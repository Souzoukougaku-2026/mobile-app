package com.example.a2026souzou.core.di

import android.content.Context
import androidx.room.Room
import com.example.a2026souzou.core.data.database.AppDatabase
import com.example.a2026souzou.core.data.database.dao.CropImageDao
import com.example.a2026souzou.core.data.database.dao.KeyFrameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "crop_image_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCropImageDao(database: AppDatabase): CropImageDao {
        return database.cropImageDao()
    }

    @Provides
    fun provideKeyFrameDao(database: AppDatabase): KeyFrameDao {
        return database.keyFrameDao()
    }
}
