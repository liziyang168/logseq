(ns frontend.handler.events.search
  (:require [frontend.state :as state]))

(defn capture-editor-info!
  [context]
  (let [explicit-editor-info? (contains? context :editor-info)
        editor-info (if explicit-editor-info?
                      (:editor-info context)
                      (or (state/get-editor-info)
                          (get-in @state/state [:search/args :editor-info])))]
    (state/update-state! :search/args
                         #(cond-> (or % {})
                            editor-info (assoc :editor-info editor-info)
                            (and explicit-editor-info? (nil? editor-info))
                            (dissoc :editor-info)))))
