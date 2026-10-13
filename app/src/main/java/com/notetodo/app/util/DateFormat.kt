package com.notetodo.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID = Locale.forLanguageTag("id-ID")
private val sameYearFormat = DateTimeFormatter.ofPattern("d MMM", ID)
private val otherYearFormat = DateTimeFormatter.ofPattern("d MMM yyyy", ID)

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** "Hari ini" / "Besok" / "Kemarin", otherwise a short date. */
fun formatDueDate(millis: Long, today: LocalDate = LocalDate.now()): String {
    val date = millis.toLocalDate()
    return when (date) {
        today -> "Hari ini"
        today.plusDays(1) -> "Besok"
        today.minusDays(1) -> "Kemarin"
        else -> if (date.year == today.year) {
            date.format(sameYearFormat)
        } else {
            date.format(otherYearFormat)
        }
    }
}

fun isOverdue(millis: Long, today: LocalDate = LocalDate.now()): Boolean =
    millis.toLocalDate().isBefore(today)

/**
 * Material's date picker speaks UTC midnight while the app stores local
 * start-of-day, so both directions need an explicit hop through [LocalDate].
 * Skipping this shifts the due date by a day for anyone east or west of UTC.
 */
fun pickerMillisToLocalStartOfDay(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochMillis()

fun localStartOfDayToPickerMillis(millis: Long): Long =
    millis.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", ID)

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatTimeOnly(millis: Long): String = millis.toLocalDateTime().format(timeFormat)

/** e.g. "Besok 08:00". */
fun formatReminder(millis: Long, today: LocalDate = LocalDate.now()): String =
    "${formatDueDate(millis, today)} ${millis.toLocalDateTime().format(timeFormat)}"
