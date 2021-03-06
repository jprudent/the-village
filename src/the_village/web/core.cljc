(ns the-village.web.core
  (:require [quil.core :as q :include-macros true]
            [the-village.utils.missing :as u]
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

;; 1 frame / second
(def frame-rate 10)

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

(defrecord Villager
  [;; id : every villager are registered at an office once
   ;; they set foot in the village. Here a pleasant bureaucrat
   ;; will give him/her a number that identifies him/her
   ;; uniquely among the other. They are just numbers after
   ;; all !
   id
   ;; hydration level : is an float ranging from 0 to 1
   ;; when it reaches 0 this villager should die
   ;; (unless he is not human, so why would he be thirsty ?)
   ;; when it's 1, this human has quenched his thirst.
   hydration-level])


;; this villager is called Donald Trump because he is
;; likely to die a lot during the early stages of the
;; development of this game
(def donald-trump
  (map->Villager {:id 0 :hydration-level 0.2}))

(defonce state (atom {:toolbar-icons
                                    {:well   well-icon
                                     :dwell  dwell-icon
                                     :bakery bakery-icon}
                      ;; a map of {[x y] icon}
                      :village-grid {}
                      ;; a map of {[x y] villager}
                      :villagers    {[0 0] donald-trump}}))

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
       (filter (fn [[_ icon :as kv]] (:dragging? icon)))
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
    (q/text (str (q/current-frame-rate)) 10 10)))

;; If a villager die of dehydration in 48h
;; that means he needs to walk regularly to the well,
;; and bring back a full bucket of water to his house
;; (this last part is not part of the simulation)

(defn find-well-path
  [start]
  (if-let [well-xy (some (fn [[xy thing]]
                           (when (= :well thing) xy))
                         (:village-grid @state))]
    (path-finder/find-path (:village-grid @state)
                           grid-size
                           start
                           well-xy)))

(defrecord Journey
  [started-frame
   path])

(defn update-villagers
  []
  (doseq [[villager-xy {:keys [hydration-level journey]}] (:villagers @state)
          :when (not journey)]
    ;; hydration level is too low so go to the well
    ;; (and there is nothing else to do in this shit hole)
    (when-let [well-path (and (< 0 hydration-level 0.8)
                              (find-well-path villager-xy))]
      (do (prn "thirsty" well-path)
          (swap! state assoc-in
                 [:villagers villager-xy :journey]
                 (map->Journey {:started-frame (q/frame-count)
                                :path          well-path}))))))

;; IRL a person can easily walk 4km per hour
(def walking-speed-m-s (/ 3600 4000))

;; 1 second in the game is that much seconds IRL
(def game-speed 0.01)

;; A cell should be more like 15 meters squared.
(def cell-m 15)

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
                [current-xy next-xy & _] path]]
    (comment (assert (= current-xy villager-xy)))
    (when (>= (- (q/frame-count) started-frame)
           nb-frames-to-walk-a-cell)
      (swap! state
             (fn [state]
               (-> (update state :villagers dissoc villager-xy)
                   (update :villagers assoc next-xy
                           (assoc villager :journey (map->Journey {:started-frame (q/frame-count)
                                                                   :path          (rest path)})))))))))
(defn draw []
  (apply q/background background-color)
  (make-grid canva-size)                
  (debug-mouse)
  (draw-toolbar-icons)
  (draw-grid-icons)
  (draw-villagers)
  (mouse-handler)
  (update-villagers)
  (walk-villager)
  (debug-framerate))

(q/defsketch app
             :draw draw
             :host "app"
             :setup setup
             :draw draw
             :size canva-size)    