(ns the-village.web.grid
  (:require [quil.core :as q :include-macros true]))


(defn ->grid
  "Defines a grid of size"
  [size]
  {:size size
   :cells {}})

(defn make-grid
  [[max-x max-y] & {:keys [cell-size grid-color]}]
  (q/with-stroke grid-color
    ;; horizontal
    (doseq [hline-i (range (/ max-x cell-size))
            :let [x (* cell-size hline-i)]]
      (q/line x 0 x max-y))
    ;; vertical
    (doseq [vline-i (range (/ max-y cell-size))
            :let [y (* cell-size vline-i)]]
      (q/line 0 y max-x y))))