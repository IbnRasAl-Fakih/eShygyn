(ns eshygyn.tg.handle-messages
  (:require [eshygyn.db.db :as db]
            [eshygyn.tg.commands :as commands]
            [eshygyn.tg.messages :as messages]
            [eshygyn.tg.new-expense :as new-expense]
            [eshygyn.tg.user-session :as user-session]
            [eshygyn.tg.category :as tg-category])
  (:import (java.time.format DateTimeFormatter DateTimeParseException)))

(def listed-commands 
  {"/start"           commands/start
   "/add"             commands/add-expense
   "/cancel"          commands/cancel
   "/change"          commands/change-category
   "/add-category"    commands/add-category
   "/skip"            commands/skip
   "/delete-category" commands/delete-category})

(defn text-not-listed-handler [bot chat-id stage user-id text draft]
  (cond
    (nil? stage) (messages/unknown-message bot chat-id)

    (= stage :enter-amount)
    (let [amt (new-expense/parse-amount text)]
      (if (nil? amt)
        (messages/wrong-amount bot chat-id)
        (do
          (user-session/set-stage! chat-id :enter-time :amount amt)
          (messages/next-time bot chat-id))))

    (= stage :enter-time)
    (let [dt (new-expense/parse-datetime text)]
      (if (nil? dt)
        (messages/wrong-time bot chat-id)
        (let [{:keys [category amount]} draft]
          (db/create-expence user-id category amount dt)
          (user-session/clear-session! chat-id)
          (messages/expense-created bot chat-id category amount dt))))
    
    (= stage :enter-category-id)
    (let [parsed-id (tg-category/parse-text text)
          is-unique (tg-category/is-unique parsed-id chat-id)]
      (if is-unique
        (if (not= 0 (count parsed-id))
          (do
            (user-session/set-stage! chat-id :enter-category-title :category-id parsed-id)
            (messages/next-category-title bot chat-id)) 
          (messages/invalid-category-id bot chat-id))
        (messages/is-not-unique-category-id bot chat-id)))
    
    (= stage :enter-category-title)
    (do
      (user-session/set-stage! chat-id :enter-category-emoji :category-title text)
      (messages/next-category-emoji bot chat-id))
    
    (= stage :enter-category-emoji)
    (let [{:keys [category-id category-title]} draft]
      (tg-category/add-category chat-id {:id category-id, :emoji text, :title category-title})
      (user-session/clear-session! chat-id)
      (messages/category-created bot chat-id category-title text))

    :else (messages/unknown-message-with-stage bot chat-id)))

(defn handle-message [bot message]
  (println "\033[34mINFO\033[0m" message) ; delete
  (try
    (let [chat-id               (get-in message [:chat :id])
          text                  (:text message)
          user-id               (get-in message [:from :id])
          {:keys [stage draft]} (user-session/get-session chat-id)]
      (println "\033[34mINFO\033[0m" "stage = " stage) ; delete
      (if (= text "/authorize")
        (commands/authorize bot user-id chat-id (get-in message [:from :first_name]) (get-in message [:from :username]))
    
        (if (db/is-authorized chat-id)
          
          (let [function (get listed-commands text)]
            (if (nil? function)
              (text-not-listed-handler bot chat-id stage user-id text draft)
              (apply function [bot chat-id]))) 
          
          (messages/authorize bot chat-id))))
    (catch Exception e
      (println "\033[91mERROR\033[0m" "Ошибка в handle-message:" e))))