package java_text_SimpleDateFormat_compatibility

import java.text.SimpleDateFormat
import java.util.Locale

fun getDayOfWeek(dateString: String, isEnglish: Boolean = false): String {
    return try {
        val cleanDate = dateString.substringBefore("T")
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(cleanDate) ?: return dateString
        val targetLocale = if (isEnglish) Locale.ENGLISH else Locale("es", "ES")
        val outFormat = SimpleDateFormat("EEEE", targetLocale)
        outFormat.format(date).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(targetLocale) else it.toString()
        }
    } catch (e: Exception) {
        dateString
    }
}

fun getDayOfWeekSpanish(dateString: String): String {
    return getDayOfWeek(dateString, isEnglish = false)
}
