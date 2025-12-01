(ns eshygyn.tg.handle-callback-queries
  (:require [telegrambot-lib.core :as tg]
            [clojure.string :as str]
            
            [eshygyn.db.db :as db]
            [eshygyn.tg.user-session :as user-session]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages]
            [eshygyn.tg.category :as tg-category])
  (:import (java.time ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter DateTimeParseException)))

(def almaty-tz (ZoneId/of "Asia/Almaty"))

(def listed-commands
  {"CMD_AUTHORIZE" 
   (fn [bot chat-id user-id callback-query & _] 
     (commands/authorize bot user-id chat-id (get-in callback-query [:from :first_name]) (get-in callback-query [:from :username])))

   "CMD_CANCEL"
   (fn [bot chat-id & _]
     (commands/cancel bot chat-id))
   
   "CMD_TIME_NOW"
   (fn [bot chat-id user-id _ draft & _]
     (let [now-odt (-> (ZonedDateTime/now almaty-tz)
                       (.withNano 0)
                       (.toOffsetDateTime))
           {:keys [category amount]} draft]
       (db/create-expence user-id category amount now-odt)
       (user-session/clear-session! chat-id)
       (messages/expense-created bot chat-id category amount now-odt)))
   
   "CMD_CHANGE_CATEGORY"
   (fn [bot chat-id & _]
     (commands/change-category bot chat-id))
   
   "CMD_SKIP"
   (fn [bot chat-id & _]
     (commands/skip bot chat-id))
   
   "CMD_YES"
   (fn [bot chat-id _ _ draft stage]
     (case stage
       :is-delete-expenses
       (do
         (user-session/set-stage! chat-id :is-sure :delete-expenses true)
         (messages/is-sure bot chat-id))
       
       :is-sure
       (let [{:keys [category-id delete-expenses]} draft]
         (user-session/clear-session! chat-id)
         (tg-category/delete-category chat-id category-id delete-expenses)
         (messages/category-deleted bot chat-id))))
   
   "CMD_NO"
   (fn [bot chat-id _ _ _ stage]
     (case stage
       :is-delete-expenses
       (do
         (user-session/set-stage! chat-id :is-sure :delete-expenses false)
         (messages/is-sure bot chat-id))))
   
   "CMD_EDIT_TITLE"
   (fn [bot chat-id & _]
     (user-session/set-stage! chat-id :edit-category-title)
     (messages/edit-category-title bot chat-id))
   
   "CMD_EDIT_EMOJI"
   (fn [bot chat-id & _]
     (user-session/set-stage! chat-id :edit-category-emoji)
     (messages/edit-category-emoji bot chat-id))
   
   "CMD_SAVE_CHANGES"
   (fn [bot chat-id _ _ draft _]
     (let [{:keys [category-id category-title category-emoji title-old]} draft]
       (user-session/clear-session! chat-id)
       (tg-category/edit-category chat-id category-id category-title category-emoji title-old)
       (messages/category-edited bot chat-id))) 

   "CMD_CATEGORY_MANAGEMENT"
   (fn [bot chat-id & _]
     (user-session/set-stage! chat-id :category-settings-options)
     (messages/category-settings-list bot chat-id))
   
   "CMD_ADD_CATEGORY"
   (fn [bot chat-id & _]
     (commands/add-category bot chat-id))
   
   "CMD_EDIT_CATEGORY"
   (fn [bot chat-id & _]
     (commands/edit-category bot chat-id))
   
   "CMD_DELETE_CATEGORY"
   (fn [bot chat-id & _]
     (commands/delete-category bot chat-id))
   })

(defn query-not-listed-handler [data bot chat-id user-id draft]
  (cond
    (str/starts-with? data "CAT_")
    (try
      (let [cat-id (subs data 4)
            cat    (tg-category/find-category chat-id cat-id)]
        (if-not cat
          (println "\033[91mERROR\033[0m" "Неизвестная категория: ошибка со стороны сервера, так как пользователь только выбирает одну из выданных вариантов")
          (do
            (user-session/set-stage! chat-id :enter-amount :category (:title cat))
            (messages/next-amount bot chat-id (:title cat)))))
      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда выбирается категория" e)))
  
    (str/starts-with? data "TIME_")
    (let [minus-time (Integer/parseInt (subs data 5))
          now-odt (-> (ZonedDateTime/now almaty-tz)
                      (.withNano 0)
                      (.toOffsetDateTime))
          final-time (.minusMinutes now-odt minus-time)
          {:keys [category amount]} draft]
      (db/create-expence user-id category amount final-time)
      (user-session/clear-session! chat-id)
      (messages/expense-created bot chat-id category amount final-time))
    
    (str/starts-with? data "EDIT_CAT_")
    (try
      (let [cat-id (subs data 9)
            cat (tg-category/find-category chat-id cat-id)
            title (:title cat)
            emoji (:emoji cat)]
        (if-not cat
          (println "\033[91mERROR\033[0m" "Неизвестная категория: ошибка со стороны сервера, так как пользователь только выбирает одну из выданных вариантов")
          (do
            (user-session/set-stage! chat-id :choose-category-part :category-id cat-id :category-title title :category-emoji emoji :title-old title)
            (messages/choose-category-part-to-edit bot chat-id title emoji))))
      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда редактируется категория" e)))
    
    (str/starts-with? data "DEL_CAT_")
    (try
      (let [cat-id (subs data 8)
            cat    (tg-category/find-category chat-id cat-id)]
        (if-not cat
          (println "\033[91mERROR\033[0m" "Неизвестная категория: ошибка со стороны сервера, так как пользователь только выбирает одну из выданных вариантов")
          (do
            (user-session/set-stage! chat-id :is-delete-expenses :category-id cat-id)
            (messages/is-delete-expenses bot chat-id (:title cat) (:emoji cat)))))
      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда удаляется категория" e)))

    :else
    (messages/unknown-command bot chat-id data)))

(defn handle-callback-query [bot callback-query]
  (println "\033[34mINFO\033[0m " callback-query) ; delete
  (try
    (let [callback-id           (:id callback-query)
          data                  (:data callback-query)
          chat-id               (get-in callback-query [:message :chat :id])
          user-id               (get-in callback-query [:from :id])
          function              (get listed-commands data)
          {:keys [stage draft]} (user-session/get-session chat-id)] 
      (tg/answer-callback-query bot callback-id)
      
      (if (nil? function)
        (query-not-listed-handler data bot chat-id user-id draft)
        (apply function [bot chat-id user-id callback-query draft stage])))
        (catch Exception e
          (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query:" e))))