package com.mato.syai.core.model

import com.mato.syai.core.composables.TrackerInterface

data class DashboardGridItem(val id: Int, val title: String, val isExpanded: Boolean = false)

data class FitnessItem(val image: Int, val text: String)

data class TrackerCardItem(val id: Int, val isExpanded: Boolean = false, val tracker: TrackerInterface)

