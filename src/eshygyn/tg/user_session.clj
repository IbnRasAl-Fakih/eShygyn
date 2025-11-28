(ns eshygyn.tg.user-session)

(def user-session (atom {}))

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