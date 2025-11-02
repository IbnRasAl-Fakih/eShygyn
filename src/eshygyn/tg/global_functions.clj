(ns eshygyn.tg.global-functions
  (:require [telegrambot-lib.core :as tg]))

(defn delete-inline-query [bot chat-id msg-id]
  (tg/edit-message-reply-markup bot {:chat_id chat-id :message_id msg-id :reply_markup {:inline_keyboard []}}))

(defn delete-previous-inline-query [bot chat-id msg-id]
  (delete-inline-query bot chat-id (dec msg-id)))