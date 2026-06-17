package com.mlo.app.data.model

import com.mlo.app.data.local.ContextEntity

/**
 * Enhanced context model for UI, with location support.
 */
data class GContextModel(
    val id: Long,
    val name: String,
    val label: String,
    val color: Int?,
    val iconName: String?,
    val includeIds: List<Long> = emptyList(),
    val locationLat: Double? = null,
    val locationLon: Double? = null,
    val locationRadiusMeters: Int = 0,
    val isLocationOnly: Boolean = false
) {
    val isOpenNow: Boolean
        get() = !isLocationOnly // Simplified; full check uses business hours

    companion object {
        fun fromEntity(entity: ContextEntity) = GContextModel(
            id = entity.id,
            name = entity.name,
            label = entity.label,
            color = entity.color,
            iconName = entity.iconName,
            includeIds = entity.includeIds.split(",").mapNotNull { it.trim().toLongOrNull() },
            locationLat = entity.locationLat,
            locationLon = entity.locationLon,
            locationRadiusMeters = entity.locationRadiusMeters,
            isLocationOnly = entity.isLocationOnly
        )
    }
}
