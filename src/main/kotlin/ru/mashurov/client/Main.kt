package ru.mashurov.client

import Keyboard.Companion.listKeyboard
import Keyboard.Companion.mainMenuKeyboard
import Keyboard.Companion.petOneKeyboard
import Keyboard.Companion.petsKeyboard
import Keyboard.Companion.regionsKeyboard
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.logging.LogLevel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.mashurov.client.IdType.TABLE
import ru.mashurov.client.IdType.TELEGRAM
import ru.mashurov.client.Messages.Companion.GENERAL_ERROR_MESSAGE
import ru.mashurov.client.Messages.Companion.startMessage
import ru.mashurov.client.Utils.Companion.determineGender
import ru.mashurov.client.Utils.Companion.getDates
import ru.mashurov.client.Utils.Companion.getRusDayName
import ru.mashurov.client.Utils.Companion.getUrlParams
import ru.mashurov.client.Utils.Companion.isSameCallbackQueryDataUrl
import ru.mashurov.client.dtos.*
import ru.mashurov.client.services.*
import java.time.format.DateTimeFormatter.ofPattern

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

val appointmentRequestCreateDto = AppointmentRequestCreateDto()

fun main() {

    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val userClient = retrofit.create(UserClient::class.java)
    val petClient = retrofit.create(PetClient::class.java)
    val appointmentRequestClient = retrofit.create(AppointmentRequestClient::class.java)
    val appointmentRequestsClient = retrofit.create(AppointmentRequestsClient::class.java)
    val appointmentClient = retrofit.create(AppointmentClient::class.java)
    val regionClient = retrofit.create(RegionClient::class.java)

    val bot = bot {
        token = System.getenv("BOT_TOKEN")
        logLevel = LogLevel.All()
        dispatch {
            startCommand(userClient)
            appointmentRequestsCommands(userClient, appointmentRequestsClient)
            petCommands(userClient, petClient, appointmentClient)
            appointmentRequestCommands(userClient, appointmentRequestClient)
            mainCommand(userClient)
            profileSettingsCommands(regionClient, userClient)
        }
    }

    bot.startPolling()
}

