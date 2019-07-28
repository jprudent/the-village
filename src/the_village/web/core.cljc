(ns the-village.web.core
  (:require [quil.core :as q :include-macros true]
            [the-village.utils.missing :as u]))

;; point utils
(def x first)
(def y second)

;; see dev/palette.png
(def background-color [0x23 0xCE 0x6B])
(def grid-color [0x03 0xAE 0x4B])
(def icon-color [0xC1 0x59 0x24])
(def other-color [0xFF 0xBE 0x5B])

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
(def toolbar [0 (- (y canva-size) icon-size)])

(defrecord Icon
  [text
   toolbar-position
   dragging?])

(def well-icon
  (map->Icon {:toolbar-position 0
              :dragging?        false
              :text             "WELL"}))

(def dwell-icon
  (map->Icon {:toolbar-position 1
              :dragging?        false
              :text             "DWELL"}))

(def bakery-icon
  (map->Icon {:toolbar-position 2
              :dragging?        false
              :text             "BAKERY"}))

(defonce state (atom {:toolbar-icons
                                {:well   well-icon
                                 :dwell  dwell-icon
                                 :bakery bakery-icon}
                  :village-grid {}}))

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

(defn draw-icon
  ([text x1 y1 icon-bg-color]
   "draw the icon"
   (q/with-fill icon-bg-color
     (q/rect x1 y1 icon-size icon-size 5))
   (q/with-stroke 15
     (q/with-fill background-color
       (q/text-align :center :center)
       (q/text text x1 y1 icon-size icon-size)))))

(defn draw-toolbar-icon
  [icon]
  (draw-icon (:text icon)
             (+ (x toolbar) (* (:toolbar-position icon) icon-size))
             (y toolbar)
             icon-color))

(defn draw-selected-toolbar-icon
  [icon]
  (draw-icon (:text icon)
             (+ (x toolbar) (* (:toolbar-position icon) icon-size))
             (y toolbar)
             (inverse-color icon-color)))

(defn draw-icon-at
  [icon x y bg-color]
  (draw-icon (:text icon) x y bg-color))

(defn cell-at-mouse-pos
  []
  [(- (q/mouse-x) (mod (q/mouse-x) cell-size))
   (- (q/mouse-y) (mod (q/mouse-y) cell-size))])

(defn preview-icon-at-mouse
  [icon]
  (let [[x y] (cell-at-mouse-pos)]
    (draw-icon-at icon x y (inverse-color icon-color))))

(defn draw-icon-at-mouse
  [icon]
  (let [[x y] (cell-at-mouse-pos)]
    (draw-icon-at icon x y icon-color)))

(defn dragging-icon? [] (:dragging-icon @state))

(defn dragging-icon
  []
  (->> (:toolbar-icons @state)
       (filter (fn [[_ icon :as kv]] (:dragging? icon)))
       (first)))

(defn move-mouse-icon
  []
  (when-let [[icon-name _] (and (not (q/mouse-pressed?))
                                (dragging-icon))]
    (do
      (swap! state
             (fn [state]
               (-> state
                   (update :toolbar-icons
                           u/map-vals #(assoc % :dragging? false))
                   (update :village-grid
                           assoc (cell-at-mouse-pos) icon-name))))))

  (when-let [icon-name (and (q/mouse-pressed?)
                            (< (y toolbar) (q/mouse-y) (y canva-size))
                            (some (fn [[icon-name icon]]
                                    (when (< (x toolbar)
                                             (q/mouse-x)
                                             (+ icon-size
                                                (* (:toolbar-position icon) icon-size)))
                                      icon-name))
                                  (:toolbar-icons @state)))]
    (swap! state assoc-in [:toolbar-icons icon-name :dragging?] true))

  (when-let [[_ icon] (dragging-icon)]
    (do (preview-icon-at-mouse icon)
        (draw-icon-at icon
                      (- (q/mouse-x) half-icon-size)
                      (- (q/mouse-y) half-icon-size)
                      icon-color))))

(defn mouse-handler
  []
  (move-mouse-icon))

(defn draw-toolbar-icons
  []
  (doseq [{:keys [dragging?] :as icon} (vals (:toolbar-icons @state))]
    (if dragging?
      (draw-selected-toolbar-icon icon)
      (draw-toolbar-icon icon))))

(defn draw-grid-icons
  []
  (doseq [[[x y] icon-name] (:village-grid @state)
          :let [icon (get-in @state [:toolbar-icons icon-name])]]
    (draw-icon-at icon x y icon-color)))

(defn draw []
  (apply q/background background-color)
  (make-grid canva-size)
  (debug-mouse)
  (draw-toolbar-icons)
  (draw-grid-icons)
  (mouse-handler))

(q/defsketch app
             :draw draw
             :host "app"
             :size canva-size)   