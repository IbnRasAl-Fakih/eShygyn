(ns eshygyn.db.sql-maps
  "File for sql-maps"
  (:require [cheshire.core :as json]))

(defn get-all-users []
  {:select [:*] 
   :from [:users]})

(defn create-user [user-id chat_id first_name username categories]
  {:insert-into [:users]
   :columns [:id :chat_id :first_name :username :categories]
   :values [[user-id chat_id first_name username [:cast (json/generate-string categories) :jsonb]]]})

(defn get-expences-exact-day [date]
  {:select [*]
   :from [:expences]
   :where [:= :date date]})

(defn get-expences-with-offset [limit offset]
  {:select [:*]
   :from [:expences]
   :limit limit
   :offset offset})

(defn create-expence [user_id category amount date]
  {:insert-into [:expences]
   :columns [:user_id :category :amount :date]
   :values [{:user_id user_id, :category category, :amount amount, :date date}]})

(defn get-user [chat_id]
  {:select [:*]
   :from [:users]
   :where [:= :chat_id chat_id]})

(defn update-user-categories [chat_id categories]
  {:update [:users]
   :set {:categories [:cast (json/generate-string categories) :jsonb]}
   :where [:= :chat_id chat_id]})