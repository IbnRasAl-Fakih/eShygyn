(ns eshygyn.tg.new-expense
  (:require [clojure.string :as str]
            
            [eshygyn.config.categories :as config-categories])
  (:import (java.time ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter DateTimeParseException)))

(def user-session (atom {}))

(def fmt-out (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm"))

(def almaty-tz (ZoneId/of "Asia/Almaty"))

(defn categories-kb [chat-id]
  (let [cats (seq (config-categories/get-user-categories chat-id))]
    {:inline_keyboard
     (->> (or cats [])
          (map (fn [{:keys [id emoji title]}]
                 [{:text (str emoji " " title) :callback_data (str "CAT_" (str/upper-case id))}]))
          (map vec)
          vec)}))

(defn time-kb []
  {:inline_keyboard [[{:text "🕒 Текущее время" :callback_data "CMD_TIME_NOW"}]
                     [{:text "15 минут назад" :callback_data "TIME_15"}]
                     [{:text "30 минут назад" :callback_data "TIME_30"}]
                     [{:text "1 час назад" :callback_data "TIME_60"}]
                     [{:text "2 часа назад" :callback_data "TIME_120"}]
                     [{:text "3 часа назад" :callback_data "TIME_180"}]
                     [{:text "❌ Отмена" :callback_data "CMD_CANCEL"}]]})

(defn parse-amount [s]
  (let [clean (-> s (str/replace #"\s+" "") (str/replace #"," "."))]
    (try
      (let [bd (bigdec clean)]
        (when (pos? bd) bd))
      (catch Exception _ nil))))

(defn parse-datetime [s]
  (when s
    (let [txt (-> (str/trim s)
                  (cond-> (re-matches #".*\d{2}:\d{2}$" (str/trim s))
                    (str ":00")))
          fmt1 (DateTimeFormatter/ofPattern "dd.MM.yy HH:mm:ss")
          fmt2 (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm:ss")]
      (some #(try
               (-> (LocalDateTime/parse txt %)
                   (.atZone almaty-tz)
                   (.withNano 0)
                   (.toOffsetDateTime))
               (catch Exception _ nil))
            [fmt1 fmt2]))))

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

(defn find-category [chat-id cat-id]
  (some #(when (= (str/upper-case (:id %)) cat-id) %) (config-categories/get-user-categories chat-id)))