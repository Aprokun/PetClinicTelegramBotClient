package ru.mashurov.client

import Keyboard.Companion.mainMenuKeyboard
import Keyboard.Companion.petOneKeyboard
import Keyboard.Companion.petsKeyboard
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.logging.LogLevel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.mashurov.client.IdType.TELEGRAM
import ru.mashurov.client.Messages.Companion.startMessage
import ru.mashurov.client.dtos.PetDto
import ru.mashurov.client.dtos.UserDto
import ru.mashurov.client.services.PetClient
import ru.mashurov.client.services.UserClient

val gson: Gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd HH:mm:ssZ")
    .create()

val httpLoggingInterceptor = HttpLoggingInterceptor()

val okHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("http://localhost:8080")
    .addConverterFactory(GsonConverterFactory.create(gson))
    .client(okHttpClient)
    .build()

var userId: Long? = null

fun main() {
    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val userClient = retrofit.create(UserClient::class.java)
    val petClient = retrofit.create(PetClient::class.java)

    val bot = bot {
        token = System.getenv("BOT_TOKEN")
        logLevel = LogLevel.All()
        dispatch {
            startCommand(userClient)
            petCommands(userClient, petClient)
            mainCommand(userClient)
        }
    }

    bot.startPolling()
}

private fun Dispatcher.mainCommand(userClient: UserClient) {

    command("main") {

        bot.sendMessage(
            ChatId.fromId(message.chat.id), "Меню бота", replyMarkup = mainMenuKeyboard
        )
    }

    callbackQuery("back") {
        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = "Меню бота",
            replyMarkup = mainMenuKeyboard
        )
    }
}

private fun Dispatcher.petCommands(userClient: UserClient, petClient: PetClient) {

    callbackQuery("pets") {

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = "Меню питомцев",
            replyMarkup = petsKeyboard
        )
    }

    callbackQuery("pets_list") {

        val user = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.typeString).execute().body()!!

        if (user.pets!!.isNotEmpty()) {

            val replyMarkup = InlineKeyboardMarkup.create(
                getPetInlineButtons(user.pets)
            )

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "Ваши питомцы",
                replyMarkup = replyMarkup
            )

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "У вас пока что нет никаких питомцев",
                replyMarkup = petsKeyboard
            )
        }
    }

    callbackQuery("pets_add") {

        val user = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.typeString).execute().body()!!

        bot.sendMessage(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            "Введите имя, пол (м/ж) и возраст питомца через пробел (пример \"Тишка м 3\")"
        )

        message(Filter.Text) {

            with(update.message?.text.toString().trim().split(" ")) {
                val name = get(0)
                val gender = determineGender(get(1))
                val age = get(2).toInt()

                val response = petClient.save(PetDto(name, age, user = user, gender = gender)).execute()

                if (response.isSuccessful) {

                    bot.editMessageText(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        callbackQuery.message!!.messageId,
                        text = "Вы добавили нового питомца! ($name)",
                        replyMarkup = petsKeyboard
                    )

                } else {

                    bot.sendMessage(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        text = "Что-то пошло не так, попробуйте позже",
                        replyMarkup = petsKeyboard
                    )
                }
            }

        }
    }

    callbackQuery("pet_one") {

        val id = getUrlParams(callbackQuery.data)["id"]!!.toLong()
        val pet = petClient.get(id).execute().body()!!

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = """
                Имя: ${pet.name}
                Возраст: ${pet.age}
                Пол: ${pet.gender}
            """.trimIndent(),
            replyMarkup = petOneKeyboard
        )
    }

    callbackQuery("pet_one_delete") {

        val id = getUrlParams(callbackQuery.data)["id"]!!.toLong()
        val response = petClient.delete(id).execute()

        val answerMessage = when (response.isSuccessful) {
            true -> "Питомец успешно удалён!"
            else -> "Что-то пошло не так, попробуйте позже"
        }

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = answerMessage,
            replyMarkup = petsKeyboard
        )
    }

    callbackQuery("pet_one_change") {

        val id = getUrlParams(callbackQuery.data)["id"]!!.toLong()


    }
}

private fun getPetInlineButtons(pets: MutableList<PetDto>): ArrayList<ArrayList<InlineKeyboardButton>> {

    val petKeyMatrix = ArrayList<ArrayList<InlineKeyboardButton>>()

    var list = ArrayList<InlineKeyboardButton>()
    for (pet in pets) {

        list.add(InlineKeyboardButton.CallbackData(pet.name, "pet_one?id=${pet.id}"))

        if (list.size == 2) {
            petKeyMatrix.add(list)
            list = ArrayList()
        }
    }

    petKeyMatrix.add(arrayListOf(InlineKeyboardButton.CallbackData("<< В меню", "back")))

    return petKeyMatrix
}

fun determineGender(gender: String): String =
    when (gender) {
        "м" -> "Мужской"
        "ж" -> "Женский"
        else -> "Не указан"
    }

private fun Dispatcher.startCommand(userClient: UserClient) {

    command("start") {

        val isUserExists = userClient.existByTelegramId(message.chat.id, TELEGRAM.typeString).execute().body()!!

        if (!isUserExists) {

            bot.sendMessage(ChatId.fromId(message.chat.id), startMessage)

            val regions = userClient.getRegions().execute().body()!!

            val regionKeyboard = KeyboardReplyMarkup(
                listOf(regions.map { KeyboardButton(it.name) }.toList()),
                resizeKeyboard = true,
                oneTimeKeyboard = true
            )

            bot.sendMessage(
                ChatId.fromId(message.chat.id),
                "Выберите ваш регион",
                replyMarkup = regionKeyboard
            )

            message(Filter.Text) {
                val regionName = message.text.toString()
                val regionDto = regions.first { dto -> dto.name == regionName }
                val userDto = UserDto(
                    message.chat.firstName!!,
                    message.chat.username,
                    message.chat.id,
                    regionDto,
                    mutableListOf()
                )
                val user = userClient.save(userDto).execute().body()
                userId = user!!.id
            }

        } else {

            val user = userClient.getUser(message.chat.id, TELEGRAM.typeString).execute().body()
            userId = user!!.id

            bot.sendMessage(
                ChatId.fromId(message.chat.id),
                "Здравствуйте, ${message.chat.firstName}! Чтобы перейти к главному меню воспользуйтесь командой /main. " +
                        "Если у вас возникли какие-то проблемы, то воспользуйтесь командой /help"
            )
        }
    }
}

private fun getUrlParams(s: String): Map<String, String> {
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

