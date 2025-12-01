(ns eshygyn.tg.commands
  (:require [eshygyn.tg.new-expense :as new-expense]
            [eshygyn.tg.user-session :as user-session]
            [eshygyn.db.db :as db]
            [eshygyn.tg.messages :as messages]
            [eshygyn.config.categories :as categories]
            [eshygyn.tg.category :as tg-category]))

(defn start [bot chat-id]
  (messages/start bot chat-id))

(defn authorize [bot user-id chat-id first-name username]
  (try
    (if (db/is-authorized chat-id)
      (messages/already-authorized bot chat-id)

      (do
        (db/create-user user-id chat-id first-name username categories/default-categories)
        (messages/successfully-authorized bot chat-id first-name)))

    (catch Exception e
      (println (str "\033[91mERROR\033[0m" "Caught a exception while creating user: " e))
      (messages/authorize-error bot chat-id))))

(defn cancel [bot chat-id]
  (user-session/clear-session! chat-id)
  (messages/cancel bot chat-id))

(defn add-expense [bot chat-id]
  (user-session/set-stage! chat-id :choose-category {:category nil :amount nil :when nil})
  (messages/next-category bot chat-id (new-expense/categories-kb chat-id "CAT_")))

(defn change-category [bot chat-id]
  (user-session/set-stage! chat-id :choose-category {:category nil :amount nil :when nil})
  (messages/change-category bot chat-id (new-expense/categories-kb chat-id "CAT_")))

(defn add-category [bot chat-id]
  (user-session/set-stage! chat-id :enter-category-id {:category-id nil :category-title nil :category-emoji nil})
  (messages/next-category-id bot chat-id))

(defn skip [bot chat-id]
  (let [draft (:draft (user-session/get-session chat-id))
        {:keys [category-id category-title]} draft]
    (tg-category/add-category chat-id {:id category-id, :emoji "", :title category-title})
    (user-session/clear-session! chat-id)
    (messages/category-created bot chat-id category-title "")))

(defn edit-category [bot chat-id]
  (user-session/set-stage! chat-id :edit-category-choose {:category-id nil :category-title nil :category-emoji nil :title-old nil})
  (messages/edit-category bot chat-id (new-expense/categories-kb chat-id "EDIT_CAT_")))

(defn delete-category [bot chat-id]
  (user-session/set-stage! chat-id :delete-category-choose {:category-id nil :delete-expenses nil :sure nil})
  (messages/delete-category bot chat-id (new-expense/categories-kb chat-id "DEL_CAT_")))