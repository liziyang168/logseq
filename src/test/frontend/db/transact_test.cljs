(ns frontend.db.transact-test
  (:require [cljs.test :refer [async deftest is]]
            [frontend.db.transact :as db-transact]
            [promesa.core :as p]))

(deftest worker-call-normalizes-request-errors
  (async done
    (let [rejected (atom 0)
          requests [#(p/rejected (js/Error. "rejected"))
                    #(p/resolved (js/Error. "fulfilled error"))
                    #(p/resolved {:ex-data {:message "worker error"}})
                    #(throw (js/Error. "synchronous error"))]]
      (-> (p/all
           (map (fn [request]
                  (-> (db-transact/worker-call request)
                      (p/then (fn [_] (is false "worker call should reject")))
                      (p/catch (fn [_] (swap! rejected inc)))))
                requests))
          (p/then (fn [] (is (= 4 @rejected))))
          (p/finally done)))))
