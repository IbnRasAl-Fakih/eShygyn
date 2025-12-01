(ns eshygyn.tg.messages
  (:require [telegrambot-lib.core :as tg]

            [eshygyn.tg.new-expense :as new-expense]))

(defn authorize [bot chat-id]
  (tg/send-message bot chat-id
                   "🚫 Вы не авторизованы!\nПожалуйста, сначала выполните авторизацию, чтобы использовать бота.\n\nВведите команду /authorize или нажмите кнопку «Авторизоваться»"
                   {:reply_markup {:inline_keyboard [[{:text "Авторизоваться" :callback_data "CMD_AUTHORIZE"}]] :resize_keyboard true}}))

(defn cancel [bot chat-id]
  (tg/send-message bot chat-id "❌ Действие отменено. Если понадобится — можете начать заново."))

(defn next-amount [bot chat-id category]
  (tg/send-message bot chat-id
                   (str "Введите сумму для категории «" category "»\n\nЕсли хотите выбрать другую категорию — нажмите кнопку «Изменить категорию» или введите команду /change")
                   {:reply_markup {:inline_keyboard [[{:text "Изменить категорию" :callback_data "CMD_CHANGE_CATEGORY"}]] :resize_keyboard true}}))

(defn expense-created [bot chat-id category amount date]
  (tg/send-message bot chat-id
                   (format "✅ Расход добавлен:\n\n• Категория: %s\n• Сумма: %s\n• Время: %s" category amount (.format date new-expense/fmt-out))))

(defn unknown-command [bot chat-id command]
  (tg/send-message bot chat-id (str "❓ Неизвестная команда: " command "\n\nПопробуй /help, чтобы увидеть, что я умею")))

(defn unknown-message [bot chat-id]
  (tg/send-message bot chat-id "ℹ️ Чтобы добавить расход, используйте команду /add"))

(defn wrong-amount [bot chat-id]
  (tg/send-message bot chat-id "⚠️ Некорректная сумма. Пример: 1200 или 1 499,50"))

(defn next-time [bot chat-id]
  (tg/send-message bot chat-id
                   "🕒 Теперь укажите время:\n\n• Нажмите кнопку «Текущее время» нижe\n• Или введите вручную в формате dd.mm.yy hh:mm — например, 14.06.04 03:32"
                   {:reply_markup (new-expense/time-kb)}))

(defn wrong-time [bot chat-id]
  (tg/send-message bot chat-id "⚠️ Неверный формат времени. Используйте формат dd.mm.yy hh:mm — например, 14.06.04 03:32"
                   {:reply_markup (new-expense/time-kb)}))

(defn unknown-message-with-stage [bot chat-id]
  (tg/send-message bot chat-id "🤔 Я вас не понял. Нажмите /cancel и начните заново с /add"))

;; TODO: нужно изменить текст когда все будет готово 
(defn start [bot chat-id]
  (tg/send-message bot chat-id "Привет! 👋 Я твой Expense Tracker бот. Отправь мне любое сообщение."))

(defn already-authorized [bot chat-id]
  (tg/send-message bot chat-id
                   "ℹ️ Вы уже авторизованы!\nНет необходимости проходить авторизацию повторно ✅\n\nВы можете сразу перейти к работе:\n• 📊 Просмотреть расходы — /stats\n• ➕ Добавить новую операцию — /add\n• ⚙️ Выйти из аккаунта — /logout"
                   {:reply_markup {:remove_keyboard true}}))

(defn successfully-authorized [bot chat-id first-name]
  (tg/send-message bot chat-id
                   (str "✅ Вы успешно авторизовались!\nДобро пожаловать, " first-name "\n\nТеперь вы можете:\n• 📊 Просматривать свои расходы\n• ➕ Добавлять новые транзакции\n• 📅 Смотреть статистику по дням и категориям\n\nВведите /help, чтобы увидеть доступные команды.")))

(defn authorize-error [bot chat-id]
  (tg/send-message bot chat-id
                   "⚠️ Не удалось авторизоваться.\n\n• Попробуйте перезапустить бота.\n• Или введите команду /start для повторной попытки."))

(defn next-category [bot chat-id categories-kb]
  (tg/send-message bot chat-id
                   "Добавление нового расхода\nВыберите категорию из списка ниже 👇"
                   {:reply_markup categories-kb}))

(defn change-category [bot chat-id categories-kb]
  (tg/send-message bot chat-id
                   "Изменение категории\nВыберите новую категорию из списка ниже 👇"
                   {:reply_markup categories-kb}))

(defn next-category-id [bot chat-id]
  (tg/send-message bot chat-id
                   "Введите, пожалуйста, уникальный ID категории.\nИспользуйте только латинские буквы, без пробелов.\nЭтот ID служит для внутренних целей и не будет показываться в интерфейсе."
                   {:reply_markup {:inline_keyboard [[{:text "Отмена" :callback_data "CMD_CANCEL"}]] :resize_keyboard true}}))

