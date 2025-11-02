(ns eshygyn.tg.handle-messages
  (:require [eshygyn.db.db :as db]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages]
            [eshygyn.tg.new-expense :as new-expense])
  (:import (java.time.format DateTimeFormatter DateTimeParseException)))

(defn handle-message [bot message]
  (println "\033[34mINFO\033[0m" message) ; delete
  (try
    (let [chat-id               (get-in message [:chat :id])
          text                  (:text message)
          user-id               (get-in message [:from :id])
          {:keys [stage draft]} (new-expense/get-session chat-id)]
      (println "\033[34mINFO\033[0m" "stage = " stage) ; delete
      (if (= text "/authorize")
        (commands/authorize bot user-id chat-id (get-in message [:from :first_name]) (get-in message [:from :username]))
    
        (if (db/is-authorized chat-id)
          (cond
            (= text "/start") (commands/start bot chat-id)
    
            (= text "/add") (commands/add-expense bot chat-id)
    
            (= text "/cancel") (commands/cancel bot chat-id)

            (= text "/change") (commands/change-category bot chat-id)
    
            (nil? stage) (messages/unknown-message bot chat-id)
    
            (= stage :enter-amount)
            (let [amt (new-expense/parse-amount text)]
              (if (nil? amt)
                (messages/wrong-amount bot chat-id)
                (do
                  (new-expense/set-stage! chat-id :enter-time :amount amt)
                  (messages/next-time bot chat-id))))
    
            (= stage :enter-time)
            (let [dt (new-expense/parse-datetime text)]
              (if (nil? dt)
                (messages/wrong-time bot chat-id)
                (let [{:keys [category amount]} draft]
                  (db/create-expence user-id category amount dt)
                  (new-expense/clear-session! chat-id)
                  (messages/expense_created bot chat-id category amount dt))))
    
            :else (messages/unknown-message-with-stage bot chat-id))
    
          (messages/authorize bot chat-id))))
          (catch Exception e
            (println "\033[91mERROR\033[0m" "Ошибка в handle-message:" e))))