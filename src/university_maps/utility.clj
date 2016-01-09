(ns university-maps.utility
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn get-file-path
  "Given a file path, it gets all the files in the current directory
  and searches for the files."
  [file-name]
  (let [fs (file-seq (io/file (System/getProperty "user.dir")))
        file-vec (vec fs)
        file-loc (first (filter
                          #(and (not (.isDirectory %))
                                (= file-name (.getName %)))
                          file-vec))]
    (if (nil? file-loc)
      nil
      (.getPath file-loc))))

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
  [file-name data]
  (doseq [line data]
    (spit file-name line :append true)))

; Reads the data from WordSentiment.csv file. Data is split into two colums for each
; word: Word and the corresponding sentiment value. Each word is a hashtag inside an
; outer list
(def word-sentiment-data
  (let [file-loc (get-file-path "WordSentiment.csv")
        line (if (nil? file-loc)
               ","
               (slurp file-loc))]
    (->> (str/split line #"\n")
         (map #(str/split % #","))
         (flatten)
         (apply hash-map))))

(defn sum-sentiment
  "Given the text from a tweet, it sums up the total sentiment inside the text
  by adding the corresponding sentiment value for each word, or 0 if the word
  does not have a sentiment"
  [text]
  (if (= (count word-sentiment-data) 0)
    (do
      (println "Warning: Unable to load WordSentiment.csv. Check to make
      sure that WordSentiment.csv is in file. Returned 0 as sum")
      0)
    (if (nil? text)
      0
      (let [word-values word-sentiment-data]
        (->> (map str/lower-case (str/split text #" "))
             (map #(read-string (get word-values % "0")))
             (reduce +))))))