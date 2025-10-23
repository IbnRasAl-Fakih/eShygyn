(ns eshygyn.db.sql-maps
  "File for sql-maps")

(defn get-all-users []
  {:select [:*] 
   :from [:users]})

(defn create-user [phone chat_id]
  {:insert-into [:users]
   :columns [:phone :chat_id]
   :values [{:phone phone, :chat_id chat_id}]})

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