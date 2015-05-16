(ns moon-dweller.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [moon-dweller.views :as views]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] (views/core))
  (route/not-found "Not Found!"))

(def app
  (wrap-defaults app-routes site-defaults))
