package ru.mashurov.client

import ru.mashurov.client.dtos.AppointmentRequestDto
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

class Utils {

    companion object {

        fun convertAppointmentPlace(appointmentPlace: String): String = when (appointmentPlace) {
            "clinic" -> "В клинике"
            "home" -> "На дому"
            else -> throw java.lang.IllegalArgumentException("Нет такого места - $appointmentPlace")
        }

        fun convertDateToNormalFormat(appointmentRequestDto: AppointmentRequestDto): String? {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            val output = SimpleDateFormat("dd.MM.yyyy HH:mm")
            val d: Date = sdf.parse(appointmentRequestDto.date)
            val formatDate = output.format(d)
            return formatDate
        }

        fun determineGender(gender: String): String = when (gender) {
            "м" -> "Мужской"
            "ж" -> "Женский"
            else -> "Не указан"
        }


        fun getDates(weeksLimit: Int): List<LocalDate> {

            val dates = mutableListOf<LocalDate>()
            val now = LocalDate.now()

            for (days in 0L..7 * weeksLimit) {

                val current = now.plusDays(days)

                if (isWorkDay(current.dayOfWeek)) {
                    dates.add(current)
                }
            }

            return dates
        }

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