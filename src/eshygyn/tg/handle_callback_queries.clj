(ns eshygyn.tg.handle-callback-queries
  (:require [telegrambot-lib.core :as tg]
            [clojure.string :as str]
            
            [eshygyn.db.db :as db]
            [eshygyn.tg.user-session :as user-session]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages]
            [eshygyn.tg.category :as tg-category]
            [eshygyn.tg.new-expense :as new-expense])
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

   "CREATE_TIME_NOW"
   (fn [bot chat-id & _]
     (let [now-odt (-> (ZonedDateTime/now almaty-tz)
                       (.withNano 0)
                       (.toOffsetDateTime))]
       (user-session/set-stage! chat-id :enter-comment :when now-odt)
       (messages/next-comment bot chat-id "CMD_SKIP_COMMENT")))

   "EDIT_TIME_NOW"
   (fn [bot chat-id _ _ draft _]
     (let [now-odt (-> (ZonedDateTime/now almaty-tz)
                       (.withNano 0)
                       (.toOffsetDateTime))
           date (.format now-odt new-expense/fmt-out)
           {:keys [category amount comment]} draft]
       (user-session/set-stage! chat-id :edit-expense {:when date})
       (messages/edit-expense-loop bot chat-id category amount date comment)))

   "CMD_CHANGE_CATEGORY"
   (fn [bot chat-id & _]
     (commands/change-category bot chat-id))

   "CMD_SKIP"
   (fn [bot chat-id & _]
     (commands/skip bot chat-id))

   "CMD_SKIP_COMMENT"
   (fn [bot chat-id user-id _ draft _]
     (let [{:keys [category amount when]} draft]
       (commands/skip-comment bot chat-id user-id category amount when "")))

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

   "CMD_RIGHT"
   (fn [bot chat-id _ callback-query draft _]
     (let [{:keys [offset total]} draft
           message-id (:message_id (:message callback-query))]
       (if (> total (+ offset 10))
         (commands/expenses-list bot chat-id (+ offset 10) message-id)
         (commands/expenses-list bot chat-id offset message-id))))

   "CMD_LEFT"
   (fn [bot chat-id _ callback-query draft _]
     (let [offset (:offset draft)
           message-id (:message_id (:message callback-query))]
       (commands/expenses-list bot chat-id (new-expense/correct-offset (- offset 10)) message-id)))

   "CMD_RIGHT_EDIT"
   (fn [bot chat-id _ callback-query draft _]
     (let [{:keys [offset total]} draft
           message-id (:message_id (:message callback-query))]
       (if (> total (+ offset 10))
         (commands/edit-expenses-list bot chat-id (+ offset 10) message-id)
         (commands/edit-expenses-list bot chat-id offset message-id))))

   "CMD_LEFT_EDIT"
   (fn [bot chat-id _ callback-query draft _]
     (let [offset (:offset draft)
           message-id (:message_id (:message callback-query))]
       (commands/edit-expenses-list bot chat-id (new-expense/correct-offset (- offset 10)) message-id)))

   "CMD_EDIT_EXPENSE_CATEGORY"
   (fn [bot chat-id & _]
     (messages/change-category bot chat-id (new-expense/categories-kb chat-id "EDIT_EXPENSE_CAT_")))

   "CMD_EDIT_EXPENSE_AMOUNT"
   (fn [bot chat-id & _]
     (user-session/set-stage! chat-id :edit-expense-amount)
     (messages/edit-expense-amount bot chat-id))

   "CMD_EDIT_EXPENSE_TIME"
   (fn [bot chat-id & _]
     (user-session/set-stage! chat-id :edit-expense-time)
     (messages/next-time bot chat-id "EDIT_"))

   "CMD_EDIT_EXPENSE_COMMENT"
   (fn [bot chat-id & _]
     (user-session/set-stage! chat-id :edit-expense-comment)
     (messages/next-comment bot chat-id "CMD_EDIT_EXPENSE_SKIP_COMMENT"))

   "CMD_EDIT_EXPENSE_SKIP_COMMENT"
   (fn [bot chat-id _ _ draft _]
     (let [{:keys [category amount when]} draft]
       (user-session/set-stage! chat-id :edit-expense {:comment ""})
       (messages/edit-expense-loop bot chat-id category amount when "")))

   "CMD_DELETE_EXPENSE"
   (fn [bot chat-id & _]
     (messages/edit-expense-is-sure bot chat-id))

   "CMD_DELETE_EXPENSE_YES"
   (fn [bot chat-id _ _ draft _]
     (let [expense-id (:id draft)]
       (db/delete-expense expense-id)
       (messages/expense-deleted bot chat-id)))

   "CMD_DELETE_EXPENSE_NO"
   (fn [bot chat-id _ _ draft _]
     (let [{:keys [category amount when comment]} draft]
       (messages/edit-expense-loop bot chat-id category amount when comment)))

   "CMD_EDIT_EXPENSE_SAVE"
   (fn [bot chat-id _ _ draft _]
     (let [{:keys [id category amount when comment]} draft]
       (commands/update-expense id category amount when comment)
       (messages/edit-expense-saved bot chat-id)))

   "CMD_ADD"
   (fn [bot chat-id & _]
     (commands/add-expense bot chat-id))

   "CMD_LIST_OF_EXPENSES"
   (fn [bot chat-id & _]
     (commands/expenses-list bot chat-id))

   "CMD_EDIT_EXPENSES"
   (fn [bot chat-id & _]
     (commands/edit-expenses-list bot chat-id))

   "CMD_SETTINGS_LIST"
   (fn [bot chat-id & _]
     (messages/settings-list bot chat-id))
   })

