(ns the-village.web.core
  (:require [quil.core :as q :include-macros true]))

;; point utils
(def x first)
(def y second)

;; see dev/palette.png
(def background-color [0x23 0xCE 0x6B])
(def grid-color [0x03 0xAE 0x4B])
(def icon-color [0xC1 0x59 0x24])

;; 1 frame / second
(def frame-rate 30)

;; the canva size in pixel
;; this is the visible part of the village
;; conceptually a village is infinite
;; values are incremented to make nice grid
(def canva-size (map inc [500 400]))
(def max-x (first canva-size))
(def max-y (second canva-size))

;; the size of a cell in the village
;; for instance dwelling cell
(def cell-size 50)

(defn setup
  []
  (q/frame-rate frame-rate)
  (apply q/background background-color))

(defn make-grid
  [[max-x max-y]]
  (apply q/stroke grid-color)
  ;; horizontal
  (doseq [hline-i (range (/ max-x cell-size))
          :let [x (* cell-size hline-i)]]
    (q/line x 0 x max-y))
  ;; vertical
  (doseq [vline-i (range (/ max-y cell-size))
          :let [y (* cell-size vline-i)]]
    (q/line 0 y max-x y)))

(defn debug-mouse []
  (doseq [[ind capt fn] [[0 "mouse-button" q/mouse-button]
                         [1 "mouse-pressed?" q/mouse-pressed?]
                         [2 "mouse-x" q/mouse-x] [3 "mouse-y" q/mouse-y]
                         [4 "pmouse-x" q/pmouse-x] [5 "pmouse-y" q/pmouse-y]]]
    (q/fill 255 0 0)
    (q/text (str capt " " (fn)) 10 (+ (* 20 ind) 20))))

(defn make-well-icon
  []
  (apply q/fill icon-color)
  (q/rect 0 (- (y canva-size) cell-size)
          cell-size cell-size
          5)
  (apply q/stroke background-color)
  (q/stroke 0 0 255)
  (q/text "WELL" 0 max-y))   

(defn draw []
  (apply q/background background-color)
  (make-grid canva-size)
  (debug-mouse)
  (make-well-icon))

(q/defsketch app
             :draw draw
             :host "app"
             :size canva-size)