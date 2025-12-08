(ns eshygyn.tg.category
  (:require [cheshire.core :as json]
            [clojure.string :as str]
  
            [eshygyn.db.db :as db]))

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

(defn find-category [chat-id cat-id]
  (some #(when (= (str/upper-case (:id %)) cat-id) %) (get-user-categories chat-id)))

(defn index-of-category [id categories]
  (first (keep-indexed #(when (= (:id %2) id) %1) categories)))

(defn add-category [chat-id new-category]
  (try
    (let [old-categories (json/parse-string (.getValue (:users/categories (db/get-user-by-chat-id chat-id))) true)
          categories (conj old-categories new-category)]
      (db/update-user-categories chat-id categories)
      (update-categories-cache! chat-id categories))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время добавлений категорий" e))))

(defn edit-category [chat-id category-id category-title category-emoji title-old]
  (try
    (let [old-categories (vec (json/parse-string (.getValue (:users/categories (db/get-user-by-chat-id chat-id))) true))
          cat-id (str/lower-case category-id)
          index (index-of-category cat-id old-categories)
          categories (assoc old-categories index {:id cat-id, :emoji category-emoji, :title category-title})]
      (db/update-expense-category chat-id category-title title-old)
      (db/update-user-categories chat-id categories)
      (update-categories-cache! chat-id categories))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время редактирование категорий" e))))

(defn delete-category [chat-id category-id is-delete-expenses]
  (try
    (when is-delete-expenses
      (db/delete-expenses chat-id (:title (find-category chat-id category-id))))
    (let [old-categories (json/parse-string (.getValue (:users/categories (db/get-user-by-chat-id chat-id))) true)
          cat-id (str/lower-case category-id)
          categories (remove #(= cat-id (:id %)) old-categories)]
      (db/update-user-categories chat-id categories)
      (update-categories-cache! chat-id categories))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка во время удаления категорий" e))))

(defn parse-text [text]
  (-> text
      str
      str/lower-case
      (str/replace #"[^a-z0-9]" "")))

(defn is-unique [category-id chat-id]
  (let [list-of-categories (get-user-categories chat-id)]
    (not (some #(= category-id (:id %)) list-of-categories))))