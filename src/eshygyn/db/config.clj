(ns eshygyn.db.config
  "File for database configuration"
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [dotenv :refer [env]]))

(def db-config {:dbtype (env :DBTYPE) :dbname (env :DBNAME) :user (env :USER) :password (env :PASSWORD) :host (env :HOST) :port (env :PORT)})

(def datasource (jdbc/get-datasource db-config))