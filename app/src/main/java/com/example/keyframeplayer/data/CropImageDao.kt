package com.example.keyframeplayer.data

class CropImageDao {
    private val items = mutableListOf<CropImage>()

    suspend fun getAll(): List<CropImage> {
        return items.toList()
    }

    suspend fun getById(id: String): CropImage? {
        return items.firstOrNull { it.id == id }
    }

    suspend fun getTimestampById(id: String): CropImageTimestamp? {
        val item = getById(id) ?: return null

        return CropImageTimestamp(
            id = item.id,
            className = item.className,
            timestampRealTime = item.timestampRealTime,
            timestampFileTime = item.timestampFileTime,
            movieAddress = item.movieAddress
        )
    }

    suspend fun replaceAll(newItems: List<CropImage>) {
        items.clear()
        items.addAll(newItems)
    }
}
