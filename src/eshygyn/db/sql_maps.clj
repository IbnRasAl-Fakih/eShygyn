(ns eshygyn.db.sql-maps
  "File for sql-maps"
  (:require [cheshire.core :as json]))

(defn get-all-users []
  {:select [:*] 
   :from [:users]})

(defn create-user [user-id chat-id first-name username categories]
  {:insert-into [:users]
   :columns [:id :chat_id :first_name :username :categories]
   :values [[user-id chat-id first-name username [:cast (json/generate-string categories) :jsonb]]]})

(defn get-expenses-exact-day [date]
  {:select [*]
   :from [:expenses]
   :where [:= :date date]})

(defn get-expenses-with-offset [limit offset]
  {:select [:*]
   :from [:expenses]
   :limit limit
   :offset offset})

(defn create-expence [user-id category amount date]
  {:insert-into [:expenses]
   :columns [:user_id :category :amount :date]
   :values [{:user_id user-id, :category category, :amount amount, :date date}]})

(defn get-user [chat-id]
  {:select [:*]
   :from [:users]
   :where [:= :chat_id chat-id]})

(defn update-user-categories [chat-id categories]
  {:update [:users]
   :set {:categories [:cast (json/generate-string categories) :jsonb]}
   :where [:= :chat_id chat-id]})

(defn delete-expenses [chat-id category-title]
  {:delete-from [:expenses]
   :where [:= :category category-title]})