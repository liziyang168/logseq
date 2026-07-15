(ns frontend.db.transact-test
  (:require [cljs.test :refer [async deftest is]]
            [frontend.db.transact :as db-transact]
            [frontend.state :as state]
            [frontend.util :as util]
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

(deftest apply-outliner-ops-pins-submission-repo
  (async done
    (let [current-repo (atom "graph-a")
          calls (atom [])]
      (-> (p/with-redefs
            [util/node-test? false
             state/get-current-repo #(deref current-repo)
             state/get-editor-info (constantly {:block-uuid (random-uuid)})
             state/<invoke-db-worker
             (fn [method repo & _args]
               (swap! calls conj [method repo])
               (reset! current-repo "graph-b")
               (p/resolved nil))]
            (db-transact/apply-outliner-ops nil [[:test []]] {}))
          (p/then (fn []
                    (is (= ["graph-a" "graph-a"] (mapv second @calls)))))
          (p/catch (fn [error] (is false (str error))))
          (p/finally done)))))
