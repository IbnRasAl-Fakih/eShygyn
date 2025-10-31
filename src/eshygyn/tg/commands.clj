(ns eshygyn.tg.commands
  (:require [telegrambot-lib.core :as tg]
            
            [eshygyn.tg.new-expense :as new-expense]
            [eshygyn.db.db :as db]))

(defn start [bot chat-id]
  (tg/send-message bot chat-id "Привет! 👋 Я твой Expense Tracker бот. Отправь мне любое сообщение."))

(defn authorize [bot chat-id first-name username]
  (try
    (if (db/is-authorized chat-id)
      (tg/send-message bot chat-id "ℹ️ Вы уже авторизованы!\nНет необходимости проходить авторизацию повторно ✅\n\nВы можете сразу перейти к работе:\n• 📊 Просмотреть расходы — /stats\n• ➕ Добавить новую операцию — /add\n• ⚙️ Выйти из аккаунта — /logout" {:reply_markup {:remove_keyboard true}})
      
      (do
        (db/create-user chat-id first-name username)
        (tg/send-message bot chat-id (str "✅ Вы успешно авторизовались!\nДобро пожаловать, " first-name "\n\nТеперь вы можете:\n• 📊 Просматривать свои расходы\n• ➕ Добавлять новые транзакции\n• 📅 Смотреть статистику по дням и категориям\n\nВведите /help, чтобы увидеть доступные команды."))))
    
    (catch Exception e
      (println (str "\033[91mERROR\033[0m " "Caught a exception while creating user: " (.getMessage e)))
      (tg/send-message bot chat-id "⚠️ Не удалось авторизоваться.\n\n• Попробуйте перезапустить бота.\n• Или введите команду /start для повторной попытки."))))

(defn cancel [bot chat-id]
  (new-expense/clear-session! chat-id)
  (tg/send-message bot chat-id "❌ Добавление расхода отменено."))