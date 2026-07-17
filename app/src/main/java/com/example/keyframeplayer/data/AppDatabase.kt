package com.example.keyframeplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.keyframeplayer.RoomTypeConverters

// ① データベースの設定
@Database(
    entities = [
        KeyFrameEntity::class,
        ClopImageEntity::class
    ],
    version = 1,                 // データベースのバージョン（構造を変えたら上げる）
    exportSchema = false         // スキーマの書き出しをオフ（通常はfalseでOK）
)
// ② UUIDを使えるようにする型変換の登録
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    // ③ 利用するDAOを登録する（Roomが自動で中身を生成します）
    abstract fun keyFrameDao(): KeyFrameDao
    abstract fun clopImageDao(): ClopImageDao

    // ④ アプリ内でデータベースのインスタンスを使い回すための設定（シングルトン）
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // すでにインスタンスがあればそれを返し、なければ新しく作る
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // 💡スマホ内に保存される実際のファイル名
                )
                    // .fallbackToDestructiveMigration() // 開発中、バージョンを上げた時にデータを全削除してリセットしたい場合はコメント解除
                    .build()

                INSTANCE = instance
                instance
            }
        }

        suspend fun clearDatabase(context: Context) {
            getDatabase(context).clearAllTables()
        }
    }
}