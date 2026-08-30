package com.example.smartguard.ui.academy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartguard.R
import com.google.android.material.card.MaterialCardView

class ModuleDetailActivity : AppCompatActivity() {

    private lateinit var moduleId: String
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_detail)

        val title = intent.getStringExtra("module_title") ?: "Модуль"
        moduleId = intent.getStringExtra("module_id") ?: "pause_master"
        val description = intent.getStringExtra("module_description") ?: ""

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        container = findViewById(R.id.containerLessons)

        findViewById<TextView>(R.id.tvModuleTitle).text = title
        findViewById<TextView>(R.id.tvModuleDescription).text = description

        when (moduleId) {
            "pause_master" -> loadPauseMasterModule()
            "digital_keys" -> loadDigitalKeysModule()
            "scam_encyclopedia" -> {
                // Запускаем Энциклопедию как отдельную активность
                startActivity(Intent(this, EncyclopediaActivity::class.java))
                finish() // закрываем ModuleDetailActivity, чтобы не оставаться на пустом экране
            }
            else -> showComingSoon()
        }
    }

    // ==================== МОДУЛЬ 1: МАСТЕР ПАУЗЫ ====================

    private fun loadPauseMasterModule() {
        addLessonCard(
            "Урок 1. Арсенал мошенника",
            "Как мошенники манипулируют нашими эмоциями: страх, срочность, авторитет.",
            listOf(
                "🚩 Торопят и заставляют действовать сразу",
                "🚩 Давят авторитетом (полиция, банк, прокуратура)",
                "🚩 Запугивают: «ваши деньги крадут», «вам грозит уголовное дело»",
                "🚩 Дискредитируют банк: «не верьте, если перезвоните — они в сговоре»",
                "✅ Настоящие сотрудники банка и полиции никогда не просят коды из SMS и не требуют срочных переводов."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "pause_1")
                putExtra("lesson_title", "Урок 1. Арсенал мошенника")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 2. Техника «Пауза»",
            "Как взять паузу, чтобы мошенник не помешал.",
            listOf(
                "📞 Скажите: «Плохо слышно, сейчас найду наушники»",
                "📞 Скажите: «Минуту, мне звонят в дверь»",
                "📞 Скажите: «Подождите, я за рулём, сейчас остановлюсь»",
                "💡 Главное — выиграть 30–60 секунд, чтобы успокоиться и включить голову."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "pause_2")
                putExtra("lesson_title", "Урок 2. Техника «Пауза»")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 3. Красные флаги",
            "Что никогда не спросит настоящий сотрудник банка или полиции.",
            listOf(
                "🔐 Код из SMS или push-уведомления",
                "🔐 Три цифры с обратной стороны карты (CVV/CVC)",
                "🔐 ПИН-код от карты",
                "🔐 Пароль от личного кабинета",
                "✅ Настоящий сотрудник банка знает ваши ФИО и последние 4 цифры карты, но никогда не запрашивает секретные данные."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "pause_3")
                putExtra("lesson_title", "Урок 3. Красные флаги")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 4. Ритуал спокойствия",
            "Что делать во время паузы, чтобы успокоиться и принять верное решение.",
            listOf(
                "🧘 Сделайте 3 глубоких вдоха и выдоха",
                "💧 Выпейте стакан воды",
                "🧼 Умойтесь холодной водой",
                "🔢 Медленно сосчитайте до 10",
                "💡 Выберите свой ритуал и используйте его каждый раз."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "pause_4")
                putExtra("lesson_title", "Урок 4. Ритуал спокойствия")
            }
            startActivity(intent)
        }

        val btnTest = Button(this).apply {
            text = "Пройти тест «Мастер паузы»"
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setOnClickListener { showPauseMasterTest() }
        }
        container.addView(btnTest)
    }

    private fun showPauseMasterTest() {
        val questions = listOf(
            TestQuestion(
                question = "Вам звонят: «Это служба безопасности Kaspi. Мы видим подозрительный вход в ваш аккаунт. Назовите код из SMS, чтобы мы отменили вход». Ваши действия?",
                options = listOf(
                    "Назову код, чтобы спасти деньги",
                    "Спрошу, из какого они отдела, и попрошу представиться",
                    "Положу трубку и позвоню в Kaspi по номеру с обратной стороны карты"
                ),
                explanations = listOf(
                    "❌ Код из SMS нельзя сообщать никому. Это даст мошенникам доступ к вашему аккаунту.",
                    "❌ Мошенник легко назовёт любой отдел. Лучше сразу завершить разговор и перезвонить по официальному номеру.",
                    "✅ Единственно правильное действие. Настоящий сотрудник банка никогда не попросит код из SMS."
                ),
                correctAnswerIndex = 2
            ),
            TestQuestion(
                question = "Вам пишет «руководитель» в WhatsApp: «Срочно! Со мной свяжется капитан Петров из МВД. Он задаст пару вопросов по нашей компании. Ответь чётко, ничего не бойся». Через минуту звонок. Вы:",
                options = listOf(
                    "Беру трубку и отвечаю на вопросы, начальник плохого не посоветует",
                    "Вешаю трубку и перезваниваю руководителю по известному мне номеру",
                    "Слушаю, что скажет «капитан», но ничего не обещаю"
                ),
                explanations = listOf(
                    "❌ Аккаунт руководителя могли взломать. Настоящий начальник не стал бы так рисковать вашими деньгами.",
                    "✅ Верное решение. Только личный звонок по известному номеру гарантирует, что вы говорите с реальным человеком.",
                    "❌ Мошенники — отличные манипуляторы. Лучше не вступать в диалог и сразу проверить информацию."
                ),
                correctAnswerIndex = 1
            ),
            TestQuestion(
                question = "Вы поняли, что говорите с мошенником. Как лучше завершить разговор?",
                options = listOf(
                    "Вежливо попрощаться и положить трубку",
                    "Просто положить трубку, не прощаясь",
                    "Сказать, что я всё понял, и положить трубку"
                ),
                explanations = listOf(
                    "❌ Пока будете прощаться, мошенник может найти новые аргументы и переубедить вас.",
                    "✅ Лучший способ. Не давайте мошеннику шанса продолжить манипуляцию.",
                    "❌ Не стоит давать обратную связь. Просто прервите разговор."
                ),
                correctAnswerIndex = 1
            ),
            TestQuestion(
                question = "Вам пришло голосовое сообщение от друга: «Слушай, выручай, срочно нужны 50 000 тенге, переведи на карту 4400 **** **** 1234». Голос точно его. Ваши действия?",
                options = listOf(
                    "Сразу переведу, друг в беде",
                    "Позвоню другу по телефону и уточню, действительно ли он просит деньги",
                    "Отвечу сообщением, что перевёл, чтобы успокоить, а сам подумаю"
                ),
                explanations = listOf(
                    "❌ Голос можно подделать нейросетью. Всегда проверяйте просьбы о деньгах личным звонком.",
                    "✅ Единственный надёжный способ. Позвоните и убедитесь, что друг действительно в беде.",
                    "❌ Не вводите друга в заблуждение. Лучше сразу прояснить ситуацию."
                ),
                correctAnswerIndex = 1
            )
        )

        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra("test_title", "Тест: Мастер паузы")
            putExtra("questions", ArrayList(questions))
        }
        startActivity(intent)
    }

    // ==================== МОДУЛЬ 2: ЦИФРОВЫЕ КЛЮЧИ ====================

    private fun loadDigitalKeysModule() {
        addLessonCard(
            "Урок 1. Крепость «Почта»",
            "Почему электронная почта — самое уязвимое место и как её защитить в первую очередь.",
            listOf(
                "📧 Через взломанную почту хакер может сбросить пароли от соцсетей, банков, облачных хранилищ.",
                "📧 В почте хранятся сканы паспортов, данные карт, переписка с банком.",
                "🔐 Включите двухфакторную аутентификацию для почты (код из SMS или приложения).",
                "🔐 Используйте уникальный и длинный пароль только для почты."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "keys_1")
                putExtra("lesson_title", "Урок 1. Крепость «Почта»")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 2. Непробиваемый пароль",
            "Как создать надёжный пароль и легко его запомнить.",
            listOf(
                "🔑 Длина: минимум 14 символов.",
                "🔑 Уникальность: для каждого сервиса свой пароль.",
                "💡 Метод ярких образов: придумайте фразу, например, «PelmeshkiSoSmetankoy», и представьте её визуально.",
                "💡 Алгоритм модификации: добавьте к базовой фразе первые буквы сайта (например, для Kaspi — «Ka»)."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "keys_2")
                putExtra("lesson_title", "Урок 2. Непробиваемый пароль")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 3. Парольный сейф",
            "Что такое менеджеры паролей и почему ими нужно пользоваться.",
            listOf(
                "🗝️ Менеджер паролей (LastPass, 1Password, встроенный в браузер) запоминает все пароли за вас.",
                "🗝️ Он проверяет адрес сайта и не подставит пароль на фишинговой странице.",
                "🔒 Все пароли хранятся в зашифрованном виде.",
                "💡 Достаточно запомнить только один мастер-пароль."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "keys_3")
                putExtra("lesson_title", "Урок 3. Парольный сейф")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 4. Второй ключ (2FA)",
            "Двухфакторная аутентификация — ваш главный щит.",
            listOf(
                "📱 2FA требует ввести код из SMS или приложения даже после ввода пароля.",
                "📱 Если хакер украдёт пароль, без доступа к вашему телефону он не войдёт.",
                "✅ Включите 2FA для почты, соцсетей, eGov.kz и банковских приложений."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "keys_4")
                putExtra("lesson_title", "Урок 4. Второй ключ (2FA)")
            }
            startActivity(intent)
        }

        addLessonCard(
            "Урок 5. Фишинговая рыбалка",
            "Как распознать поддельные сайты и ссылки.",
            listOf(
                "🎣 Проверяйте адресную строку: мошенники используют похожие домены (например, kaspi-kz.com вместо kaspi.kz).",
                "🎣 Не переходите по коротким ссылкам (bit.ly и т.п.) от незнакомцев.",
                "🎣 Менеджер паролей не предложит заполнить пароль на поддельном сайте — это верный признак обмана.",
                "📵 Если сайт просит ввести логин и пароль от другого сервиса (например, от почты), это фишинг."
            )
        ) {
            val intent = Intent(this, LessonActivity::class.java).apply {
                putExtra("lesson_id", "keys_5")
                putExtra("lesson_title", "Урок 5. Фишинговая рыбалка")
            }
            startActivity(intent)
        }

        val btnTest = Button(this).apply {
            text = "Пройти тест «Цифровые ключи»"
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setOnClickListener { showDigitalKeysTest() }
        }
        container.addView(btnTest)
    }

    private fun showDigitalKeysTest() {
        val questions = listOf(
            TestQuestion(
                question = "Какой пароль нужно сделать надежным в первую очередь?",
                options = listOf(
                    "От почты, остальные можно восстановить",
                    "От iCloud и Dropbox, там личные фото",
                    "От Apple Pay и Google Pay, там карты",
                    "Надёжными должны быть все пароли"
                ),
                explanations = listOf(
                    "❌ Пароль от почты очень важен, но если другие пароли слабые, злоумышленник может добраться и до почты.",
                    "❌ Облачные хранилища важны, но проблема шире — все пароли должны быть надёжными.",
                    "❌ Платёжные сервисы тоже важны, но парольная гигиена нужна везде.",
                    "✅ Верно! Получив доступ к одному сервису, хакер может найти данные для взлома других, включая банк и соцсети."
                ),
                correctAnswerIndex = 3,
                imageName = ""
            ),
            TestQuestion(
                question = "Вам нужно придумать надёжные пароли для всех сервисов. Как лучше всего это сделать?",
                options = listOf(
                    "Взять один любимый пароль и везде его использовать",
                    "Выбрать несколько паролей и сохранить их в заметках",
                    "Придумать один очень сложный пароль и везде его использовать",
                    "Для каждого аккаунта придумать отдельный сложный пароль"
                ),
                explanations = listOf(
                    "❌ Если база одного сайта утечёт, хакер получит доступ ко всем остальным аккаунтам.",
                    "❌ Хранить пароли в незашифрованных заметках опасно — любой вирус или физический доступ к телефону их украдёт.",
                    "❌ Сложность не спасает от утечки. Если пароль один, его кража откроет всё.",
                    "✅ Можно использовать парольную фразу и модифицировать её под каждый сайт, либо пользоваться менеджером паролей."
                ),
                correctAnswerIndex = 3,
                imageName = ""
            ),
            TestQuestion(
                question = "Ваш почтовый сервис (Gmail, Mail.kz) предлагает указать номер телефона. Стоит ли это сделать?",
                options = listOf(
                    "Не стоит, подпишут на платные SMS-рассылки",
                    "Стоит указать — так быстрее восстановить пароль",
                    "Стоит, это защитит почту кодом из SMS при входе"
                ),
                explanations = listOf(
                    "❌ Обычно сервисы не подписывают на рассылки за указание номера. Это нужно для безопасности.",
                    "❌ Восстановление пароля — это удобно, но главная цель — двухфакторная аутентификация (2FA).",
                    "✅ Почтовый сервис запрашивает номер, чтобы защитить ваши данные. Даже если хакер подберёт пароль, ему понадобится код из SMS, который есть только у вас."
                ),
                correctAnswerIndex = 2,
                imageName = ""
            ),
            TestQuestion(
                question = "Ивану в Telegram приходит сообщение от «Службы поддержки» о блокировке аккаунта и ссылка для верификации. В ссылке указано его имя. Что делать?",
                options = listOf(
                    "Пройти верификацию, чтобы разблокировать аккаунт",
                    "Закрыть сайт и удалить сообщение"
                ),
                explanations = listOf(
                    "❌ Это мошенники. Они создали фейковый аккаунт, похожий на сервисный, и прислали ссылку на поддельный сайт, где украдут ваш номер и код.",
                    "✅ Не переходите по неизвестным ссылкам. Если перешли — не вводите данные. Пользуйтесь только официальными приложениями мессенджеров."
                ),
                correctAnswerIndex = 1,
                imageName = "test_telegram_fake.jpg"
            ),
            TestQuestion(
                question = "Нина узнала из Telegram-канала, что появилось приложение «Все банки Казахстана», дающее доступ сразу ко всем её счетам (Kaspi, Halyk, Forte). Стоит ли устанавливать?",
                options = listOf(
                    "Да, выглядит удобно",
                    "Нет, ни в коем случае"
                ),
                explanations = listOf(
                    "❌ Загружать приложения, связанные с деньгами, можно только из официальных магазинов (App Store, Google Play) или с сайта банка.",
                    "✅ Такое приложение запросит логины и пароли от всех банков, перехватит SMS и поможет мошенникам украсть деньги. Если приложения нет в официальном магазине — это обман."
                ),
                correctAnswerIndex = 1,
                imageName = "test_fake_bank_app.jpg"
            ),
            TestQuestion(
                question = "Даниилу пришло SMS: «Даниил, Есения поделилась с Вами фото — bit.ly/2FTbqio». Он не знает Есению. Стоит ли переходить по ссылке?",
                options = listOf(
                    "Лучше удалить сообщение",
                    "Почему бы и нет, вдруг старая знакомая"
                ),
                explanations = listOf(
                    "✅ Ссылка на bit.ly может вести на фишинговый сайт, который украдёт пароли, оформит платные подписки или заразит телефон вирусом.",
                    "❌ Это мошенники. Они массово рассылают такие SMS в надежде, что кто-то клюнет. Никогда не переходите по ссылкам от незнакомцев."
                ),
                correctAnswerIndex = 0,
                imageName = "test_sms_photo.jpg"
            ),
            TestQuestion(
                question = "Знакомый прислал ссылку на распродажу iPhone. Сайт выглядит как Apple, есть «замочек» (SSL). Чтобы увидеть цены, нужно ввести Apple ID и пароль. Введёте?",
                options = listOf(
                    "Да, адрес apple.com и зелёный значок",
                    "Нет, зачем Apple мой пароль для просмотра"
                ),
                explanations = listOf(
                    "❌ Мошенники могут зарегистрировать домен с кириллическими буквами, похожими на латиницу (например, аррӏе.com). Сайт будет выглядеть как настоящий, но украдёт ваш пароль.",
                    "✅ Настоящий Apple не просит вводить пароль просто для просмотра страницы. Всегда проверяйте адресную строку и не вводите пароль на подозрительных сайтах."
                ),
                correctAnswerIndex = 1,
                imageName = "test_apple_fake.jpg"
            ),
            TestQuestion(
                question = "Друзья делают репост в соцсетях: «Air Astana дарит бесплатные билеты! Перейдите по ссылке». Что будете делать?",
                options = listOf(
                    "Перейду, друзья плохого не посоветуют",
                    "Не перейду, кажется, друзей взломали"
                ),
                explanations = listOf(
                    "❌ Мошенники создали фейковый сайт, похожий на сайт авиакомпании, и взломали аккаунты ваших друзей, чтобы распространять ссылку.",
                    "✅ На таком сайте вас попросят сделать репост или ввести пароль, а затем украдут данные. Проверяйте акции только на официальном сайте авиакомпании."
                ),
                correctAnswerIndex = 1,
                imageName = "test_airastana_repost.jpg"
            ),
            TestQuestion(
                question = "Анна продаёт кроссовки на Kaspi Объявлениях. Ей приходит SMS: «Готовы купить, ваше? kaspi.aaa4x.ru/1». Это ссылка на официальный сайт или поддельный?",
                options = listOf(
                    "Официальный, в ссылке есть kaspi",
                    "Поддельный, сайт aaa4x.ru"
                ),
                explanations = listOf(
                    "❌ Кто угодно может зарегистрировать домен вида kaspi.что-угодно.kz. Настоящий сайт Kaspi — kaspi.kz, а всё, что левее последней точки — поддомены.",
                    "✅ Настоящий сайт находится перед последней точкой. У kaspi.aaa4x.ru настоящий сайт — aaa4x.ru, а kaspi — лишь поддомен, созданный мошенниками."
                ),
                correctAnswerIndex = 1,
                imageName = "test_kaspi_fake_link.jpg"
            ),
            TestQuestion(
                question = "Покупатель с объявлений хочет перевести деньги. Вы сообщили номер карты. Вам приходит SMS от банка с кодом. Покупатель звонит и просит сказать код — якобы без него его банк не подтвердит перевод. Ваши действия?",
                options = listOf(
                    "Скажу код, SMS же от банка",
                    "Не скажу код, это мошенники"
                ),
                explanations = listOf(
                    "❌ SMS действительно от банка, но спрашивают код мошенники. Они пытаются привязать ваш номер к своему устройству и украсть деньги.",
                    "✅ Никому не говорите коды из SMS. Для перевода на вашу карту достаточно номера карты. Проверяйте поступление денег в приложении банка, а не со слов покупателя."
                ),
                correctAnswerIndex = 1,
                imageName = "test_sms_code_warning.jpg"
            )
        )

        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra("test_title", "Тест: Цифровые ключи")
            putExtra("questions", ArrayList(questions))
        }
        startActivity(intent)
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    private fun addLessonCard(title: String, subtitle: String, items: List<String>, onClick: () -> Unit) {
        val card = MaterialCardView(this).apply {
            radius = 12f
            cardElevation = 4f
            setContentPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
            setOnClickListener { onClick() }
        }

        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        })

        content.addView(TextView(this).apply {
            text = subtitle
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setPadding(0, 4, 0, 12)
        })

        items.forEach { item ->
            content.addView(TextView(this).apply {
                text = item
                textSize = 14f
                setPadding(0, 4, 0, 4)
            })
        }

        card.addView(content)
        container.addView(card)
    }

    private fun showComingSoon() {
        container.addView(TextView(this).apply {
            text = "🚧 Этот модуль находится в разработке. Скоро здесь появятся уроки."
            textSize = 16f
            setPadding(32, 32, 32, 32)
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    data class TestQuestion(
        val question: String,
        val options: List<String>,
        val explanations: List<String>,
        val correctAnswerIndex: Int,
        val imageName: String = ""
    ) : java.io.Serializable
}