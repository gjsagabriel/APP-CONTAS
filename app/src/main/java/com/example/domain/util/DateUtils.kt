package com.example.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    val ptBrLocale: Locale = Locale("pt", "BR")

    fun formatToBrazilianDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
        return sdf.format(Date(millis))
    }

    fun formatDate(millis: Long): String = formatToBrazilianDate(millis)

    fun formatToShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd 'de' MMM", ptBrLocale)
        return sdf.format(Date(millis))
    }

    fun formatToMonthYear(calendar: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(calendar.time)
    }

    fun formatToMonthYear(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(monthYear: String): String = formatMonthYearDisplayName(monthYear)

    fun getNextMonthYear(monthYear: String): String = getOffsetMonth(monthYear, 1)

    fun formatMonthYearDisplayName(monthYear: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdfInput.parse(monthYear) ?: return monthYear
            val sdfOutput = SimpleDateFormat("MMMM 'de' yyyy", ptBrLocale)
            val formatted = sdfOutput.format(date)
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBrLocale) else it.toString() }
        } catch (_: Exception) {
            monthYear
        }
    }

    fun getCurrentMonthYear(): String {
        val cal = Calendar.getInstance()
        return formatToMonthYear(cal)
    }

    fun getMonthRangeMillis(monthYear: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(monthYear)
            if (date != null) {
                cal.time = date
            }
        } catch (_: Exception) {
            // fallback current
        }

        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis

        return Pair(startMillis, endMillis)
    }

    fun getOffsetMonth(monthYear: String, offsetMonths: Int): String {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(monthYear)
            if (date != null) {
                cal.time = date
            }
        } catch (_: Exception) {
            // fallback current
        }
        cal.add(Calendar.MONTH, offsetMonths)
        return formatToMonthYear(cal)
    }

    fun isToday(millis: Long): Boolean {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        return target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(millis: Long): Boolean {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return target.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }
}
