(ns eshygyn.tg.new-expense
  (:require [telegrambot-lib.core :as tg]
            [clojure.string :as str]
            
            [eshygyn.config.categories :as config-categories])
  (:import (java.time ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter DateTimeParseException)))

(def user-session (atom {}))

(def almaty-tz (ZoneId/of "Asia/Almaty"))
(def fmt-yy   (DateTimeFormatter/ofPattern "dd.MM.yy HH:mm"))
(def fmt-yyyy (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm"))

(def categories config-categories/categories)

(defn categories-kb []
  (let [cats (seq categories)]
    {:inline_keyboard
     (->> (or cats [])
          (map (fn [{:keys [id title]}]
                 [{:text title :callback_data (str "CAT_" (str/upper-case id))}]))
          (map vec)
          vec)}))

(defn time-kb []
  {:inline_keyboard [[{:text "🕒 Текущее время" :callback_data "CMD_TIME_NOW"}]
                     [{:text "❌ Отмена"        :callback_data "CMD_CANCEL"}]]})

(defn parse-amount [s]
  (let [clean (-> s (str/replace #"\s+" "") (str/replace #"," "."))]
    (try
      (let [bd (bigdec clean)]
        (when (pos? bd) bd))
      (catch Exception _ nil))))

(defn parse-datetime [s]
  (when s
    (let [txt (str/trim s)]
      (or
       (try
         (-> (LocalDateTime/parse txt fmt-yy)
             (.atZone almaty-tz))
         (catch DateTimeParseException _ nil))
       (try
         (-> (LocalDateTime/parse txt fmt-yyyy)
             (.atZone almaty-tz))
         (catch DateTimeParseException _ nil))))))

(defn set-stage! [chat-id stage & kvs]
  (swap! user-session
         (fn [m]
           (let [cur (get m chat-id {})
                 draft-add (cond
                             (and (= 1 (count kvs)) (map? (first kvs))) (first kvs)
                             (even? (count kvs)) (apply hash-map kvs)
                             :else (throw (ex-info "set-stage!: odd kvs" {:kvs kvs})))]
             (assoc m chat-id
                    (-> cur
                        (assoc :stage stage)
                        (update :draft #(merge (or % {}) draft-add))))))))

(defn clear-session! [chat-id]
  (swap! user-session dissoc chat-id))

(defn get-session [chat-id]
  (get @user-session chat-id))

(defn handle-add-cmd [bot chat-id]
  (try
    (set-stage! chat-id :choose-category {:category nil :amount nil :when nil})
    (tg/send-message bot chat-id "➕ Добавляем расход.\nВыберите категорию:" {:reply_markup (categories-kb)})
    (catch Exception e
      (println "\033[91mERROR\033[0m " "Ошибка в handle-add-cmd: " e))))

(defn find-category [cat-id]
  (some #(when (= (:id (str/upper-case %)) cat-id) %) categories))