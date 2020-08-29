package org.silvermoon.osm_kotlin.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object DateUtil {
    const val SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    fun stringToCalendar(date: String?): Calendar? {
        var myNewDate: Date? = null
        val calendar = Calendar.getInstance()
        if (date == null || date.length == 0) return null
        val dateFormat =
            SimpleDateFormat(SQL_DATE_FORMAT)
        try {
            myNewDate = dateFormat.parse(date)
            calendar.time = myNewDate
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return calendar
    }

    fun longToSqlDateFormat(date: Long): String {
        val d = Date(date)
        val dateFormat =
            SimpleDateFormat(SQL_DATE_FORMAT)
        return dateFormat.format(d)
    }

    fun calendarFromDate(d: Date?): Calendar {
        val c = Calendar.getInstance()
        c.time = d
        return c
    }

    fun longToCalendar(time: Long?): Calendar? {
        var c: Calendar? = null
        if (time != null) {
            c = Calendar.getInstance()
            c.timeInMillis = time
        }
        return c
    }

    fun CalendarTolong(c: Calendar?): Long? {
        return c?.timeInMillis
    }

    fun getFormatedDate(c: Calendar, format: String?): String {
        return try {
            val d = Date(c.timeInMillis)
            val dateFormat = SimpleDateFormat(format)
            dateFormat.format(d)
        } catch (e: Exception) {
            String()
        }
    }

    fun compare(d1: Calendar?, d2: Calendar?): Int {
        var result = 0
        if (d1 == null && d2 == null) return result
        if (d1 == null) return -1
        if (d2 == null) return 1
        result = if (d1.after(d2)) 1 else if (d1.before(d2)) -1 else 0
        return result
    }
}