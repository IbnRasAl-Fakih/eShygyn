(ns eshygyn.tg.messages
  (:require [telegrambot-lib.core :as tg]))

(defn authorize [bot chat_id]
  (tg/send-message bot chat_id "🚫 Вы не авторизованы!\nПожалуйста, сначала выполните авторизацию, чтобы использовать бота.\n\nВведите команду /authorize или нажмите кнопку \"Авторизоваться\"" {:reply_markup {:inline_keyboard [[{:text "Авторизоваться" :callback_data "CMD_AUTHORIZE"}]] :resize_keyboard true}}))