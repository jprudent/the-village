(ns the-village.web.core
  (:require [quil.core :as q :include-macros true]
            [the-village.utils.missing :as u]
            [the-village.web.grid :as grid]
            [the-village.engine.villager :as villager]
            [the-village.engine.journey :as journey]
            [the-village.engine.path-finder :as path-finder]))

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

(def frame-rate 5)

;; the canva size in pixel
;; this is the visible part of the village
;; conceptually a village is infinite
;; values are incremented to make nice grid
(def canva-size (map inc [500 400]))
(def max-x (x canva-size))
(def max-y (y canva-size))

;; the size of a cell in the village
;; for instance dwelling cell
(def cell-size 50)

(def grid-size [(/ (dec max-x) cell-size)
                (/ (dec max-y) cell-size)])

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


;; IRL a person can easily walk 4km per hour
(def walking-speed-m-s (/ 3600 4000))

;; 1 second in the game is that much seconds IRL
;; if game-speed is 0.01, time passes 100 times
;; faster in game than IRL. So imagine you can run IRL
;; a 100m speed race in 10s (not bad!), if you lived in
;; the game, you would run it in 100ms. This ratio is used
;; to speed up the game, otherwise it would take forever
;; to walk a 15m cell !
(def game-speed (/ 1 12))

;; A cell should be more like 15 meters squared.
(def cell-m 15)


(defn find-path
  "find a path from coordinate to something matching pred"
  [{:keys [village-grid] :as _state}
   from-xy
   pred]
  (if-let [well-xy (some (fn [[xy thing]]
                           (when (pred thing) xy))
                         village-grid)]
    (path-finder/find-path village-grid
                           grid-size
                           from-xy
                           well-xy)))

(defonce state (atom {:toolbar-icons
                      {:village.factory/well   well-icon
                       :village.factory/dwell  dwell-icon
                       :village.factory/bakery bakery-icon}
                      ;; a map of {[x y] icon}
                      :village-grid {}
                      ;; a map of {[x y] villager}
                      :villagers    {}}))

(defonce cpt (atom 0))
(def conf {;; give time as an natural number. It should be
           ;; interpreted as number of ticks since game
           ;; started. A tick being the smallest unit of
           ;; time of the (discrete) simulation.
           :time-provider  q/frame-count
           :id-provider    #(swap! cpt inc)
           :path-finder    (partial find-path @state)
           ;; IRL durations must be applied to this ratio
           ;; 1 second IRL is that much ticks in the game.
           ;; If the simulation need to be "time consistant"
           ;; game-speed must be related to what time-provider
           ;; is based upon.
           ;; (game-speed 2) will give you the number of
           :seconds->ticks #(* % (/ 1 frame-rate) game-speed)})

(defn spawn-villager
  [state]
  (swap! state update :villagers conj [[0 0] (villager/spawn conf)]))

(defn setup
  []
  (q/frame-rate frame-rate)
  (apply q/background background-color)
  (spawn-villager state))



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

(defn cell-pos-at-mouse-pos
  []
  [(- (q/mouse-x) (mod (q/mouse-x) cell-size))
   (- (q/mouse-y) (mod (q/mouse-y) cell-size))])

(defn preview-icon-at-mouse
  [icon]
  (let [[x y] (cell-pos-at-mouse-pos)]
    (draw-icon-at icon x y (inverse-color icon-color))))

(defn draw-icon-at-mouse
  [icon]
  (let [[x y] (cell-pos-at-mouse-pos)]
    (draw-icon-at icon x y icon-color)))

(defn dragging-icon? [] (:dragging-icon @state))

(defn dragging-icon
  []
  (->> (:toolbar-icons @state)
       (filter (fn [[_ icon :as _kv]] (:dragging? icon)))
       (first)))

(defn grid-pos-at-mouse-pos
  []
  (map #(/ % cell-size) (cell-pos-at-mouse-pos)))

(defn move-mouse-icon
  []
  ;; user was dragging an icon and he released
  (when-let [[icon-name _] (and (not (q/mouse-pressed?))
                                (dragging-icon))]
    (do
      (swap! state
             (fn [state]
               (-> state
                   (update :toolbar-icons
                           u/map-vals #(assoc % :dragging? false))
                   (update :village-grid
                           assoc (grid-pos-at-mouse-pos) icon-name))))))

  ;; user started to drag an icon
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

  ;; user is dragging an icon
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
    (draw-icon-at icon (* cell-size x) (* cell-size y) icon-color)))

(defn draw-villager
  [villager x y]
  (draw-icon (str (:id villager)) x y other-color))

(defn draw-villagers
  []
  (doseq [[pos villager] (:villagers @state)]
    (draw-villager villager
                   (* (x pos) cell-size)
                   (* cell-size (y pos)))))

(defn debug-framerate
  []
  (q/with-fill [255 0 0]
    (q/text-align :left)
    (q/text (str (q/current-frame-rate) "fps|" (q/frame-count) "frames") 10 10)))





(defn update-villagers
  "update the state with updated villagers"
  []
  (swap! state update :villagers u/map-vals #(villager/update-villager % conf)))



(def nb-frames-to-walk-a-cell
  ;; nb of second to walk a cell IRL
  (* cell-m walking-speed-m-s
     ;; nb of frames to walk a cell IRL
     frame-rate
     ;; nb of frames to walk a cell in game
     game-speed))

(defn walk-villager
  []
  (doseq [[villager-xy {:keys [journey] :as villager}] (:villagers @state)
          :when (some? journey)
          :let [{:keys [started-frame path]} journey
                [current-xy next-xy & _] path
                current-frame (q/frame-count)]
          :when (>= (- current-frame started-frame) nb-frames-to-walk-a-cell)]
    (comment (assert (= current-xy villager-xy)))
    (swap! state
           (fn [state]
             (-> (update state :villagers dissoc villager-xy)
                 (update :villagers assoc next-xy
                         (assoc villager
                           :journey
                           (journey/step-traveled journey q/frame-count))))))))
(defn draw []
  (apply q/background background-color)
  (grid/make-grid canva-size :cell-size cell-size :grid-color grid-color)
  (debug-mouse)
  (debug-framerate)
  (draw-toolbar-icons)
  (draw-grid-icons)
  (mouse-handler)
  (draw-villagers)
  (update-villagers)
  #_(walk-villager))

(q/defsketch app
             :draw draw
             :host "app"
             :setup setup
             :draw draw
             :size canva-size)    