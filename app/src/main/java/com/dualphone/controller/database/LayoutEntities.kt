package com.dualphone.controller.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.dualphone.controller.core.ControlPosition
import com.dualphone.controller.core.ControllerLayout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "controller_layouts")
@TypeConverters(LayoutConverters::class)
data class LayoutEntity(
    @PrimaryKey val layoutId: String,
    val layoutName: String,
    val controlsJson: String,
    val opacity: Float,
    val orientation: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ControllerLayout {
        val controls = Json.decodeFromString<List<ControlPosition>>(controlsJson)
        return ControllerLayout(
            layoutId = layoutId,
            layoutName = layoutName,
            controls = controls,
            opacity = opacity,
            orientation = orientation
        )
    }

    companion object {
        fun fromDomain(layout: ControllerLayout): LayoutEntity {
            return LayoutEntity(
                layoutId = layout.layoutId,
                layoutName = layout.layoutName,
                controlsJson = Json.encodeToString(layout.controls),
                opacity = layout.opacity,
                orientation = layout.orientation
            )
        }
    }
}

class LayoutConverters {
    @TypeConverter
    fun fromString(value: String): List<ControlPosition> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun toString(list: List<ControlPosition>): String {
        return Json.encodeToString(list)
    }
}
