(ns eshygyn.db.sql-maps
  "File for sql-maps")

(defn get-all-users []
  {:select [:*] 
   :from [:users]})

(defn create-user [user-id chat_id first_name username]
  {:insert-into [:users]
   :columns [:id :chat_id :first_name :username]
   :values [{:id user-id :chat_id chat_id, :first_name first_name, :username username}]})

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