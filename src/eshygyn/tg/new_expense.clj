(ns eshygyn.tg.new-expense
  (:require [clojure.string :as str]

            [eshygyn.tg.category :as tg-category]
            [eshygyn.db.db :as db]
            [eshygyn.tg.user-session :as user-session])
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

(defn javatimestamp->zoneddatetime [date]
  (-> date
      .toInstant
      (java.time.ZonedDateTime/ofInstant almaty-tz)
      (.format fmt-out)))

(defn parse-amount [s]
  (let [clean (-> s (str/replace #"\s+" "") (str/replace #"," "."))]
    (try
      (let [bd (bigdec clean)]
        (when (pos? bd) bd))
      (catch Exception _ nil))))

(defn pretty-amount [n]
  (let [bd (BigDecimal. (str n))]
    (if (zero? (.remainder bd BigDecimal/ONE))
      (.toPlainString (.setScale bd 0 java.math.RoundingMode/FLOOR))
      (.toPlainString bd))))

(defn group-3-from-right [s]
  (->> s
       str/reverse
       (partition-all 3)
       (map (partial apply str))
       (interpose " ")
       (apply str)
       str/reverse))

(defn divide-numbers [text]
  (let [[int-part frac-part] (str/split text #"\.")]
    (str (group-3-from-right int-part)
         (when frac-part
           (str "." frac-part)))))

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

(defn get-total-of-expenses [chat-id]
  (let [total (:total (user-session/get-session chat-id))]
    (if (nil? total)
      (let [total-from-db (:count (db/get-number-of-expenses chat-id))]
       (user-session/set-stage! chat-id :expenses-list {:total total-from-db})
        total-from-db)
      total)))

(defn expenses-list [chat-id offset]
  (str/join "\n\n" (mapv
                    (fn [idx {:keys [expenses/category expenses/amount expenses/date expenses/comment]}]
                      (str (inc (+ offset idx)) ". "
                           (divide-numbers (pretty-amount amount)) "тг - "
                           category " - "
                           (javatimestamp->zoneddatetime date)
                           (when (and (not= nil comment) (not= "" comment)) (str "\n" comment))))
                    (range)
                    (db/get-expenses-with-offset chat-id offset))))

(defn correct-offset [offset]
  (if (> offset 0)
    offset
    0))
    