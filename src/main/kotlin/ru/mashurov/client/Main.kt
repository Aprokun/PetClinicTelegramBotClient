package ru.mashurov.client

import Keyboard.Companion.listKeyboard
import Keyboard.Companion.mainMenuKeyboard
import Keyboard.Companion.petOneKeyboard
import Keyboard.Companion.petsKeyboard
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
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
import ru.mashurov.client.Messages.Companion.GENERAL_ERROR_MESSAGE
import ru.mashurov.client.Messages.Companion.startMessage
import ru.mashurov.client.dtos.*
import ru.mashurov.client.services.AppointmentClient
import ru.mashurov.client.services.AppointmentRequestClient
import ru.mashurov.client.services.PetClient
import ru.mashurov.client.services.UserClient

class Main

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

val appointmentRequestDto = AppointmentRequestDto()

fun main() {

    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val userClient = retrofit.create(UserClient::class.java)
    val petClient = retrofit.create(PetClient::class.java)
    val appointmentRequestClient = retrofit.create(AppointmentRequestClient::class.java)
    val appointmentClient = retrofit.create(AppointmentClient::class.java)

    val bot = bot {
        token = System.getenv("BOT_TOKEN")
        logLevel = LogLevel.All()
        dispatch {
            startCommand(userClient)
            petCommands(userClient, petClient, appointmentClient)
            appointmentCommands(userClient, appointmentRequestClient)
            mainCommand(userClient)
        }
    }

    bot.startPolling()
}

private fun Dispatcher.appointmentCommands(userClient: UserClient, appointmentRequestClient: AppointmentRequestClient) {

    callbackQuery("appointment_req") {

        val userRequest = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute()

        if (userRequest.isSuccessful && userRequest.body() != null) {

            val user = userRequest.body()!!

            appointmentRequestDto.userId = user.id!!
            appointmentRequestDto.regionCode = user.region!!.code

            if (callbackQuery.data == "appointment_req") {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Выберите питомца",
                    replyMarkup = listKeyboard(
                        "appointment_req_1",
                        user.pets!!.map(PetDto::toNamedEntityDto).toMutableList()
                    )
                )
            }

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
    }

    callbackQuery("appointment_req_1") {

        appointmentRequestDto.petId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val clinicsRequest = appointmentRequestClient.findAllClinics(appointmentRequestDto.regionCode).execute()

        if (clinicsRequest.isSuccessful) {

            val clinics = clinicsRequest.body()!!

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "Выберите клинику",
                replyMarkup = listKeyboard(
                    "appointment_req_2",
                    clinics.map(ClinicDto::toNamedEntityDto).toMutableList()
                )
            )

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
    }

    callbackQuery("appointment_req_2") {

        appointmentRequestDto.clinicId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val appointmentPlaceKeyboard = InlineKeyboardMarkup.create(
            listOf(
                listOf(
                    InlineKeyboardButton.CallbackData("На дому", "appointment_req_3?place=home"),
                    InlineKeyboardButton.CallbackData("В клинике", "appointment_req_3?place=clinic")
                )
            )
        )

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = "На дому или в клинике",
            replyMarkup = appointmentPlaceKeyboard
        )
    }

    callbackQuery("appointment_req_3") {

        appointmentRequestDto.appointmentPlace = getUrlParams(callbackQuery.data)["place"]!!

        val clinicRequest = appointmentRequestClient.findClinic(appointmentRequestDto.clinicId).execute()

        if (clinicRequest.isSuccessful) {

            val clinic = clinicRequest.body()!!

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "Выберите необходимую услугу",
                replyMarkup = listKeyboard(
                    "appointment_req_4",
                    clinic.services.map(ServiceDto::toNamedEntityDto).toMutableList()
                )
            )
        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }

    }

    callbackQuery("appointment_req_4") {

        appointmentRequestDto.serviceId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val veterinariansRequest =
            appointmentRequestClient.findAllVeterinarians(appointmentRequestDto.clinicId).execute()

        if (veterinariansRequest.isSuccessful) {

            val veterinarians = veterinariansRequest.body()!!

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "Выберите ветеринара",
                replyMarkup = listKeyboard(
                    "appointment_req_5",
                    veterinarians.map(VeterinarianDto::toNamedEntityDto).toMutableList()
                )
            )
        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
    }

    callbackQuery("appointment_req_5") {

        appointmentRequestDto.veterinarianId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val response = appointmentRequestClient.createRequest(appointmentRequestDto).execute()

        val answerMessage = when (response.isSuccessful) {
            true -> "Ваше заявление на приём успешно создано!"
            else -> "К сожалению, при создании вашего заявления произошла какая-то ошибка. Повторите попытку позже"
        }

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = answerMessage,
            replyMarkup = mainMenuKeyboard
        )
    }
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

