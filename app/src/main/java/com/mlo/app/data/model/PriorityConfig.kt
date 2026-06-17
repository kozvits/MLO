package com.mlo.app.data.model

data class PriorityConfig(
    val wI: Double = 1.0,  // Importance weight
    val wU: Double = 1.0,  // Urgency weight
    val wT: Double = 0.5,  // Time factor weight
    val wG: Double = 0.3   // Weekly goal weight
)
