(ns eshygyn.tg.handle-callback-queries
  (:require [telegrambot-lib.core :as tg]
            [clojure.string :as str]
            
            [eshygyn.db.db :as db]
            [eshygyn.tg.new-expense :as new-expense]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages])
  (:import (java.time ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter DateTimeParseException)))

(def almaty-tz (ZoneId/of "Asia/Almaty"))

(defn handle-callback-query [bot callback-query]
  (println "\033[34mINFO\033[0m " callback-query) ; delete
  (try
    (let [callback-id   (:id callback-query)
          data    (:data callback-query)
          chat-id (get-in callback-query [:message :chat :id])
          msg-id  (get-in callback-query [:message :message_id])
          user-id (get-in callback-query [:from :id])] 
      (tg/edit-message-reply-markup bot {:chat_id chat-id :message_id msg-id :reply_markup {:inline_keyboard []}}) 
      (cond
        (= data "CMD_AUTHORIZE")
        (commands/authorize bot user-id chat-id (get-in callback-query [:from :first_name]) (get-in callback-query [:from :username]))
    
        (= data "CMD_CANCEL")
        (do 
          (new-expense/clear-session! chat-id)
          (tg/answer-callback-query bot callback-id)
          (messages/cancel bot chat-id))
    
        (str/starts-with? data "CAT_")
        (try
          (let [cat-id (subs data 4)
                cat    (new-expense/find-category cat-id)]
            (if-not cat
              (do
                (tg/answer-callback-query bot callback-id)
                (println "\033[91mERROR\033[0m" "Неизвестная категория: ошибка со стороны сервера, так как пользователь только выбирает одну из выданных вариантов"))
              (do
                (new-expense/set-stage! chat-id :enter-amount :category (:title cat))
                (tg/answer-callback-query bot callback-id)
                (messages/next-amount bot chat-id (:title cat)))))
          (catch Exception e
            (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда выбирается категория" e)))
    
        (= data "CMD_TIME_NOW")
        (try
          (let [{:keys [stage]} (new-expense/get-session chat-id)]
            (if (not= stage :enter-time)
              (tg/answer-callback-query bot callback-id)
              (let [now-odt (-> (ZonedDateTime/now almaty-tz)
                                (.withNano 0)
                                (.toOffsetDateTime))
                    {:keys [category amount]} (:draft (new-expense/get-session chat-id))]
                (println "\033[34mINFO\033[0m" (:draft (new-expense/get-session chat-id))) ; delete
                (db/create-expence user-id category amount now-odt)
                (new-expense/clear-session! chat-id)
                (tg/answer-callback-query bot callback-id)
                (messages/expense_created bot chat-id category amount now-odt))))
    
          (catch Exception e
            (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда установливается время" e)))
    
        :else
        (messages/unknown-command bot chat-id data)))
        (catch Exception e
          (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query:" e))))