(defn query-not-listed-handler [data bot chat-id draft]
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

    (str/starts-with? data "CREATE_TIME_")
    (let [minus-time (Integer/parseInt (subs data 12))
          now-odt (-> (ZonedDateTime/now almaty-tz)
                      (.withNano 0)
                      (.toOffsetDateTime))
          final-time (.minusMinutes now-odt minus-time)]
      (user-session/set-stage! chat-id :enter-comment :when final-time)
      (messages/next-comment bot chat-id "CMD_SKIP_COMMENT"))

    (str/starts-with? data "EXP_")
    (try
      (let [index (subs data 4)
            expense (db/get-expense-by-index chat-id (Integer/parseInt index))
            {:keys [expenses/id expenses/category expenses/amount expenses/date expenses/comment]} expense
            converted-date (new-expense/javatimestamp->zoneddatetime date)]
        (user-session/set-stage! chat-id :edit-expense {:id id :category category :amount amount :when converted-date :comment comment})
        (messages/edit-expense-loop bot chat-id category amount converted-date comment))

      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда редактируется расход" e)))

    (str/starts-with? data "EDIT_EXPENSE_CAT_")
    (try
      (let [cat-id (subs data 17)
            title (:title (tg-category/find-category chat-id cat-id))
            {:keys [amount when comment]} draft]
        (if-not title
          (println "\033[91mERROR\033[0m" "Неизвестная категория: ошибка со стороны сервера, так как пользователь только выбирает одну из выданных вариантов")
          (do
            (user-session/set-stage! chat-id :edit-expense {:category title})
            (messages/edit-expense-loop bot chat-id title amount when comment))))
      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда редактируется расход" e)))

    (str/starts-with? data "EDIT_TIME_")
    (try
      (let [minus-time (Integer/parseInt (subs data 10))
            now-odt (-> (ZonedDateTime/now almaty-tz)
                        (.withNano 0)
                        (.toOffsetDateTime))
            final-time (.minusMinutes now-odt minus-time)
            date (.format final-time new-expense/fmt-out)
            {:keys [category amount comment]} draft]
        (user-session/set-stage! chat-id :edit-expense {:when date})
        (messages/edit-expense-loop bot chat-id category amount date comment))
      (catch Exception e
        (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query, когда редактируется расход" e)))

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
        (query-not-listed-handler data bot chat-id draft)
        (apply function [bot chat-id user-id callback-query draft stage])))
        (catch Exception e
          (println "\033[91mERROR\033[0m" "Ошибка в handle-callback-query:" e))))