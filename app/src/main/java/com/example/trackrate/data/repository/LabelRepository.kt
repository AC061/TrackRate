package com.example.trackrate.data.repository

import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.CreateRecordLabelDto
import com.example.trackrate.data.remote.dto.TopRatedTrackDto
import com.example.trackrate.domain.model.RecordLabel
import com.example.trackrate.domain.model.TopRatedTrack
import com.example.trackrate.util.MediaUrlResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabelRepository @Inject constructor(
    private val api: TrackRateApi
) {
    suspend fun getLabels(): List<RecordLabel> =
        api.getRecordLabels().map { RecordLabel(id = it.id, name = it.name) }

    suspend fun createLabel(name: String): RecordLabel {
        val dto = api.createRecordLabel(CreateRecordLabelDto(name.trim()))
        return RecordLabel(id = dto.id, name = dto.name)
    }
}

fun TopRatedTrackDto.toDomain(): TopRatedTrack = TopRatedTrack(
    id = id,
    title = title,
    subtitle = subtitle,
    imageUrl = MediaUrlResolver.resolve(imageUrl),
    averageRating = averageRating,
    ratingCount = ratingCount
)
