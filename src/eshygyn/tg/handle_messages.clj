(ns eshygyn.tg.handle-messages
  (:require [telegrambot-lib.core :as tg]
            
            [eshygyn.db.db :as db]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages]
            [eshygyn.tg.new-expense :as new-expense])
  (:import (java.time.format DateTimeFormatter DateTimeParseException)))

(defn handle-message [bot message]
  (println "\033[34mINFO\033[0m " message) ; delete
  (let [chat-id (get-in message [:chat :id])
        text (:text message)
        user-id (get-in message [:from :id])
        {:keys [stage draft]} (new-expense/get-session chat-id)]
    (if (= text "/authorize")
      (commands/authorize bot chat-id (get-in message [:from :first_name]) (get-in message [:from :username]))

      (if (db/is-authorized chat-id)
        (cond
          (= text "/start") (commands/start bot chat-id)

          (= text "/add") (new-expense/handle-add-cmd bot chat-id) 

          (= text "/cancel") (commands/cancel bot chat-id)

          (nil? stage)
          (tg/send-message bot chat-id "ℹ️ Чтобы добавить расход, используйте команду /add")
          
          (= stage :enter-amount)
          (let [amt (new-expense/parse-amount text)]
            (if (nil? amt)
              (tg/send-message bot chat-id "⚠️ Некорректная сумма. Пример: 1200 или 1 499,50")
              (do
                (new-expense/set-stage! chat-id :enter-time :amount amt)
                (tg/send-message bot chat-id
                                 (str "🕒 Теперь укажите время.\n"
                                      "• Нажмите «Текущее время» ниже\n"
                                      "• Или введите вручную в формате: dd.MM.yy HH:mm (напр. 31.10.25 14:05)\n"
                                      "Также принимается dd.MM.yyyy HH:mm")
                                 :reply_markup (new-expense/time-kb)))))
          
          (= stage :enter-time)
          (let [dt (new-expense/parse-datetime text)]
            (if (nil? dt)
              (tg/send-message bot chat-id "⚠️ Неверный формат времени. Используйте dd.MM.yy HH:mm или dd.MM.yyyy HH:mm.\nНапр.: 31.10.25 14:05")
              (let [{:keys [category amount]} draft]
                (db/create-expence user-id category amount dt)
                (new-expense/clear-session! chat-id)
                (tg/send-message bot chat-id
                                 (format "✅ Расход добавлен:\n• Категория: %s\n• Сумма: %s\n• Время: %s"
                                         category amount (.format dt (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm z")))))))

          :else (tg/send-message bot chat-id "🤔 Я вас не понял. Нажмите /cancel и начните заново с /add"))

        (messages/authorize bot chat-id)))))