(ns the-village.engine.path-finder
  "no brainer path finding algorithm
  inspired from https://www.redblobgames.com/pathfinding/a-star/introduction.html")

(defn neighbors
  [[x y :as _point] [width height :as _grid-size]]

  ;; modulo is what makes edge traversal
  ;; up
  [[x (mod (dec y) height)]
   ;; down
   [x (mod (inc y) height)]
   ;; left
   [(mod (dec x) width) y]
   ;; right
   [(mod (inc x) width) y]])

(defn path-chunks
  [grid grid-size start goal]
  (loop [[current & others :as _frontier] [start]
         came-from {start nil}]
    (if (or (not current) (= current goal))
      came-from
      (let [current-neighbors (filter (every-pred
                                        ; not already visited
                                        #(not (contains? came-from %))
                                        ; not an obstacle
                                        #(or (not (contains? grid %))
                                             ; except if goal
                                             (= % goal)))
                                      (neighbors current grid-size))]
        (recur (reduce conj others current-neighbors)
               (merge came-from
                      (zipmap current-neighbors
                              (repeat current))))))))

(defn find-path
  "grid is a map where {[1 3] :thingy} means there is
  a thingy a coordinate (1,3) and you can't go that way
  except when your destination is that thingy
  start and goal are your planned journey
  It returns a vector of points that will plan your journey"
  [grid grid-size start goal]
  (let [came-from (path-chunks grid grid-size start goal)]
    (loop [[h & _ :as path] (list goal)]
      (cond
        (nil? h) nil
        (= start h) path
        :else (recur (conj path (get came-from h)))))))


