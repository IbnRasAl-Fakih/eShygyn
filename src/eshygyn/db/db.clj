(ns eshygyn.db.db
  "File for database connection functions"
  (:require [next.jdbc :as jdbc] 
            [honey.sql :as sql] 

            [eshygyn.db.config :as config]
            [eshygyn.db.sql-maps :as sql-maps]))

(defn get-all-users []
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-all-users))))

(defn create-user [user-id chat-id first-name username]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/create-user user-id chat-id first-name username)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания пользователя" e))))

(defn get-expences-exact-day [date]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expences-exact-day date))))

(defn get-expences-with-offset [limit offset]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expences-with-offset limit offset))))

(defn create-expence [user_id category amount date]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/create-expence user_id category amount date)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания расхода (db/create-expence)" e))))

(defn is-authorized [chat_id]
  (< 0 (count (jdbc/execute! config/datasource (sql/format (sql-maps/get-user (str chat_id)))))))