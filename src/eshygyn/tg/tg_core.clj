(ns eshygyn.tg.tg-core
  (:require [telegrambot-lib.core :as tg]
            [dotenv :refer [env]]))

(defonce last-offset (atom 0))

(def token (env :TOKEN))

(defonce bot (tg/create token))

(defn handle-message [message]
  (println message)
  (let [chat-id (get-in message [:chat :id])
        text (:text message)]
    (cond
      (= text "/start")
      (tg/send-message bot chat-id "Привет! 👋 Я твой Expense Tracker бот. Отправь мне любое сообщение.")

      :else
      (tg/send-message bot chat-id (str "Ты написал: " text)))))

(defn poll-updates [offset]
  (try
    (let [updates (tg/get-updates bot {:offset offset :timeout 30})]
      (doseq [update (:result updates)]
        (let [message (:message update)
              update-id (:update_id update)]
          (when message
            (handle-message message))
          (reset! last-offset (inc update-id)))))
    (catch Exception e
      (println "Ошибка polling:" (.getMessage e)))))

(defn poll-updates-caller []
  (poll-updates @last-offset))