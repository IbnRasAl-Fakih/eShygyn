(ns eshygyn.tg.new-expense
  (:require [clojure.string :as str]
            
            [eshygyn.tg.category :as tg-category])
  (:import (java.time ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter DateTimeParseException)))

(def fmt-out (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm"))

(def almaty-tz (ZoneId/of "Asia/Almaty"))

(defn categories-kb [chat-id prefix]
  (let [cats (seq (tg-category/get-user-categories chat-id))]
    {:inline_keyboard
     (->> (or cats [])
          (map (fn [{:keys [id emoji title]}]
                 [{:text (str emoji " " title) :callback_data (str prefix (str/upper-case id))}]))
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