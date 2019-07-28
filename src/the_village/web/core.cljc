(ns the-village.web.core
  (:require [quil.core :as q :include-macros true]))

;; point utils
(def x first)
(def y second)

;; see dev/palette.png
(def background-color [0x23 0xCE 0x6B])
(def grid-color [0x03 0xAE 0x4B])
(def icon-color [0xC1 0x59 0x24])
(def highlight-color [0xAA 0x40 0x7E])
(defn inverse-color [color]
  (map #(- 0xFF %) color))

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

;; [x y]
(def icon-size cell-size)
(def half-icon-size (int (/ icon-size 2)))
(def toolbar-size [0 (- (y canva-size) icon-size)])
(def toolbar-nb-icons 1)

(defonce state (atom {:dragging-icon false}))

(defn setup
  []
  (q/frame-rate frame-rate)
  (apply q/background background-color))

(defn make-grid
  [[max-x max-y]]
  (q/with-stroke grid-color
    ;; horizontal
    (doseq [hline-i (range (/ max-x cell-size))
            :let [x (* cell-size hline-i)]]
      (q/line x 0 x max-y))
    ;; vertical
    (doseq [vline-i (range (/ max-y cell-size))
            :let [y (* cell-size vline-i)]]
      (q/line 0 y max-x y))))

(defn debug-mouse []
  (doseq [[ind capt fn] [[0 "mouse-button" q/mouse-button]
                         [1 "mouse-pressed?" q/mouse-pressed?]
                         [2 "mouse-x" (comp int q/mouse-x)] [3 "mouse-y" (comp int q/mouse-y)]
                         [4 "pmouse-x" q/pmouse-x] [5 "pmouse-y" (comp int q/pmouse-y)]]]
    (q/with-fill [255 0 0]
      (q/text-align :left)
      (q/text (str capt " " (fn)) 10 (+ (* 20 ind) 20)))))

(defn make-icon
  ([x1 y1 icon-bg-color text]
   (q/with-fill icon-bg-color
     (q/rect x1 y1 icon-size icon-size 5))
   (q/with-stroke 15
     (q/with-fill background-color
       (q/text-align :center :center)
       (q/text text x1 y1 icon-size icon-size)))))

(defn make-well-icon
  ([] (make-well-icon icon-color))
  ([icon-bg-color] (make-well-icon 0 (- (y canva-size) icon-size) icon-bg-color))
  ([x1 y1 icon-bg-color]
   (make-icon x1 y1 icon-bg-color "WELL")))

(defn highlight-cell-at-mouse
  []
  (let [x (- (q/mouse-x) (mod (q/mouse-x) cell-size))
        y (- (q/mouse-y) (mod (q/mouse-y) cell-size))]
    (make-well-icon x y (inverse-color icon-color))))

(defn dragging-icon? [] (:dragging-icon @state))

(defn move-mouse-icon
  []

  (when (and (not (q/mouse-pressed?)) (dragging-icon?))
    (swap! state assoc :dragging-icon false))

  (when (and (q/mouse-pressed?)
             (< (x toolbar-size) (q/mouse-x) (* toolbar-nb-icons icon-size))
             (< (y toolbar-size) (q/mouse-y) (y canva-size)))
    (swap! state assoc :dragging-icon true))

  (when (dragging-icon?)
    (do (make-well-icon (inverse-color icon-color))
        (highlight-cell-at-mouse)
        (make-well-icon (- (q/mouse-x) half-icon-size)
                        (- (q/mouse-y) half-icon-size)
                        icon-color))))

(defn mouse-handler
  []
  (move-mouse-icon))

(defn keyboard-handler
  [])

(defn draw []
  (apply q/background background-color)
  (make-grid canva-size)
  (debug-mouse)
  (make-well-icon)
  (mouse-handler)
  (keyboard-handler))

(q/defsketch app
             :draw draw
             :host "app"
             :size canva-size)