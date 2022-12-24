package ru.mashurov.client

import java.time.DayOfWeek

class Utils {

    companion object {

        fun getRusDayName(day: DayOfWeek) = when (day) {
            DayOfWeek.MONDAY -> "Понедельник"
            DayOfWeek.TUESDAY -> "Вторник"
            DayOfWeek.WEDNESDAY -> "Среда"
            DayOfWeek.THURSDAY -> "Четверг"
            DayOfWeek.FRIDAY -> "Пятница"
            else -> throw IllegalArgumentException("Такой день недели не поддерживается - ${day.name}")
        }

        fun isWorkDay(day: DayOfWeek) = when (day) {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY -> true
            else -> false
        }

        fun isSameCallbackQueryDataUrl(excepted: String, actual: String): Boolean {

            if (actual.contains("?")) {
                return excepted == actual.split("?").first()
            }

            return excepted == actual
        }

        fun getUrlParams(s: String): Map<String, String> {
            val indexBeginParams = s.indexOf('?')
            val buffer = StringBuffer(s)
            val varVal = mutableMapOf<String, String>()

            buffer.substring(indexBeginParams + 1).split("&").map { param ->
                with(param.split("=")) {
                    varVal[get(0)] = get(1)
                }
            }

            return varVal
        }
    }
}