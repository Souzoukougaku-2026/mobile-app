package com.example.keyframeplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ClopImageDao {

    // 挿入(上書き)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClopImage(clopImage: ClopImageEntity): Void

    // 検索(keyFrameId)
    @Query("SELECT * FROM clopimages WHERE idKeyFrame = :keyFrameId")
    suspend fun getClopImageByKeyFrameId(keyFrameId: UUID): ClopImageEntity

    // 一覧(realTime降順)
//    @Query("""
//        SELECT clopimages.* FROM clopimages
//        INNER JOIN keyframes
//        ON clopimages.idKeyFrame = keyframes.id
//        WHERE clopimages.idKeyFrame = :keyFrameId
//        ORDER BY keyframes.realTime DESC
//    """)
//    fun getClopImageSortedByRealTime(keyFrameId: UUID): List<ClopImageEntity>


     // 一覧(ID昇順)
    @Query("""
        SELECT * FROM clopimages
        WHERE idKeyFrame = :keyFrameId
        ORDER BY idKeyFrame ASC
    """)
    fun getClopImageSortedById(keyFrameId: UUID): List<ClopImageEntity>
}