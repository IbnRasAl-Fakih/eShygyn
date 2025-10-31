(ns eshygyn.tg.handle-callback-queries
  (:require [telegrambot-lib.core :as tg]
            [clojure.string :as str]
            
            [eshygyn.db.db :as db]
            [eshygyn.tg.new-expense :as new-expense]
            [eshygyn.tg.commands :as commands])
  (:import (java.time ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter DateTimeParseException)))

(def almaty-tz (ZoneId/of "Asia/Almaty"))

(defn handle-callback-query [bot callback-query]
  (println "\033[34mINFO\033[0m " callback-query) ; delete
  (let [callback-id   (:id callback-query)
        data    (:data callback-query)
        chat-id (get-in callback-query [:message :chat :id])
        msg-id  (get-in callback-query [:message :message_id])
        user-id (get-in callback-query [:from :id])]

    (cond
      (= data "CMD_AUTHORIZE")
      (do
        (tg/edit-message-reply-markup bot
                                      {:chat_id chat-id
                                       :message_id msg-id
                                       :reply_markup {:inline_keyboard []}})

        (commands/authorize bot chat-id (get-in callback-query [:from :first_name]) (get-in callback-query [:from :username])))
      
      (= data "CMD_CANCEL") 
      (do
        (new-expense/clear-session! chat-id)
        (tg/answer-callback-query bot callback-id :text "Операция отменена")
        (tg/send-message bot chat-id "❌ Добавление расхода отменено."))
      
      (str/starts-with? data "CAT_")
      (try
        (let [cat-id (subs data 4)
              cat    (new-expense/find-category cat-id)]
          (println "\033[34mINFO\033[0m " "start command" "cat-id" cat-id "cat" cat) ; delete
          (if-not cat
            (do
              (println "\033[34mINFO\033[0m" "first") ; delete
              (tg/answer-callback-query bot callback-id :text "Неизвестная категория")
              (tg/send-message bot chat-id "Неизвестная категория"))
            (do
              (println "\033[34mINFO\033[0m" "second") ; delete
              (new-expense/set-stage! chat-id :enter-amount :category (:id cat))
              (tg/answer-callback-query bot callback-id {:text (str "Категория: " (:title cat))})
              (tg/send-message bot chat-id
                                  (str "Введите сумму для " (:title cat) " (например: 1200, 1 499,50):")
                                  )))) 
        (catch Exception e
          (println "\033[91mERROR\033[0m " "Ошибка в handle-callback-query, когда выбирается категория" e)))
      
      (= data "CMD_TIME:NOW")
      (let [{:keys [stage]} (new-expense/get-session chat-id)]
        (if (not= stage :enter-time)
          (tg/answer-callback-query bot callback-id :text "Сначала введите сумму")
          (let [now (ZonedDateTime/now almaty-tz)
                {:keys [category amount]} (:draft (new-expense/get-session chat-id))]
            (db/create-expence user-id category amount now) 
            (new-expense/clear-session! chat-id)
            (tg/answer-callback-query bot callback-id :text "Установлено текущее время")
            (tg/send-message bot chat-id
                             (format "✅ Расход добавлен:\n• Категория: %s\n• Сумма: %s\n• Время: %s"
                                     category amount (.format now (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm z"))))))) 
      :else
      (tg/send-message bot
                       {:chat_id chat-id
                        :text (str "Неизвестная команда: " data)}))))