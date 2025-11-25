(ns eshygyn.tg.commands
  (:require [eshygyn.tg.new-expense :as new-expense]
            [eshygyn.db.db :as db]
            [eshygyn.tg.messages :as messages]
            [eshygyn.config.categories :as categories]))

(defn start [bot chat-id]
  (messages/start bot chat-id))

(defn authorize [bot user-id chat-id first-name username]
  (try
    (if (db/is-authorized chat-id)
      (messages/already-authorized bot chat-id)
      
      (do
        (println categories/default-categories)
        (db/create-user user-id chat-id first-name username categories/default-categories)
        (messages/successfully-authorized bot chat-id first-name)))
    
    (catch Exception e
      (println (str "\033[91mERROR\033[0m" "Caught a exception while creating user: " e))
      (messages/authorize-error bot chat-id))))

(defn cancel [bot chat-id]
  (new-expense/clear-session! chat-id)
  (messages/cancel bot chat-id))

(defn add-expense [bot chat-id]
  (new-expense/set-stage! chat-id :choose-category {:category nil :amount nil :when nil})
  (messages/next-category bot chat-id (new-expense/categories-kb chat-id)))

(defn change-category [bot chat-id]
  (new-expense/set-stage! chat-id :choose-category {:category nil :amount nil :when nil})
  (messages/change-category bot chat-id (new-expense/categories-kb chat-id)))