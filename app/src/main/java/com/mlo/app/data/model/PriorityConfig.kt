package com.mlo.app.data.model

data class PriorityConfig(
    val wI: Double = 0.4,  // Importance weight
    val wU: Double = 0.3,  // Urgency weight
    val wT: Double = 0.2,  // Time factor weight
    val wG: Double = 0.1   // Weekly goal weight
)
