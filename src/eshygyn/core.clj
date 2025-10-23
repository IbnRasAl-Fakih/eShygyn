(ns eshygyn.core
  (:require [eshygyn.db.config :as db.config]))

(defn -main []
  (println "Hello World!")
  (println (db.config/get-all-users)))