private fun Dispatcher.petCommands(userClient: UserClient, petClient: PetClient, appointmentClient: AppointmentClient) {

    callbackQuery("pets") {

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = "Меню питомцев",
            replyMarkup = petsKeyboard
        )
    }

    callbackQuery("pets_list") {

        val userRequest = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute()

        if (userRequest.isSuccessful) {

            val user = userRequest.body()!!

            if (user.pets!!.isNotEmpty()) {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Ваши питомцы",
                    replyMarkup = listKeyboard(
                        "pet_one",
                        user.pets.map(PetDto::toNamedEntityDto).toMutableList()
                    )
                )

            } else {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "У вас пока что нет никаких питомцев",
                    replyMarkup = petsKeyboard
                )
            }

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
    }

    callbackQuery("pets_add") {

        val user = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute().body()!!

        bot.sendMessage(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            "Введите имя, пол (м/ж) и возраст питомца через пробел (пример \"Тишка м 3\")"
        )

        message(Filter.Text) {

            with(update.message?.text.toString().trim().split(" ")) {
                val name = get(0)
                val gender = determineGender(get(1))
                val age = get(2).toInt()

                val response = petClient
                    .save(PetDto(name, age, user = user, gender = gender, appointments = ArrayList()))
                    .execute()

                if (response.isSuccessful) {

                    bot.editMessageText(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        callbackQuery.message!!.messageId,
                        text = "Вы добавили нового питомца! ($name)",
                        replyMarkup = petsKeyboard
                    )

                } else {

                    bot.editMessageText(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        text = GENERAL_ERROR_MESSAGE,
                        replyMarkup = petsKeyboard
                    )
                }
            }

        }
    }

    callbackQuery("pet_one") {

        val id = getUrlParams(callbackQuery.data)["id"]!!.toLong()
        val petRequest = petClient.get(id).execute()

        if (petRequest.isSuccessful) {

            val pet = petRequest.body()!!

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = """
                    Имя: ${pet.name}
                    Возраст: ${pet.age}
                    Пол: ${pet.gender}
                """.trimIndent(),
                replyMarkup = petOneKeyboard(id)
            )

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
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
        val user = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute().body()!!

        bot.sendMessage(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            "Введите новые данные для питомца (Имя, пол, возраст через пробел)"
        )

        text {

            //TODO довести до ума, а то пока печально
        }
    }

    callbackQuery("pet_one_disease") {

        val id = getUrlParams(callbackQuery.data)["id"]!!.toLong()
        val petRequest = petClient.get(id).execute()

        if (petRequest.isSuccessful) {

            val pet = petRequest.body()!!

            if (pet.appointments.isNotEmpty()) {

                pet.appointments.forEach {
                    bot.sendMessage(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        """
                        Причина посещения: ${it.serviceDto.name},
                        Описание посещения: ${it.serviceDto.description}
                        Дата посещения: ${it.appointmentDate}
                        Место посещения: ${it.appointmentPlace}
                        Врач: ${it.veterinarianDto.getSNP()}
                    """.trimIndent()
                    )
                }

            } else {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "У питомца не было посещений ветеринара",
                    replyMarkup = petOneKeyboard(id)
                )
            }

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
    }
}

fun determineGender(gender: String): String =
    when (gender) {
        "м" -> "Мужской"
        "ж" -> "Женский"
        else -> "Не указан"
    }

private fun Dispatcher.startCommand(userClient: UserClient) {

    command("start") {

        val isUserExists = userClient.existByTelegramId(message.chat.id, TELEGRAM.type).execute().body()!!

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

                val userSaveRequest = userClient.save(userDto).execute()

                if (userSaveRequest.isSuccessful) {
                    bot.sendMessage(ChatId.fromId(message.chat.id), "Вы успешно авторизовались!")
                }
            }

        } else {

            val user = userClient.getUser(message.chat.id, TELEGRAM.type).execute().body()

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
