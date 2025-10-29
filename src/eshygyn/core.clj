(ns eshygyn.core
  (:require [eshygyn.tg.tg-core :as tcore]))

(defonce running? (atom true))

(defn -main []
  (println "Бот запущен. Ожидаю сообщения...")
  (while @running?
    (tcore/poll-updates-caller)
    (Thread/sleep 1000)))