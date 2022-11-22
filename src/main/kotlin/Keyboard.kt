import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

sealed class Keyboard {

    companion object {

        val mainMenuKeyboard = InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData("Записать к ветеринару", "appointment_req")),
                listOf(
                    InlineKeyboardButton.CallbackData("Ваши питомцы", "pets"),
                    InlineKeyboardButton.CallbackData("Настройки профиля", "profile_settings")
                ),
                listOf(InlineKeyboardButton.CallbackData("Помощь", "help"))
            )
        )

        val petsKeyboard = InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData("Список питомцев", "pets_list"), InlineKeyboardButton.CallbackData("Добавить", "pets_add")),
                listOf(InlineKeyboardButton.CallbackData("<< В начало", "back"))
            )
        )

        val petOneKeyboard = { id: Long ->
            InlineKeyboardMarkup.create(
                listOf(
                    listOf(InlineKeyboardButton.CallbackData("Изменить", "pet_one_change?id=$id"), InlineKeyboardButton.CallbackData("Удалить", "pet_one_delete?id=$id")),
                    listOf(InlineKeyboardButton.CallbackData("<< В начало", "back"))
                )
            )
        }

    }

}
