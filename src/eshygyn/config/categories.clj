(ns eshygyn.config.categories
  (:require [cheshire.core :as json]
            
            [eshygyn.db.db :as db]))

(def default-categories
  [{:id "food"      :emoji "🍔" :title "Еда"}
   {:id "transport" :emoji "🚌" :title "Транспорт"}
   {:id "clothes"   :emoji "🧥" :title "Одежда и обувь"}
   {:id "travel"    :emoji "✈️" :title "Путешествия"}
   {:id "rent"      :emoji "🏠" :title "Аренда"}
   {:id "other"     :emoji "🧩" :title "Другое"}])

(def categories-cache (atom {}))

(defn update-categories-cache! [chat-id categories]
  (swap! categories-cache assoc (str chat-id) categories))

(defn get-categories-from-cache [chat-id]
  (get @categories-cache (str chat-id)))

(defn get-user-categories [chat-id]
  (if-let [cached (get-categories-from-cache chat-id)]
    cached
    (let [user (db/get-user-by-chat-id chat-id)]
      (when user
        (let [categories-raw (:users/categories user)
              categories (json/parse-string (.getValue categories-raw) true)]
          (update-categories-cache! chat-id categories)
          categories)))))

(defn update-user-categories [chat-id categories]
  (try
    (let [user (db/get-user-by-chat-id chat-id)]
      (when user
        (db/update-user-categories chat-id categories)
        (update-categories-cache! chat-id categories)))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время обновления категорий пользователя" e))))