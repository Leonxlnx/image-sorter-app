package com.leonxlnx.imagesorter.data

import java.util.concurrent.TimeUnit

/** Predefined and custom date filters applied to the photo query. */
sealed interface DateRange {
    val id: String
    val label: String

    data object Any : DateRange {
        override val id = "any"
        override val label = "Any time"
    }
    data object Today : DateRange {
        override val id = "today"
        override val label = "Today"
    }
    data object Last7Days : DateRange {
        override val id = "7d"
        override val label = "Last 7 days"
    }
    data object Last30Days : DateRange {
        override val id = "30d"
        override val label = "Last 30 days"
    }
    data object LastYear : DateRange {
        override val id = "1y"
        override val label = "Last year"
    }
    data class Custom(val fromMillis: Long, val toMillis: Long) : DateRange {
        override val id: String = "custom:$fromMillis:$toMillis"
        override val label: String = "Custom"
    }

    fun toMillisRange(now: Long = System.currentTimeMillis()): Pair<Long, Long>? = when (this) {
        Any -> null
        Today -> {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis to now
        }
        Last7Days -> (now - TimeUnit.DAYS.toMillis(7)) to now
        Last30Days -> (now - TimeUnit.DAYS.toMillis(30)) to now
        LastYear -> (now - TimeUnit.DAYS.toMillis(365)) to now
        is Custom -> fromMillis to toMillis
    }

    companion object {
        val Presets: List<DateRange> = listOf(Any, Today, Last7Days, Last30Days, LastYear)

        fun fromId(id: String?): DateRange = when (id) {
            null, Any.id -> Any
            Today.id -> Today
            Last7Days.id -> Last7Days
            Last30Days.id -> Last30Days
            LastYear.id -> LastYear
            else -> {
                if (id.startsWith("custom:")) {
                    val parts = id.removePrefix("custom:").split(":")
                    val from = parts.getOrNull(0)?.toLongOrNull()
                    val to = parts.getOrNull(1)?.toLongOrNull()
                    if (from != null && to != null) Custom(from, to) else Any
                } else Any
            }
        }
    }
}
