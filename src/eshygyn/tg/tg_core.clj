(ns eshygyn.tg.tg-core
  (:require [telegrambot-lib.core :as tg]
            [dotenv :refer [env]]

            [eshygyn.db.db :as db]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages]))

(defonce last-offset (atom 0))

(def token (env :TOKEN))

(defonce bot (tg/create token))

(defn handle-message [message]
  (let [chat_id (get-in message [:chat :id])
        text (:text message)]
    (if (= text "/authorize") 
      (commands/authorize bot chat_id (get-in message [:from :first_name]) (get-in message [:from :username]))

      (if (db/is-authorized chat_id)
        (cond
          (= text "/start") (commands/start bot chat_id)
      
          :else (tg/send-message bot chat_id (str "Ты написал: " text)))
      
        (messages/authorize bot chat_id))) 
    ))

(defn handle-callback-query [callback-query]
  (let [callback-id   (:id callback-query)
        data    (:data callback-query)
        chat_id (get-in callback-query [:message :chat :id])
        msg-id  (get-in callback-query [:message :message_id])]
    (tg/answer-callback-query bot
                              {:callback_query_id callback-id
                               :text ""
                               :show_alert false})

    (case data
      "CMD_AUTHORIZE"
      (do
        (tg/edit-message-reply-markup bot
                                      {:chat_id chat_id
                                       :message_id msg-id
                                       :reply_markup {:inline_keyboard []}})

        (commands/authorize bot chat_id (get-in callback-query [:from :first_name]) (get-in callback-query [:from :username])))

      (tg/send-message bot
                       {:chat_id chat_id
                        :text (str "Неизвестная команда: " data)}))))

(defn poll-updates [offset]
  (try
    (let [updates (tg/get-updates bot {:offset offset :timeout 30})]
      (doseq [update (:result updates)]
        (let [message (:message update)
              callback_query (:callback_query update)
              update_id (:update_id update)]
          (when message
            (handle-message message))
          (when callback_query
            (handle-callback-query callback_query))
          (reset! last-offset (inc update_id)))))
    (catch Exception e
      (println "Ошибка polling:" (.getMessage e)))))

(defn poll-updates-caller []
  (poll-updates @last-offset))