(defn is-not-unique-category-id [bot chat-id]
  (tg/send-message bot chat-id
                   "Категория с таким ID уже существует.\nПожалуйста, введите другой уникальный ID."
                   {:reply_markup {:inline_keyboard [[{:text "Отмена" :callback_data "CMD_CANCEL"}]] :resize_keyboard true}}))

(defn invalid-category-id [bot chat-id]
  (tg/send-message bot chat-id
                   "Невалидный ID категории.\nВведите ID латинскими буквами, без пробелов."
                   {:reply_markup {:inline_keyboard [[{:text "Отмена" :callback_data "CMD_CANCEL"}]] :resize_keyboard true}}))

(defn next-category-title [bot chat-id]
  (tg/send-message bot chat-id "ID сохранён. Отправьте название категории."
                   {:reply_markup {:inline_keyboard [[{:text "Отмена" :callback_data "CMD_CANCEL"}]] :resize_keyboard true}}))

(defn next-category-emoji [bot chat-id]
  (tg/send-message bot chat-id
                   "Отлично! Теперь можете прислать стикер для категории.\nЕсли хотите пропустить этот шаг — отправьте /skip"
                   {:reply_markup {:inline_keyboard [[{:text "Пропустить" :callback_data "CMD_SKIP"}]
                                                     [{:text "Отмена" :callback_data "CMD_CANCEL"}]]
                                   :resize_keyboard true}}))

(defn category-created [bot chat-id title emoji]
  (tg/send-message bot chat-id
                   (str "Категория " emoji " " title " добавлена! Можете использовать её для новых расходов.")))

(defn edit-category [bot chat-id categories]
  (tg/send-message bot chat-id
                   "Пожалуйста, выберите категорию, которую хотите редактировать."
                   {:reply_markup categories}))

(defn choose-category-part-to-edit [bot chat-id title emoji]
  (tg/send-message bot chat-id
                   (str "Для категории «" emoji " " title "» что хотите изменить — название или стикер?")
                   {:reply_markup {:inline_keyboard [[{:text "Название" :callback_data "CMD_EDIT_TITLE"}]
                                                     [{:text "Стикер" :callback_data "CMD_EDIT_EMOJI"}]
                                                     [{:text "Сохранить изменение" :callback_data "CMD_SAVE_CHANGES"}]
                                                     [{:text "Отменить редактирование" :callback_data "CMD_CANCEL"}]]
                                   :resize_keyboard true}}))

(defn edit-category-title [bot chat-id]
  (tg/send-message bot chat-id "Введите новое название категории."))

(defn edit-category-loop [bot chat-id]
  (tg/send-message bot chat-id
                   "Отлично! Категория подправлена.\nХотите ещё что-то изменить или уже сохранить результат?"
                   {:reply_markup {:inline_keyboard [[{:text "Название" :callback_data "CMD_EDIT_TITLE"}]
                                                     [{:text "Стикер" :callback_data "CMD_EDIT_EMOJI"}]
                                                     [{:text "Сохранить изменение" :callback_data "CMD_SAVE_CHANGES"}]
                                                     [{:text "Отменить редактирование" :callback_data "CMD_CANCEL"}]]
                                   :resize_keyboard true}}))

(defn category-edited [bot chat-id]
  (tg/send-message bot chat-id "Редактирование завершено. Категория успешно обновлена."))

(defn edit-category-emoji [bot chat-id]
  (tg/send-message bot chat-id "Отправьте новый стикер для категории."))

(defn delete-category [bot chat-id categories]
  (tg/send-message bot chat-id
                   "Пожалуйста, выберите категорию, которую хотите удалить."
                   {:reply_markup categories}))

(defn is-delete-expenses [bot chat-id title emoji]
  (tg/send-message bot chat-id
                   (str "Категория " emoji " " title " выбрана.\nХотите также удалить все расходы, связанные с этой категорией?")
                   {:reply_markup {:inline_keyboard [[{:text "Удалить" :callback_data "CMD_YES"}]
                                                     [{:text "Оставить" :callback_data "CMD_NO"}]
                                                     [{:text "Отменить удаление категорий" :callback_data "CMD_CANCEL"}]]
                                   :resize_keyboard true}}))

(defn is-sure [bot chat-id]
  (tg/send-message bot chat-id
                   "Вы уверены? После удаления вернуть категорию не получится."
                   {:reply_markup {:inline_keyboard [[{:text "Уверен" :callback_data "CMD_YES"}]
                                                     [{:text "Отменить удаление категорий" :callback_data "CMD_CANCEL"}]]
                                   :resize_keyboard true}}))

(defn category-deleted [bot chat-id]
  (tg/send-message bot chat-id "Категория успешно удалена."))