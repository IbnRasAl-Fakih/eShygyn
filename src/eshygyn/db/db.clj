(ns eshygyn.db.db
  "File for database connection functions"
  (:require [next.jdbc :as jdbc] 
            [honey.sql :as sql]

            [eshygyn.db.config :as config]
            [eshygyn.db.sql-maps :as sql-maps]))

(defn get-user-by-chat-id [chat-id]
  (jdbc/execute-one! config/datasource (sql/format (sql-maps/get-user (str chat-id)))))

(defn create-user [user-id chat-id first-name username categories]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/create-user user-id chat-id first-name username categories)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания пользователя" e))))

(defn get-expenses-exact-day [date]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expenses-exact-day date))))

(defn get-expenses-with-offset [chat-id offset]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expenses-with-offset chat-id config/expenses-limit offset))))

(defn get-expense-by-index [chat-id offset]
  (first (jdbc/execute! config/datasource (sql/format (sql-maps/get-expenses-with-offset chat-id 1 offset)))))

(defn get-number-of-expenses [chat-id]
  (first (jdbc/execute! config/datasource (sql/format (sql-maps/get-number-of-expenses chat-id)))))

(defn create-expense [user-id category amount date comment]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/create-expense user-id category amount date (str comment))))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время создания расхода (db/create-expense)" e))))

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

(defn update-expense-category [chat-id title-new title-old]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/update-expense-category chat-id title-new title-old)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время редактирование расхода (db/update-expense-category)" e))))

(defn delete-expense [expense-id]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/delete-expense expense-id))) 
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время удаление расхода (db/delete-expense)" e))))

(defn update-expense [id category amount when comment]
  (try
    (jdbc/execute! config/datasource (sql/format (sql-maps/update-expense id category amount when comment)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время обновление расхода (db/update-expense)" e))))