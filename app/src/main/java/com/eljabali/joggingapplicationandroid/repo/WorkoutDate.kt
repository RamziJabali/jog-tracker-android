package com.eljabali.joggingapplicationandroid.repo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "user_workout_schedule")
data class WorkoutDate(
    @PrimaryKey @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "did_user_attend_date") val didUserAttend: Boolean,
    @ColumnInfo(name = "latitude") val latitude: Long,
    @ColumnInfo(name = "longitude") val longitude: Long

)
