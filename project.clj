(defproject university-maps "0.1.2"
  :description "Gets tweets and facebook posts about universities,
   does sentiment analysis based on the text and returns the data
   in a csv file with data that can be presented in Google Maps Javascript Project"
  :url "https://github.com/lqiu96/University-Maps"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [twitter-api "0.7.8"]
                 [mavericklou/clj-facebook-graph "0.5.3"]]
  :aot [university-maps.core]
  :main university-maps.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
