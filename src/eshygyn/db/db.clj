(ns eshygyn.db.db
  "File for database connection functions"
  (:require [next.jdbc :as jdbc] 
            [honey.sql :as sql]

            [eshygyn.db.config :as config]
            [eshygyn.db.sql-maps :as sql-maps]))

(defn get-all-users []
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-all-users))))

(defn get-user-by-chat-id [chat-id]
  (jdbc/execute-one! config/datasource (sql/format (sql-maps/get-user (str chat-id)))))

(defn create-user [user-id chat-id first-name username categories]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/create-user user-id chat-id first-name username categories)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания пользователя" e))))

(defn get-expenses-exact-day [date]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expenses-exact-day date))))

(defn get-expenses-with-offset [limit offset]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expenses-with-offset limit offset))))

(defn create-expence [user-id category amount date comment]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/create-expence user-id category amount date (str comment))))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания расхода (db/create-expence)" e))))

(defn is-authorized [chat-id]
  (< 0 (count (jdbc/execute! config/datasource (sql/format (sql-maps/get-user (str chat-id)))))))

(defn update-user-categories [chat-id categories]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/update-user-categories (str chat-id) categories)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания категории (db/update-user-categories)" e))))

(defn delete-expenses [chat-id category-title]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/delete-expenses chat-id category-title)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время удаления расходов (db/delete-expenses)" e))))

(defn update-expence-category [chat-id title-new title-old]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/update-expence-category chat-id title-new title-old)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время редактирование расхода (db/update-expence-category)" e))))