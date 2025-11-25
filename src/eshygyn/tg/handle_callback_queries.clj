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

(def listed-commands
  {"CMD_AUTHORIZE" 
   (fn [bot chat-id user-id callback-query] 
     (commands/authorize bot user-id chat-id (get-in callback-query [:from :first_name]) (get-in callback-query [:from :username])))

   "CMD_CANCEL"
   (fn [bot chat-id & _]
     (commands/cancel bot chat-id))
   
   "CMD_TIME_NOW"
   (fn [bot chat-id user-id & _]
     (let [now-odt (-> (ZonedDateTime/now almaty-tz)
                       (.withNano 0)
                       (.toOffsetDateTime))
           {:keys [category amount]} (:draft (new-expense/get-session chat-id))]
       (db/create-expence user-id category amount now-odt)
       (new-expense/clear-session! chat-id)
       (messages/expense_created bot chat-id category amount now-odt)))
   
   "CMD_CHANGE_CATEGORY"
   (fn [bot chat-id & _]
     (commands/change-category bot chat-id))
   })

(defn query-not-listed-handler [data bot chat-id user-id]
  (cond
    (str/starts-with? data "CAT_")
    (try
      (let [cat-id (subs data 4)
            cat    (new-expense/find-category chat-id cat-id)]
        (if-not cat
          (println "\033[91mERROR\033[0m" "Неизвестная категория: ошибка со стороны сервера, так как пользователь только выбирает одну из выданных вариантов")
          (do
            (new-expense/set-stage! chat-id :enter-amount :category (:title cat))
            (messages/next-amount bot chat-id (:title cat)))))
      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда выбирается категория" e)))
  
    (str/starts-with? data "TIME_")
    (let [minus-time (Integer/parseInt (subs data 5))
          now-odt (-> (ZonedDateTime/now almaty-tz)
                      (.withNano 0)
                      (.toOffsetDateTime))
          final-time (.minusMinutes now-odt minus-time)
          {:keys [category amount]} (:draft (new-expense/get-session chat-id))]
      (db/create-expence user-id category amount final-time)
      (new-expense/clear-session! chat-id)
      (messages/expense_created bot chat-id category amount final-time))

    :else
    (messages/unknown-command bot chat-id data)))

(defn handle-callback-query [bot callback-query]
  (println "\033[34mINFO\033[0m " callback-query) ; delete
  (try
    (let [callback-id (:id callback-query)
          data        (:data callback-query)
          chat-id     (get-in callback-query [:message :chat :id])
          user-id     (get-in callback-query [:from :id])
          function    (get listed-commands data)] 
      (tg/answer-callback-query bot callback-id)
      
      (if (nil? function)
        (query-not-listed-handler data bot chat-id user-id)
        (apply function [bot chat-id user-id callback-query])))
        (catch Exception e
          (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query:" e))))