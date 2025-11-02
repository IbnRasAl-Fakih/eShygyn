(ns eshygyn.tg.tg-core
  (:require [telegrambot-lib.core :as tg]
            [dotenv :refer [env]]

            [eshygyn.tg.handle-messages :as handle-messages]
            [eshygyn.tg.handle-callback-queries :as handle-callback-queries]
            [eshygyn.tg.global-functions :as global-functions]))

(defonce last-offset (atom 0))

(def token (env :TOKEN))

(defonce bot (tg/create token))

(defn poll-updates [offset]
  (try
    (let [updates (tg/get-updates bot {:offset offset :timeout 30})]
      (doseq [update (:result updates)]
        (let [message (:message update)
              callback_query (:callback_query update)
              update_id (:update_id update)]

          (when message
            (global-functions/delete-previous-inline-query bot (get-in message [:chat :id]) (:message_id message))
            (handle-messages/handle-message bot message))
          (when callback_query
            (global-functions/delete-inline-query bot (get-in callback_query [:message :chat :id]) (get-in callback_query [:message :message_id]))
            (handle-callback-queries/handle-callback-query bot callback_query))
          (reset! last-offset (inc update_id)))))
    (catch Exception e
      (println "\033[91mERROR\033[0m " "Ошибка polling:" (.getMessage e)))))

(defn poll-updates-caller []
  (poll-updates @last-offset))