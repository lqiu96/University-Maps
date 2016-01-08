(ns university-maps.data
  (:require
    [clojure.string :as str :only (replace)]
    [university-maps.config :as config]
    [clojure.java.io :as io]
    [university-maps.utility :as util])
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful])
  (:import
    (twitter.callbacks.protocols SyncSingleCallback)
    (java.time LocalDate))
  (:gen-class
    :name "TwitterSearch"
    :methods [#^{:static true} [SearchQuery [String String] void]]))

; Create the ouath credentials to be able to make twitter API calls
(def my-creds (make-oauth-creds config/twitter-consumer-key
                                config/twitter-consumer-secret
                                config/twitter-access-token
                                config/twitter-access-toke-secret))

(defn search-twitter-query
  "Searches twitter given a hashtag and returns the JSON response.
  It gets 100 of the most ppular english responses back on the certain day
  or up to the previous week if there are not enough tweets"
  [search-query date]
  (search-tweets :oauth-creds my-creds
                 :callbacks (SyncSingleCallback. response-return-body
                                                 response-throw-error
                                                 exception-rethrow)
                 :params {:q                search-query
                          :count            100
                          :until            date
                          :lang             "en"
                          :result-type      "mixed"
                          :include-entities false}))

(defn get-twitter-query-text
  "Based on the hash-tag, it gets the responses puts the each text from the tweet
  in a list of strings"
  [hash-tag date]
  (->> (get-in (search-twitter-query hash-tag date) [:statuses])
       (map #(get % :text))))

(defn create-line
  "Creates the line to add to the file. It removes all the extra information
  such as commas, periods, new line, tabvs, etc. The line, the sentiment sum
  and new line are added to te file"
  [text]
  (let [line (if (nil? text)
               nil
               (str/replace text #"[.,?!'\"\r\n\t]" ""))]
    (str line "," (util/sum-sentiment line) "\n")))

(defn search-query-by-day
  "For each day of the week, starting a week ago and ending today, it
  gets the list of tweets corresponding to the day and query and writes
  it to the file location supplied"
  [query file-location]
  (let [dates (->> (LocalDate/now)
                   (iterate #(.minusDays % 1))
                   (take 7)
                   (map #(str %))
                   (reverse))]
    (do
      (util/create-file file-location)
      (println "Starting...")
      (let [data-list (map #(get-twitter-query-text query %) dates)]
        (doseq [data data-list]
          (util/write-to-file file-location (map #(create-line %) data))))
      (println "Done."))))

(defn -SearchQuery
  "Simple Java Wrapper for the Clojure (search-query-by-day) function
  which takes in a wuery to search for and a file location where the
  data should be written to"
  [query file-location]
  (search-query-by-day query file-location))