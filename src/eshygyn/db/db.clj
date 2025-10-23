(ns eshygyn.db.db
  "File for database connection functions"
  (:require [next.jdbc :as jdbc] 
            [honey.sql :as sql] 

            [eshygyn.db.config :as config]
            [eshygyn.db.sql-maps :as sql-maps]))

(defn get-all-users []
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-all-users))))

(defn create-user [phone chat_id]
  (jdbc/execute! config/datasource (sql/format (sql-maps/create-user phone chat_id))))

(defn get-expences-exact-day [date]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expences-exact-day date))))

(defn get-expences-with-offset [limit offset]
  (jdbc/execute! config/datasource (sql/format (sql-maps/get-expences-with-offset limit offset))))

(defn create-expence [user_id category amount date]
  (jdbc/execute! config/datasource (sql/format (sql-maps/create-expence user_id category amount date))))