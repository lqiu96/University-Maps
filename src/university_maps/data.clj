;
; The MIT License
;
; Copyright 2015 Lawrence.
;
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.
;
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
; THE SOFTWARE.
;

(ns university-maps.data
  (:require
    [clojure.string :as str :only (replace)]
    [university-maps.config :as config])
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful])
  (:import
    (twitter.callbacks.protocols SyncSingleCallback)
    (java.text SimpleDateFormat)
    (java.util Date)))

; Create the ouath credentials to be able to make twitter API calls
(def my-creds (make-oauth-creds config/twitter-consumer-key
                                config/twitter-consumer-secret
                                config/twitter-access-token
                                config/twitter-access-toke-secret))

(defn search-twitter-query
  [search-query num-tweets geocode date]
  (search-tweets :oauth-creds my-creds
                 :callbacks (SyncSingleCallback. response-return-body
                                                 response-throw-error
                                                 exception-rethrow)
                 :params {:q           search-query
                          :count       num-tweets
                          :geocode     geocode
                          :until       date
                          :lang        "en"}))

(defn get-twitter-query-text
  [uni-hashtag num-tweets geo-code date]
  (->> (get-in (search-twitter-query uni-hashtag num-tweets geo-code date) [:statuses])
       (map #(get % :text))))

(defn get-query-by-city
  [query geo-code]
  (doseq [status (get-twitter-query-text query 10 geo-code (.format (SimpleDateFormat. "yyyy-MM-dd") (Date.)))]
    (println (str/replace status #"[.,?!'\"\r\n\t]" ""))))

(println "Philadelphia: ")
(get-query-by-city "#Tesla" "39.95,-75.1667,50mi")
(println "Palo Alto: ")
(get-query-by-city "#Tesla" "37.4292,-122.1381,50mi")