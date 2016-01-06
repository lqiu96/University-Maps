(ns university-maps.core
  (require
    [clojure.string :as str :only (split)]
    [clj-facebook-graph [client :as client]]
    [clojure.java.io :as io])
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful]
    [clj-facebook-graph.auth])
  (:import
    (twitter.callbacks.protocols SyncSingleCallback))
  (:gen-class
    :name "UniversityData"
    :methods [#^{:static true} [getData [String String] void]]))

(defn get-file-path
  "Given a file path, it gets all the files in the current directory
  and searches for the files."
  [file-name]
  (let [fs (file-seq (io/file (System/getProperty "user.dir")))
        file-vec (vec fs)]
    (.getPath (first (filter
                       #(and (not (.isDirectory %))
                             (= file-name (.getName %)))
                       file-vec)))))

; Reads the data from UnivDataInfo.csv file. Data is split into 4 columsn for each
; university: name, latitude, longitude, hash-tag, and facebook-id. Each university is a hash-map
; inside an outer list
(def university-data
  ;(let [data-string (slurp "src/university_maps/UnivDataInfo.csv")
  (let [data-string (slurp (get-file-path "UnivDataInfo.csv"))
        all-uni-vec (str/split data-string #"\r\n")
        all-individual-uni-vec (map #(str/split % #",") all-uni-vec)]
    (map #(zipmap [:uni-name :lat :long :uni-hash-tag :uni-facebook-id] %) all-individual-uni-vec)))

; Reads the data from WordSentiment.csv file. Data is split into two colums for each
; word: Word and the corresponding sentiment value. Each word is a hashtag inside an
; outer list
(def word-sentiment-data
  ;(let [line (slurp "src/university_maps/WordSentiment.csv")
  (let [line (slurp (get-file-path "WordSentiment.csv"))
        all-word-vector (str/split line #"\n")]
    (apply hash-map (flatten (map #(str/split % #",") all-word-vector)))))

(defn sum-sentiment
  "Given the text from a tweet, it sums up the total sentiment inside the text
  by adding the corresponding sentiment value for each word, or 0 if the word
  does not have a sentiment"
  [text]
  (if (nil? text)
    0
    (let [word-values word-sentiment-data
          tweet-words (map str/lower-case (str/split text #" "))]
      (reduce + (map #(read-string (get word-values % "0")) tweet-words)))))

; Create the ouath credentials to be able to make twitter API calls
(def my-creds (make-oauth-creds "MYzAR76HJ1l3abTtpbaAldFQs"
                                "xoViuhYOFQrhGmPXKdaoVVH1svrnpwZpWUVwKhxCF9P4jQdwDp"
                                "2987971120-Iw5cvmU86JsORVpFNJmFk2el4g2xP8zVG7gpWpW"
                                "opMJOnxrwPTsMUhlx9eYGgVcFjTZgTWnojBWHMK33DIPi"))

(defn search-university-by-hashtag
  "Searches twitter given a hashtag and returns the JSON response.
  It gets ten of the most recent english responses back"
  [uni-hashtag]
  (search-tweets :oauth-creds my-creds
                 :callbacks (SyncSingleCallback. response-return-body
                                                 response-throw-error
                                                 exception-rethrow)
                 :params {:q                uni-hashtag
                          :count            10
                          :lang             "en"
                          :result-type      "recent"
                          :include-entities false}))

(defn get-university-tweets
  "Based on the hash-tag, it gets the responses puts the each text from the tweet
  in a list of strings"
  [uni-hashtag]
  (->> (get-in (search-university-by-hashtag uni-hashtag) [:statuses])
       (map #(get % :text))))

; Creates a facebook authentication of an app access token
(def facebook-auth {:access-token "1511065829189005|mvSxiANHsZZrtreOpRVwRJ6dovA"})

(defn get-facebook-page-feed
  "Based on the page id, it gets the list of 10 posts on the page's feed.
  Facebook Graph API returns a list of JSON Responses and it only keeps a
  list of the messages on the page's feed."
  [page-id]
  (map
    #(get % :message)
    (with-facebook-auth facebook-auth (client/get [page-id :feed] {:query-params {:limit 10} :extract :data}))))

(defn write-to-file
  "Takes in a file location a list of the lines to be put into the file.
  Iterates through the list and appends it to the end of the file"
  [file-name uni-csv-data]
  (doseq [line uni-csv-data]
    (spit file-name line :append true)))

(defn create-line
  "Creates a line to be inputted into the csv file given university data
  (name, lat, long, hashtag, facebook id) and a tweet. Creates a line
  with the data deliminated by a comma"
  [uni text]
  (let [line (if (nil? text)
               ""
               (str/replace text #"[.,?!\r\n\t]" ""))]
    (str (get uni :uni-name) "," (get uni :lat) "," (get uni :long) "," (get uni :uni-hash-tag) "," (get uni :uni-facebook-id)
         "," line "," (sum-sentiment line) "\n")))

(defn create-file
  "Checks if the file location exists. If not, it creates the file all
  the necessary parent directories"
  [file-location]
  (if (.exists (io/file file-location))
    nil
    (io/make-parents file-location)))

(defn get-data
  "For each university, it gets the tweets and facebook page feed,
  calculates the sentiment for each post, and puts each line back
  into the respective csv files"
  [twitter-location facebook-location]
  (do
    (create-file twitter-location)
    (create-file facebook-location)
    (doseq [uni university-data]
      (println (get uni :uni-name))
      (let [twitter-data (->> (get uni :uni-hash-tag)
                              (get-university-tweets)
                              (map #(create-line uni %))
                              (future))
            facebook-data (->> (get uni :uni-facebook-id)
                               (get-facebook-page-feed)
                               (map #(create-line uni %))
                               (future))]
        (do
          (write-to-file twitter-location @twitter-data)
          (write-to-file facebook-location @facebook-data))))
    (println "Done")))

(defn -getData
  "Simple wrapper function for Java class that can call Clojure function (get-data)
  Takes in two parameters which determine the locations to store the Twitter and Facebook
  data respectively"
  [twitter-location facebook-location]
  (get-data twitter-location facebook-location))

(defn -main
  "Main function that will be called when the Jar file is called by itself.
  If two locations are not provided, it will output to a default directory (called output)"
  [& args]
  (if (not (= (count args) 2))
    (get-data "output/TwitterOutput.csv" "output/FacebookOutput.csv")
    (get-data (first args) (second args))))