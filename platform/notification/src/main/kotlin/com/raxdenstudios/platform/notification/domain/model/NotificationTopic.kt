package com.raxdenstudios.platform.notification.domain.model

import kotlin.math.abs

/**
 * An FCM topic the device listens to.
 *
 * @property value The topic name as sent to FCM, e.g. `radio_daily_utc_p1`.
 */
@JvmInline
value class NotificationTopic(
    val value: String,
) {

    companion object {

        /** Lowest and highest whole-hour UTC offsets in use around the world. */
        const val MIN_UTC_OFFSET_HOURS = -12
        const val MAX_UTC_OFFSET_HOURS = 14

        /**
         * The topic of the devices whose clock is [utcOffsetHours] hours from UTC, clamped to
         * [MIN_UTC_OFFSET_HOURS]..[MAX_UTC_OFFSET_HOURS]: with the prefix `app_daily_utc_`, `app_daily_utc_p1` for +1,
         * `app_daily_utc_m5` for -5 and `app_daily_utc_0` for UTC itself.
         */
        fun utcOffset(
            prefix: String,
            utcOffsetHours: Int,
        ): NotificationTopic {
            val offset = utcOffsetHours.coerceIn(MIN_UTC_OFFSET_HOURS, MAX_UTC_OFFSET_HOURS)
            val suffix = when {
                offset > 0 -> "p$offset"
                offset < 0 -> "m${abs(offset)}"
                else -> "0"
            }
            return NotificationTopic("$prefix$suffix")
        }
    }
}
