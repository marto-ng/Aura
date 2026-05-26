package java_text_SimpleDateFormat_compatibility

import java.text.SimpleDateFormat
import java.util.Locale

fun getDayOfWeekSpanish(dateString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(dateString) ?: return dateString
        val outFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
        outFormat.format(date).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
        }
    } catch (e: Exception) {
        dateString
    }
}