private fun Dispatcher.appointmentRequestsCommands(
    userClient: UserClient,
    appointmentRequestsClient: AppointmentRequestsClient
) {

    callbackQuery("my_appointments") {

        val userRequest = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute()

        if (userRequest.isSuccessful) {

            val userId = userRequest.body()!!.id!!

            val requestsRequest = appointmentRequestsClient.findAllByUserId(userId).execute()

            if (requestsRequest.isSuccessful) {

                val requests = requestsRequest.body()!!

                if (requests.content.isNotEmpty()) {

                    requests.content.forEach {

                        val keyboard = InlineKeyboardMarkup.create(
                            listOf(
                                listOf(
                                    InlineKeyboardButton.CallbackData("Отменить", "req_cancel?id=${it.id}")
                                )
                            )
                        )

                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.message!!.chat.id),
                            """
                            Заявление №${it.id}
                            Клиника: ${it.clinicName}
                            Адрес: ${if (it.appointmentPlace == "clinic") it.clinicAddress else "На дому"}
                            Ветеринар: ${it.veterinarianName}
                            Услуга: ${it.serviceName}
                            Дата: ${it.date}
                            Статус заявления: ${it.status}
                            Питомец: ${it.petName}
                        """.trimIndent(),
                            replyMarkup = keyboard
                        )
                    }

                } else {

                    bot.editMessageText(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        callbackQuery.message!!.messageId,
                        text = "У вас нет никаких необработанных заявлений",
                        replyMarkup = mainMenuKeyboard
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

        } else {

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = GENERAL_ERROR_MESSAGE,
                replyMarkup = mainMenuKeyboard
            )
        }
    }

    callbackQuery("req_cancel") {

        val reqId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val userRequest = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute()

        if (userRequest.isSuccessful) {

            val userId = userRequest.body()!!.id!!

            val cancelRequest = appointmentRequestsClient.cancelById(userId, reqId).execute()

            if (cancelRequest.isSuccessful) {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Заявление №$reqId отменено"
                )

            } else {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = GENERAL_ERROR_MESSAGE,
                    replyMarkup = mainMenuKeyboard
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

private fun Dispatcher.profileSettingsCommands(
    regionClient: RegionClient,
    userClient: UserClient
) {

    callbackQuery("profile_settings") {

        bot.editMessageText(
            ChatId.fromId(callbackQuery.message!!.chat.id),
            callbackQuery.message!!.messageId,
            text = "Настройка профиля",
            replyMarkup = InlineKeyboardMarkup.create(
                listOf(listOf(InlineKeyboardButton.CallbackData("Сменить регион", "change_reg")))
            )
        )
    }

    callbackQuery("change_reg") {

        val regionsRequest = regionClient.findAll().execute()

        if (regionsRequest.isSuccessful) {

            val regions = regionsRequest.body()!!

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "Выберите новый регион",
                replyMarkup = listKeyboard(
                    "select_reg",
                    regions.map(RegionDto::toNamedEntityDto).toMutableList()
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

    callbackQuery("select_reg") {

        val regionCode = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val userRequest = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute()

        if (userRequest.isSuccessful) {

            val user = userRequest.body()!!

            val setRegionRequest = userClient.setRegion(user.id!!, regionCode).execute()

            if (setRegionRequest.isSuccessful) {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Новый регион успешно установлен!",
                    replyMarkup = mainMenuKeyboard
                )

            } else {

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = GENERAL_ERROR_MESSAGE,
                    replyMarkup = mainMenuKeyboard
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

private fun Dispatcher.appointmentRequestCommands(
    userClient: UserClient,
    appointmentRequestClient: AppointmentRequestClient
) {

    callbackQuery("appointment_req") {

        if (isSameCallbackQueryDataUrl("appointment_req", callbackQuery.data)) {

            val userRequest = userClient.getUser(callbackQuery.message!!.chat.id, TELEGRAM.type).execute()

            if (userRequest.isSuccessful && userRequest.body() != null) {

                val user = userRequest.body()!!

                appointmentRequestCreateDto.userId = user.id!!

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Выберите питомца",
                    replyMarkup = listKeyboard(
                        "appointment_req_1",
                        user.pets.map(PetDto::toNamedEntityDto).toMutableList()
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
    }

    callbackQuery("appointment_req_1") {

        if (isSameCallbackQueryDataUrl("appointment_req_1", callbackQuery.data)) {

            appointmentRequestCreateDto.petId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

            val user = userClient.getUser(appointmentRequestCreateDto.userId, TABLE.type).execute().body()!!
            val clinicsRequest = appointmentRequestClient.findAllClinics(user.region!!.code).execute()

            if (clinicsRequest.isSuccessful) {

                val clinics = clinicsRequest.body()!!.content

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Выберите клинику",
                    replyMarkup = listKeyboard(
                        "appointment_req_1_conf",
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
    }


    callbackQuery("appointment_req_1_conf") {

        if (isSameCallbackQueryDataUrl("appointment_req_1_conf", callbackQuery.data)) {

            appointmentRequestCreateDto.clinicId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

            val clinicRequest = appointmentRequestClient.findClinic(appointmentRequestCreateDto.clinicId).execute()

            if (clinicRequest.isSuccessful) {

                val clinic = clinicRequest.body()!!

                val confirmationKeyboard = InlineKeyboardMarkup.create(
                    listOf(
                        listOf(
                            InlineKeyboardButton.CallbackData("Далее", "appointment_req_2?id=${clinic.id}"),
                            InlineKeyboardButton.CallbackData(
                                "Назад",
                                "appointment_req_1?id=${appointmentRequestCreateDto.petId}"
                            )
                        )
                    )
                )

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = """
                    Вы хотите выбрать данную поликлинику?
                    Название: ${clinic.name}
                    Адрес: ${clinic.address}
                    """.trimIndent(),
                    replyMarkup = confirmationKeyboard
                )
            }
        }
    }

    callbackQuery("appointment_req_2") {

        if (isSameCallbackQueryDataUrl("appointment_req_2", callbackQuery.data)) {

            appointmentRequestCreateDto.clinicId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

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
    }

    callbackQuery("appointment_req_3") {

        if (isSameCallbackQueryDataUrl("appointment_req_3", callbackQuery.data)) {

            appointmentRequestCreateDto.appointmentPlace = getUrlParams(callbackQuery.data)["place"]!!

            val clinicRequest = appointmentRequestClient.findClinic(appointmentRequestCreateDto.clinicId).execute()

            if (clinicRequest.isSuccessful) {

                val clinic = clinicRequest.body()!!

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Выберите необходимую услугу",
                    replyMarkup = listKeyboard(
                        "appointment_req_3_conf",
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
    }

    callbackQuery("appointment_req_3_conf") {

        if (isSameCallbackQueryDataUrl("appointment_req_3_conf", callbackQuery.data)) {

            appointmentRequestCreateDto.serviceId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

            val serviceRequest = appointmentRequestClient.findService(appointmentRequestCreateDto.serviceId).execute()

            if (serviceRequest.isSuccessful) {

                val service = serviceRequest.body()!!

                val confirmationKeyboard = InlineKeyboardMarkup.create(
                    listOf(
                        listOf(
                            InlineKeyboardButton.CallbackData("Далее", "appointment_req_4?id=${service.id}"),
                            InlineKeyboardButton.CallbackData(
                                "Назад",
                                "appointment_req_3?place=${appointmentRequestCreateDto.appointmentPlace}"
                            )
                        )
                    )
                )

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = """
                    Вы хотите выбрать данную услугу?
                    Название: ${service.name}
                    Описание: ${service.description}
                    Стоимость: ${service.cost}
                    """.trimIndent(),
                    replyMarkup = confirmationKeyboard
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
    }

    callbackQuery("appointment_req_4") {

        if (isSameCallbackQueryDataUrl("appointment_req_4", callbackQuery.data)) {

            appointmentRequestCreateDto.serviceId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

            val veterinariansRequest = appointmentRequestClient
                .findAllVeterinariansByClinicId(appointmentRequestCreateDto.clinicId)
                .execute()

            if (veterinariansRequest.isSuccessful) {

                val veterinarians = veterinariansRequest.body()!!.content

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = "Выберите ветеринара",
                    replyMarkup = listKeyboard(
                        "appointment_req_4_conf",
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
    }

    callbackQuery("appointment_req_4_conf") {

        if (isSameCallbackQueryDataUrl("appointment_req_4_conf", callbackQuery.data)) {

            appointmentRequestCreateDto.veterinarianId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

            val veterinarianRequest =
                appointmentRequestClient.findVeterinarian(appointmentRequestCreateDto.veterinarianId).execute()

            if (veterinarianRequest.isSuccessful) {

                val veterinarian = veterinarianRequest.body()!!

                val confirmationKeyboard = InlineKeyboardMarkup.create(
                    listOf(
                        listOf(
                            InlineKeyboardButton.CallbackData("Далее", "appointment_req_5?id=${veterinarian.id}"),
                            InlineKeyboardButton.CallbackData(
                                "Назад",
                                "appointment_req_4?id=${appointmentRequestCreateDto.serviceId}"
                            )
                        )
                    )
                )

                bot.editMessageText(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    callbackQuery.message!!.messageId,
                    text = """
                    Вы хотите выбрать данного ветеринара?
                    ФИО: ${veterinarian.getSNP()}
                    Опыт работы (годы): ${veterinarian.experience}
                    """.trimIndent(),
                    replyMarkup = confirmationKeyboard
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
    }

    callbackQuery("appointment_req_5") {

        if (isSameCallbackQueryDataUrl("appointment_req_5", callbackQuery.data)) {

            appointmentRequestCreateDto.veterinarianId = getUrlParams(callbackQuery.data)["id"]!!.toLong()

            // Расписание на две недели вперёд
            val dates = getDates(2)

            val inlineTable = mutableListOf<List<InlineKeyboardButton.CallbackData>>()
            var row = mutableListOf<InlineKeyboardButton.CallbackData>()

            for ((k, date) in dates.withIndex()) {

                val formatDate = date.format(ofPattern("dd.MM.yyyy"))

                row.add(
                    InlineKeyboardButton.CallbackData(
                        "${getRusDayName(date.dayOfWeek)}, $formatDate", "appointment_req_6?date=${date}"
                    )
                )

                if (k % 2 == 0) {
                    inlineTable.add(row.toList())
                    row = mutableListOf()
                }
            }

            val keyboard = InlineKeyboardMarkup.create(inlineTable)

            bot.editMessageText(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                callbackQuery.message!!.messageId,
                text = "Выберите дату посещения",
                replyMarkup = keyboard
            )
        }
    }

    callbackQuery("appointment_req_6") {

        if (isSameCallbackQueryDataUrl("appointment_req_6", callbackQuery.data)) {

            appointmentRequestCreateDto.date = getUrlParams(callbackQuery.data)["date"]!!

            val timesRequest = appointmentRequestClient
                .findAllowTimePeriodsByVeterinarianIdAndDate(
                    appointmentRequestCreateDto.veterinarianId, appointmentRequestCreateDto.date
                )
                .execute()

            if (timesRequest.isSuccessful) {

                val times = timesRequest.body()!!

                if (times.isNotEmpty()) {

                    val inlineTable = mutableListOf<List<InlineKeyboardButton.CallbackData>>()
                    var row = mutableListOf<InlineKeyboardButton.CallbackData>()

                    for ((k, time) in times.withIndex()) {

                        row.add(
                            InlineKeyboardButton.CallbackData(
                                time.start.removeSuffix(":00"), "appointment_req_7?time=${time.start}"
                            )
                        )

                        if (k % 2 == 0 && k != 0) {
                            inlineTable.add(row.toList())
                            row = mutableListOf()
                        }
                    }

                    if (row.isNotEmpty()) inlineTable.add(row)

                    val keyboard = InlineKeyboardMarkup.create(inlineTable)

                    bot.editMessageText(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        callbackQuery.message!!.messageId,
                        text = "Выберите время посещения",
                        replyMarkup = keyboard
                    )

                } else {

                    bot.editMessageText(
                        ChatId.fromId(callbackQuery.message!!.chat.id),
                        callbackQuery.message!!.messageId,
                        text = "Для выбранного дня нет расписания",
                        replyMarkup = mainMenuKeyboard
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

    callbackQuery("appointment_req_7") {

        if (isSameCallbackQueryDataUrl("appointment_req_7", callbackQuery.data)) {

            appointmentRequestCreateDto.date += "T" + getUrlParams(callbackQuery.data)["time"]!!

            val response = appointmentRequestClient.createRequest(appointmentRequestCreateDto).execute()

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

            if (user.pets.isNotEmpty()) {

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

private fun Dispatcher.startCommand(userClient: UserClient) {

    command("start") {

        val isUserExists = userClient.existByTelegramId(message.chat.id, TELEGRAM.type).execute().body()!!

        if (!isUserExists) {

            bot.sendMessage(ChatId.fromId(message.chat.id), startMessage)

            val regionsRequest = userClient.getRegions().execute()

            if (regionsRequest.isSuccessful) {

                val regions = regionsRequest.body()!!

                val namedEntityRegions = regions
                    .map { region -> NamedEntityDto(region.code, region.name) }
                    .toMutableList()

                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    "Выберите ваш регион",
                    replyMarkup = regionsKeyboard("regions", namedEntityRegions)
                )
            }

        } else {

            bot.sendMessage(
                ChatId.fromId(message.chat.id),
                "Здравствуйте, ${message.chat.firstName}! Чтобы перейти к главному меню воспользуйтесь командой /main. " +
                        "Если у вас возникли какие-то проблемы, то воспользуйтесь командой /help"
            )
        }
    }

    callbackQuery("regions") {

        val regionCode = getUrlParams(callbackQuery.data)["id"]!!.toLong()

        val regionRequest = userClient.getRegion(regionCode).execute()

        if (regionRequest.isSuccessful) {

            val region = regionRequest.body()!!

            val userDto = UserDto(
                callbackQuery.message!!.chat.firstName!!,
                callbackQuery.message!!.chat.username,
                callbackQuery.message!!.chat.id,
                region
            )

            val userSaveRequest = userClient.save(userDto).execute()

            if (userSaveRequest.isSuccessful) {

                bot.sendMessage(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    "Вы успешно авторизовались!",
                    replyMarkup = mainMenuKeyboard
                )
            }
        }
    }
}
