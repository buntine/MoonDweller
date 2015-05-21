(defproject moon-dweller "0.1.1-SNAPSHOT"
  :description "Moon Dweller: An out-of-this-world text adventure."
  :url "http://moondweller.io/"
  :min-lein-version "2.1.2"
  :dependencies [[org.clojure/clojure "1.7.0-beta3"]
                 [org.clojure/clojurescript "0.0-3269"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [prismatic/dommy "1.1.0"]
                 [ring/ring-defaults "0.1.2"]]
  :plugins [[lein-ring "0.9.3"]
            [lein-beanstalk "0.2.7"]
            [lein-cljsbuild "1.0.6"]]
  :cljsbuild {
    :builds [{
      :source-paths ["src-cljs"]
      :compiler {
        :output-to "resources/public/js/main.js"
        :optimizations :advanced
        :pretty-print false}}]}
  :ring {:handler moon-dweller.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
