(ns university-maps.core
  (require
    [clojure.string :as str :only (split replace)]
    [clj-facebook-graph [client :as client]]
    [clojure.java.io :as io]
    [university-maps.config :as config]
    [university-maps.utility :as util])
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
    :methods [#^{:static true} [getData [String String] void]
              #^{:static true} [getData [String String int] void]]))

; Reads the data from UnivDataInfo.csv file. Data is split into 4 columsn for each
; university: name, latitude, longitude, hash-tag, and facebook-id. Each university is a hash-map
; inside an outer list
(def university-data
  (let [file-loc (util/get-file-path "UnivDataInfo.csv")
        data-string (if (nil? file-loc)
                      ""
                      (slurp file-loc))]
    (->> (str/split data-string #"\r\n")
         (map #(str/split % #","))
         (map #(zipmap [:uni-name :lat :long :uni-hash-tag :uni-facebook-id] %)))))

; Create the ouath credentials to be able to make twitter API calls
(def my-creds (make-oauth-creds config/twitter-consumer-key
                                config/twitter-consumer-secret
                                config/twitter-access-token
                                config/twitter-access-toke-secret))

(defn search-university-by-hashtag
  "Searches twitter given a hashtag and returns the JSON response.
  It gets a certain number (defualt 15) of the most recent english responses back"
  [uni-hashtag num-tweets]
  (search-tweets :oauth-creds my-creds
                 :callbacks (SyncSingleCallback. response-return-body
                                                 response-throw-error
                                                 exception-rethrow)
                 :params {:q                uni-hashtag
                          :count            num-tweets
                          :lang             "en"
                          :result-type      "recent"
                          :include-entities false}))

(defn get-university-tweets
  "Based on the hash-tag, it gets the responses puts the each text from the tweet
  in a list of strings"
  [num-tweets uni-hashtag]
  (->> (get-in (search-university-by-hashtag uni-hashtag num-tweets) [:statuses])
       (map #(get % :text))))

; Creates a facebook authentication of an app access token
(def facebook-auth {:access-token config/facebook-access-token})

(defn get-facebook-page-feed
  "Based on the page id, it gets the list of 10 posts on the page's feed.
  Facebook Graph API returns a list of JSON Responses and it only keeps a
  list of the messages on the page's feed."
  [num-posts page-id]
  (map
    #(get % :message)
    (with-facebook-auth
      facebook-auth
      (client/get [page-id :feed] {:query-params {:limit num-posts} :extract :data}))))

(defn create-line
  "Creates a line to be inputted into the csv file given university data
  (name, lat, long, hashtag, facebook id) and a tweet. Creates a line
  with the data deliminated by a comma"
  [uni text]
  (let [line (if (nil? text)
               ""
               (str/replace text #"[.,?!'\"\r\n\t]" ""))]
    (str (get uni :uni-name) "," (get uni :lat) "," (get uni :long) ","
         (get uni :uni-hash-tag) "," (get uni :uni-facebook-id) ","
         line "," (util/sum-sentiment line) "\n")))

(defn create-file
  "Checks if the file location exists. If not, it creates the file all
  the necessary parent directories"
  [& file-locations]
  (doseq [file-location file-locations]
    (if (.exists (io/file file-location))
      nil
      (io/make-parents file-location))))

(defn get-data
  "For each university, it gets the tweets and facebook page feed,
  calculates the sentiment for each post, and puts each line back
  into the respective csv files"
  ([twitter-location facebook-location]
   (get-data twitter-location facebook-location 15))
  ([twitter-location facebook-location num-results]
   (if (or (< num-results 1) (= (count university-data) 1))
     (println "Error: Cannot get the data. Check to make sure both
     UnivDataInfo.csv is in the directory. Also check to make sure you
     are asking for one or more tweets/posts per university")
     (do
       (create-file twitter-location facebook-location)
       (println "Start")
       (doseq [uni university-data]
         (println (str "Getting " (get uni :uni-name) "'s data"))
         (let [twitter-data (->> (get uni :uni-hash-tag)
                                 (get-university-tweets num-results)
                                 (map #(create-line uni %))
                                 (future))
               facebook-data (->> (get uni :uni-facebook-id)
                                  (get-facebook-page-feed num-results)
                                  (map #(create-line uni %))
                                  (future))]
           (util/write-to-file twitter-location @twitter-data)
           (util/write-to-file facebook-location @facebook-data)))
       (println "Done")))))

(defn -getData
  "Simple wrapper function for Java class that can call Clojure function (get-data)
  Takes in two parameters which determine the locations to store the Twitter and Facebook
  data respectively"
  ([twitter-location facebook-location]
   (-getData twitter-location facebook-location 15))
  ([twitter-location facebook-location num-results]
   (if (or (nil? twitter-location) (nil? facebook-location) (< num-results 1))
     (get-data "output/TwitterOutput.csv" "output/FacebookOutput.csv" 15)
     (get-data twitter-location facebook-location num-results))))

(defn -main
  "Main function that will be called when the Jar file is called by itself.
  If two locations are not provided, it will output to a default directory (called output)
  with 10 of the most recent tweets/posts from each university. Otherwise, it
  it outputs to the desired place with the correct number of tweets/posts for each university"
  [& args]
  (if (or (< (count args) 3)
          (or (nil? (nth args 0)) (nil? (nth args 1)) (< (nth args 2) 1)))
    (get-data "output/TwitterOutput.csv" "output/FacebookOutput.csv")
    (get-data (nth args 0) (nth args 1) (nth args 2))))