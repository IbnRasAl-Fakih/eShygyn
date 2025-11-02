(ns eshygyn.tg.messages
  (:require [telegrambot-lib.core :as tg]
            
            [eshygyn.tg.new-expense :as new-expense]))

(defn authorize [bot chat-id]
  (tg/send-message bot chat-id 
                   "🚫 Вы не авторизованы!\nПожалуйста, сначала выполните авторизацию, чтобы использовать бота.\n\nВведите команду /authorize или нажмите кнопку «Авторизоваться»" 
                   {:reply_markup {:inline_keyboard [[{:text "Авторизоваться" :callback_data "CMD_AUTHORIZE"}]] :resize_keyboard true}}))

(defn cancel [bot chat-id]
  (tg/send-message bot chat-id "❌ Добавление расхода отменено"))

(defn next-amount [bot chat-id category]
  (tg/send-message bot chat-id
                   (str "Введите сумму для категории «" category "»\n\nЕсли хотите выбрать другую категорию — нажмите кнопку «Изменить категорию» или введите команду /change")
                   {:reply_markup {:inline_keyboard [[{:text "Изменить категорию" :callback_data "CMD_CHANGE_CATEGORY"}]] :resize_keyboard true}}))

(defn expense_created [bot chat-id category amount date]
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
  (tg/send-message bot chat-id "⚠️ Неверный формат времени. Используйте формат dd.mm.yy hh:mm — например, 14.06.04 03:32"))

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