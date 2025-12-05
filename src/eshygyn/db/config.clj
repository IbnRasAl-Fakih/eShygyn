(ns eshygyn.db.config
  "File for database configuration"
  (:require [next.jdbc :as jdbc]
            [dotenv :refer [env]]))

(def db-config {:dbtype (env :DBTYPE) :dbname (env :DBNAME) :user (env :USER) :password (env :PASSWORD) :host (env :HOST) :port (env :PORT)})

(def datasource (jdbc/get-datasource db-config))

;; number of expenses in one get request
(def expenses-limit 10)