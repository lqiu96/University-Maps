(ns university-maps.data
  (:require
    [clojure.string :as str :only (replace)]
    [university-maps.config :as config]
    [clojure.java.io :as io])
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful])
  (:import
    (twitter.callbacks.protocols SyncSingleCallback)
    (java.time LocalDate)))

; Create the ouath credentials to be able to make twitter API calls
(def my-creds (make-oauth-creds config/twitter-consumer-key
                                config/twitter-consumer-secret
                                config/twitter-access-token
                                config/twitter-access-toke-secret))

(defn search-twitter-query
  [search-query date]
  (search-tweets :oauth-creds my-creds
                 :callbacks (SyncSingleCallback. response-return-body
                                                 response-throw-error
                                                 exception-rethrow)
                 :params {:q     search-query
                          :count 100
                          :until date
                          :lang  "en"}))

(defn get-twitter-query-text
  [hash-tag date]
  (->> (get-in (search-twitter-query hash-tag date) [:statuses])
       (map #(get % :text))))

(defn create-line
  [date status]
  (str date "," (str/replace status #"[.,?!'\"\r\n\t]" "") "\n"))

(defn create-file
  "Checks if the file location exists. If not, it creates the file all
  the necessary parent directories"
  [& file-locations]
  (doseq [file-location file-locations]
    (if (.exists (io/file file-location))
      nil
      (io/make-parents file-location))))

(defn write-to-file
  "Takes in a file location a list of the lines to be put into the file.
  Iterates through the list and appends it to the end of the file"
  [file-name status-data]
  (doseq [line status-data]
    (spit file-name line :append true)))

(defn write-list-to-file
  [list file-location]
  (doseq [line list]
    (write-to-file file-location (str (str/replace line #"[.,?!'\"\r\n\t]" "") "\n"))))

(defn search-query-by-day
  [query file-location]
  (let [dates (->> (LocalDate/now)
                   (iterate #(.minusDays % 1))
                   (take 7)
                   (map #(str %))
                   (reverse))]
    (do
      (create-file file-location)
      (println "Starting...")
      (let [data-list (map #(get-twitter-query-text query %) dates)]
        (doseq [data data-list]
          (write-list-to-file data file-location)))
      (println "Done."))))

(search-query-by-day "#Tesla" "output/WeekQueryOutput.csv")