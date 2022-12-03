import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import ru.mashurov.client.dtos.NamedEntityDto

sealed class Keyboard {

    companion object {

        val mainMenuKeyboard = InlineKeyboardMarkup.create(
            listOf(
                listOf(InlineKeyboardButton.CallbackData("Записать к ветеринару", "appointment_req")),
                listOf(
                    InlineKeyboardButton.CallbackData("Ваши питомцы", "pets"),
                    InlineKeyboardButton.CallbackData("Ваши заявления", "my_appointments")
                ),
                listOf(
                    InlineKeyboardButton.CallbackData("Настройки профиля", "profile_settings"),
                    InlineKeyboardButton.CallbackData("Помощь", "help")
                )
            )
        )

        val petsKeyboard = InlineKeyboardMarkup.create(
            listOf(
                listOf(
                    InlineKeyboardButton.CallbackData("Список питомцев", "pets_list"),
                    InlineKeyboardButton.CallbackData("Добавить", "pets_add")
                ),
                listOf(InlineKeyboardButton.CallbackData("<< В начало", "back"))
            )
        )

        val petOneKeyboard = { id: Long ->
            InlineKeyboardMarkup.create(
                listOf(
                    listOf(
                        InlineKeyboardButton.CallbackData("Изменить", "pet_one_change?id=$id"),
                        InlineKeyboardButton.CallbackData("Удалить", "pet_one_delete?id=$id")
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData("Список болезней", "pet_one_disease?id=$id")
                    ),
                    listOf(InlineKeyboardButton.CallbackData("<< В начало", "back"))
                )
            )
        }

        val listKeyboard = { callbackQuery: String, entities: MutableList<NamedEntityDto> ->
            InlineKeyboardMarkup.create(getPetInlineButtons(callbackQuery, entities))
        }

        val oneDeleteKeyboard = { callbackQuery: String, id: Long ->
            InlineKeyboardMarkup.create(
                listOf(
                    listOf(InlineKeyboardButton.CallbackData("Отменить", "$callbackQuery?id=$id"))
                )
            )
        }

        private fun getPetInlineButtons(
            callbackQuery: String,
            entities: MutableList<NamedEntityDto>
        ): ArrayList<ArrayList<InlineKeyboardButton>> {

            val petKeyMatrix = ArrayList<ArrayList<InlineKeyboardButton>>()
            var list = ArrayList<InlineKeyboardButton>()

            for (entity in entities) {

                list.add(InlineKeyboardButton.CallbackData(entity.name, "$callbackQuery?id=${entity.id}"))

                if (list.size == 2) {
                    petKeyMatrix.add(list)
                    list = ArrayList()
                }
            }

            if (list.size != 2) {
                petKeyMatrix.add(list)
            }

            return petKeyMatrix.apply {
                petKeyMatrix.add(arrayListOf(InlineKeyboardButton.CallbackData("<< В меню", "back")))
            }
        }
    }
}
